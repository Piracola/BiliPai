// 文件路径: feature/video/screen/VideoContentSection.kt
package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.common.copyOnLongPress
import com.android.purebilibili.core.util.ShareUtils
import com.android.purebilibili.core.ui.rememberAppCommentIcon
import com.android.purebilibili.core.ui.rememberAppChevronUpIcon
import com.android.purebilibili.core.ui.rememberAppPlayIcon
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import com.android.purebilibili.core.ui.resolveCompactCapsuleChromeSpec
import com.android.purebilibili.core.ui.performance.TrackJankStateFlag
import com.android.purebilibili.core.ui.performance.TrackScrollJank
import com.android.purebilibili.core.store.DanmakuSettings
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.VideoTag
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.video.ui.section.VideoTitleWithDesc
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.section.resolveDisplayBgmList
import com.android.purebilibili.feature.video.ui.section.shouldShowAiSummaryEntry
import com.android.purebilibili.feature.video.ui.section.resolveVideoDetailMotionBudget
import com.android.purebilibili.feature.video.ui.section.shouldAnimateVideoDetailLayout
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsPanel
import com.android.purebilibili.feature.video.ui.components.RelatedVideoGridRow
import com.android.purebilibili.feature.video.ui.components.chunkRelatedVideosForHomeStyleGrid
import com.android.purebilibili.feature.video.ui.components.filterRelatedVideosByHiddenBvids
import com.android.purebilibili.feature.video.ui.components.CollectionRow
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
import com.android.purebilibili.feature.video.ui.components.CommentSortFilterBar
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.ui.components.UpPreviewSheet
import com.android.purebilibili.feature.video.ui.components.rememberVideoCommentAppearance
import com.android.purebilibili.feature.video.ui.components.resolveReplyItemContentType
import com.android.purebilibili.feature.video.ui.components.shouldShowReplyTopAction
import com.android.purebilibili.feature.video.ui.components.shouldShowVideoCommentBackToTop
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.dynamic.components.ImagePreviewTextContent
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import com.android.purebilibili.data.model.response.AiSummaryData
import com.android.purebilibili.feature.video.ui.section.AiSummaryCard
import com.android.purebilibili.feature.video.ui.section.AiSummaryPromptCard
import com.android.purebilibili.feature.video.ui.section.VideoNoteCard
import com.android.purebilibili.feature.video.ui.section.VideoNoteDeleteConfirmDialog
import com.android.purebilibili.feature.video.ui.section.VideoNoteEditorSheet
import com.android.purebilibili.feature.video.note.VideoNoteEditorDocument
import com.android.purebilibili.feature.video.note.VideoNoteUiState
import com.android.purebilibili.feature.video.note.buildVideoNoteShareText
import com.android.purebilibili.feature.video.note.shouldShowVideoNoteCard
import kotlin.math.abs
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal fun shouldShowDanmakuSendInput(isPlayerCollapsed: Boolean): Boolean = !isPlayerCollapsed

internal data class VideoContentTabBarLayoutSpec(
    val tabsRowWeight: Float,
    val tabsRowScrollable: Boolean,
    val containerHorizontalPaddingDp: Int,
    val tabHorizontalPaddingDp: Int,
    val tabVerticalPaddingDp: Int,
    val tabSpacingDp: Int,
    val selectedTabFontSizeSp: Int,
    val unselectedTabFontSizeSp: Int,
    val indicatorWidthDp: Int,
    val segmentedControlHeightDp: Int,
    val segmentedControlIndicatorHeightDp: Int
)

internal fun hasVideoContentTabBarIndicatorScaleClearance(
    containerHeightDp: Int,
    indicatorHeightDp: Int
): Boolean {
    val bottomBarScale = 78f / 56f
    return containerHeightDp >= indicatorHeightDp * bottomBarScale + 2f
}

internal const val VIDEO_CONTENT_LIQUID_DOCK_HEIGHT_DP = 40
internal const val VIDEO_CONTENT_LIQUID_DOCK_INDICATOR_HEIGHT_DP = 27
internal const val VIDEO_CONTENT_LIQUID_DOCK_LABEL_FONT_SIZE_SP = 14

internal data class VideoContentTabBarLiquidChromeSpec(
    val reusesLiquidGlassDock: Boolean,
    val segmentedControlHeightDp: Int,
    val segmentedControlIndicatorHeightDp: Int,
    val labelFontSizeSp: Int,
    val liquidGlassEffectsEnabled: Boolean,
    val useTransparentTabRowBackground: Boolean,
)

internal fun shouldReuseVideoContentTabBarLiquidGlassDock(
    androidNativeLiquidGlassEnabled: Boolean,
    hasBackdrop: Boolean,
): Boolean = androidNativeLiquidGlassEnabled && hasBackdrop

internal fun resolveVideoContentTabBarLiquidChromeSpec(
    androidNativeLiquidGlassEnabled: Boolean,
    hasBackdrop: Boolean,
    layoutSpec: VideoContentTabBarLayoutSpec,
): VideoContentTabBarLiquidChromeSpec {
    val reusesLiquidGlassDock = shouldReuseVideoContentTabBarLiquidGlassDock(
        androidNativeLiquidGlassEnabled = androidNativeLiquidGlassEnabled,
        hasBackdrop = hasBackdrop,
    )
    return if (reusesLiquidGlassDock) {
        VideoContentTabBarLiquidChromeSpec(
            reusesLiquidGlassDock = true,
            segmentedControlHeightDp = VIDEO_CONTENT_LIQUID_DOCK_HEIGHT_DP,
            segmentedControlIndicatorHeightDp = VIDEO_CONTENT_LIQUID_DOCK_INDICATOR_HEIGHT_DP,
            labelFontSizeSp = VIDEO_CONTENT_LIQUID_DOCK_LABEL_FONT_SIZE_SP,
            liquidGlassEffectsEnabled = true,
            useTransparentTabRowBackground = true,
        )
    } else {
        VideoContentTabBarLiquidChromeSpec(
            reusesLiquidGlassDock = false,
            segmentedControlHeightDp = layoutSpec.segmentedControlHeightDp,
            segmentedControlIndicatorHeightDp = layoutSpec.segmentedControlIndicatorHeightDp,
            labelFontSizeSp = layoutSpec.unselectedTabFontSizeSp,
            liquidGlassEffectsEnabled = hasBackdrop,
            useTransparentTabRowBackground = false,
        )
    }
}

internal fun resolveVideoContentTabBarLayoutSpec(widthDp: Int): VideoContentTabBarLayoutSpec {
    return if (widthDp < 400) {
        VideoContentTabBarLayoutSpec(
            tabsRowWeight = 1f,
            tabsRowScrollable = true,
            containerHorizontalPaddingDp = 8,
            tabHorizontalPaddingDp = 8,
            tabVerticalPaddingDp = 7,
            tabSpacingDp = 10,
            selectedTabFontSizeSp = 16,
            unselectedTabFontSizeSp = 15,
            indicatorWidthDp = 28,
            segmentedControlHeightDp = 40,
            segmentedControlIndicatorHeightDp = 27
        )
    } else {
        VideoContentTabBarLayoutSpec(
            tabsRowWeight = 1f,
            tabsRowScrollable = true,
            containerHorizontalPaddingDp = 12,
            tabHorizontalPaddingDp = 12,
            tabVerticalPaddingDp = 8,
            tabSpacingDp = 16,
            selectedTabFontSizeSp = 17,
            unselectedTabFontSizeSp = 16,
            indicatorWidthDp = 32,
            segmentedControlHeightDp = 40,
            segmentedControlIndicatorHeightDp = 27
        )
    }
}

