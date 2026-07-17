package com.android.purebilibili.feature.video.screen

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.shouldAllowRuntimeShaderBackedHazeEffect
import com.android.purebilibili.core.ui.rememberAppChevronUpIcon
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.feature.video.share.VideoSharePayload
import com.android.purebilibili.feature.video.share.buildVideoSharePayload
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.ui.components.BottomInputBar
import com.android.purebilibili.feature.video.ui.components.resolveBottomInputBarContentBottomPadding
import com.android.purebilibili.feature.video.ui.components.shouldUseFloatingLiquidBottomInputBar
import com.android.purebilibili.feature.video.usecase.seekPlayerFromUserAction
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.withEngagementUiState
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun VideoDetailPhoneSuccessContentLayer(
    success: VideoPlaybackUiState.Success,
    introListState: LazyListState,
    commentListState: LazyListState,
    videoContentPagerState: PagerState,
    commentState: CommentUiState,
    commentMemberDecorationsEnabled: Boolean,
    viewModel: VideoPlaybackViewModel,
    engagementViewModel: VideoEngagementViewModel,
    commentViewModel: VideoCommentViewModel,
    context: Context,
    sortPreferenceScope: CoroutineScope,
    playerState: VideoPlayerState,
    motionSpec: VideoDetailMotionSpec,
    hazeState: HazeState,
    isTransitionFinished: Boolean,
    isLeaving: Boolean,
    rootTransitionOwnsContentAlpha: Boolean,
    keepContentVisibleAfterBackPreview: Boolean = false,
    shouldShowExternalPlaylistQueueBar: Boolean,
    selectedVideoContentTabIndex: Int,
    useTabletLayout: Boolean,
    isFullscreenMode: Boolean,
    isPortraitFullscreen: Boolean,
    showCommentInput: Boolean,
    isCommentThreadVisible: Boolean,
    showFavoriteFolderDialog: Boolean,
    downloadProgress: Float,
    danmakuEnabledForDetail: Boolean,
    isQuickReturnLimitedForSharedElements: Boolean,
    transitionEnabled: Boolean,
    sourceRouteForSharedElement: String?,
    favoriteFolders: List<FavFolder>,
    isFavoriteFoldersLoading: Boolean,
    selectedFavoriteFolderIds: Set<Long>,
    isSavingFavoriteFolders: Boolean,
    isPlayerCollapsed: Boolean,
    onRestorePlayer: () -> Unit,
    onBgmClick: (BgmInfo) -> Unit,
    homeUpBadgesVisible: Boolean,
    isVideoPlaying: Boolean,
    onSelectedTabChange: (Int) -> Unit,
    onIntroScrollThresholdChange: (Boolean) -> Unit,
    openFavoriteFolders: (VideoFavoriteEntryPoint) -> Unit,
    navigateToUserSpaceFromVideo: (Long) -> Unit,
    navigateToRelatedVideo: (String, android.os.Bundle?) -> Unit,
    openCommentUrl: (String) -> Unit,
    onOpenBilibiliLink: ((String) -> Unit)?,
    onShareVideo: (VideoSharePayload) -> Unit,
    externalPlaylistQueueTitle: String,
    playlistItems: List<PlaylistItem>,
    onShowExternalPlaylistQueueSheet: () -> Unit
) {
    val engagementState by engagementViewModel.uiState.collectAsStateWithLifecycle()
    val engagementSuccess = success.withEngagementUiState(engagementState)
    val relatedVideoTransitionEnabled = LocalSharedTransitionEnabled.current
    // Android 16 ART 曾拒绝校验 VideoDetailScreen 中捕获过多状态的匿名 Compose lambda。
    // 保持这个成功态为命名边界，避免 R8/Compose 再生成单个超大内容块。
    key(success.info.bvid) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSourceCompat(hazeState)
            ) {
                val detailContentRevealEnter = fadeIn(
                    tween(
                        motionSpec.contentRevealFadeDurationMillis,
                        easing = com.android.purebilibili.core.ui.motion.AppMotionEasing.Continuity
                    )
                )
                val detailContentExitFade = fadeOut(
                    tween(
                        durationMillis = 180,
                        delayMillis = 60,
                        easing = com.android.purebilibili.core.ui.motion.AppMotionEasing.Continuity
                    )
                )
                AnimatedVisibility(
                    visible = shouldShowVideoDetailContent(
                        isTransitionFinished = isTransitionFinished,
                        isLeaving = isLeaving,
                        rootTransitionOwnsContentAlpha = rootTransitionOwnsContentAlpha,
                        keepContentVisibleAfterBackPreview = keepContentVisibleAfterBackPreview,
                    ),
                    enter = if (rootTransitionOwnsContentAlpha) {
                        EnterTransition.None
                    } else {
                        detailContentRevealEnter
                    },
                    exit = if (rootTransitionOwnsContentAlpha) {
                        ExitTransition.None
                    } else {
                        detailContentExitFade
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val homeSettings by SettingsManager
                            .getHomeSettings(context)
                            .collectAsStateWithLifecycle(initialValue = HomeSettings())
                        val floatingLiquidBottomInputBar = shouldUseFloatingLiquidBottomInputBar(
                            androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
                        )
                        // Capture scrolling detail content only; BottomInputBar stays outside
                        // so drawBackdrop does not self-sample (same contract as tab chrome).
                        val bottomInputBarBackdrop = rememberLayerBackdrop()
                        val showExternalPlaylistQueueBarOnCurrentTab =
                            shouldShowExternalPlaylistQueueBarOnContentTab(
                                queueAvailable = shouldShowExternalPlaylistQueueBar,
                                selectedTabIndex = selectedVideoContentTabIndex
                            )
                        val showFrozenCommentBar = shouldShowVideoDetailBottomInteractionBar(
                            useTabletLayout = useTabletLayout,
                            selectedTabIndex = selectedVideoContentTabIndex,
                            isFullscreenMode = isFullscreenMode,
                            isPortraitFullscreen = isPortraitFullscreen,
                            isCommentInputVisible = showCommentInput,
                            isCommentThreadVisible = isCommentThreadVisible,
                            isFavoriteFolderDialogVisible = showFavoriteFolderDialog,
                            isExternalPlaylistQueueBarVisible = showExternalPlaylistQueueBarOnCurrentTab
                        )
                        val videoContentBottomPadding = resolveBottomInputBarContentBottomPadding(
                            showBar = showFrozenCommentBar,
                            floatingLiquidGlass = floatingLiquidBottomInputBar,
                            showActionButtonsFallback = shouldShowVideoDetailActionButtons()
                        )
                        val currentPageIndex = success.info.pages
                            .indexOfFirst { it.cid == success.info.cid }
                            .coerceAtLeast(0)

                        Box(
                            modifier = if (floatingLiquidBottomInputBar) {
                                Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(bottomInputBarBackdrop)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            VideoContentSection(
                                info = engagementSuccess.info,
                                introListState = introListState,
                                commentListState = commentListState,
                                pagerState = videoContentPagerState,
                                relatedVideos = success.related,
                                replies = commentState.replies,
                                replyCount = commentState.replyCount,
                                emoteMap = success.emoteMap,
                                isRepliesLoading = commentState.isRepliesLoading,
                                isRepliesEnd = commentState.isRepliesEnd,
                                isLoggedIn = success.isLoggedIn,
                                currentMid = commentState.currentMid,
                                showUpFlag = commentState.showUpFlag,
                                showIdentityDecorations = commentMemberDecorationsEnabled,
                                dissolvingIds = commentState.dissolvingIds,
                                onDeleteComment = { rpid -> commentViewModel.deleteComment(rpid) },
                                onDissolveStart = { rpid -> commentViewModel.startDissolve(rpid) },
                                onCommentLike = commentViewModel::likeComment,
                                likedComments = commentState.likedComments,
                                isFollowing = engagementState.isFollowing,
                                isFavorited = engagementState.isFavorited,
                                isLiked = engagementState.isLiked,
                                coinCount = engagementState.coinCount,
                                currentPageIndex = currentPageIndex,
                                downloadProgress = downloadProgress,
                                isInWatchLater = engagementState.isInWatchLater,
                                followingMids = engagementState.followingMids,
                                videoTags = success.videoTags,
                                sortMode = commentState.sortMode,
                                upOnlyFilter = commentState.upOnlyFilter,
                                onSortModeChange = { mode ->
                                    commentViewModel.setSortMode(mode)
                                    sortPreferenceScope.launch {
                                        com.android.purebilibili.core.store.SettingsManager
                                            .setCommentDefaultSortMode(context, mode.apiMode)
                                    }
                                },
                                onUpOnlyToggle = { commentViewModel.toggleUpOnly() },
                                onFollowClick = { engagementViewModel.toggleFollow() },
                                onFavoriteClick = {
                                    openFavoriteFolders(VideoFavoriteEntryPoint.DetailActionRow)
                                },
                                onLikeClick = { engagementViewModel.toggleLike() },
                                onCoinClick = { engagementViewModel.openCoinDialog() },
                                onTripleClick = { engagementViewModel.doTripleAction() },
                                onPageSelect = { viewModel.switchPage(it) },
                                onUpClick = navigateToUserSpaceFromVideo,
                                onRelatedVideoClick = navigateToRelatedVideo,
                                onSubReplyClick = commentViewModel::openSubReply,
                                onCommentReplyClick = { replyItem ->
                                    viewModel.setReplyingTo(replyItem)
                                    viewModel.showCommentInputDialog()
                                },
                                onLoadMoreReplies = { commentViewModel.loadComments() },
                                onCommentUrlClick = openCommentUrl,
                                onDescriptionUrlClick = onOpenBilibiliLink,
                                onReportComment = commentViewModel::reportComment,
                                onToggleTopComment = commentViewModel::toggleTopComment,
                                onDownloadClick = { viewModel.openDownloadDialog() },
                                onWatchLaterClick = { engagementViewModel.toggleWatchLater() },
                                onShareClick = {
                                    onShareVideo(
                                        buildVideoSharePayload(
                                            title = success.info.title,
                                            bvid = success.info.bvid,
                                            coverUrl = success.info.pic
                                        )
                                    )
                                },
                                onTimestampClick = { positionMs ->
                                    seekPlayerFromUserAction(playerState.player, positionMs)
                                },
                                onDanmakuSendClick = {
                                    android.util.Log.d("VideoDetailScreen", "Danmaku send clicked")
                                    viewModel.showDanmakuSendDialog()
                                },
                                danmakuEnabled = danmakuEnabledForDetail,
                                onDanmakuToggle = {
                                    val newValue = !danmakuEnabledForDetail
                                    sortPreferenceScope.launch {
                                        com.android.purebilibili.core.store.SettingsManager
                                            .setDanmakuEnabled(
                                                context,
                                                newValue,
                                                com.android.purebilibili.core.store.DanmakuSettingsScope.PORTRAIT
                                            )
                                    }
                                },
                                transitionEnabled = transitionEnabled,
                                relatedVideoTransitionEnabled = relatedVideoTransitionEnabled,
                                isQuickReturnLimitedForSharedElements = isQuickReturnLimitedForSharedElements,
                                sourceRouteForSharedElement = sourceRouteForSharedElement,
                                favoriteFolderDialogVisible = showFavoriteFolderDialog,
                                favoriteFolders = favoriteFolders,
                                isFavoriteFoldersLoading = isFavoriteFoldersLoading,
                                onFavoriteLongClick = { viewModel.showFavoriteFolderDialog() },
                                selectedFavoriteFolderIds = selectedFavoriteFolderIds,
                                isSavingFavoriteFolders = isSavingFavoriteFolders,
                                onFavoriteFolderToggle = { folder ->
                                    viewModel.toggleFavoriteFolderSelection(folder)
                                },
                                onSaveFavoriteFolders = { viewModel.saveFavoriteFolderSelection() },
                                onDismissFavoriteFolderDialog = {
                                    viewModel.dismissFavoriteFolderDialog()
                                },
                                onCreateFavoriteFolder = { title, intro, isPrivate ->
                                    viewModel.createFavoriteFolder(title, intro, isPrivate)
                                },
                                isPlayerCollapsed = isPlayerCollapsed,
                                onRestorePlayer = onRestorePlayer,
                                aiSummary = success.aiSummary,
                                aiSummaryPrompt = success.aiSummaryPrompt,
                                onRetryAiSummary = { viewModel.retryAiSummary() },
                                onCreateNoteDraftFromAiSummary = {
                                    viewModel.createVideoNoteDraftFromAiSummary()
                                },
                                videoNoteState = success.videoNoteState,
                                onOpenVideoNoteEditor = { viewModel.openVideoNoteEditor() },
                                onCloseVideoNoteEditor = { viewModel.closeVideoNoteEditor() },
                                onVideoNoteDocumentChange = {
                                    viewModel.updateVideoNoteEditorDocument(it)
                                },
                                onInsertVideoNoteTimestamp = {
                                    viewModel.insertCurrentPlaybackTimestampIntoNote()
                                },
                                onVideoNoteTimestampClick = { positionMs -> viewModel.seekTo(positionMs) },
                                onSaveVideoNote = { viewModel.saveVideoNote(it) },
                                onDeleteVideoNote = { viewModel.deleteVideoNote() },
                                onRetryVideoNote = { viewModel.retryVideoNote() },
                                onPublicVideoNoteClick = { _, url ->
                                    if (url.isNotBlank()) onOpenBilibiliLink?.invoke(url)
                                },
                                bgmInfo = success.bgmInfo,
                                bgmInfoList = success.bgmInfoList,
                                onBgmClick = onBgmClick,
                                onlineCount = success.onlineCount,
                                ownerFollowerCount = success.ownerFollowerCount,
                                ownerVideoCount = success.ownerVideoCount,
                                showUpBadge = homeUpBadgesVisible,
                                showInteractionActions = shouldShowVideoDetailActionButtons(),
                                isVideoPlaying = isVideoPlaying,
                                onSelectedTabChange = onSelectedTabChange,
                                onIntroScrollThresholdChange = onIntroScrollThresholdChange,
                                bottomContentPadding = videoContentBottomPadding
                            )
                        }

                        if (showFrozenCommentBar) {
                            BottomInputBar(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                isLiked = engagementState.isLiked,
                                isFavorited = engagementState.isFavorited,
                                isCoined = engagementState.coinCount > 0,
                                onLikeClick = { engagementViewModel.toggleLike() },
                                onFavoriteClick = {
                                    openFavoriteFolders(VideoFavoriteEntryPoint.BottomInputBar)
                                },
                                onCoinClick = { engagementViewModel.openCoinDialog() },
                                onShareClick = {
                                    onShareVideo(
                                        buildVideoSharePayload(
                                            title = success.info.title,
                                            bvid = success.info.bvid,
                                            coverUrl = success.info.pic
                                        )
                                    )
                                },
                                onCommentClick = {
                                    android.util.Log.d("VideoDetailScreen", "Comment input clicked")
                                    viewModel.openRootCommentComposer()
                                },
                                backdrop = if (floatingLiquidBottomInputBar) {
                                    bottomInputBarBackdrop
                                } else {
                                    null
                                }
                            )
                        }

                        if (showExternalPlaylistQueueBarOnCurrentTab) {
                            ExternalPlaylistQueueCollapsedBar(
                                title = externalPlaylistQueueTitle,
                                videoCount = playlistItems.size,
                                onClick = onShowExternalPlaylistQueueSheet,
                                hazeState = hazeState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun ExternalPlaylistQueueCollapsedBar(
    title: String,
    videoCount: Int,
    onClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val shape = AppShapes.borderedContainer(ContainerLevel.Dialog)
    val useHazeEffect = shouldAllowRuntimeShaderBackedHazeEffect(Build.VERSION.SDK_INT)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (useHazeEffect) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.ultraThin()
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        shape = shape,
        color = AppSurfaceTokens.cardContainer().copy(alpha = 0.74f),
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${videoCount}个视频",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = rememberAppChevronUpIcon(),
                contentDescription = "展开${title}队列",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
