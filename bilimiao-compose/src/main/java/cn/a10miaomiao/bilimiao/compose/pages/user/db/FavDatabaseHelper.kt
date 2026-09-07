package cn.a10miaomiao.bilimiao.compose.pages.user.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.a10miaomiao.bilimiao.comm.entity.media.MediasInfo

class FavDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION
) {

    companion object {
        const val DB_NAME = "fav_organizer.db"
        const val DB_VERSION = 1

        const val TABLE_FOLDER = "fav_folder"
        const val TABLE_VIDEO = "fav_video"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_FOLDER (
                id TEXT PRIMARY KEY,
                title TEXT,
                media_count INTEGER,
                attr INTEGER,
                mid INTEGER,
                sync_time INTEGER
            )
        """)
        db.execSQL("""
            CREATE TABLE $TABLE_VIDEO (
                aid TEXT NOT NULL,
                fav_id TEXT NOT NULL,
                title TEXT,
                cover TEXT,
                duration INTEGER,
                ctime INTEGER,
                fav_time INTEGER,
                attr INTEGER,
                up_mid INTEGER,
                up_name TEXT,
                PRIMARY KEY (aid, fav_id)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VIDEO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLDER")
        onCreate(db)
    }

    fun clearAll() {
        writableDatabase.run {
            delete(TABLE_VIDEO, null, null)
            delete(TABLE_FOLDER, null, null)
        }
    }

    fun insertFolder(id: String, title: String, mediaCount: Int, attr: Int, mid: Long) {
        val cv = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("media_count", mediaCount)
            put("attr", attr)
            put("mid", mid)
            put("sync_time", System.currentTimeMillis() / 1000)
        }
        writableDatabase.insertWithOnConflict(TABLE_FOLDER, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun insertVideos(favId: String, videos: List<MediasInfo>) {
        if (videos.isEmpty()) return
        writableDatabase.run {
            beginTransaction()
            try {
                videos.forEach { v ->
                    val cv = ContentValues().apply {
                        put("aid", v.id)
                        put("fav_id", favId)
                        put("title", v.title)
                        put("cover", v.cover)
                        put("duration", v.duration)
                        put("ctime", v.ctime)
                        put("fav_time", v.fav_time)
                        put("attr", v.attr)
                        put("up_mid", v.upper.mid)
                        put("up_name", v.upper.name)
                    }
                    insertWithOnConflict(TABLE_VIDEO, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    fun getTotalFolderCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_FOLDER", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun getTotalVideoCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_VIDEO", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun getDeadVideoCount(): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_VIDEO WHERE attr != 0",
            null
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun getDuplicateGroupCount(): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM (SELECT aid FROM $TABLE_VIDEO GROUP BY aid HAVING COUNT(*) > 1)",
            null
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun getSleepingVideoCount(monthsThreshold: Int = 6): Int {
        val thresholdSec = System.currentTimeMillis() / 1000 - monthsThreshold * 30L * 24 * 3600
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_VIDEO WHERE fav_time > 0 AND fav_time < ?",
            arrayOf(thresholdSec.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    data class OverloadedFolder(
        val id: String,
        val title: String,
        val count: Int,
    )

    fun getOverloadedFolders(threshold: Int = 100): List<OverloadedFolder> {
        val result = mutableListOf<OverloadedFolder>()
        readableDatabase.rawQuery(
            "SELECT f.id, f.title, COUNT(v.aid) as cnt FROM $TABLE_FOLDER f " +
            "INNER JOIN $TABLE_VIDEO v ON f.id = v.fav_id " +
            "GROUP BY f.id HAVING cnt >= ? ORDER BY cnt DESC",
            arrayOf(threshold.toString())
        ).use { c ->
            while (c.moveToNext()) {
                result.add(OverloadedFolder(
                    id = c.getString(0),
                    title = c.getString(1),
                    count = c.getInt(2),
                ))
            }
        }
        return result
    }
}
