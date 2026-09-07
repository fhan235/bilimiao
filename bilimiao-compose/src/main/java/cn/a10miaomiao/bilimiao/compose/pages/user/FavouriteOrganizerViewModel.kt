package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.a10miaomiao.bilimiao.compose.pages.user.db.FavDatabaseHelper
import com.a10miaomiao.bilimiao.comm.entity.ListAndCountInfo
import com.a10miaomiao.bilimiao.comm.entity.ResponseData
import com.a10miaomiao.bilimiao.comm.entity.media.MediaDetailInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediaListInfo
import com.a10miaomiao.bilimiao.comm.entity.media.MediasInfo
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.json
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.string
import com.a10miaomiao.bilimiao.comm.miao.MiaoJson
import com.a10miaomiao.bilimiao.comm.store.UserStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

data class SyncProgress(
    val message: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val isComplete: Boolean = false,
)

internal class FavouriteOrganizerViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val fragment by instance<Fragment>()
    private val userStore: UserStore by instance()

    private var dbHelper: FavDatabaseHelper? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress

    private val _totalFolders = MutableStateFlow(0)
    val totalFolders: StateFlow<Int> = _totalFolders

    private val _totalVideos = MutableStateFlow(0)
    val totalVideos: StateFlow<Int> = _totalVideos

    private val _deadVideoCount = MutableStateFlow(0)
    val deadVideoCount: StateFlow<Int> = _deadVideoCount

    private val _duplicateGroupCount = MutableStateFlow(0)
    val duplicateGroupCount: StateFlow<Int> = _duplicateGroupCount

    private val _sleepingVideoCount = MutableStateFlow(0)
    val sleepingVideoCount: StateFlow<Int> = _sleepingVideoCount

    private val _overloadedFolders = MutableStateFlow<List<FavDatabaseHelper.OverloadedFolder>>(emptyList())
    val overloadedFolders: StateFlow<List<FavDatabaseHelper.OverloadedFolder>> = _overloadedFolders

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    private val _hasLocalData = MutableStateFlow(false)
    val hasLocalData: StateFlow<Boolean> = _hasLocalData

    init {
        checkLocalData()
    }

    private fun checkLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = getDb()
            _hasLocalData.value = db.getTotalFolderCount() > 0
        }
    }

    private fun getDb(): FavDatabaseHelper {
        return dbHelper ?: FavDatabaseHelper(fragment.requireContext()).also { dbHelper = it }
    }

    fun startSync() {
        if (_isSyncing.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _syncError.value = null

            try {
                val mid = userStore.state.info?.mid?.toString()
                if (mid.isNullOrBlank()) {
                    _syncError.value = "请先登录"
                    return@launch
                }

                val db = getDb()
                db.clearAll()
                _hasLocalData.value = false

                // --- 第一步：获取所有自建收藏夹 ---
                _syncProgress.value = SyncProgress(message = "正在获取收藏夹列表...")

                val allFolders = mutableListOf<MediaListInfo>()
                var pageNum = 1
                while (true) {
                    val rawText = BiliApiService.userApi.favCreatedList(
                        upMid = mid,
                        pageNum = pageNum,
                        pageSize = 20
                    ).awaitCall().string()
                    val res = parseJsonOrFail<ResponseData<ListAndCountInfo<MediaListInfo>>>(
                        rawText, "获取收藏夹列表"
                    )
                    if (!res.isSuccess) {
                        _syncError.value = "获取收藏夹列表失败: ${res.message}"
                        return@launch
                    }
                    val result = res.requireData()
                    allFolders.addAll(result.list)
                    if (!result.has_more) break
                    pageNum++
                }

                for (folder in allFolders) {
                    db.insertFolder(
                        id = folder.id,
                        title = folder.title,
                        mediaCount = folder.media_count,
                        attr = folder.attr,
                        mid = folder.mid,
                    )
                }
                _totalFolders.value = allFolders.size

                // --- 第二步：逐个收藏夹拉取视频 ---
                var totalVidCount = 0
                allFolders.forEachIndexed { index, folder ->
                    _syncProgress.value = SyncProgress(
                        message = "同步: ${folder.title}",
                        current = index + 1,
                        total = allFolders.size,
                    )

                    var vidPageNum = 1
                    while (true) {
                        val vidRawText = BiliApiService.userApi.mediaDetail(
                            media_id = folder.id,
                            pageNum = vidPageNum,
                            pageSize = 20
                        ).awaitCall().string()
                        val vidRes = try {
                            parseJsonOrFail<ResponseData<MediaDetailInfo>>(
                                vidRawText, "收藏夹[${folder.title}]视频列表"
                            )
                        } catch (e: Exception) {
                            // 单个收藏夹拉失败时跳过该收藏夹，不中断整体流程
                            e.printStackTrace()
                            break
                        }
                        if (!vidRes.isSuccess) break
                        val vidResult = vidRes.requireData()
                        val videos = vidResult.medias ?: emptyList()
                        db.insertVideos(folder.id, videos)
                        totalVidCount += videos.size
                        _totalVideos.value = totalVidCount
                        _deadVideoCount.value = db.getDeadVideoCount()
                        if (!vidResult.has_more) break
                        vidPageNum++
                    }
                }

                // --- 第三步：汇总分析 ---
                _syncProgress.value = SyncProgress(message = "正在分析数据...")
                _duplicateGroupCount.value = db.getDuplicateGroupCount()
                _sleepingVideoCount.value = db.getSleepingVideoCount()
                _overloadedFolders.value = db.getOverloadedFolders()

                _syncProgress.value = SyncProgress(isComplete = true)
                _hasLocalData.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _syncError.value = "同步失败: ${e.message ?: e.toString()}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * 把响应体字符串解析为目标类型；如果解析失败（比如服务端返回了 HTML 登录页/风控页），
     * 抛出一个带响应预览的异常，便于在 UI 上看到具体原因，而不是直接崩溃。
     */
    private inline fun <reified T> parseJsonOrFail(rawText: String, scene: String): T {
        return try {
            MiaoJson.fromJson(rawText)
        } catch (e: Exception) {
            val preview = rawText.take(200).replace("\n", " ")
            throw IllegalStateException("[$scene] 服务器返回非 JSON 内容: $preview", e)
        }
    }
}