internal data class VideoContentTabBarDanmakuActionLayoutPolicy(
    val toggleIconSizeDp: Int,
    val toggleHorizontalPaddingDp: Int,
    val toggleVerticalPaddingDp: Int,
    val toggleTextSizeSp: Int,
    val toggleTrailingPaddingDp: Int,
    val sendHorizontalPaddingDp: Int,
    val sendVerticalPaddingDp: Int,
    val sendTextSizeSp: Int,
    val sendLabel: String,
    val secondaryControlHeightDp: Int,
    val secondaryControlCornerRadiusDp: Int,
    val settingsButtonSizeDp: Int,
    val settingsIconSizeDp: Int,
    val settingsLeadingPaddingDp: Int
)

internal fun resolveVideoContentTabBarDanmakuActionLayoutPolicy(widthDp: Int): VideoContentTabBarDanmakuActionLayoutPolicy {
    val compactChrome = resolveCompactCapsuleChromeSpec(UiPreset.IOS, AndroidNativeVariant.MATERIAL3)
    return if (widthDp < 400) {
        VideoContentTabBarDanmakuActionLayoutPolicy(
            toggleIconSizeDp = 14,
            toggleHorizontalPaddingDp = 8,
            toggleVerticalPaddingDp = 5,
            toggleTextSizeSp = 10,
            toggleTrailingPaddingDp = 6,
            sendHorizontalPaddingDp = 10,
            sendVerticalPaddingDp = 7,
            sendTextSizeSp = 11,
            sendLabel = "发弹幕",
            secondaryControlHeightDp = 36,
            secondaryControlCornerRadiusDp = compactChrome.secondaryButtonCornerRadiusDp,
            settingsButtonSizeDp = 36,
            settingsIconSizeDp = 18,
            settingsLeadingPaddingDp = 4
        )
    } else {
        VideoContentTabBarDanmakuActionLayoutPolicy(
            toggleIconSizeDp = 16,
            toggleHorizontalPaddingDp = 10,
            toggleVerticalPaddingDp = 6,
            toggleTextSizeSp = 11,
            toggleTrailingPaddingDp = 8,
            sendHorizontalPaddingDp = 12,
            sendVerticalPaddingDp = 8,
            sendTextSizeSp = 12,
            sendLabel = "发弹幕",
            secondaryControlHeightDp = 36,
            secondaryControlCornerRadiusDp = compactChrome.secondaryButtonCornerRadiusDp,
            settingsButtonSizeDp = 36,
            settingsIconSizeDp = 18,
            settingsLeadingPaddingDp = 6
        )
    }
}

internal data class VideoContentTabSwitchAnimationSpec(
    val durationMs: Int
)

internal fun resolveVideoContentTabSwitchAnimationSpec(
    uiPreset: UiPreset
): VideoContentTabSwitchAnimationSpec {
    return when (uiPreset) {
        UiPreset.IOS -> VideoContentTabSwitchAnimationSpec(durationMs = 360)
        UiPreset.MD3 -> VideoContentTabSwitchAnimationSpec(durationMs = 240)
    }
}

internal fun resolveVideoContentEffectiveSelectedTabIndex(
    currentPage: Int,
    targetPage: Int,
    isScrollInProgress: Boolean,
    pageCount: Int
): Int {
    if (pageCount <= 0) return 0
    val current = currentPage.takeIf { it in 0 until pageCount } ?: 0
    return if (isScrollInProgress && targetPage in 0 until pageCount) {
        targetPage
    } else {
        current
    }
}

/**
 * 视频详情内容区域
 * 从 VideoDetailScreen.kt 提取出来，提高代码可维护性
 */
