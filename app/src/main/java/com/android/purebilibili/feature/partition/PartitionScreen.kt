// 文件路径: feature/partition/PartitionScreen.kt
package com.android.purebilibili.feature.partition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.CutePersonLoadingIndicator
import com.android.purebilibili.core.ui.globalWallpaperAwareBackground
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.resolveEffectiveLiquidGlassEnabled
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
import com.android.purebilibili.feature.home.components.LiquidGlassTuning
import com.android.purebilibili.feature.home.components.resolveLiquidGlassTuning
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.blur.unifiedBlur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 *  分区数据类
 */
data class PartitionCategory(
    val id: Int,
    val name: String,
    val emoji: String,
    val color: Color
)

/**
 *  所有分区列表 (参考官方 Bilibili API)
 * tid 是 Bilibili 官方的分区 ID，用于 x/web-interface/newlist 接口
 * 注意：番剧/国创/电影/电视剧/纪录片是特殊分区，使用不同的 API
 */
val allPartitions = listOf(
    // === 视频分区（支持 newlist API）===
    PartitionCategory(1, "动画", "🎬", Color(0xFF7BBEEC)),
    PartitionCategory(13, "番剧", "📺", Color(0xFFFF6B9D)),      // 特殊分区
    PartitionCategory(167, "国创", "🇨🇳", Color(0xFFFF7575)),     // 特殊分区
    PartitionCategory(3, "音乐", "🎵", Color(0xFF6BB5FF)),
    PartitionCategory(129, "舞蹈", "💃", Color(0xFFFF7777)),
    PartitionCategory(4, "游戏", "🎮", Color(0xFF7FD37F)),
    PartitionCategory(36, "知识", "📚", Color(0xFFFFD166)),
    PartitionCategory(188, "科技", "💻", Color(0xFF6ECFFF)),
    PartitionCategory(234, "运动", "⚽", Color(0xFF7BC96F)),
    PartitionCategory(223, "汽车", "🚗", Color(0xFF74C0FC)),
    PartitionCategory(160, "生活", "🏠", Color(0xFFFFB366)),
    PartitionCategory(211, "美食", "🍜", Color(0xFFFFAB5C)),
    PartitionCategory(217, "动物圈", "🐾", Color(0xFFB5D9A8)),
    PartitionCategory(119, "鬼畜", "👻", Color(0xFFA8E6CF)),
    PartitionCategory(155, "时尚", "👗", Color(0xFFFF9ECD)),
    PartitionCategory(202, "资讯", "📰", Color(0xFF98D8C8)),
    PartitionCategory(5, "娱乐", "🎪", Color(0xFFFFB347)),
    // === 特殊分区（番剧/电影等使用不同 API）===
    PartitionCategory(23, "电影", "🎬", Color(0xFFFF9E7A)),      // 特殊分区
    PartitionCategory(11, "电视剧", "📺", Color(0xFFFF85A2)),    // 特殊分区
    PartitionCategory(177, "纪录片", "🎥", Color(0xFF7BC8F6)),   // 特殊分区
    PartitionCategory(181, "影视", "🎦", Color(0xFFC7A4FF))      // 特殊分区
)

private val partitionTabs = listOf(
    PartitionCategory(0, "全站", "⌂", Color(0xFFFFA15F))
) + allPartitions

data class PartitionFeedUiState(
    val selectedPartition: PartitionCategory = partitionTabs.first(),
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class PartitionFeedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PartitionFeedUiState())
    val uiState = _uiState.asStateFlow()

    private var currentPage = 1
    private var hasMore = true
    private var requestGeneration = 0

    init {
        loadSelectedPartition(reset = true)
    }

    fun selectPartition(partition: PartitionCategory) {
        if (_uiState.value.selectedPartition.id == partition.id) return
        _uiState.update {
            it.copy(
                selectedPartition = partition,
                videos = emptyList(),
                error = null
            )
        }
        loadSelectedPartition(reset = true)
    }

    fun loadMore() {
        loadSelectedPartition(reset = false)
    }

    private fun loadSelectedPartition(reset: Boolean) {
        if (_uiState.value.isLoading && !reset) return
        if (!reset && !hasMore) return

        if (reset) {
            currentPage = 1
            hasMore = true
            requestGeneration++
        }
        val generation = requestGeneration
        val partition = _uiState.value.selectedPartition

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (partition.id == 0) {
                VideoRepository.getPopularVideos(page = currentPage)
            } else {
                VideoRepository.getRegionVideos(tid = partition.id, page = currentPage)
            }
            if (generation != requestGeneration) return@launch

            result
                .onSuccess { newVideos ->
                    hasMore = newVideos.isNotEmpty()
                    _uiState.update { state ->
                        state.copy(
                            videos = if (reset) newVideos else state.videos + newVideos,
                            isLoading = false
                        )
                    }
                    if (newVideos.isNotEmpty()) currentPage++
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                }
        }
    }
}

