package cn.a10miaomiao.bilimiao.compose.components.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.a10miaomiao.bilimiao.comm.entity.video.VideoStatRatioInfo

private fun formatPercent(ratio: Float): String {
    val v = ratio * 100f
    return when {
        v >= 100f -> "99.99%"
        v < 0.01f && v > 0f -> "<0.01%"
        else -> String.format("%.2f%%", v)
    }
}

/**
 * 在视频缩略图上叠加显示 点赞率 / 投币率 的小徽章。
 *
 * 使用：
 * ```
 * Box(...) {
 *     GlideImage(...)
 *     VideoStatRatioBadge(ratio, Modifier.align(Alignment.TopStart))
 * }
 * ```
 */
@Composable
fun VideoStatRatioBadge(
    info: VideoStatRatioInfo,
    modifier: Modifier = Modifier,
) {
    val isGood = info.isGood
    val bgColor = if (isGood) Color(0xE6FB3A5E) else Color(0xCC404858)
    val likeColor = if (info.likePass) Color(0xFFFFD9E1) else Color(0xFFD0D5DD)
    val coinColor = if (info.coinPass) Color(0xFFFFD9E1) else Color(0xFFD0D5DD)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics {
                contentDescription = "点赞率：${formatPercent(info.likeRatio)}，" +
                    "投币率：${formatPercent(info.coinRatio)}" +
                    if (isGood) "，优质视频" else ""
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "👍",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatPercent(info.likeRatio),
            color = likeColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "🪙",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatPercent(info.coinRatio),
            color = coinColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** 加载中占位徽章 */
@Composable
fun VideoStatRatioBadgeLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x88000000))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "分析中…",
            color = Color(0xFFEAEAEA),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
