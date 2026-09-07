package cn.a10miaomiao.bilimiao.compose.pages.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilimiao.compose.base.ComposePage
import cn.a10miaomiao.bilimiao.compose.common.diViewModel
import cn.a10miaomiao.bilimiao.compose.common.localPageNavigation
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
class FavouriteOrganizerPage : ComposePage() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: FavouriteOrganizerViewModel = diViewModel()
        val pageNavigation = localPageNavigation()

        val isSyncing by viewModel.isSyncing.collectAsState()
        val syncProgress by viewModel.syncProgress.collectAsState()
        val syncError by viewModel.syncError.collectAsState()
        val hasLocalData by viewModel.hasLocalData.collectAsState()

        val totalFolders by viewModel.totalFolders.collectAsState()
        val totalVideos by viewModel.totalVideos.collectAsState()
        val deadVideoCount by viewModel.deadVideoCount.collectAsState()
        val duplicateGroupCount by viewModel.duplicateGroupCount.collectAsState()
        val sleepingVideoCount by viewModel.sleepingVideoCount.collectAsState()
        val overloadedFolders by viewModel.overloadedFolders.collectAsState()

        val scope = rememberCoroutineScope()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "收藏夹体检报告",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { pageNavigation.popBackStack() }) {
                            Text(
                                text = "← 返回",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 同步状态区域
                SyncStatusCard(
                    isSyncing = isSyncing,
                    syncProgress = syncProgress,
                    syncError = syncError,
                    hasData = hasLocalData,
                    onStartSync = {
                        scope.launch { viewModel.startSync() }
                    },
                )

                // 体检报告主体
                AnimatedVisibility(
                    visible = hasLocalData,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 总览卡片
                        HealthSummaryCard(
                            totalFolders = totalFolders,
                            totalVideos = totalVideos,
                        )

                        // 失效视频
                        StatCard(
                            emoji = "⚠️",
                            title = "失效视频",
                            value = "$deadVideoCount 个",
                            subtitle = "已失效/被删除/转为私密的视频",
                        )

                        // 跨夹重复
                        StatCard(
                            emoji = "🔁",
                            title = "跨夹重复",
                            value = "$duplicateGroupCount 组",
                            subtitle = "同一视频出现在多个收藏夹",
                        )

                        // 沉睡视频
                        StatCard(
                            emoji = "💤",
                            title = "沉睡视频",
                            value = "$sleepingVideoCount 个",
                            subtitle = "收藏超过 6 个月、大概率没再看过",
                        )

                        // 单夹超载
                        if (overloadedFolders.isNotEmpty()) {
                            OverloadedCard(folders = overloadedFolders)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    isSyncing: Boolean,
    syncProgress: SyncProgress,
    syncError: String?,
    hasData: Boolean,
    onStartSync: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isSyncing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = syncProgress.message.ifEmpty { "正在同步..." },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (syncProgress.total > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { syncProgress.current.toFloat() / syncProgress.total },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = "${syncProgress.current} / ${syncProgress.total}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (syncError != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "同步失败",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = syncError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(
                    onClick = onStartSync,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("重试")
                }
            } else {
                Text(
                    text = if (hasData) "上次同步完成" else "尚未同步数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onStartSync,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing,
                ) {
                    Text(if (hasData) "重新同步" else "开始同步")
                }
            }
        }
    }
}

@Composable
private fun HealthSummaryCard(
    totalFolders: Int,
    totalVideos: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryItem(
                emoji = "📊",
                label = "收藏夹",
                value = totalFolders.toString(),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .align(Alignment.CenterVertically)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            SummaryItem(
                emoji = "🎬",
                label = "视频",
                value = totalVideos.toString(),
            )
        }
    }
}

@Composable
private fun SummaryItem(
    emoji: String,
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(
    emoji: String,
    title: String,
    value: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun OverloadedCard(
    folders: List<cn.a10miaomiao.bilimiao.compose.pages.user.db.FavDatabaseHelper.OverloadedFolder>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📦",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "单夹超载",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "超过 100 个视频的收藏夹",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${folders.size} 个",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            folders.forEach { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = folder.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${folder.count} 个",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