/**
 *  分区页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionScreen(
    onBack: () -> Unit,
    onVideoClick: (String, Long, String) -> Unit = { _, _, _ -> }
) {
    val hazeState = com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    AdaptiveScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AdaptiveTopAppBar(
                title = "分区",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.unifiedBlur(
                    hazeState = hazeState
                )
            )
        }
    ) { paddingValues ->
        PartitionContent(
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            hazeState = hazeState,
            onVideoClick = { video -> onVideoClick(video.bvid, video.cid, video.pic) }
        )
    }
}

/**
 * 分区主体内容。独立页面和首页内嵌分区页共用，避免两套分区网格状态分叉。
 */
@Composable
fun PartitionContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        top = 8.dp,
        bottom = 16.dp,
        start = 16.dp,
        end = 16.dp
    ),
    hazeState: HazeState? = null,
    onVideoClick: (VideoItem) -> Unit = {},
    viewModel: PartitionFeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiPreset = LocalUiPreset.current
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(initial = HomeSettings())
    val liquidGlassIndicatorEnabled = remember(
        homeSettings.isBottomBarLiquidGlassEnabled,
        homeSettings.androidNativeLiquidGlassEnabled,
        uiPreset
    ) {
        resolveEffectiveLiquidGlassEnabled(
            requestedEnabled = homeSettings.isBottomBarLiquidGlassEnabled,
            uiPreset = uiPreset,
            androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
        )
    }
    val liquidGlassTuning = remember(
        homeSettings.liquidGlassMode,
        homeSettings.liquidGlassStrength
    ) {
        resolveLiquidGlassTuning(
            mode = homeSettings.liquidGlassMode,
            strength = homeSettings.liquidGlassStrength
        )
    }
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = contentPadding.calculateStartPadding(layoutDirection)
    val endPadding = contentPadding.calculateEndPadding(layoutDirection)
    val topPadding = contentPadding.calculateTopPadding()
    val bottomPadding = contentPadding.calculateBottomPadding()

    val shouldLoadMore by remember(state.videos.size, state.isLoading) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisibleIndex != null && lastVisibleIndex >= state.videos.lastIndex - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoading && state.videos.isNotEmpty()) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .globalWallpaperAwareBackground()
            .responsiveContentWidth(maxWidth = 1000.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState)
                    } else {
                        Modifier
                    }
                )
        ) {
            PartitionSideRail(
                partitions = partitionTabs,
                selectedId = state.selectedPartition.id,
                modifier = Modifier.width(92.dp),
                contentPadding = PaddingValues(
                    start = startPadding,
                    top = topPadding + 8.dp,
                    bottom = bottomPadding,
                    end = 4.dp
                ),
                liquidGlassIndicatorEnabled = liquidGlassIndicatorEnabled,
                liquidGlassTuning = liquidGlassTuning,
                onPartitionSelected = viewModel::selectPartition
            )

            PartitionVideoList(
                state = state,
                listState = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = topPadding + 8.dp,
                    end = endPadding,
                    bottom = bottomPadding
                ),
                onVideoClick = onVideoClick
            )
        }
    }
}

@Composable
private fun PartitionSideRail(
    partitions: List<PartitionCategory>,
    selectedId: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    liquidGlassIndicatorEnabled: Boolean,
    liquidGlassTuning: LiquidGlassTuning,
    onPartitionSelected: (PartitionCategory) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = partitions,
            key = { _, partition -> partition.id }
        ) { _, partition ->
            PartitionSideRailItem(
                partition = partition,
                selected = partition.id == selectedId,
                liquidGlassIndicatorEnabled = liquidGlassIndicatorEnabled,
                liquidGlassTuning = liquidGlassTuning,
                onClick = { onPartitionSelected(partition) }
            )
        }
    }
}