@Composable
fun VideoContentSection(
    info: ViewInfo,
    introListState: LazyListState,
    commentListState: LazyListState,
    pagerState: PagerState,
    relatedVideos: List<RelatedVideo>,
    replies: List<ReplyItem>,
    replyCount: Int,
    emoteMap: Map<String, String>,
    isRepliesLoading: Boolean,
    isRepliesEnd: Boolean = false,
    isLoggedIn: Boolean = false,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    currentPageIndex: Int,
    downloadProgress: Float = -1f,
    isInWatchLater: Boolean = false,
    followingMids: Set<Long> = emptySet(),
    videoTags: List<VideoTag> = emptyList(),
    sortMode: CommentSortMode = CommentSortMode.HOT,
    upOnlyFilter: Boolean = false,
    onSortModeChange: (CommentSortMode) -> Unit = {},
    onUpOnlyToggle: () -> Unit = {},
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onPageSelect: (Int) -> Unit,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    onSubReplyClick: (ReplyItem, Long) -> Unit,
    onCommentReplyClick: (ReplyItem) -> Unit = {},
    onLoadMoreReplies: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onWatchLaterClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onTimestampClick: ((Long) -> Unit)? = null,
    onDanmakuSendClick: () -> Unit = {},
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    // [新增] 删除与动画参数
    currentMid: Long = 0,
    showUpFlag: Boolean = false,
    showIdentityDecorations: Boolean = false,
    dissolvingIds: Set<Long> = emptySet(),
    onDeleteComment: (Long) -> Unit = {},
    onDissolveStart: (Long) -> Unit = {},
    // [新增] 点赞回调
    onCommentLike: (Long) -> Unit = {},
    // [新增] 已点赞的评论 ID 集合
    likedComments: Set<Long> = emptySet(),
    onCommentUrlClick: (String) -> Unit = {},
    onDescriptionUrlClick: ((String) -> Unit)? = null,
    onSearchKeywordClick: (String) -> Unit = {},
    onReportComment: (Long, Int) -> Unit = { _, _ -> },
    onToggleTopComment: (ReplyItem) -> Unit = {},
    // 🔗 [新增] 共享元素过渡开关
    transitionEnabled: Boolean = false,
    relatedVideoTransitionEnabled: Boolean = transitionEnabled,
    isQuickReturnLimitedForSharedElements: Boolean = false,
    sourceRouteForSharedElement: String? = null,
    // [新增] 收藏夹相关参数
    onFavoriteLongClick: () -> Unit = {},
    // [新增] 恢复播放器 (音频模式 -> 视频模式)
    isPlayerCollapsed: Boolean = false,
    onRestorePlayer: () -> Unit = {},
    // [新增] AI Summary & BGM
    aiSummary: AiSummaryData? = null,
    aiSummaryPrompt: com.android.purebilibili.feature.video.viewmodel.AiSummaryPromptState? = null,
    onRetryAiSummary: () -> Unit = {},
    onCreateNoteDraftFromAiSummary: () -> Unit = {},
    videoNoteState: VideoNoteUiState = VideoNoteUiState(),
    onOpenVideoNoteEditor: () -> Unit = {},
    onCloseVideoNoteEditor: () -> Unit = {},
    onVideoNoteDocumentChange: (VideoNoteEditorDocument) -> Unit = {},
    onInsertVideoNoteTimestamp: () -> Unit = {},
    onVideoNoteTimestampClick: (Long) -> Unit = {},
    onSaveVideoNote: (VideoNoteEditorDocument) -> Unit = {},
    onDeleteVideoNote: () -> Unit = {},
    onRetryVideoNote: () -> Unit = {},
    onPublicVideoNoteClick: (Long, String) -> Unit = { _, _ -> },
    bgmInfo: BgmInfo? = null,
    bgmInfoList: List<BgmInfo> = emptyList(),
    onBgmClick: (BgmInfo) -> Unit = {},
    onlineCount: String = "",
    showOnlineCount: Boolean = true,
    ownerFollowerCount: Int? = null,
    ownerVideoCount: Int? = null,
    showUpBadge: Boolean = true,
    showInteractionActions: Boolean = true,
    isVideoPlaying: Boolean = false,
    onSelectedTabChange: (Int) -> Unit = {},
    onIntroScrollThresholdChange: (Boolean) -> Unit = {},
    onCommentScrollStateChange: (Int, Int) -> Unit = { _, _ -> },
    bottomContentPadding: Dp = if (showInteractionActions) 84.dp else 12.dp
) {
    val context = LocalContext.current
    val tabs = listOf("简介", "评论 $replyCount")
    val scope = rememberCoroutineScope()
    TrackJankStateFlag(
        stateName = "video_detail:tab_swipe",
        isActive = pagerState.isScrollInProgress
    )
    TrackScrollJank(
        scrollableState = introListState,
        stateName = "video_detail:intro_scroll"
    )
    TrackScrollJank(
        scrollableState = commentListState,
        stateName = "video_detail:comment_scroll"
    )
    val isIntroListScrolling by remember {
        derivedStateOf { introListState.isScrollInProgress }
    }
    val isCommentListScrolling by remember {
        derivedStateOf { commentListState.isScrollInProgress }
    }
    val videoDetailMotionBudget by remember {
        derivedStateOf {
            resolveVideoDetailMotionBudget(
                isTabSwitching = pagerState.isScrollInProgress,
                isContentScrolling = isIntroListScrolling || isCommentListScrolling
            )
        }
    }
    val animateVideoDetailLayout = shouldAnimateVideoDetailLayout(videoDetailMotionBudget)
    val lightweightCommentRendering by remember {
        derivedStateOf {
            shouldUseLightweightCommentRendering(
                selectedTabIndex = pagerState.currentPage,
                isVideoPlaying = isVideoPlaying,
                isCommentListScrolling = isCommentListScrolling
            )
        }
    }
    
    // 评论图片预览状态
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewInitialIndex by remember { mutableIntStateOf(0) }
    var sourceRect by remember { mutableStateOf<Rect?>(null) }
    var previewTextContent by remember { mutableStateOf<ImagePreviewTextContent?>(null) }
    
    // 合集展开状态
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showDanmakuSettings by remember { mutableStateOf(false) }
    var confirmDeleteNote by remember { mutableStateOf(false) }
    val onShareVideoNote: (VideoNoteEditorDocument, Boolean) -> Unit = { document, isDraft ->
        ShareUtils.shareText(
            context = context,
            subject = document.title.ifBlank { info.title },
            text = buildVideoNoteShareText(
                videoTitle = info.title,
                bvid = info.bvid,
                document = document,
                isDraft = isDraft
            ),
            chooserTitle = "分享视频笔记"
        )
    }
    val uiPreset = LocalUiPreset.current
    val tabSwitchAnimationSpec = remember(uiPreset) {
        resolveVideoContentTabSwitchAnimationSpec(uiPreset)
    }
    val latestOnSelectedTabChange by rememberUpdatedState(onSelectedTabChange)

    val onTabSelected: (Int) -> Unit = { index ->
        scope.launch {
            pagerState.animateScrollToPage(
                page = index,
                animationSpec = tween(
                    durationMillis = tabSwitchAnimationSpec.durationMs,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    LaunchedEffect(pagerState, tabs.size) {
        snapshotFlow {
            resolveVideoContentEffectiveSelectedTabIndex(
                currentPage = pagerState.currentPage,
                targetPage = pagerState.targetPage,
                isScrollInProgress = pagerState.isScrollInProgress,
                pageCount = tabs.size
            )
        }
            .distinctUntilChanged()
            .collect { effectiveTabIndex ->
                latestOnSelectedTabChange(effectiveTabIndex)
            }
    }
    LaunchedEffect(introListState) {
        snapshotFlow {
            isVideoDetailIntroScrollPastCollapseThreshold(
                firstVisibleItemIndex = introListState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = introListState.firstVisibleItemScrollOffset
            )
        }
            .distinctUntilChanged()
            .collect(onIntroScrollThresholdChange)
    }
    LaunchedEffect(commentListState) {
        snapshotFlow { commentListState.firstVisibleItemIndex to commentListState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { state: Pair<Int, Int> ->
                onCommentScrollStateChange(state.first, state.second)
            }
    }

    // 采样层只挂在 Tab 页滚动内容上；排序栏/顶栏分段控件必须在捕获区外，避免 drawBackdrop 自引用导致 RenderThread 栈溢出。
    val videoContentChromeBackdrop = rememberLayerBackdrop()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Inline 弹幕设置不是 Dialog，必须在详情内容之后绘制，避免被列表盖住。
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            VideoContentTabBar(
                tabs = tabs,
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = onTabSelected,
                onDanmakuSendClick = onDanmakuSendClick,
                danmakuEnabled = danmakuEnabled,
                onDanmakuToggle = onDanmakuToggle,
                onDanmakuSettingsClick = { showDanmakuSettings = true },
                modifier = Modifier,
                isPlayerCollapsed = isPlayerCollapsed,
                onRestorePlayer = onRestorePlayer,
                backdrop = videoContentChromeBackdrop
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = resolveVideoDetailBeyondViewportPageCount(
                    isVideoPlaying = isVideoPlaying,
                    selectedTabIndex = pagerState.currentPage
                ),
                userScrollEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> VideoIntroTab(
                        listState = introListState,
                        modifier = Modifier,
                        chromeBackdrop = videoContentChromeBackdrop,
                        info = info,
                        relatedVideos = relatedVideos,
                        currentPageIndex = currentPageIndex,
                        followingMids = followingMids,
                        videoTags = videoTags,
                        isFollowing = isFollowing,
                        isFavorited = isFavorited,
                        isLiked = isLiked,
                        coinCount = coinCount,
                        downloadProgress = downloadProgress,
                        isInWatchLater = isInWatchLater,
                        isLoggedIn = isLoggedIn,
                        onFollowClick = onFollowClick,
                        onFavoriteClick = onFavoriteClick,
                        onLikeClick = onLikeClick,
                        onCoinClick = onCoinClick,
                        onTripleClick = onTripleClick,
                        onCommentClick = { onTabSelected(1) },
                        onPageSelect = onPageSelect,
                        onUpClick = onUpClick,
                        onRelatedVideoClick = onRelatedVideoClick,
                        onOpenCollectionSheet = { showCollectionSheet = true },
                        onDownloadClick = onDownloadClick,
                        onWatchLaterClick = onWatchLaterClick,
                        onShareClick = onShareClick,
                        contentPadding = PaddingValues(bottom = bottomContentPadding),
                        transitionEnabled = transitionEnabled,
                        relatedVideoTransitionEnabled = relatedVideoTransitionEnabled,
                        isQuickReturnLimitedForSharedElements = isQuickReturnLimitedForSharedElements,
                        sourceRouteForSharedElement = sourceRouteForSharedElement,
                        ownerFollowerCount = ownerFollowerCount,
                        ownerVideoCount = ownerVideoCount,
                        showUpBadge = showUpBadge,
                        onFavoriteLongClick = onFavoriteLongClick,
                        aiSummary = aiSummary,
                        aiSummaryPrompt = aiSummaryPrompt,
                        onRetryAiSummary = onRetryAiSummary,
                        onCreateNoteDraftFromAiSummary = onCreateNoteDraftFromAiSummary,
                        videoNoteState = videoNoteState,
                        onOpenVideoNoteEditor = onOpenVideoNoteEditor,
                        onRetryVideoNote = onRetryVideoNote,
                        onDeleteVideoNoteClick = { confirmDeleteNote = true },
                        onShareVideoNote = { document -> onShareVideoNote(document, false) },
                        onPublicVideoNoteClick = onPublicVideoNoteClick,
                        bgmInfo = bgmInfo,
                        bgmInfoList = bgmInfoList,
                        onlineCount = onlineCount,
                        showOnlineCount = showOnlineCount,
                        onTimestampClick = onTimestampClick,
                        onBgmClick = onBgmClick,
                        onDescriptionUrlClick = onDescriptionUrlClick,
                        onSearchKeywordClick = onSearchKeywordClick,
                        showInteractionActions = showInteractionActions,
                        animateVideoDetailLayout = animateVideoDetailLayout
                    )
                    1 -> VideoCommentTab(
                        listState = commentListState,
                        modifier = Modifier,
                        info = info,
                        replies = replies,
                        replyCount = replyCount,
                        emoteMap = emoteMap,
                        isRepliesLoading = isRepliesLoading,
                        isRepliesEnd = isRepliesEnd,
                        videoTags = videoTags,
                        sortMode = sortMode,
                        upOnlyFilter = upOnlyFilter,
                        onSortModeChange = onSortModeChange,
                        onUpOnlyToggle = onUpOnlyToggle,
                        onUpClick = onUpClick,
                        onSubReplyClick = onSubReplyClick,
                        onCommentReplyClick = onCommentReplyClick,
                        onLoadMoreReplies = onLoadMoreReplies,
                        onImagePreview = { images, index, rect, textContent ->
                            previewImages = images
                            previewInitialIndex = index
                            sourceRect = rect
                            previewTextContent = textContent
                            showImagePreview = true
                        },
                        onTimestampClick = onTimestampClick,
                        showUpFlag = showUpFlag,
                        contentPadding = PaddingValues(bottom = bottomContentPadding),
                        currentMid = currentMid,
                        dissolvingIds = dissolvingIds,
                        onDeleteComment = onDeleteComment,
                        onDissolveStart = onDissolveStart,
                        onCommentLike = onCommentLike,
                        likedComments = likedComments,
                        onCommentUrlClick = onCommentUrlClick,
                        onReportComment = onReportComment,
                        onToggleTopComment = onToggleTopComment,
                        showIdentityDecorations = showIdentityDecorations,
                        lightweightCommentRendering = lightweightCommentRendering,
                        chromeBackdrop = videoContentChromeBackdrop
                    )
                }
            }
        }

        if (showImagePreview && previewImages.isNotEmpty()) {
            ImagePreviewDialog(
                images = previewImages,
                initialIndex = previewInitialIndex,
                sourceRect = sourceRect,
                textContent = previewTextContent,
                onDismiss = {
                    showImagePreview = false
                    previewTextContent = null
                }
            )
        }

        info.ugc_season?.let { season ->
            if (showCollectionSheet) {
                CollectionSheet(
                    ugcSeason = season,
                    currentBvid = info.bvid,
                    currentCid = info.cid,
                    onDismiss = { showCollectionSheet = false },
                    onEpisodeClick = { episode ->
                        showCollectionSheet = false
                        onRelatedVideoClick(
                            episode.bvid,
                            buildVideoNavigationOptions(targetCid = episode.cid)
                        )
                    }
                )
            }
        }

        if (showDanmakuSettings) {
            VideoDetailDanmakuSettingsPanel(
                onDismiss = { showDanmakuSettings = false }
            )
        }

        VideoNoteEditorSheet(
            noteState = videoNoteState,
            onDismiss = onCloseVideoNoteEditor,
            onDocumentChange = onVideoNoteDocumentChange,
            onInsertTimestamp = onInsertVideoNoteTimestamp,
            onTimestampClick = onVideoNoteTimestampClick,
            onShare = { document -> onShareVideoNote(document, videoNoteState.editorFromAiSummary) },
            onSave = onSaveVideoNote
        )

        VideoNoteDeleteConfirmDialog(
            visible = confirmDeleteNote,
            deleting = videoNoteState.deleting,
            onConfirm = {
                confirmDeleteNote = false
                onDeleteVideoNote()
            },
            onDismiss = { confirmDeleteNote = false }
        )
    }
}

// ... VideoIntroTab signature ...
@Composable
private fun VideoIntroTab(
    listState: LazyListState,
    modifier: Modifier,
    info: ViewInfo,
    relatedVideos: List<RelatedVideo>,
    currentPageIndex: Int,
    followingMids: Set<Long>,
    videoTags: List<VideoTag>,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    downloadProgress: Float,
    isInWatchLater: Boolean,
    isLoggedIn: Boolean = false,
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onCommentClick: () -> Unit,
    onPageSelect: (Int) -> Unit,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    onOpenCollectionSheet: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onDescriptionUrlClick: ((String) -> Unit)? = null,
    onSearchKeywordClick: (String) -> Unit = {},
    contentPadding: PaddingValues,
    transitionEnabled: Boolean = false,  // 🔗 共享元素过渡开关
    relatedVideoTransitionEnabled: Boolean = transitionEnabled,
    isQuickReturnLimitedForSharedElements: Boolean = false,
    sourceRouteForSharedElement: String? = null,
    ownerFollowerCount: Int? = null,
    ownerVideoCount: Int? = null,
    showUpBadge: Boolean = true,
    onFavoriteLongClick: () -> Unit = {},
    aiSummary: AiSummaryData? = null,
    aiSummaryPrompt: com.android.purebilibili.feature.video.viewmodel.AiSummaryPromptState? = null,
    onRetryAiSummary: () -> Unit = {},
    onCreateNoteDraftFromAiSummary: () -> Unit = {},
    videoNoteState: VideoNoteUiState = VideoNoteUiState(),
    onOpenVideoNoteEditor: () -> Unit = {},
    onRetryVideoNote: () -> Unit = {},
    onDeleteVideoNoteClick: () -> Unit = {},
    onShareVideoNote: (VideoNoteEditorDocument) -> Unit = {},
    onPublicVideoNoteClick: (Long, String) -> Unit = { _, _ -> },
    bgmInfo: BgmInfo? = null,
    bgmInfoList: List<BgmInfo> = emptyList(),
    onTimestampClick: ((Long) -> Unit)? = null,
    onBgmClick: (BgmInfo) -> Unit = {},
    onlineCount: String = "",
    showOnlineCount: Boolean = true,
    showInteractionActions: Boolean = true,
    animateVideoDetailLayout: Boolean = true,
    chromeBackdrop: LayerBackdrop? = null
) {
    val hasPages = info.pages.size > 1
    var hiddenRelatedBvids by remember(info.bvid) { mutableStateOf(emptySet<String>()) }
    val visibleRelatedVideos = remember(relatedVideos, hiddenRelatedBvids) {
        filterRelatedVideosByHiddenBvids(relatedVideos, hiddenRelatedBvids)
    }
    val isRelatedListScrolling by remember(listState) {
        derivedStateOf { listState.isScrollInProgress }
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .then(
                if (chromeBackdrop != null) {
                    Modifier.layerBackdrop(chromeBackdrop)
                } else {
                    Modifier
                }
            ),
        contentPadding = contentPadding
    ) {
        // 1. 移入的 Header 区域
        item {
            VideoHeaderContent(
                info = info,
                videoTags = videoTags,
                isFollowing = isFollowing,
                isFavorited = isFavorited,
                isLiked = isLiked,
                coinCount = coinCount,
                downloadProgress = downloadProgress,
                isInWatchLater = isInWatchLater,
                isLoggedIn = isLoggedIn,
                onFollowClick = onFollowClick,
                onFavoriteClick = onFavoriteClick,
                onLikeClick = onLikeClick,
                onCoinClick = onCoinClick,
                onTripleClick = onTripleClick,
                onCommentClick = onCommentClick,
                onUpClick = onUpClick,
                onOpenCollectionSheet = onOpenCollectionSheet,
                onDownloadClick = onDownloadClick,
                onWatchLaterClick = onWatchLaterClick,
                onShareClick = onShareClick,

                onGloballyPositioned = { },
                transitionEnabled = transitionEnabled,  // 🔗 传递共享元素开关
                isQuickReturnLimitedForSharedElements = isQuickReturnLimitedForSharedElements,
                sourceRouteForSharedElement = sourceRouteForSharedElement,
                ownerFollowerCount = ownerFollowerCount,
                ownerVideoCount = ownerVideoCount,
                onFavoriteLongClick = onFavoriteLongClick,
                aiSummary = aiSummary,
                aiSummaryPrompt = aiSummaryPrompt,
                onRetryAiSummary = onRetryAiSummary,
                onCreateNoteDraftFromAiSummary = onCreateNoteDraftFromAiSummary,
                videoNoteState = videoNoteState,
                onOpenVideoNoteEditor = onOpenVideoNoteEditor,
                onRetryVideoNote = onRetryVideoNote,
                onDeleteVideoNoteClick = onDeleteVideoNoteClick,
                onShareVideoNote = onShareVideoNote,
                onPublicVideoNoteClick = onPublicVideoNoteClick,
                bgmInfo = bgmInfo,
                bgmInfoList = bgmInfoList,
                relatedVideos = relatedVideos,
                onlineCount = onlineCount,
                showOnlineCount = showOnlineCount,
                onTimestampClick = onTimestampClick,
                onBgmClick = onBgmClick,
                onDescriptionUrlClick = onDescriptionUrlClick,
                onRelatedVideoClick = onRelatedVideoClick,
                onSearchKeywordClick = onSearchKeywordClick,
                showInteractionActions = showInteractionActions,
                animateVideoDetailLayout = animateVideoDetailLayout
            )
        }
        if (hasPages) {
            item {
                PagesSelector(
                    pages = info.pages,
                    currentPageIndex = currentPageIndex,
                    onPageSelect = onPageSelect
                )
            }
        }

        item {
            VideoRecommendationHeader()
        }

        val relatedRows = chunkRelatedVideosForHomeStyleGrid(visibleRelatedVideos)
        itemsIndexed(
            items = relatedRows,
            key = { rowIndex, row ->
                val first = row.firstOrNull()
                resolveIndexedVideoLazyKey(
                    namespace = "video_related_row",
                    index = rowIndex,
                    bvid = first?.bvid.orEmpty(),
                    aid = first?.aid ?: 0L,
                    cid = first?.cid ?: 0L
                )
            }
        ) { _, row ->
            CompositionLocalProvider(
                LocalVideoCardSharedElementSourceRoute provides "video/${info.bvid}"
            ) {
                RelatedVideoGridRow(
                    videos = row,
                    followingMids = followingMids,
                    transitionEnabled = relatedVideoTransitionEnabled,
                    isListScrolling = isRelatedListScrolling,
                    showUpBadge = showUpBadge,
                    onVideoClick = { video ->
                        val navOptions = if (video.cid > 0L) {
                            android.os.Bundle().apply {
                                putLong(VIDEO_NAV_TARGET_CID_KEY, video.cid)
                            }
                        } else {
                            null
                        }
                        onRelatedVideoClick(video.bvid, navOptions)
                    },
                    onVideoHidden = { video ->
                        hiddenRelatedBvids = hiddenRelatedBvids + video.bvid
                    }
                )
            }
        }
    }
}

// ... VideoCommentTab signature ...
@Composable
private fun VideoCommentTab(
    listState: LazyListState,
    modifier: Modifier,
    info: ViewInfo,
    replies: List<ReplyItem>,
    replyCount: Int,
    emoteMap: Map<String, String>,
    isRepliesLoading: Boolean,
    isRepliesEnd: Boolean,
    videoTags: List<VideoTag>,
    sortMode: CommentSortMode,
    upOnlyFilter: Boolean,
    onSortModeChange: (CommentSortMode) -> Unit,
    onUpOnlyToggle: () -> Unit,
    onUpClick: (Long) -> Unit,
    onSubReplyClick: (ReplyItem, Long) -> Unit,
    onCommentReplyClick: (ReplyItem) -> Unit,
    onLoadMoreReplies: () -> Unit,
    onImagePreview: (List<String>, Int, Rect?, ImagePreviewTextContent?) -> Unit,
    onTimestampClick: ((Long) -> Unit)?,
    contentPadding: PaddingValues,
    // [新增] 参数
    currentMid: Long,
    showUpFlag: Boolean,
    dissolvingIds: Set<Long>,
    onDeleteComment: (Long) -> Unit,
    onDissolveStart: (Long) -> Unit,
    // [新增] 点赞回调
    onCommentLike: (Long) -> Unit,
    likedComments: Set<Long>,
    onCommentUrlClick: (String) -> Unit,
    onReportComment: (Long, Int) -> Unit,
    onToggleTopComment: (ReplyItem) -> Unit,
    showIdentityDecorations: Boolean,
    lightweightCommentRendering: Boolean,
    chromeBackdrop: LayerBackdrop? = null
) {
    val commentAppearance = rememberVideoCommentAppearance()
    val scope = rememberCoroutineScope()
    val shouldShowBackToTop by remember(listState) {
        derivedStateOf {
            shouldShowVideoCommentBackToTop(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            )
        }
    }
    val shouldLoadMore by remember(
        listState,
        replies.size,
        replyCount,
        isRepliesLoading,
        isRepliesEnd
    ) {
        derivedStateOf {
            shouldLoadMoreVideoComments(
                lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1,
                totalItemsCount = listState.layoutInfo.totalItemsCount,
                isLoading = isRepliesLoading,
                isEnd = isRepliesEnd || replies.isEmpty() || replyCount <= 0 || replies.size >= replyCount
            )
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMoreReplies()
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        CommentSortFilterBar(
            count = replyCount,
            sortMode = sortMode,
            onSortModeChange = onSortModeChange,
            upOnly = upOnlyFilter,
            onUpOnlyToggle = onUpOnlyToggle,
            backdrop = chromeBackdrop
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (chromeBackdrop != null) {
                            Modifier.layerBackdrop(chromeBackdrop)
                        } else {
                            Modifier
                        }
                    ),
                contentPadding = contentPadding
            ) {
            if (isRepliesLoading && replies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CupertinoActivityIndicator()
                    }
                }
            } else if (replies.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (upOnlyFilter) "这个视频没有 UP 主的评论" else "暂无评论",
                            color = commentAppearance.secondaryTextColor
                        )
                    }
                }
            } else {
                items(
                    items = replies,
                    key = { it.rpid },
                    contentType = { resolveReplyItemContentType(it) }
                ) { reply ->
                    // [新增] 使用 DissolvableVideoCard 包裹
                    com.android.purebilibili.core.ui.animation.MaybeDissolvableVideoCard(
                        isDissolving = reply.rpid in dissolvingIds,
                        onDissolveComplete = { onDeleteComment(reply.rpid) },
                        cardId = "comment_${reply.rpid}",
                        modifier = Modifier.padding(bottom = 1.dp) // 小间距防止裁剪
                    ) {
                        ReplyItemView(
                            showUpFlag = showUpFlag,
                            item = reply,
                            upMid = info.owner.mid,
                            emoteMap = emoteMap,
                            lightweightMode = lightweightCommentRendering,
                            showIdentityDecorations = showIdentityDecorations,
                            onClick = {},
                            onSubClick = onSubReplyClick,
                            onTimestampClick = onTimestampClick,
                            maxTimestampMs = info.pages.firstOrNull { it.cid == info.cid }?.duration?.times(1000L)
                                ?: info.pages.firstOrNull()?.duration?.times(1000L),
                            onImagePreview = { images, index, rect, textContent ->
                                onImagePreview(images, index, rect, textContent)
                            },
                            // [新增] 点赞事件
                            onLikeClick = { onCommentLike(reply.rpid) },
                            onReplyClick = { onCommentReplyClick(reply) },
                            onReportClick = { reason -> onReportComment(reply.rpid, reason) },
                            canToggleTop = shouldShowReplyTopAction(
                                currentMid = currentMid,
                                upMid = info.owner.mid,
                                item = reply
                            ),
                            onToggleTopClick = { onToggleTopComment(reply) },
                            // [修复] 正确传递点赞状态 (API数据 或 本地乐观更新)
                            isLiked = reply.action == 1 || reply.rpid in likedComments,
                            // [新增] 仅当评论 mid 与当前登录用户 mid 一致时显示删除按钮
                            onDeleteClick = if (currentMid > 0 && reply.mid == currentMid) {
                                { onDissolveStart(reply.rpid) }
                            } else null,
                            // [新增] URL 点击跳转
                            onUrlClick = onCommentUrlClick,
                            // [新增] 头像点击
                            onAvatarClick = { mid -> mid.toLongOrNull()?.let { onUpClick(it) } }
                        )
                    }
                }

                // 加载更多
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isRepliesLoading -> CupertinoActivityIndicator()
                            isRepliesEnd || replies.size >= replyCount -> {
                                Text("—— end ——", color = commentAppearance.secondaryTextColor, fontSize = 12.sp)
                            }
                            // 当 shouldLoadMore 为 true 时才显示加载指示器
                            shouldLoadMore -> CupertinoActivityIndicator()
                        }
                    }
                }
            }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = shouldShowBackToTop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = contentPadding.calculateBottomPadding() + 12.dp
                    ),
                enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.92f),
                exit = fadeOut(animationSpec = tween(140)) + scaleOut(targetScale = 0.92f)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = rememberAppChevronUpIcon(),
                        contentDescription = "回到顶部"
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoHeaderContent(
    info: ViewInfo,
    videoTags: List<VideoTag>,
    isFollowing: Boolean,
    isFavorited: Boolean,
    isLiked: Boolean,
    coinCount: Int,
    downloadProgress: Float,
    isInWatchLater: Boolean,
    isLoggedIn: Boolean = false,
    onFollowClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onCommentClick: () -> Unit,
    onUpClick: (Long) -> Unit,
    onOpenCollectionSheet: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onGloballyPositioned: (Float) -> Unit,
    transitionEnabled: Boolean = false,  // 🔗 共享元素过渡开关
    isQuickReturnLimitedForSharedElements: Boolean = false,
    sourceRouteForSharedElement: String? = null,
    ownerFollowerCount: Int? = null,
    ownerVideoCount: Int? = null,
    onFavoriteLongClick: () -> Unit = {},
    aiSummary: AiSummaryData? = null,
    aiSummaryPrompt: com.android.purebilibili.feature.video.viewmodel.AiSummaryPromptState? = null,
    onRetryAiSummary: () -> Unit = {},
    onCreateNoteDraftFromAiSummary: () -> Unit = {},
    videoNoteState: VideoNoteUiState = VideoNoteUiState(),
    onOpenVideoNoteEditor: () -> Unit = {},
    onRetryVideoNote: () -> Unit = {},
    onDeleteVideoNoteClick: () -> Unit = {},
    onShareVideoNote: (VideoNoteEditorDocument) -> Unit = {},
    onPublicVideoNoteClick: (Long, String) -> Unit = { _, _ -> },
    bgmInfo: BgmInfo? = null,
    bgmInfoList: List<BgmInfo> = emptyList(),
    relatedVideos: List<RelatedVideo> = emptyList(),
    onTimestampClick: ((Long) -> Unit)? = null,
    onBgmClick: (BgmInfo) -> Unit = {},
    onDescriptionUrlClick: ((String) -> Unit)? = null,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit = { _, _ -> },
    onSearchKeywordClick: (String) -> Unit = {},
    onlineCount: String = "",
    showOnlineCount: Boolean = true,
    showInteractionActions: Boolean = true,
    animateVideoDetailLayout: Boolean = true
) {
    val context = LocalContext.current
    val videoAiSummaryEntryEnabled by com.android.purebilibili.core.store.SettingsManager
        .getVideoAiSummaryEntryEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true
        )
    val videoNoteEnabled by com.android.purebilibili.core.store.SettingsManager
        .getVideoNoteEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true
        )
    val videoNoteDefaultCollapsed by com.android.purebilibili.core.store.SettingsManager
        .getVideoNoteDefaultCollapsed(context)
        .collectAsStateWithLifecycle(initialValue = false
        )
    var showUpPreview by remember(info.bvid, info.owner.mid) { mutableStateOf(false) }
    // 同 UP 的相关推荐作首屏种子，再异步拉投稿列表
    val upPreviewSeedVideos = remember(relatedVideos, info.owner.mid) {
        relatedVideos.filter { it.owner.mid == info.owner.mid || it.owner.mid <= 0L }
    }

    if (showUpPreview) {
        UpPreviewSheet(
            visible = true,
            owner = info.owner,
            isFollowing = isFollowing,
            followerCount = ownerFollowerCount,
            videoCount = ownerVideoCount,
            seedVideos = upPreviewSeedVideos,
            onDismiss = { showUpPreview = false },
            onFollowClick = onFollowClick,
            onEnterSpace = { mid ->
                showUpPreview = false
                onUpClick(mid)
            },
            onVideoClick = { bvid, _ ->
                showUpPreview = false
                onRelatedVideoClick(bvid, null)
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // 🎨 [修复] 与 TabBar 统一使用 Surface (通常为白色/深灰色)，消除割裂感
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned(coordinates.size.height.toFloat())
            }
    ) {
        UpInfoSection(
            info = info,
            isFollowing = isFollowing,
            onFollowClick = onFollowClick,
            onUpClick = {
                if (info.owner.mid > 0L) {
                    showUpPreview = true
                } else {
                    onUpClick(it)
                }
            },
            showOwnerAvatar = true,
            followerCount = ownerFollowerCount,
            videoCount = ownerVideoCount,
            transitionEnabled = transitionEnabled,  // 🔗 传递共享元素开关
            isQuickReturnLimitedForSharedElements = isQuickReturnLimitedForSharedElements,
            sourceRouteForSharedElement = sourceRouteForSharedElement
        )

        VideoTitleWithDesc(
            info = info,
            videoTags = videoTags,
            transitionEnabled = transitionEnabled,  // 🔗 传递共享元素开关
            isQuickReturnLimitedForSharedElements = isQuickReturnLimitedForSharedElements,
            sourceRouteForSharedElement = sourceRouteForSharedElement,
            bgmList = resolveDisplayBgmList(
                bgmInfo = bgmInfo,
                bgmInfoList = bgmInfoList
            ),
            onlineCount = onlineCount,
            showOnlineCount = showOnlineCount,
            onBgmClick = onBgmClick,
            onDescriptionUrlClick = onDescriptionUrlClick,
            onRelatedVideoClick = onRelatedVideoClick,
            animateLayout = animateVideoDetailLayout,
            onTagClick = onSearchKeywordClick
        )

        // [新增] AI Summary
        if (shouldShowAiSummaryEntry(
                aiSummary = aiSummary,
                isAiSummaryEntryEnabled = videoAiSummaryEntryEnabled
            )
        ) {
            AiSummaryCard(
                aiSummary = aiSummary,
                onTimestampClick = onTimestampClick,
                onCreateNoteDraftClick = onCreateNoteDraftFromAiSummary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else if (videoAiSummaryEntryEnabled && aiSummaryPrompt != null) {
            AiSummaryPromptCard(
                promptState = aiSummaryPrompt,
                onActionClick = onRetryAiSummary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (shouldShowVideoNoteCard(videoNoteEnabled)) {
            VideoNoteCard(
                noteState = videoNoteState,
                isLoggedIn = isLoggedIn,
                onCreateOrEditClick = onOpenVideoNoteEditor,
                onRetryClick = onRetryVideoNote,
                onDeleteClick = onDeleteVideoNoteClick,
                onShareClick = onShareVideoNote,
                onPublicNoteClick = onPublicVideoNoteClick,
                defaultCollapsed = videoNoteDefaultCollapsed,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (showInteractionActions) {
            ActionButtonsRow(
                info = info,
                isFavorited = isFavorited,
                isLiked = isLiked,
                coinCount = coinCount,
                downloadProgress = downloadProgress,
                isInWatchLater = isInWatchLater,
                onFavoriteClick = onFavoriteClick,
                onLikeClick = onLikeClick,
                onCoinClick = onCoinClick,
                onTripleClick = onTripleClick,
                onCommentClick = onCommentClick,
                onDownloadClick = onDownloadClick,
                onWatchLaterClick = onWatchLaterClick,
                onFavoriteLongClick = onFavoriteLongClick,
                onShareClick = onShareClick,
                showCommentAction = false,
            )
        }

        info.ugc_season?.let { season ->
            CollectionRow(
                ugcSeason = season,
                currentBvid = info.bvid,
                currentCid = info.cid,
                onClick = onOpenCollectionSheet
            )
        }
    }
}

@Composable
private fun VideoDetailDanmakuSettingsPanel(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val danmakuScope = com.android.purebilibili.core.store.DanmakuSettingsScope.PORTRAIT
    val danmakuSettings by SettingsManager
        .getDanmakuSettings(context, danmakuScope)
        .collectAsStateWithLifecycle(initialValue = DanmakuSettings(),
            context = kotlin.coroutines.EmptyCoroutineContext
        )

    var localOpacity by remember(danmakuSettings.opacity) { mutableFloatStateOf(danmakuSettings.opacity) }
    var localFontScale by remember(danmakuSettings.fontScale) { mutableFloatStateOf(danmakuSettings.fontScale) }
    var localSpeed by remember(danmakuSettings.speed) { mutableFloatStateOf(danmakuSettings.speed) }
    var localDisplayArea by remember(danmakuSettings.displayArea) { mutableFloatStateOf(danmakuSettings.displayArea) }
    var localMergeDuplicates by remember(danmakuSettings.mergeDuplicates) { mutableStateOf(danmakuSettings.mergeDuplicates) }
    var localDuplicateMergeWindowMs by remember(danmakuSettings.duplicateMergeWindowMs) {
        mutableStateOf(danmakuSettings.duplicateMergeWindowMs)
    }
    var localDuplicateMergeCountThreshold by remember(danmakuSettings.duplicateMergeCountThreshold) {
        mutableStateOf(danmakuSettings.duplicateMergeCountThreshold)
    }
    var localAllowScroll by remember(danmakuSettings.allowScroll) { mutableStateOf(danmakuSettings.allowScroll) }
    var localAllowTop by remember(danmakuSettings.allowTop) { mutableStateOf(danmakuSettings.allowTop) }
    var localAllowBottom by remember(danmakuSettings.allowBottom) { mutableStateOf(danmakuSettings.allowBottom) }
    var localAllowColorful by remember(danmakuSettings.allowColorful) { mutableStateOf(danmakuSettings.allowColorful) }
    var localAllowSpecial by remember(danmakuSettings.allowSpecial) { mutableStateOf(danmakuSettings.allowSpecial) }
    var localHideInteractiveCommands by remember(danmakuSettings.hideInteractiveCommands) {
        mutableStateOf(danmakuSettings.hideInteractiveCommands)
    }
    var localPortraitDisplayAreaMode by remember(danmakuSettings.portraitDisplayAreaMode) {
        mutableStateOf(danmakuSettings.portraitDisplayAreaMode)
    }
    var localBlockRulesRaw by remember(danmakuSettings.blockRulesRaw) { mutableStateOf(danmakuSettings.blockRulesRaw) }

    DanmakuSettingsPanel(
        isFullscreen = false,
        settingsScope = danmakuScope,
        opacity = localOpacity,
        fontScale = localFontScale,
        speed = localSpeed,
        displayArea = localDisplayArea,
        mergeDuplicates = localMergeDuplicates,
        duplicateMergeWindowMs = localDuplicateMergeWindowMs,
        duplicateMergeCountThreshold = localDuplicateMergeCountThreshold,
        allowScroll = localAllowScroll,
        allowTop = localAllowTop,
        allowBottom = localAllowBottom,
        allowColorful = localAllowColorful,
        allowSpecial = localAllowSpecial,
        hideInteractiveCommands = localHideInteractiveCommands,
        portraitDisplayAreaMode = localPortraitDisplayAreaMode,
        showBlockRuleEditor = true,
        showSmartOcclusionSection = false,
        blockRulesRaw = localBlockRulesRaw,
        smartOcclusion = false,
        onOpacityChange = {
            localOpacity = it
            scope.launch { SettingsManager.setDanmakuOpacity(context, it, danmakuScope) }
        },
        onFontScaleChange = {
            localFontScale = it
            scope.launch { SettingsManager.setDanmakuFontScale(context, it, danmakuScope) }
        },
        onSpeedChange = {
            localSpeed = it
            scope.launch { SettingsManager.setDanmakuSpeed(context, it, danmakuScope) }
        },
        onDisplayAreaChange = {
            localDisplayArea = it
            scope.launch { SettingsManager.setDanmakuArea(context, it, danmakuScope) }
        },
        onMergeDuplicatesChange = {
            localMergeDuplicates = it
            scope.launch { SettingsManager.setDanmakuMergeDuplicates(context, it, danmakuScope) }
        },
        onDuplicateMergeWindowMsChange = {
            localDuplicateMergeWindowMs = it
            scope.launch { SettingsManager.setDanmakuDuplicateMergeWindowMs(context, it, danmakuScope) }
        },
        onDuplicateMergeCountThresholdChange = {
            localDuplicateMergeCountThreshold = it
            scope.launch { SettingsManager.setDanmakuDuplicateMergeCountThreshold(context, it, danmakuScope) }
        },
        onAllowScrollChange = {
            localAllowScroll = it
            scope.launch { SettingsManager.setDanmakuAllowScroll(context, it, danmakuScope) }
        },
        onAllowTopChange = {
            localAllowTop = it
            scope.launch { SettingsManager.setDanmakuAllowTop(context, it, danmakuScope) }
        },
        onAllowBottomChange = {
            localAllowBottom = it
            scope.launch { SettingsManager.setDanmakuAllowBottom(context, it, danmakuScope) }
        },
        onAllowColorfulChange = {
            localAllowColorful = it
            scope.launch { SettingsManager.setDanmakuAllowColorful(context, it, danmakuScope) }
        },
        onAllowSpecialChange = {
            localAllowSpecial = it
            scope.launch { SettingsManager.setDanmakuAllowSpecial(context, it, danmakuScope) }
        },
        onHideInteractiveCommandsChange = {
            localHideInteractiveCommands = it
            scope.launch { SettingsManager.setDanmakuHideInteractiveCommands(context, it) }
        },
        onPortraitDisplayAreaModeChange = {
            localPortraitDisplayAreaMode = it
            scope.launch { SettingsManager.setPortraitDanmakuDisplayAreaMode(context, it) }
        },
        onBlockRulesRawChange = {
            localBlockRulesRaw = it
            scope.launch { SettingsManager.setDanmakuBlockRulesRaw(context, it, danmakuScope) }
        },
        onDismiss = onDismiss
    )
}

/**
 * Tab 栏组件
 */
@Composable
private fun VideoContentTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onDanmakuSendClick: () -> Unit,
    danmakuEnabled: Boolean,
    onDanmakuToggle: () -> Unit,
    onDanmakuSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlayerCollapsed: Boolean = false,
    onRestorePlayer: () -> Unit = {},
    backdrop: Backdrop? = null
) {
    val context = LocalContext.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val configuration = LocalConfiguration.current
    val layoutSpec = remember(configuration.screenWidthDp) {
        resolveVideoContentTabBarLayoutSpec(widthDp = configuration.screenWidthDp)
    }
    val danmakuActionLayoutPolicy = remember(configuration.screenWidthDp) {
        resolveVideoContentTabBarDanmakuActionLayoutPolicy(widthDp = configuration.screenWidthDp)
    }
    val liquidChromeSpec = remember(
        homeSettings.androidNativeLiquidGlassEnabled,
        backdrop,
        layoutSpec
    ) {
        resolveVideoContentTabBarLiquidChromeSpec(
            androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled,
            hasBackdrop = backdrop != null,
            layoutSpec = layoutSpec,
        )
    }
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (liquidChromeSpec.useTransparentTabRowBackground) {
                        Modifier
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    }
                )
                .padding(horizontal = layoutSpec.containerHorizontalPaddingDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarLiquidSegmentedControl(
                items = tabs,
                selectedIndex = selectedTabIndex,
                onSelected = onTabSelected,
                modifier = Modifier
                    .weight(layoutSpec.tabsRowWeight)
                    .padding(
                        start = 0.dp,
                        top = if (liquidChromeSpec.reusesLiquidGlassDock) 0.dp else 2.dp,
                        end = 8.dp,
                        bottom = if (liquidChromeSpec.reusesLiquidGlassDock) 0.dp else 2.dp,
                    ),
                height = liquidChromeSpec.segmentedControlHeightDp.dp,
                indicatorHeight = liquidChromeSpec.segmentedControlIndicatorHeightDp.dp,
                labelFontSize = liquidChromeSpec.labelFontSizeSp.sp,
                backdrop = backdrop,
                forceLiquidChrome = homeSettings.androidNativeLiquidGlassEnabled,
                liquidGlassEffectsEnabled = liquidChromeSpec.liquidGlassEffectsEnabled,
                // Avoid extra press refraction in this compact in-content chrome.
                tapPressRefractionEnabled = false,
            )

            // [新增] 恢复画面按钮 (仅在播放器折叠时显示)
            AnimatedVisibility(
                visible = isPlayerCollapsed,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onRestorePlayer() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = rememberAppPlayIcon(),
                        contentDescription = "恢复画面",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "恢复画面",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            val danmakuToggleInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val danmakuActiveColor = MaterialTheme.colorScheme.primary
            val danmakuInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = danmakuActionLayoutPolicy.toggleTrailingPaddingDp.dp)
                    .height(danmakuActionLayoutPolicy.secondaryControlHeightDp.dp)
                    .clip(RoundedCornerShape(danmakuActionLayoutPolicy.secondaryControlCornerRadiusDp.dp))
                    .background(
                        if (danmakuEnabled) {
                            danmakuActiveColor.copy(alpha = 0.16f)
                        } else {
                            danmakuInactiveColor.copy(alpha = 0.12f)
                        }
                    )
                    .clickable(
                        interactionSource = danmakuToggleInteraction,
                        indication = null,
                        onClick = onDanmakuToggle
                    )
                    .padding(
                        horizontal = danmakuActionLayoutPolicy.toggleHorizontalPaddingDp.dp,
                        vertical = danmakuActionLayoutPolicy.toggleVerticalPaddingDp.dp
                    )
            ) {
                Icon(
                    imageVector = rememberAppCommentIcon(),
                    contentDescription = if (danmakuEnabled) "关闭弹幕" else "开启弹幕",
                    tint = if (danmakuEnabled) danmakuActiveColor else danmakuInactiveColor,
                    modifier = Modifier.size(danmakuActionLayoutPolicy.toggleIconSizeDp.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (danmakuEnabled) "开" else "关",
                    fontSize = danmakuActionLayoutPolicy.toggleTextSizeSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (danmakuEnabled) danmakuActiveColor else danmakuInactiveColor
                )
            }

            AnimatedVisibility(
                visible = shouldShowDanmakuSendInput(isPlayerCollapsed = isPlayerCollapsed),
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(danmakuActionLayoutPolicy.secondaryControlHeightDp.dp)
                        .clip(RoundedCornerShape(danmakuActionLayoutPolicy.secondaryControlCornerRadiusDp.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onDanmakuSendClick)
                        .padding(
                            horizontal = danmakuActionLayoutPolicy.sendHorizontalPaddingDp.dp,
                            vertical = danmakuActionLayoutPolicy.sendVerticalPaddingDp.dp
                        )
                ) {
                    Text(
                        text = danmakuActionLayoutPolicy.sendLabel,
                        fontSize = danmakuActionLayoutPolicy.sendTextSizeSp.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .padding(start = danmakuActionLayoutPolicy.settingsLeadingPaddingDp.dp)
                    .size(danmakuActionLayoutPolicy.settingsButtonSizeDp.dp)
                    .clickable(onClick = onDanmakuSettingsClick),
                shape = RoundedCornerShape(danmakuActionLayoutPolicy.secondaryControlCornerRadiusDp.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = rememberAppSettingsIcon(),
                        contentDescription = "弹幕设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(danmakuActionLayoutPolicy.settingsIconSizeDp.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    }
}

/**
 * 推荐视频标题
 */
@Composable
private fun VideoRecommendationHeader() {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp) // 优化：减少底部间距，使视频卡片更紧凑
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "相关推荐",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

internal fun resolveFirstRelatedItemIndex(hasPages: Boolean): Int {
    return if (hasPages) 3 else 2
}

/**
 * 视频标签行
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoTagsRow(
    tags: List<VideoTag>,
    onTagClick: (String) -> Unit = {}
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(10).forEach { tag ->
            VideoTagChip(
                tagName = tag.tag_name,
                onClick = onTagClick
            )
        }
    }
}

/**
 * 视频标签芯片
 */
@Composable
fun VideoTagChip(
    tagName: String,
    onClick: (String) -> Unit = {}
) {
    Surface(
        onClick = { onClick(tagName) },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = tagName,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .copyOnLongPress(tagName, "标签")
        )
    }
}