@Composable
private fun PartitionSideRailItem(
    partition: PartitionCategory,
    selected: Boolean,
    liquidGlassIndicatorEnabled: Boolean,
    liquidGlassTuning: LiquidGlassTuning,
    onClick: () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val shape = AppShapes.container(ContainerLevel.Pill)
    val useLiquidGlassIndicator = selected && liquidGlassIndicatorEnabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(
                if (useLiquidGlassIndicator) {
                    Modifier.partitionLiquidGlassIndicator(
                        baseColor = selectedColor,
                        highlightColor = MaterialTheme.colorScheme.onPrimary,
                        tuning = liquidGlassTuning,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useLiquidGlassIndicator) {
            Spacer(modifier = Modifier.width(14.dp))
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(AppShapes.container(ContainerLevel.Pill))
                    .background(if (selected) selectedColor else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = partition.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Modifier.partitionLiquidGlassIndicator(
    baseColor: Color,
    highlightColor: Color,
    tuning: LiquidGlassTuning,
    shape: Shape
): Modifier {
    val progress = tuning.progress.coerceIn(0f, 1f)
    val baseAlpha = (tuning.surfaceAlpha + tuning.indicatorTintAlpha * 0.45f).coerceIn(0.18f, 0.52f)
    val highlightAlpha = (0.08f + tuning.whiteOverlayAlpha + tuning.indicatorLensBoost * 0.018f).coerceIn(0.08f, 0.22f)
    val rimAlpha = (0.18f + progress * 0.16f).coerceIn(0.18f, 0.34f)

    return this
        .background(baseColor.copy(alpha = baseAlpha), shape)
        .drawBehind {
            val radius = size.height / 2f
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = highlightAlpha),
                        baseColor.copy(alpha = 0.02f)
                    ),
                    center = Offset(size.width * 0.32f, size.height * 0.18f),
                    radius = size.width * 0.78f
                ),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = highlightAlpha * 0.72f),
                        Color.Transparent,
                        baseColor.copy(alpha = 0.08f + progress * 0.06f)
                    )
                ),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        .border(
            width = 0.7.dp,
            color = highlightColor.copy(alpha = rimAlpha),
            shape = shape
        )
}

@Composable
private fun PartitionVideoList(
    state: PartitionFeedUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    onVideoClick: (VideoItem) -> Unit
) {
    when {
        state.videos.isEmpty() && state.isLoading -> {
            Box(modifier = modifier.fillMaxHeight()) {
                CutePersonLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        state.videos.isEmpty() && state.error != null -> {
            Box(modifier = modifier.fillMaxHeight()) {
                Text(
                    text = state.error,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxHeight(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(
                    items = state.videos,
                    key = { index, video ->
                        resolveIndexedVideoLazyKey(
                            namespace = "partition_feed",
                            index = index,
                            bvid = video.bvid,
                            id = video.id,
                            aid = video.aid,
                            cid = video.cid
                        )
                    }
                ) { _, video ->
                    PartitionVideoRow(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }

                if (state.isLoading) {
                    item(key = "partition_loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CutePersonLoadingIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 *  分区视频条目
 */
@Composable
private fun PartitionVideoRow(
    video: VideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(146.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(AppSurfaceTokens.cardContainer())
        ) {
            AsyncImage(
                model = FormatUtils.resolveVideoCoverUrl(video.pic, useLowQuality = true),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (video.duration > 0) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(AppShapes.container(ContainerLevel.Pill))
                        .background(Color.Black.copy(alpha = 0.56f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 82.dp)
                .padding(vertical = 2.dp)
        ) {
            Text(
                text = video.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildPartitionVideoMeta(video),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildPartitionVideoMeta(video: VideoItem): String {
    val publishTime = FormatUtils.formatPublishTime(video.pubdate)
    val ownerName = video.owner.name.ifBlank { video.tname }
    val primaryStat = video.stat.view.takeIf { it > 0 }?.let { "播放 ${FormatUtils.formatStat(it.toLong())}" }
    val secondaryStat = video.stat.danmaku.takeIf { it > 0 }?.let { "弹幕 ${FormatUtils.formatStat(it.toLong())}" }
    return listOf(publishTime, ownerName, primaryStat, secondaryStat)
        .filter { !it.isNullOrBlank() }
        .joinToString("  ")
}
