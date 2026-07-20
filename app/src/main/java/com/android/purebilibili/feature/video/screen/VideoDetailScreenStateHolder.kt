// 文件路径: feature/video/screen/VideoDetailScreen.kt
package com.android.purebilibili.feature.video.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.activity.compose.BackHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.layout.ContentScale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.android.purebilibili.data.model.response.BgmInfo
import com.android.purebilibili.data.model.CommentFraudStatus
import com.android.purebilibili.data.repository.resolveCommentFraudLightMessage
import com.android.purebilibili.data.repository.shouldShowCommentFraudResultDialog
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.Player
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import com.android.purebilibili.core.theme.LocalUiPreset
//  已改用 MaterialTheme.colorScheme.primary

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.VideoTag
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.ViewPoint
import com.android.purebilibili.feature.common.resolveIndexedVideoLazyKey
// Refactored UI components
import com.android.purebilibili.feature.video.ui.section.VideoTitleSection
import com.android.purebilibili.feature.video.ui.section.VideoTitleWithDesc
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import com.android.purebilibili.feature.video.ui.section.DescriptionSection
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.section.ActionButton
import com.android.purebilibili.feature.video.ui.components.RelatedVideosHeader
import com.android.purebilibili.feature.video.ui.components.RelatedVideoItem
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.ui.components.CollectionRow
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
// Imports for moved classes
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoComposerViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementEvent
import com.android.purebilibili.feature.video.viewmodel.VideoSupplementViewModel
import com.android.purebilibili.feature.video.viewmodel.toEngagementSeed
import com.android.purebilibili.feature.video.viewmodel.toSupplementSeed
import com.android.purebilibili.feature.video.viewmodel.QualitySwitchFailureDialogState
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.state.rememberVideoPlayerState
import com.android.purebilibili.feature.video.state.shouldReuseMiniPlayerAtEntry
import com.android.purebilibili.feature.video.ui.section.VideoPlayerSection
import com.android.purebilibili.feature.video.ui.section.shouldKeepVideoPlaybackAwake
import com.android.purebilibili.feature.video.ui.components.ReplyHeader
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.ui.components.CommentFraudResultDialog
import com.android.purebilibili.feature.video.ui.components.VideoCommentSheetHost

import com.android.purebilibili.feature.video.viewmodel.CommentSortMode  //  新增
import com.android.purebilibili.feature.video.ui.components.LikeBurstAnimation
import com.android.purebilibili.feature.video.ui.components.TripleSuccessAnimation
import com.android.purebilibili.feature.video.ui.components.VideoDetailSkeleton
import com.android.purebilibili.feature.video.ui.components.VideoActionFeedbackHost
import com.android.purebilibili.feature.video.subtitle.SubtitleAutoPreference
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.resolveSubtitleDisplayModePreference
import com.android.purebilibili.feature.video.progress.PbpProgressData
import com.android.purebilibili.feature.video.usecase.playPlayerFromUserAction
import com.android.purebilibili.feature.video.usecase.seekPlayerFromUserAction
import com.android.purebilibili.feature.video.policy.reduceVideoDetailPostScroll
import com.android.purebilibili.feature.video.policy.reduceVideoDetailPreScroll
import com.android.purebilibili.feature.video.policy.resolveVideoDetailCollapseProgress
import com.android.purebilibili.feature.video.subtitle.resolveSubtitlePreferenceSession
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionPlaybackIntent
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionEnterEasing
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionReturnEasing
import com.android.purebilibili.core.ui.transition.resolveVideoSharedCoverCacheKey
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionPlaybackIntent
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionSourceCornerDp
import com.android.purebilibili.core.ui.transition.resolveVideoSharedTransitionVisualSpec
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellContainerTransform
import com.android.purebilibili.core.ui.transition.VideoCardShellSharedBoundsRole
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.ui.transition.videoSharedElementBoundsTransformSpec
import com.android.purebilibili.core.ui.rememberAppCollectionIcon
import com.android.purebilibili.core.ui.rememberAppDownloadIcon
import com.android.purebilibili.core.ui.rememberAppMusicIcon
import com.android.purebilibili.core.ui.rememberAppPhotoIcon
import com.android.purebilibili.core.ui.rememberAppPlayIcon
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.player.PlaybackService
import com.android.purebilibili.feature.video.player.PlaylistItem
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.player.PlaylistUiState
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.core.ui.performance.TrackJankStateFlag
// 📱 [新增] 竖屏全屏
import com.android.purebilibili.feature.video.ui.overlay.PortraitFullscreenOverlay
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.core.ui.blur.shouldAllowRuntimeShaderBackedHazeEffect
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.IOSModalBottomSheet
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.android.purebilibili.feature.video.ui.components.DanmakuContextMenu
import com.android.purebilibili.feature.video.ui.components.DanmakuBlockActionTarget
import com.android.purebilibili.feature.video.ui.components.resolveDanmakuBlockActionFeedbackMessage
import com.android.purebilibili.feature.video.danmaku.appendDanmakuKeywordBlockRule
import com.android.purebilibili.feature.video.danmaku.appendDanmakuUserHashBlockRule
import com.android.purebilibili.feature.video.ui.components.InteractiveChoiceOverlay
import com.android.purebilibili.feature.video.ui.feedback.VideoFeedbackAnchor
import com.android.purebilibili.feature.video.ui.feedback.TripleCelebrationPlacement
import com.android.purebilibili.feature.video.ui.feedback.resolveQualityReminderPlacement
import com.android.purebilibili.feature.video.ui.feedback.resolveTripleCelebrationPlacement
import com.android.purebilibili.feature.video.ui.feedback.resolveVideoFeedbackPlacement
import com.android.purebilibili.feature.video.ui.section.resolveForcedReturnCoverSharedElementSourceRoute
import com.android.purebilibili.feature.video.share.VideoSharePayload
import com.android.purebilibili.feature.video.share.VideoShareSheet
import com.android.purebilibili.feature.video.viewmodel.PlayerToastPresentation
import kotlin.math.abs
import kotlin.math.roundToInt

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun VideoDetailScreenStateHolder(
    bvid: String,
    cid: Long = 0L,
    coverUrl: String = "",
    startInFullscreen: Boolean = false,
    startAudioFromRoute: Boolean = false,
    autoEnterPortraitFromRoute: Boolean = false,
    initialVerticalFromRoute: Boolean = false,
    resumePositionMsFromRoute: Long = 0L,
    openCommentRootRpidFromRoute: Long = 0L,
    openCommentTargetRpidFromRoute: Long = 0L,
    sourceRouteForSharedElement: String? = null,
    keepLoadedContentForBackPreview: Boolean = false,
    bindLivePlayerForBackPreview: Boolean = keepLoadedContentForBackPreview,
    predictiveBackCancelRecoveryGeneration: Int = 0,
    isReturningFromDetail: Boolean = false,
    isQuickReturningFromDetail: Boolean = false,
    onMarkReturningFromDetail: () -> Unit = {},
    onClearReturningFromDetail: () -> Unit = {},
    transitionEnabled: Boolean = false,
    transitionEnterDurationMillis: Int = 320,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = onBack,
    onNavigateToAudioMode: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onSearchKeywordClick: (String) -> Unit = {},
    onOpenBilibiliLink: ((String) -> Unit)? = null,
    onVideoClick: (String, android.os.Bundle?) -> Unit,
    onUpClick: (Long) -> Unit = {},
    miniPlayerManager: MiniPlayerManager? = null,
    isInPipMode: Boolean = false,
    isVisible: Boolean = true,
    viewModel: VideoPlaybackViewModel = viewModel(),
    engagementViewModel: VideoEngagementViewModel = viewModel(),
    composerViewModel: VideoComposerViewModel = viewModel(),
    supplementViewModel: VideoSupplementViewModel = viewModel(),
    commentViewModel: VideoCommentViewModel = viewModel(),
    onBgmClick: (BgmInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val homeUpBadgesVisible by com.android.purebilibili.core.store.SettingsManager
        .getHomeUpBadgesVisible(context)
        .collectAsStateWithLifecycle(initialValue = true
        )
    val motionSpec = remember(transitionEnterDurationMillis) {
        resolveVideoDetailMotionSpec(transitionEnterDurationMillis)
    }
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val homeSharedTransitionMotionSpec = remember(
        sourceRouteForSharedElement,
        transitionEnabled,
        sharedTransitionSpeedSettings,
        isQuickReturningFromDetail,
    ) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRouteForSharedElement,
            transitionEnabled = transitionEnabled,
            speedSettings = sharedTransitionSpeedSettings,
            isQuickReturn = isQuickReturningFromDetail,
        )
    }
    val sharedTransitionSourceCornerDp = remember(sourceRouteForSharedElement) {
        CardPositionManager.lastClickedVideoSourceCornerDp
            ?: resolveVideoSharedTransitionSourceCornerDp(sourceRouteForSharedElement)
    }
    val videoSharedPlaybackIntent = remember(context, startAudioFromRoute) {
        resolveVideoSharedTransitionPlaybackIntent(
            clickToPlayEnabled = com.android.purebilibili.core.store.SettingsManager.getClickToPlaySync(context),
            forceImmediatePlayback = startAudioFromRoute
        )
    }
    val routeSheetMotion = remember(sourceRouteForSharedElement, transitionEnabled) {
        resolveVideoDetailRouteSheetMotion(
            sourceRoute = sourceRouteForSharedElement,
            transitionEnabled = transitionEnabled
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val subjectSnapshot by viewModel.subjectSnapshot.collectAsStateWithLifecycle()
    val engagementState by engagementViewModel.uiState.collectAsStateWithLifecycle()
    val favoriteFolderSaveEvent by viewModel.favoriteFolderSaveEvent.collectAsStateWithLifecycle()
    val playbackActions = remember(viewModel, context) {
        VideoDetailPlaybackActions(
            changeQuality = viewModel::changeQuality,
            reloadVideo = viewModel::reloadVideo,
            switchCdn = viewModel::switchCdn,
            switchCdnTo = viewModel::switchCdnTo,
            probeCdnCandidates = viewModel::probeCurrentCdnCandidates,
            setAudioMode = viewModel::setAudioMode,
            setSleepTimer = viewModel::setSleepTimer,
            switchPage = viewModel::switchPage,
            openDownloadDialog = viewModel::openDownloadDialog,
            showDanmakuSendDialog = viewModel::showDanmakuSendDialog,
            skipSponsorSegment = viewModel::skipCurrentSponsorSegment,
            dismissSponsorSkipButton = viewModel::dismissSponsorSkipButton,
            notifyExplicitSeek = viewModel::notifyPluginsOfExplicitSeek,
            setVideoCodec = viewModel::setVideoCodec,
            setVideoSecondCodec = viewModel::setVideoSecondCodec,
            setAudioQuality = viewModel::setAudioQuality,
            applyPlaybackSpeed = viewModel::applyPlaybackSpeedFromUi,
            changeAudioLanguage = viewModel::changeAudioLanguage,
            saveCover = { viewModel.saveCover(context) },
            downloadAudio = { viewModel.downloadAudio(context) },
            selectSubtitleTrack = viewModel::selectSubtitleTrack,
            showFavoriteFolderDialog = viewModel::showFavoriteFolderDialog,
            toggleFavoriteFolderSelection = viewModel::toggleFavoriteFolderSelection,
            saveFavoriteFolderSelection = viewModel::saveFavoriteFolderSelection,
            dismissFavoriteFolderDialog = viewModel::dismissFavoriteFolderDialog,
            createFavoriteFolder = viewModel::createFavoriteFolder,
            retryAiSummary = viewModel::retryAiSummary,
            createVideoNoteDraftFromAiSummary = viewModel::createVideoNoteDraftFromAiSummary,
            openVideoNoteEditor = viewModel::openVideoNoteEditor,
            closeVideoNoteEditor = viewModel::closeVideoNoteEditor,
            updateVideoNoteEditorDocument = viewModel::updateVideoNoteEditorDocument,
            insertCurrentPlaybackTimestampIntoNote = viewModel::insertCurrentPlaybackTimestampIntoNote,
            seekTo = viewModel::seekTo,
            saveVideoNote = viewModel::saveVideoNote,
            deleteVideoNote = viewModel::deleteVideoNote,
            retryVideoNote = viewModel::retryVideoNote,
            openRootCommentComposer = viewModel::openRootCommentComposer,
            replyTo = {
                viewModel.setReplyingTo(it)
                viewModel.showCommentInputDialog()
            },
            markVideoNotInterested = viewModel::markVideoNotInterested
        )
    }
    val engagementActions = remember(engagementViewModel) {
        VideoDetailEngagementActions(
            toggleFollow = engagementViewModel::toggleFollow,
            toggleFavorite = engagementViewModel::toggleFavorite,
            toggleLike = engagementViewModel::toggleLike,
            openCoinDialog = engagementViewModel::openCoinDialog,
            doTripleAction = engagementViewModel::doTripleAction,
            toggleWatchLater = engagementViewModel::toggleWatchLater
        )
    }
    val commentActions = remember(commentViewModel) {
        VideoDetailCommentActions(
            loadComments = commentViewModel::loadComments,
            setSortMode = commentViewModel::setSortMode,
            toggleUpOnly = commentViewModel::toggleUpOnly,
            deleteComment = commentViewModel::deleteComment,
            startDissolve = commentViewModel::startDissolve,
            loadMoreSubReplies = commentViewModel::loadMoreSubReplies,
            openSubReply = commentViewModel::openSubReply,
            openSubReplyConversation = commentViewModel::openSubReplyConversation,
            closeSubReplyConversation = commentViewModel::closeSubReplyConversation,
            closeSubReply = commentViewModel::closeSubReply,
            startSubDissolve = commentViewModel::startSubDissolve,
            deleteSubComment = commentViewModel::deleteSubComment,
            likeComment = commentViewModel::likeComment,
            reportComment = { rpid, reason -> commentViewModel.reportComment(rpid, reason) },
            toggleTopComment = commentViewModel::toggleTopComment
        )
    }
    VideoDetailDomainEffects(
        context = context,
        isVisible = isVisible,
        uiState = uiState,
        subjectSnapshot = subjectSnapshot,
        favoriteFolderSaveEvent = favoriteFolderSaveEvent,
        playbackViewModel = viewModel,
        engagementViewModel = engagementViewModel,
        composerViewModel = composerViewModel,
        supplementViewModel = supplementViewModel,
    )
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val presentationState = rememberVideoDetailPresentationState(
        routeBvid = bvid,
        initialCid = 0L,
        initialPortraitFullscreen = shouldStartInPortraitFullscreenFromRouteHint(
            autoEnterPortraitFromRoute = autoEnterPortraitFromRoute,
            startAudioFromRoute = startAudioFromRoute,
            initialVerticalFromRoute = initialVerticalFromRoute,
        ),
        initialPipMode = isInPipMode,
    )
    var isNavigatingToVideo by presentationState.navigatingToVideoState
    var isNavigatingToAudioMode by presentationState.navigatingToAudioModeState
    var isNavigatingToMiniMode by presentationState.navigatingToMiniModeState
    var hasAutoEnteredAudioMode by rememberSaveable { mutableStateOf(false) }
    var hasAutoEnteredPortraitFromRoute by rememberSaveable(bvid) { mutableStateOf(false) }
    var hasHandledCommentRootFromRoute by rememberSaveable(
        bvid,
        openCommentRootRpidFromRoute,
        openCommentTargetRpidFromRoute
    ) { mutableStateOf(false) }
    // 🔄 [Seamless Playback] Internal BVID state to support seamless switching in portrait mode
    var currentBvid by presentationState.currentBvidState
    var currentBvidCid by presentationState.currentCidState
    var isPipMode by presentationState.pipModeState
    var isPortraitFullscreen by presentationState.portraitFullscreenState
    var selectedVideoContentTabIndex by presentationState.selectedTabIndexState
    val playbackTargetCid = resolveVideoDetailPlaybackTargetCid(
        routeBvid = bvid,
        routeCid = cid,
        currentBvid = currentBvid,
        currentBvidCid = currentBvidCid
    )
    val introListState = rememberSaveable(currentBvid, saver = LazyListState.Saver) {
        LazyListState()
    }
    val commentListState = rememberSaveable(currentBvid, saver = LazyListState.Saver) {
        LazyListState()
    }
    val videoContentPagerState: PagerState = key(currentBvid) {
        rememberPagerState(pageCount = { 2 })
    }

    val entryRootAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val entryRootSharedTransitionScope = LocalSharedTransitionScope.current
    val detailShellSharedBoundsEnabledForEntry = shouldUseVideoCardShellContainerTransform(
        sourceRoute = sourceRouteForSharedElement,
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = entryRootSharedTransitionScope != null,
        hasAnimatedVisibilityScope = entryRootAnimatedVisibilityScope != null
    )
    val reuseFromMiniPlayerAtEntry = remember(currentBvid, playbackTargetCid, miniPlayerManager) {
        val manager = miniPlayerManager
        if (manager == null) {
            false
        } else {
            shouldReuseMiniPlayerAtEntry(
                isMiniPlayerActive = manager.isActive,
                miniPlayerBvid = manager.currentBvid,
                miniPlayerCid = manager.currentCid,
                hasMiniPlayerInstance = manager.player != null,
                requestBvid = currentBvid,
                requestCid = playbackTargetCid
            )
        }
    }
    val deferVideoDetailEntryLoad = shouldDeferVideoDetailLoadUntilEntryTransitionFinished(
        transitionEnabled = transitionEnabled,
        detailShellSharedBoundsEnabled = detailShellSharedBoundsEnabledForEntry,
        reuseFromMiniPlayerAtEntry = reuseFromMiniPlayerAtEntry,
        isReturningFromDetail = isReturningFromDetail,
    )
    val entryTransitionFinished = rememberVideoDetailEntryTransitionFinished(
        deferLoad = deferVideoDetailEntryLoad,
        sharedTransitionScope = entryRootSharedTransitionScope,
        animatedVisibilityScope = entryRootAnimatedVisibilityScope,
        fallbackDurationMillis = homeSharedTransitionMotionSpec.durationMillis,
    )

    fun markSecondaryNavigationLeave(expectedBvid: String = currentBvid) {
        miniPlayerManager?.markLeavingByNavigation(expectedBvid = expectedBvid)
    }

    val navigateToUserSpaceFromVideo: (Long) -> Unit = { mid ->
        onUpClick(mid)
    }

    val navigateToSearchFromVideo: () -> Unit = {
        onNavigateToSearch()
    }

    val navigateToSearchKeywordFromVideo: (String) -> Unit = { keyword ->
        onSearchKeywordClick(keyword)
    }

    fun switchVideoInCurrentDetailPage(
        targetBvid: String,
        targetCid: Long,
        autoPlay: Boolean = true
    ) {
        val normalizedBvid = targetBvid.trim()
        if (normalizedBvid.isBlank()) return
        val safeCid = targetCid.coerceAtLeast(0L)
        val success = uiState as? VideoPlaybackUiState.Success
        if (success?.info?.bvid == normalizedBvid && (safeCid <= 0L || success.info.cid == safeCid)) {
            return
        }
        presentationState.switchVideo(normalizedBvid, safeCid)
        viewModel.loadVideo(
            bvid = normalizedBvid,
            cid = safeCid,
            autoPlay = autoPlay
        )
    }

    val navigateToRelatedVideo = remember(onVideoClick, miniPlayerManager, uiState, currentBvid) {
        { targetBvid: String, options: android.os.Bundle? ->
            val success = uiState as? VideoPlaybackUiState.Success
            val explicitCid = options?.getLong(VIDEO_NAV_TARGET_CID_KEY) ?: 0L
            val resolvedCid = resolveNavigationTargetCid(
                targetBvid = targetBvid,
                explicitCid = explicitCid,
                relatedVideos = success?.related.orEmpty(),
                ugcSeason = success?.info?.ugc_season
            )
            com.android.purebilibili.core.util.Logger.d(
                "VideoDetailScreen",
                "navigateToRelatedVideo: current=${success?.info?.bvid ?: "unknown"} target=$targetBvid explicitCid=$explicitCid resolvedCid=$resolvedCid"
            )
            if (
                shouldSwitchCollectionVideoInsideCurrentDetailPage(
                    targetBvid = targetBvid,
                    currentBvid = success?.info?.bvid ?: currentBvid,
                    ugcSeason = success?.info?.ugc_season
                )
            ) {
                miniPlayerManager?.isNavigatingToVideo = false
                switchVideoInCurrentDetailPage(
                    targetBvid = targetBvid,
                    targetCid = resolvedCid,
                    autoPlay = true
                )
            } else {
                presentationState.markNavigatingToVideo()
                miniPlayerManager?.isNavigatingToVideo = true
                markSecondaryNavigationLeave(expectedBvid = success?.info?.bvid ?: currentBvid)
                val navOptions = android.os.Bundle(options ?: android.os.Bundle.EMPTY)
                if (resolvedCid > 0L) {
                    navOptions.putLong(VIDEO_NAV_TARGET_CID_KEY, resolvedCid)
                }
                onVideoClick(targetBvid, navOptions)
            }
        }
    }

    LaunchedEffect(bvid, cid) {
        com.android.purebilibili.core.util.Logger.d(
            "VideoDetailScreen",
            "SUB_DBG screen entry args: bvid=$bvid, cid=$cid"
        )
    }

    val openCommentUrl: (String) -> Unit = openCommentUrl@{ rawUrl ->
        val url = rawUrl.trim()
        if (url.isEmpty()) return@openCommentUrl
        if (onOpenBilibiliLink != null) {
            onOpenBilibiliLink(url)
            return@openCommentUrl
        }

        when (val target = resolveCommentUrlNavigationTarget(url)) {
            is CommentUrlNavigationTarget.Video -> {
                navigateToRelatedVideo(target.videoId, null)
                return@openCommentUrl
            }

            is CommentUrlNavigationTarget.Search -> {
                navigateToSearchKeywordFromVideo(target.keyword)
                return@openCommentUrl
            }

            is CommentUrlNavigationTarget.Space -> {
                navigateToUserSpaceFromVideo(target.mid)
                return@openCommentUrl
            }

            null -> Unit
        }

        runCatching { uriHandler.openUri(url) }
    }

    // 🎭 [性能优化] 进场视觉帧 + 重型组件延迟加载
    // 卡片 shell morph 进场时：loadVideo 与 Tab 内容均等待 entryTransitionFinished，
    // 但不再叠加二级 fadeIn/slide，避免与 sharedBounds 冲突。
    val shellSharedBoundsLikely = transitionEnabled && !sourceRouteForSharedElement.isNullOrBlank()
    val entryVisualEnabled = transitionEnabled && !deferVideoDetailEntryLoad && !shellSharedBoundsLikely
    // 注意：不要把 entryTransitionFinished 放进 remember key，否则返回时 finished 回抖会重建状态并隐藏内容。
    var isTransitionFinished by remember(deferVideoDetailEntryLoad, entryVisualEnabled, shellSharedBoundsLikely) {
        mutableStateOf(
            when {
                deferVideoDetailEntryLoad -> entryTransitionFinished
                !transitionEnabled || shellSharedBoundsLikely -> true
                else -> false
            }
        )
    }
    val entryVisualProgress = remember(entryVisualEnabled) {
        Animatable(if (entryVisualEnabled) 0f else 1f)
    }

    LaunchedEffect(
        deferVideoDetailEntryLoad,
        entryTransitionFinished,
        entryVisualEnabled,
        motionSpec.entryPhaseDurationMillis,
        shellSharedBoundsLikely,
        transitionEnabled
    ) {
        when {
            deferVideoDetailEntryLoad -> {
                // 只允许 true 锁存：相关推荐返回的二次 morph 不得把内容区重新藏起来。
                if (entryTransitionFinished) {
                    entryVisualProgress.snapTo(1f)
                    isTransitionFinished = true
                } else if (!isTransitionFinished) {
                    entryVisualProgress.snapTo(0f)
                }
            }

            !transitionEnabled || shellSharedBoundsLikely -> {
                entryVisualProgress.snapTo(1f)
                isTransitionFinished = true
            }

            else -> {
                if (isTransitionFinished) {
                    entryVisualProgress.snapTo(1f)
                    return@LaunchedEffect
                }
                entryVisualProgress.snapTo(0f)
                entryVisualProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = motionSpec.entryPhaseDurationMillis,
                        easing = FastOutSlowInEasing
                    )
                )
                isTransitionFinished = true
            }
        }
    }

    //  监听评论状态
    val commentState by commentViewModel.commentState.collectAsStateWithLifecycle()
    val subReplyState by commentViewModel.subReplyState.collectAsStateWithLifecycle()

    LaunchedEffect(
        openCommentRootRpidFromRoute,
        openCommentTargetRpidFromRoute,
        commentState.replies,
        commentState.isRepliesLoading,
        subReplyState.visible
    ) {
        if (openCommentRootRpidFromRoute <= 0L || hasHandledCommentRootFromRoute || subReplyState.visible) {
            return@LaunchedEffect
        }

        val rootReply = commentState.replies.firstOrNull { it.rpid == openCommentRootRpidFromRoute }
        if (rootReply != null) {
            commentViewModel.openSubReply(rootReply, openCommentTargetRpidFromRoute)
            hasHandledCommentRootFromRoute = true
        } else if (!commentState.isRepliesLoading) {
            val openStarted = commentViewModel.openSubReplyFromRoute(
                rootReplyId = openCommentRootRpidFromRoute,
                targetReplyId = openCommentTargetRpidFromRoute
            )
            if (openStarted) {
                hasHandledCommentRootFromRoute = true
            }
        }
    }
    val commentDefaultSortMode by com.android.purebilibili.core.store.SettingsManager
        .getCommentDefaultSortMode(context)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.SettingsManager.getCommentDefaultSortModeSync(context),
            lifecycle = lifecycleOwner.lifecycle
        )
    val commentFraudDetectionEnabled by com.android.purebilibili.core.store.SettingsManager
        .getCommentFraudDetectionEnabled(context)
        .collectAsStateWithLifecycle(
            initialValue = true,
            lifecycle = lifecycleOwner.lifecycle
        )
    val commentMemberDecorationsEnabled by com.android.purebilibili.core.store.SettingsManager
        .getCommentMemberDecorationsEnabled(context)
        .collectAsStateWithLifecycle(
            initialValue = false,
            lifecycle = lifecycleOwner.lifecycle
        )
    val homeSettings by com.android.purebilibili.core.store.SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.HomeSettings(),
            lifecycle = lifecycleOwner.lifecycle
        )
    val tabletCommentPanelWidthPreset by com.android.purebilibili.core.store.SettingsManager
        .getTabletCommentPanelWidthPreset(context)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.TabletCommentPanelWidthPreset.STANDARD,
            lifecycle = lifecycleOwner.lifecycle
        )
    val videoAiSummaryEntryEnabled by com.android.purebilibili.core.store.SettingsManager
        .getVideoAiSummaryEntryEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true, lifecycle = lifecycleOwner.lifecycle)
    val videoNoteEnabled by com.android.purebilibili.core.store.SettingsManager
        .getVideoNoteEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true, lifecycle = lifecycleOwner.lifecycle)
    val videoNoteDefaultCollapsed by com.android.purebilibili.core.store.SettingsManager
        .getVideoNoteDefaultCollapsed(context)
        .collectAsStateWithLifecycle(initialValue = false, lifecycle = lifecycleOwner.lifecycle)
    val preferredCommentSortMode = remember(commentDefaultSortMode) {
        CommentSortMode.fromApiMode(commentDefaultSortMode)
    }
    VideoDetailCommentFraudOverlayAdapter(
        context = context,
        playbackViewModel = viewModel,
        commentViewModel = commentViewModel,
        aid = (uiState as? VideoPlaybackUiState.Success)?.info?.aid,
        fraudDetectionEnabled = commentFraudDetectionEnabled,
    )
    val sortPreferenceScope = rememberCoroutineScope()
    val danmakuEnabledForDetail by com.android.purebilibili.core.store.SettingsManager
        .getDanmakuEnabled(
            context,
            com.android.purebilibili.core.store.DanmakuSettingsScope.PORTRAIT
        )
        .collectAsStateWithLifecycle(
            initialValue = true,
            lifecycle = lifecycleOwner.lifecycle
        )
    val showFavoriteFolderDialog by viewModel.favoriteFolderDialogVisible.collectAsStateWithLifecycle()
    val showCommentInput by viewModel.showCommentDialog.collectAsStateWithLifecycle()
    // [Blur] Haze State
    val hazeState = rememberRecoverableHazeState()

    val sponsorSegment by viewModel.currentSponsorSegment.collectAsStateWithLifecycle()
    val showSponsorSkipButton by viewModel.showSkipButton.collectAsStateWithLifecycle()

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val interactiveChoicePanel by viewModel.interactiveChoicePanel.collectAsStateWithLifecycle()

    // 📐 [大屏适配] 仅 Expanded 才启用平板分栏布局
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    val horizontalAdaptationEnabled by com.android.purebilibili.core.store.SettingsManager
        .getHorizontalAdaptationEnabled(context)
        .collectAsStateWithLifecycle(
            initialValue = windowSizeClass.isTabletDevice,
            lifecycle = lifecycleOwner.lifecycle
        )
    val hideVideoPageStatusBar by com.android.purebilibili.core.store.SettingsManager
        .getHideVideoPageStatusBar(context)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.SettingsManager
                .getHideVideoPageStatusBarSync(context),
            lifecycle = lifecycleOwner.lifecycle
        )
    val useTabletLayout = shouldUseTabletVideoLayout(
        isExpandedScreen = windowSizeClass.isExpandedScreen,
        isTabletDevice = windowSizeClass.isTabletDevice
    ) && horizontalAdaptationEnabled

    // 🔧 [修复] 追踪用户是否主动请求全屏（点击全屏按钮）
    // 使用 rememberSaveable 确保状态在横竖屏切换时保持
    var userRequestedFullscreen by rememberSaveable { mutableStateOf(false) }
    var manualPortraitHoldActive by rememberSaveable { mutableStateOf(false) }
    val activity = remember { context.findActivity() }
    val isActivityInMultiWindowMode = activity?.let { host ->
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && host.isInMultiWindowMode
    } ?: false

    // 📐 全屏模式逻辑：
    // - 手机：横屏时自动进入全屏
    // - 平板：仅用户主动切换全屏
    val fullscreenMode by com.android.purebilibili.core.store.SettingsManager
        .getFullscreenMode(context)
        .collectAsStateWithLifecycle(
            initialValue = com.android.purebilibili.core.store.FullscreenMode.AUTO,
            lifecycle = lifecycleOwner.lifecycle
        )
    val prefersManualFullscreenMode = remember(fullscreenMode) {
        fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.NONE ||
            fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL
    }
    val isOrientationDrivenFullscreen = !prefersManualFullscreenMode &&
        shouldUseOrientationDrivenFullscreen(
        isCompactDevice = windowSizeClass.isCompactDevice
    )
    val isFullscreenMode = resolveVideoDetailFullscreenMode(
        isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
        isLandscape = isLandscape,
        userRequestedFullscreen = userRequestedFullscreen,
        isInMultiWindowMode = isActivityInMultiWindowMode
    )
    ManualFullscreenRequestLifecycleEffect(
        manualFullscreenRequested = userRequestedFullscreen,
        isFullscreenMode = isFullscreenMode,
        onReleaseManualFullscreenRequest = { userRequestedFullscreen = false }
    )
    var previousPipMode by remember { mutableStateOf(isInPipMode) }
    LaunchedEffect(isInPipMode) { presentationState.syncPipMode(isInPipMode) }
    LaunchedEffect(isPipMode, subReplyState.visible) {
        val shouldDismissThreadDetail = shouldDismissCommentThreadDetailForPip(
            wasInPipMode = previousPipMode,
            isInPipMode = isPipMode,
            subReplyVisible = subReplyState.visible
        )
        previousPipMode = isPipMode
        if (shouldDismissThreadDetail) {
            commentViewModel.closeSubReply()
        }
    }
    val openFavoriteFolders: (VideoFavoriteEntryPoint) -> Unit = { entryPoint ->
        when (resolveVideoFavoriteAction(entryPoint)) {
            VideoFavoriteAction.ToggleFavorite -> engagementViewModel.toggleFavorite()
        }
    }

    //  [新增] 监听定时关闭状态
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()

    // 📖 [新增] 监听视频章节数据
    // 📖 [新增] 监听视频章节数据
    val viewPoints by viewModel.viewPoints.collectAsStateWithLifecycle()
    val pbpProgressData by viewModel.pbpProgressData.collectAsStateWithLifecycle()
    val sponsorProgressMarkers by viewModel.sponsorProgressMarkers.collectAsStateWithLifecycle()

    // [New] Codec & Audio Preferences
    val codecPreference by viewModel.videoCodecPreference.collectAsStateWithLifecycle()
    val secondCodecPreference by viewModel.videoSecondCodecPreference.collectAsStateWithLifecycle()
    val audioQualityPreference by viewModel.audioQualityPreference.collectAsStateWithLifecycle()

    //  [PiP修复] 记录视频播放器在屏幕上的位置，用于PiP窗口只显示视频区域
    var videoPlayerBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    var videoPlayerRootBottomPx by remember { mutableIntStateOf(0) }

    // 📱 [优化] isPortraitFullscreen 和 isVerticalVideo 现在从 playerState 获取（见 playerState 定义后）

    // 🔁 [优化] 合并播放队列状态订阅，减少同帧多次重组
    val playlistUiState by PlaylistManager.uiState.collectAsStateWithLifecycle(
        initialValue = PlaylistUiState(),
        lifecycle = lifecycleOwner.lifecycle
    )
    val currentPlayMode = playlistUiState.playMode
    val playlistItems = playlistUiState.playlist
    val playlistCurrentIndex = playlistUiState.currentIndex
    val isExternalPlaylist = playlistUiState.isExternalPlaylist
    val externalPlaylistSource = playlistUiState.externalPlaylistSource
    val shouldShowExternalPlaylistQueueBar = shouldShowExternalPlaylistQueueBarByPolicy(
        isExternalPlaylist = isExternalPlaylist,
        externalPlaylistSource = externalPlaylistSource,
        playlistSize = playlistItems.size
    )
    val externalPlaylistQueueTitle = resolveExternalPlaylistQueueTitle(externalPlaylistSource)
    var showExternalPlaylistQueueSheet by rememberSaveable { mutableStateOf(false) }
    var pendingVideoShare by remember { mutableStateOf<VideoSharePayload?>(null) }
    val externalPlaylistQueueSheetPresentation = remember {
        resolveExternalPlaylistQueueSheetPresentation(requireRealtimeHaze = true)
    }

    LaunchedEffect(shouldShowExternalPlaylistQueueBar) {
        if (!shouldShowExternalPlaylistQueueBar) {
            showExternalPlaylistQueueSheet = false
        }
    }

    LaunchedEffect(startAudioFromRoute, hasAutoEnteredAudioMode, uiState) {
        if (shouldAutoEnterAudioModeFromRoute(
                startAudioFromRoute = startAudioFromRoute,
                hasAutoEnteredAudioMode = hasAutoEnteredAudioMode,
                isVideoLoadSuccess = uiState is VideoPlaybackUiState.Success
            )
        ) {
            hasAutoEnteredAudioMode = true
            presentationState.markNavigatingToAudioMode()
            viewModel.setAudioMode(true)
            onNavigateToAudioMode()
        }
    }

    //  从小窗展开时自动进入全屏
    LaunchedEffect(startInFullscreen, isOrientationDrivenFullscreen, isLandscape) {
        if (startInFullscreen) {
            if (!isOrientationDrivenFullscreen) {
                userRequestedFullscreen = true
            } else {
                context.findActivity()?.let { activity ->
                    val isInMultiWindowMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        activity.isInMultiWindowMode
                    if (!shouldApplyStartFullscreenOrientationRequest(
                            startInFullscreen = startInFullscreen,
                            isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
                            isLandscape = isLandscape,
                            isInMultiWindowMode = isInMultiWindowMode
                        )
                    ) {
                        if (isInMultiWindowMode) {
                            userRequestedFullscreen = true
                        }
                        return@let
                    }
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
        }
    }

    //  用于跟踪组件是否正在退出，防止 SideEffect 覆盖恢复操作
    var isScreenActive by rememberSaveable(currentBvid) { mutableStateOf(true) }

    //  [关键] 保存进入前的状态栏配置（在 DisposableEffect 外部定义以便复用）
    val window = remember { activity?.window }
    var entryRequestedOrientation by rememberSaveable {
        mutableIntStateOf(
            activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        )
    }
    val insetsController = remember {
        if (window != null && activity != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else null
    }
    val originalSystemBarsSnapshot = remember(window, insetsController) {
        resolveVideoDetailSystemBarsSnapshot(
            statusBarColor = window?.statusBarColor,
            navigationBarColor = window?.navigationBarColor,
            lightStatusBars = insetsController?.isAppearanceLightStatusBars,
            lightNavigationBars = insetsController?.isAppearanceLightNavigationBars,
            systemBarsBehavior = insetsController?.systemBarsBehavior,
            fallbackColor = android.graphics.Color.TRANSPARENT,
            fallbackLightBars = true,
            fallbackSystemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        )
    }

    //  [新增] 恢复状态栏的函数（可复用）
    val restoreStatusBar = remember {
        {
            if (window != null && insetsController != null) {
                if (shouldShowSystemBarsOnVideoDetailExit()) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
                insetsController.systemBarsBehavior = originalSystemBarsSnapshot.systemBarsBehavior
                insetsController.isAppearanceLightStatusBars = originalSystemBarsSnapshot.lightStatusBars
                insetsController.isAppearanceLightNavigationBars = originalSystemBarsSnapshot.lightNavigationBars
                window.statusBarColor = originalSystemBarsSnapshot.statusBarColor
                window.navigationBarColor = originalSystemBarsSnapshot.navigationBarColor
            }
        }
    }

    //  [修复] 包装的 onBack，在导航之前立即恢复状态栏并通知小窗管理器
    val latestOnBack by rememberUpdatedState(onBack)
    val latestOnHomeClick by rememberUpdatedState(onHomeClick)
    val latestOnMarkReturningFromDetail by rememberUpdatedState(onMarkReturningFromDetail)
    val topBarActionHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var pendingTopBarActionRunnable by remember { mutableStateOf<Runnable?>(null) }
    var isActuallyLeaving by rememberSaveable(currentBvid) { mutableStateOf(false) }
    var forceCoverOnlyOnReturn by remember { mutableStateOf(false) }
    val transitionState = rememberVideoDetailTransitionState(
        bvid = bvid,
        sourceRoute = sourceRouteForSharedElement,
        transitionEnabled = transitionEnabled,
        keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
        motionSpec = homeSharedTransitionMotionSpec,
        routeSheetMotion = routeSheetMotion,
    )
    val rootAnimatedVisibilityScope = transitionState.animatedVisibilityScope
    val rootSharedTransitionScope = transitionState.sharedTransitionScope
    val isExitTransitionInProgress = transitionState.isExitTransitionInProgress
    val detailShellSharedBoundsEnabled = transitionState.detailShellSharedBoundsEnabled
    val suppressEnterFadeAfterBackPreview = transitionState.suppressEnterFadeAfterBackPreview
    val detailTransitionProgress = transitionState.progress
    val detailChildTransitionEnabled = transitionState.detailChildTransitionEnabled
    val coverSharedBoundsActive = transitionState.coverSharedBoundsActive
    val sharedBoundsActive = transitionState.sharedBoundsActive
    val routeSheetFrameProvider = transitionState.routeSheetFrameProvider
    val detailShellShape = remember(sharedTransitionSourceCornerDp) {
        RoundedCornerShape(sharedTransitionSourceCornerDp.dp)
    }
    val detailShellModifier = Modifier.videoCardShellSharedBoundsOrEmpty(
        enabled = detailShellSharedBoundsEnabled,
        sharedTransitionScope = rootSharedTransitionScope,
        animatedVisibilityScope = rootAnimatedVisibilityScope,
        bvid = bvid,
        sourceRoute = sourceRouteForSharedElement,
        motionSpec = homeSharedTransitionMotionSpec,
        clipShape = detailShellShape,
        role = VideoCardShellSharedBoundsRole.DetailShell,
    )
    val coverTakeoverBeforeBackDelayMillis = remember {
        resolveCoverTakeoverDelayBeforeBackNavigationMillis()
    }
    // 仅当详情页自身正在回收到来源卡片时进入离开态。详情页作为上层页面的
    // 直接返回目标时必须保留已加载的播放器与内容，避免预测返回预览变成封面占位。
    val isCardReturnExitInProgress = shouldTreatVideoDetailCardExitAsReturning(
        isExitTransitionInProgress = isExitTransitionInProgress,
        sharedBoundsActive = sharedBoundsActive,
        keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
    )
    val forceCoverOnlyForReturn = resolveForceCoverOnlyForReturn(
        forceCoverOnlyOnReturn = forceCoverOnlyOnReturn,
        transitionEnabled = transitionEnabled,
        isCardReturnExitInProgress = isCardReturnExitInProgress
    )
    // 离开态：次要内容淡出等。ImmediatePlayback live morph 时不把视觉交给常驻封面。
    val useReturningVideoDetailVisualState = shouldUseReturningVideoDetailVisualState(
        forceCoverOnlyForReturn = forceCoverOnlyForReturn,
        isCardReturnExitInProgress = isCardReturnExitInProgress,
        isSessionReturningToCard = isReturningFromDetail &&
            transitionEnabled &&
            sharedBoundsActive &&
            !keepLoadedContentForBackPreview,
    )
    val hasResidentReturnCover = coverUrl.isNotBlank()
    val detailContentReadyForLiveReturnMorph = shouldTreatVideoDetailContentReadyForLiveReturnMorph(
        hasSuccessfulDetailContent = uiState is VideoPlaybackUiState.Success,
    )
    val liveReturnMorph = shouldUseLiveReturnMorph(
        transitionEnabled = transitionEnabled,
        sharedBoundsActive = sharedBoundsActive,
        keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
        playbackIntent = videoSharedPlaybackIntent,
        detailContentReady = detailContentReadyForLiveReturnMorph,
    )
    val useResidentCoverForCommittedReturn = shouldHandVisualOwnershipToResidentCover(
        useReturningVisualState = useReturningVideoDetailVisualState,
        hasResidentCover = hasResidentReturnCover,
        liveReturnMorph = liveReturnMorph,
    )

    val handleTopBarAction = remember(
        miniPlayerManager,
        currentBvid,
        coverTakeoverBeforeBackDelayMillis,
        topBarActionHandler
    ) {
        action@{ action: VideoDetailTopBarAction ->
            if (isActuallyLeaving) return@action
            isActuallyLeaving = true // 标记确实是用户通过点击或返回键离开
            isScreenActive = false  // 标记页面正在退出
            latestOnMarkReturningFromDetail()
            // 🎯 通知小窗管理器这是用户主动导航离开（用于控制后台音频）
            miniPlayerManager?.markLeavingByNavigation(expectedBvid = currentBvid)

            restoreStatusBar() // 立即恢复状态栏（动画开始前）
            pendingTopBarActionRunnable?.let(topBarActionHandler::removeCallbacks)
            val navigationRunnable = Runnable {
                pendingTopBarActionRunnable = null
                when (action) {
                    VideoDetailTopBarAction.BACK -> latestOnBack()
                    VideoDetailTopBarAction.HOME -> latestOnHomeClick()
                }
            }
            pendingTopBarActionRunnable = navigationRunnable
            if (coverTakeoverBeforeBackDelayMillis > 0L) {
                topBarActionHandler.postDelayed(navigationRunnable, coverTakeoverBeforeBackDelayMillis)
            } else {
                navigationRunnable.run()
            }
        }
    }
    val handleBack = remember(handleTopBarAction) {
        {
            handleTopBarAction(resolveVideoDetailTopBarAction(isHomeButton = false))
        }
    }

    LaunchedEffect(isExitTransitionInProgress, isActuallyLeaving) {
        if (!shouldRestoreSystemBarsDuringVideoDetailExitTransition(
                isExitTransitionInProgress = isExitTransitionInProgress,
                isActuallyLeaving = isActuallyLeaving
            )
        ) {
            return@LaunchedEffect
        }

        isScreenActive = false
        restoreStatusBar()
    }

    LaunchedEffect(currentBvid) {
        pendingTopBarActionRunnable?.let(topBarActionHandler::removeCallbacks)
        pendingTopBarActionRunnable = null
        isScreenActive = true
        forceCoverOnlyOnReturn = false
        if (shouldClearStaleReturningStateOnVideoDetailEnter(isReturningFromDetail)) {
            onClearReturningFromDetail()
        }
    }

    DisposableEffect(lifecycleOwner, currentBvid) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                pendingTopBarActionRunnable?.let(topBarActionHandler::removeCallbacks)
                pendingTopBarActionRunnable = null
                isScreenActive = true
                forceCoverOnlyOnReturn = false
                if (shouldClearStaleReturningStateOnVideoDetailEnter(isReturningFromDetail)) {
                    onClearReturningFromDetail()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🔄 [新增] 自动横竖屏切换 - 跟随手机传感器方向
    val autoRotateEnabled by com.android.purebilibili.core.store.SettingsManager
        .getAutoRotateEnabled(context).collectAsStateWithLifecycle(
            initialValue = false,
            lifecycle = lifecycleOwner.lifecycle
        )
    val systemAutoRotateEnabled by rememberSystemAutoRotateEnabled(context)
    val cardAnimationEnabled by com.android.purebilibili.core.store.SettingsManager
        .getCardAnimationEnabled(context).collectAsStateWithLifecycle(
            initialValue = true,
            lifecycle = lifecycleOwner.lifecycle
        )

    VideoDetailHighRefreshRateEffect(
        activity = activity,
        isScreenActive = isScreenActive,
    )

    DisposableEffect(Unit) {
        //  [沉浸式] 启用边到边显示，让内容延伸到状态栏下方
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        onDispose {
            pendingTopBarActionRunnable?.let(topBarActionHandler::removeCallbacks)
            pendingTopBarActionRunnable = null
            //  [关键] 标记页面正在退出，防止 SideEffect 覆盖
            isScreenActive = false

            // ⚡ [性能优化] Phase 1: 同步执行 — 仅保留影响视觉的关键操作
            val layoutParams = window?.attributes
            layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = layoutParams
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            restoreStatusBar()

            // ⚡ [性能优化] Phase 1b: CardPositionManager 状态（影响首页卡片动画，必须同步）
            val shouldHandleAsNavigationExit = shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = isNavigatingToAudioMode,
                isNavigatingToMiniMode = isNavigatingToMiniMode,
                isMiniModeActive = miniPlayerManager?.isMiniMode == true,
                isChangingConfigurations = activity?.isChangingConfigurations == true,
                isNavigatingToVideo = resolveIsNavigatingToVideoDuringDispose(
                    localNavigatingToVideo = isNavigatingToVideo,
                    managerNavigatingToVideo = miniPlayerManager?.isNavigatingToVideo == true
                )
            )
            if (shouldMarkReturningStateOnVideoDetailDispose(shouldHandleAsNavigationExit)) {
                onMarkReturningFromDetail()
            } else {
                onClearReturningFromDetail()
            }

            // ⚡ [性能优化] Phase 2: 延迟执行 — 非视觉的系统调用推迟到下一帧
            // PiP 重置、通知清理、Service 停止、屏幕方向恢复等操作不影响退出动画
            // 将它们 post 到主线程 Handler，在导航转场动画完成后再执行
            val deferredActivity = activity
            val deferredContext = context
            val deferredShouldHandleAsNavExit = shouldHandleAsNavigationExit
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                // 🔧 重置 PiP 参数
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    deferredActivity?.let { act ->
                        try {
                            val pipParams = android.app.PictureInPictureParams.Builder()
                                .setAutoEnterEnabled(false)
                                .build()
                            act.setPictureInPictureParams(pipParams)
                        } catch (_: Exception) {}
                    }
                }

                // 🔕 通知清理 + Service 停止
                if (deferredShouldHandleAsNavExit) {
                    val notificationManager = deferredContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager
                    notificationManager.cancel(1001)
                    notificationManager.cancel(PlaybackService.NOTIFICATION_ID)
                    try {
                        deferredContext.startService(
                            android.content.Intent(deferredContext, PlaybackService::class.java).apply {
                                action = PlaybackService.ACTION_STOP_FOREGROUND
                            }
                        )
                    } catch (_: Exception) {}
                }

                // 恢复进入详情页前的方向请求，避免平板误横屏后退不回去。
                deferredActivity?.requestedOrientation = resolveVideoDetailExitRequestedOrientation(
                    originalRequestedOrientation = entryRequestedOrientation
                )
            }
        }
    }

    val playbackEventState = rememberVideoDetailPlaybackEventState()
    VideoDetailPlaybackEventEffects(
        context = context,
        viewModel = viewModel,
        state = playbackEventState,
    )
    val danmakuManager = playbackEventState.danmakuManager

    //  [PiP修复] 当视频播放器位置更新时，同步更新PiP参数
    //  [修复] 只有支持系统 PiP 的模式才启用自动进入 PiP
    val pipModeEnabled = remember {
        com.android.purebilibili.core.store.SettingsManager.getMiniPlayerModeSync(context)
            .supportsSystemPip
    }
    val isReducedActionMotion = !cardAnimationEnabled

    VideoDetailPipParamsEffect(
        context = context,
        activity = activity,
        playerBounds = videoPlayerBounds,
        pipModeEnabled = pipModeEnabled,
        player = miniPlayerManager?.player,
    )

    // 📱 [修复] 提升竖屏全屏状态到 Screen 级别，防止 VideoPlayerState 重建时状态丢失
    val useSharedPortraitPlayer = shouldUseSharedPlayerForPortraitFullscreen()
    val portraitPagerMotionSpec = remember {
        resolveStandalonePortraitPagerMotionSpec()
    }
    val shouldAnimatePortraitPager = remember(useSharedPortraitPlayer) {
        shouldAnimateStandalonePortraitPager(useSharedPlayer = useSharedPortraitPlayer)
    }
    val inlinePlayerAlpha = animateFloatAsState(
        targetValue = if (isPortraitFullscreen) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (shouldAnimatePortraitPager) {
                portraitPagerMotionSpec.inlineReturnDurationMillis
            } else {
                0
            },
            easing = FastOutSlowInEasing
        ),
        label = "inline-player-alpha"
    )
    val inlinePlayerScale = animateFloatAsState(
        targetValue = if (isPortraitFullscreen) {
            portraitPagerMotionSpec.inlineReturnInitialScale
        } else {
            1f
        },
        animationSpec = tween(
            durationMillis = if (shouldAnimatePortraitPager) {
                portraitPagerMotionSpec.inlineReturnDurationMillis
            } else {
                0
            },
            easing = FastOutSlowInEasing
        ),
        label = "inline-player-return-scale"
    )
    var portraitSyncSnapshotBvid by rememberSaveable { mutableStateOf<String?>(null) }
    var portraitSyncSnapshotCid by remember { mutableLongStateOf(0L) }
    var portraitSyncSnapshotPositionMs by remember { mutableLongStateOf(0L) }
    var hasPendingPortraitSync by remember { mutableStateOf(false) }
    var hasDeferredPortraitRestoreAfterExternalNavigation by rememberSaveable { mutableStateOf(false) }
    var pendingMainReloadBvidAfterPortrait by rememberSaveable { mutableStateOf<String?>(null) }
    var portraitPendingSelectionBvid by rememberSaveable { mutableStateOf<String?>(null) }
    // 初始化播放器状态
    val playerState = rememberVideoPlayerState(
        context = context,
        viewModel = viewModel,
        bvid = currentBvid,
        cid = playbackTargetCid,
        fallbackResumePositionMs = resumePositionMsFromRoute,
        startPaused = isPortraitFullscreen && !useSharedPortraitPlayer,
        entryTransitionFinished = entryTransitionFinished,
        playbackSessionActive = isVisible,
    )
    VideoDetailKeepScreenOnEffect(
        window = window,
        player = playerState.player,
    )
    val isVideoPlaying by produceState(
        initialValue = playerState.player.isPlaying,
        key1 = playerState.player
    ) {
        val player = playerState.player
        value = player.isPlaying
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                value = player.isPlaying
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                value = player.isPlaying
            }
        }
        player.addListener(listener)
        awaitDispose {
            player.removeListener(listener)
        }
    }
    val routedCommentInteractionActive =
        openCommentRootRpidFromRoute > 0L &&
            (subReplyState.visible || subReplyState.isLoading)
    val commentInteractionActive =
        routedCommentInteractionActive || subReplyState.visible || showCommentInput
    LaunchedEffect(commentInteractionActive) {
        viewModel.setCommentInteractionActive(commentInteractionActive)
    }
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.setCommentInteractionActive(false)
        }
    }
    var hasAutoPausedForRoutedComment by rememberSaveable(
        bvid,
        openCommentRootRpidFromRoute,
        openCommentTargetRpidFromRoute
    ) {
        mutableStateOf(false)
    }
    LaunchedEffect(
        openCommentRootRpidFromRoute,
        videoSharedPlaybackIntent,
        isVideoPlaying,
        hasAutoPausedForRoutedComment
    ) {
        val shouldAutoPause =
            openCommentRootRpidFromRoute > 0L &&
                videoSharedPlaybackIntent == VideoSharedTransitionPlaybackIntent.ImmediatePlayback &&
                isVideoPlaying &&
                !hasAutoPausedForRoutedComment
        if (shouldAutoPause) {
            com.android.purebilibili.feature.video.usecase.pausePlayerFromUserAction(playerState.player)
            hasAutoPausedForRoutedComment = true
        }
    }
    val isPlaybackPaused by produceState(
        initialValue = resolveIsPlaybackPausedForCollapse(
            playWhenReady = playerState.player.playWhenReady,
            playbackState = playerState.player.playbackState
        ),
        key1 = playerState.player,
        key2 = currentBvid,
        key3 = currentBvidCid
    ) {
        val player = playerState.player

        fun updatePausedState() {
            value = resolveIsPlaybackPausedForCollapse(
                playWhenReady = player.playWhenReady,
                playbackState = player.playbackState
            )
        }

        updatePausedState()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePausedState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePausedState()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updatePausedState()
            }
        }
        player.addListener(listener)
        awaitDispose {
            player.removeListener(listener)
        }
    }
    val subtitleAutoPreference by com.android.purebilibili.core.store.SettingsManager
        .getSubtitleAutoPreference(context)
        .collectAsStateWithLifecycle(
            initialValue = SubtitleAutoPreference.OFF,
            lifecycle = lifecycleOwner.lifecycle
        )
    val subtitleAudioManager = remember {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }
    val subtitleAutoModeMuted = remember(playerState.player, subtitleAudioManager, currentBvid) {
        val systemMuted = runCatching {
            subtitleAudioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) <= 0
        }.getOrDefault(false)
        systemMuted || playerState.player.volume <= 0f
    }
    val subtitlePreferenceSession = remember(uiState, currentBvid, subtitleAutoPreference, subtitleAutoModeMuted) {
        val success = uiState as? VideoPlaybackUiState.Success
        if (success == null) {
            resolveSubtitlePreferenceSession(
                bvid = currentBvid,
                cid = 0L,
                primaryLanguage = null,
                secondaryLanguage = null,
                primaryTrackLikelyAi = false,
                secondaryTrackLikelyAi = false,
                hasPrimaryTrack = false,
                hasSecondaryTrack = false,
                preference = subtitleAutoPreference,
                isMuted = subtitleAutoModeMuted
            )
        } else {
            val subtitleBelongsToCurrentVideo =
                success.subtitleOwnerBvid == success.info.bvid &&
                    success.subtitleOwnerCid == success.info.cid &&
                    success.info.cid > 0L
            val hasPrimaryTrack = subtitleBelongsToCurrentVideo &&
                (!success.subtitlePrimaryTrackKey.isNullOrBlank() || !success.subtitlePrimaryLanguage.isNullOrBlank())
            val hasSecondaryTrack = subtitleBelongsToCurrentVideo &&
                (!success.subtitleSecondaryTrackKey.isNullOrBlank() || !success.subtitleSecondaryLanguage.isNullOrBlank())
            resolveSubtitlePreferenceSession(
                bvid = success.info.bvid,
                cid = success.info.cid,
                primaryLanguage = success.subtitlePrimaryLanguage,
                secondaryLanguage = success.subtitleSecondaryLanguage,
                primaryTrackLikelyAi = subtitleBelongsToCurrentVideo && success.subtitlePrimaryLikelyAi,
                secondaryTrackLikelyAi = subtitleBelongsToCurrentVideo && success.subtitleSecondaryLikelyAi,
                hasPrimaryTrack = hasPrimaryTrack,
                hasSecondaryTrack = hasSecondaryTrack,
                preference = subtitleAutoPreference,
                isMuted = subtitleAutoModeMuted
            )
        }
    }
    var subtitlePreferenceSessionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var subtitleDisplayModeOverride by rememberSaveable { mutableStateOf(SubtitleDisplayMode.OFF) }
    LaunchedEffect(subtitlePreferenceSession.key) {
        subtitleDisplayModeOverride = resolveSubtitleDisplayModePreference(
            previousSessionKey = subtitlePreferenceSessionKey,
            nextSessionKey = subtitlePreferenceSession.key,
            previousMode = subtitleDisplayModeOverride,
            nextInitialMode = subtitlePreferenceSession.initialMode
        )
        subtitlePreferenceSessionKey = subtitlePreferenceSession.key
    }

    var hasAppliedInitialPageSwitch by remember(currentBvid, playbackTargetCid) { mutableStateOf(false) }
    LaunchedEffect(uiState, currentBvid, playbackTargetCid, hasAppliedInitialPageSwitch) {
        if (hasAppliedInitialPageSwitch) return@LaunchedEffect
        val success = uiState as? VideoPlaybackUiState.Success ?: return@LaunchedEffect
        if (success.info.bvid != currentBvid) return@LaunchedEffect

        val targetPageIndex = resolveInitialPageIndex(
            requestedCid = playbackTargetCid,
            currentCid = success.info.cid,
            pages = success.info.pages
        )
        hasAppliedInitialPageSwitch = true
        if (targetPageIndex != null) {
            viewModel.switchPage(targetPageIndex)
        }
    }

    // 🎯 [修复] 确保在 VideoPlayerState 销毁之前通知 MiniPlayerManager 页面退出
    // 必须在 playerState 之后声明此 Effect，这样它会在 playerState.onDispose 之前执行（LIFO 顺序）
    DisposableEffect(playerState) {
        onDispose {
            // 标记页面正在退出
            // 配置切换不标记离开；音频模式/小窗模式为主动保活场景，也不标记离开。
            val isChangingConfigurations = activity?.isChangingConfigurations == true
            val shouldHandleAsNavigationExit = shouldHandleVideoDetailDisposeAsNavigationExit(
                isNavigatingToAudioMode = isNavigatingToAudioMode,
                isNavigatingToMiniMode = isNavigatingToMiniMode,
                isMiniModeActive = miniPlayerManager?.isMiniMode == true,
                isChangingConfigurations = isChangingConfigurations,
                isNavigatingToVideo = resolveIsNavigatingToVideoDuringDispose(
                    localNavigatingToVideo = isNavigatingToVideo,
                    managerNavigatingToVideo = miniPlayerManager?.isNavigatingToVideo == true
                )
            )
            if (shouldHandleAsNavigationExit) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoDetailScreen",
                    "🛑 Disposing screen as navigation exit, notifying MiniPlayerManager"
                )
                miniPlayerManager?.markLeavingByNavigation(expectedBvid = currentBvid)
            } else {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoDetailScreen",
                    "💤 Screen disposed without navigation-exit mark (audioMode=$isNavigatingToAudioMode, miniMode=$isNavigatingToMiniMode, changingConfig=$isChangingConfigurations)"
                )
            }
        }
    }

    //  [性能优化] 生命周期感知：进入后台时暂停播放，返回前台时继续
    //  [修复] 此处逻辑已移至 VideoPlayerState.kt 统一处理
    // 删除冗余的暂停逻辑，避免与 VideoPlayerState 中的生命周期处理冲突
    // VideoPlayerState 会检查 PiP/小窗模式来决定是否暂停

    // 📱 [优化] 竖屏视频检测已移至 VideoPlayerState 集中管理
    val isVerticalVideo by playerState.isVerticalVideo.collectAsStateWithLifecycle()
    val activeVideoSharedTransitionVisualSpec = remember(
        sourceRouteForSharedElement,
        sharedTransitionSourceCornerDp,
        videoSharedPlaybackIntent,
        startInFullscreen,
        autoEnterPortraitFromRoute,
        initialVerticalFromRoute,
        isVerticalVideo,
        useReturningVideoDetailVisualState
    ) {
        resolveVideoSharedTransitionVisualSpec(
            sourceRoute = sourceRouteForSharedElement,
            sourceCornerDp = sharedTransitionSourceCornerDp,
            playbackIntent = videoSharedPlaybackIntent,
            fullscreen = startInFullscreen,
            autoPortrait = autoEnterPortraitFromRoute,
            initialVertical = initialVerticalFromRoute,
            isVerticalVideo = isVerticalVideo,
            isReturning = useReturningVideoDetailVisualState
        )
    }
    LaunchedEffect(
        autoRotateEnabled,
        systemAutoRotateEnabled,
        fullscreenMode,
        useTabletLayout,
        isOrientationDrivenFullscreen,
        isFullscreenMode,
        windowSizeClass.isCompactDevice,
        isActivityInMultiWindowMode,
        userRequestedFullscreen,
        manualPortraitHoldActive,
        isVerticalVideo
    ) {
        val requestedOrientation = resolvePhoneVideoRequestedOrientation(
            autoRotateEnabled = autoRotateEnabled,
            systemAutoRotateEnabled = systemAutoRotateEnabled,
            fullscreenMode = fullscreenMode,
            isCompactDevice = windowSizeClass.isCompactDevice,
            isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
            isFullscreenMode = isFullscreenMode,
            manualFullscreenRequested = userRequestedFullscreen,
            manualPortraitHoldActive = manualPortraitHoldActive,
            isVerticalVideo = isVerticalVideo,
            currentRequestedOrientation = activity?.requestedOrientation,
            isInMultiWindowMode = isActivityInMultiWindowMode
        ) ?: return@LaunchedEffect

        if (activity?.requestedOrientation != requestedOrientation) {
            activity?.requestedOrientation = requestedOrientation
        }
        com.android.purebilibili.core.util.Logger.d(
            "VideoDetailScreen",
            "🔄 Auto-rotate: enabled=$autoRotateEnabled, system=$systemAutoRotateEnabled, hold=$manualPortraitHoldActive, mode=$fullscreenMode, horizontal=$horizontalAdaptationEnabled, requested=$requestedOrientation, fullscreen=$isFullscreenMode, verticalVideo=$isVerticalVideo, isCompactDevice=${windowSizeClass.isCompactDevice}, multiWindow=$isActivityInMultiWindowMode"
        )
    }
    var lastPhoneAutoRotateLandscapeAppliedAtMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(
        autoRotateEnabled,
        systemAutoRotateEnabled,
        windowSizeClass.isCompactDevice,
        isOrientationDrivenFullscreen,
        fullscreenMode,
        manualPortraitHoldActive,
        isActivityInMultiWindowMode
    ) {
        if (!shouldObservePhoneAutoRotate(
                autoRotateEnabled = autoRotateEnabled,
                systemAutoRotateEnabled = systemAutoRotateEnabled,
                isCompactDevice = windowSizeClass.isCompactDevice,
                isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
                fullscreenMode = fullscreenMode,
                manualPortraitHoldActive = manualPortraitHoldActive,
                isInMultiWindowMode = isActivityInMultiWindowMode
            )
        ) {
            lastPhoneAutoRotateLandscapeAppliedAtMs = null
        }
    }

    DisposableEffect(
        activity,
        autoRotateEnabled,
        systemAutoRotateEnabled,
        fullscreenMode,
        useTabletLayout,
        isOrientationDrivenFullscreen,
        manualPortraitHoldActive,
        isActivityInMultiWindowMode
    ) {
        val hostActivity = activity
        if (
            hostActivity == null ||
            !shouldObservePhoneAutoRotate(
                autoRotateEnabled = autoRotateEnabled,
                systemAutoRotateEnabled = systemAutoRotateEnabled,
                isCompactDevice = windowSizeClass.isCompactDevice,
                isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
                fullscreenMode = fullscreenMode,
                manualPortraitHoldActive = manualPortraitHoldActive,
                isInMultiWindowMode = isActivityInMultiWindowMode
            ) ||
            !isOrientationDrivenFullscreen
        ) {
            return@DisposableEffect onDispose {}
        }

        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (manualPortraitHoldActive) {
                    if (shouldReleasePhoneManualPortraitHold(orientation)) {
                        manualPortraitHoldActive = false
                    }
                    return
                }
                val isCurrentlyLandscape =
                    hostActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val targetOrientation = resolvePhoneAutoRotateRequestedOrientation(
                    orientationDegrees = orientation,
                    isCurrentlyLandscape = isCurrentlyLandscape
                )
                val nowMs = SystemClock.elapsedRealtime()
                val targetToApply = resolvePhoneAutoRotateTargetToApply(
                    candidateOrientation = targetOrientation,
                    lastLandscapeAppliedAtMs = lastPhoneAutoRotateLandscapeAppliedAtMs,
                    nowMs = nowMs
                ) ?: return
                if (hostActivity.requestedOrientation != targetToApply) {
                    hostActivity.requestedOrientation = targetToApply
                }
                lastPhoneAutoRotateLandscapeAppliedAtMs =
                    if (isLandscapeRequestedOrientation(targetToApply)) nowMs else null
            }
        }

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }

        onDispose {
            orientationListener.disable()
            lastPhoneAutoRotateLandscapeAppliedAtMs = null
        }
    }
    val portraitExperienceEnabled = shouldEnablePortraitExperience()
    val useOfficialInlinePortraitDetailExperience = shouldUseOfficialInlinePortraitDetailExperience(
        useTabletLayout = useTabletLayout,
        isVerticalVideo = isVerticalVideo,
        portraitExperienceEnabled = portraitExperienceEnabled
    )
    val allowStandalonePortraitExperience = portraitExperienceEnabled &&
        !useOfficialInlinePortraitDetailExperience
    val isCurrentRouteVideoLoaded = remember(uiState, currentBvid) {
        val success = uiState as? VideoPlaybackUiState.Success
        success?.info?.bvid == currentBvid
    }
    val enterPortraitFullscreen = {
        if (shouldActivatePortraitFullscreenState(portraitExperienceEnabled)) {
            portraitSyncSnapshotBvid = (uiState as? VideoPlaybackUiState.Success)?.info?.bvid
            portraitSyncSnapshotCid = (uiState as? VideoPlaybackUiState.Success)?.info?.cid ?: 0L
            portraitSyncSnapshotPositionMs = playerState.player.currentPosition.coerceAtLeast(0L)
            hasPendingPortraitSync = false
            presentationState.setPortraitFullscreen(true)
        }
    }
    LaunchedEffect(
        autoEnterPortraitFromRoute,
        startAudioFromRoute,
        portraitExperienceEnabled,
        useOfficialInlinePortraitDetailExperience,
        windowSizeClass.widthSizeClass,
        isCurrentRouteVideoLoaded,
        isVerticalVideo,
        isPortraitFullscreen,
        hasAutoEnteredPortraitFromRoute
    ) {
        if (
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = autoEnterPortraitFromRoute,
                startAudioFromRoute = startAudioFromRoute,
                portraitExperienceEnabled = portraitExperienceEnabled,
                useOfficialInlinePortraitDetailExperience = useOfficialInlinePortraitDetailExperience,
                allowStandalonePortraitAutoEnter = windowSizeClass.widthSizeClass ==
                    com.android.purebilibili.core.util.WindowWidthSizeClass.Compact,
                isCurrentRouteVideoLoaded = isCurrentRouteVideoLoaded,
                isVerticalVideo = isVerticalVideo,
                isPortraitFullscreen = isPortraitFullscreen,
                hasAutoEnteredPortraitFromRoute = hasAutoEnteredPortraitFromRoute
            )
        ) {
            enterPortraitFullscreen()
            hasAutoEnteredPortraitFromRoute = true
        }
    }
    val shouldMirrorPortraitProgressToMainPlayer = com.android.purebilibili.feature.video.ui.pager
        .shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = useSharedPortraitPlayer)

    val tryApplyPortraitProgressSync = remember(playerState, viewModel) {
        { snapshotBvid: String?, snapshotPositionMs: Long ->
            val currentSuccess = viewModel.uiState.value as? VideoPlaybackUiState.Success
            val currentBvid = currentSuccess?.info?.bvid
            val currentCid = currentSuccess?.info?.cid ?: 0L
            if (!com.android.purebilibili.feature.video.ui.pager.shouldApplyPortraitProgressSync(
                    snapshotBvid = snapshotBvid,
                    snapshotCid = portraitSyncSnapshotCid,
                    currentBvid = currentBvid,
                    currentCid = currentCid
                )
            ) {
                false
            } else {
                playerState.player.seekTo(snapshotPositionMs.coerceAtLeast(0L))
                true
            }
        }
    }

    fun applyPortraitExitRestore() {
        val target = com.android.purebilibili.feature.video.ui.pager.resolvePortraitExitRestoreTarget(
            pendingMainReloadBvidAfterPortrait = pendingMainReloadBvidAfterPortrait,
            portraitPendingSelectionBvid = portraitPendingSelectionBvid,
            portraitSyncSnapshotBvid = portraitSyncSnapshotBvid,
            portraitSyncSnapshotCid = portraitSyncSnapshotCid,
            currentBvidCid = currentBvidCid
        ) ?: return
        presentationState.switchVideo(target.bvid, target.cid)
    }



    // 同步状态到 playerState (可选，用于日志或内部逻辑)
    LaunchedEffect(isPortraitFullscreen) {
        playerState.setPortraitFullscreen(isPortraitFullscreen)
        viewModel.setPortraitPlaybackSessionActive(isPortraitFullscreen)
        val shouldPauseMainPlayer = com.android.purebilibili.feature.video.ui.pager
            .shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = useSharedPortraitPlayer)
        if (isPortraitFullscreen) {
            if (shouldPauseMainPlayer) {
                playerState.player.pause()
                playerState.player.volume = 0f
                playerState.player.playWhenReady = false
            }
            portraitSyncSnapshotBvid = (uiState as? VideoPlaybackUiState.Success)?.info?.bvid
            portraitSyncSnapshotCid = (uiState as? VideoPlaybackUiState.Success)?.info?.cid ?: 0L
            portraitSyncSnapshotPositionMs = playerState.player.currentPosition.coerceAtLeast(0L)
            hasPendingPortraitSync = shouldPauseMainPlayer
        } else {
             if (shouldPauseMainPlayer) {
                 // 退出时恢复音量 (不自动播放，等待用户操作或 onResume)
                 com.android.purebilibili.core.player.PlayerVolumeController
                     .applyPreferredVolume(playerState.player)
             }
            if (!com.android.purebilibili.feature.video.ui.pager
                    .shouldApplyDeferredPortraitRestoreOnResume(
                        hasDeferredRestore = hasDeferredPortraitRestoreAfterExternalNavigation,
                        isPortraitFullscreen = isPortraitFullscreen
                    )
            ) {
                applyPortraitExitRestore()
                pendingMainReloadBvidAfterPortrait = null
                portraitPendingSelectionBvid = null
            }
        }
    }

    DisposableEffect(
        lifecycleOwner,
        isPortraitFullscreen,
        hasDeferredPortraitRestoreAfterExternalNavigation,
        pendingMainReloadBvidAfterPortrait,
        portraitPendingSelectionBvid,
        portraitSyncSnapshotBvid,
        portraitSyncSnapshotCid,
        currentBvidCid
    ) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event != androidx.lifecycle.Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            if (!com.android.purebilibili.feature.video.ui.pager
                    .shouldApplyDeferredPortraitRestoreOnResume(
                        hasDeferredRestore = hasDeferredPortraitRestoreAfterExternalNavigation,
                        isPortraitFullscreen = isPortraitFullscreen
                    )
            ) {
                return@LifecycleEventObserver
            }
            applyPortraitExitRestore()
            pendingMainReloadBvidAfterPortrait = null
            portraitPendingSelectionBvid = null
            hasDeferredPortraitRestoreAfterExternalNavigation = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.setPortraitPlaybackSessionActive(false)
        }
    }

    LaunchedEffect(
        uiState,
        hasPendingPortraitSync,
        portraitSyncSnapshotBvid,
        portraitSyncSnapshotPositionMs
    ) {
        if (hasPendingPortraitSync && tryApplyPortraitProgressSync(
                portraitSyncSnapshotBvid,
                portraitSyncSnapshotPositionMs
            )
        ) {
            hasPendingPortraitSync = false
        }
    }

    LaunchedEffect(uiState, currentBvid, currentBvidCid, isPortraitFullscreen, bvid, isVisible) {
        if (!isVisible) return@LaunchedEffect
        val success = uiState as? VideoPlaybackUiState.Success ?: return@LaunchedEffect
        if (!shouldSyncMainPlayerToInternalBvid(
                isPortraitFullscreen = isPortraitFullscreen,
                routeBvid = bvid,
                currentBvid = currentBvid,
                currentBvidCid = currentBvidCid,
                loadedBvid = success.info.bvid,
                loadedCid = success.info.cid
            )
        ) {
            return@LaunchedEffect
        }
        viewModel.loadVideo(
            bvid = currentBvid,
            cid = currentBvidCid.takeIf { it > 0L } ?: 0L,
            autoPlay = resolveAutoPlayOverrideForInternalBvidSync(forceAutoPlay = false)
        )
    }

    // 📲 小窗模式（手机/平板统一逻辑）
    val handlePipClick = {
        // 使用 MiniPlayerManager 进入应用内小窗模式
        miniPlayerManager?.let { manager ->
            val stopPlaybackOnExit = com.android.purebilibili.core.store.SettingsManager
                .getStopPlaybackOnExitSync(context)
            if (stopPlaybackOnExit) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoDetailScreen",
                    "Stop-on-exit enabled, skip mini mode and leave page directly"
                )
                manager.markLeavingByNavigation(expectedBvid = currentBvid)
                onBack()
                return@let
            }

            //  [埋点] PiP 进入事件
            com.android.purebilibili.core.util.AnalyticsHelper.logPictureInPicture(
                videoId = currentBvid,
                action = "enter_mini"
            )

            // 1. 将当前播放器信息传递给小窗管理器
            val info = uiState as? VideoPlaybackUiState.Success
            manager.setVideoInfo(
                bvid = currentBvid,
                title = info?.info?.title ?: "",
                cover = info?.info?.pic ?: "",
                owner = info?.info?.owner?.name ?: "",
                cid = info?.info?.cid ?: 0L,
                aid = info?.info?.aid ?: 0L,
                externalPlayer = playerState.player
            )

            // 2. 进入小窗模式（强制，不管当前模式设置）
            manager.enterMiniMode(forced = true)

            // 3. 返回上一页（首页）
            presentationState.markNavigatingToMiniMode()
            onBack()
        } ?: run {
            // 如果 miniPlayerManager 不存在，直接返回
            com.android.purebilibili.core.util.Logger.w("VideoDetailScreen", "⚠️ miniPlayerManager 为 null，无法进入小窗")
            onBack()
        }
    }

    // 🔧 [性能优化] 记录上次缓存的 bvid，避免重复缓存 MiniPlayer 信息
    var lastCachedMiniPlayerBvid by remember { mutableStateOf<String?>(null) }

    //  核心修改：初始化评论 & 媒体中心信息
    LaunchedEffect(uiState, isVisible) {
        if (uiState is VideoPlaybackUiState.Success) {
            val info = (uiState as VideoPlaybackUiState.Success).info
            val success = uiState as VideoPlaybackUiState.Success

            // 初始化评论（传入 UP 主 mid 用于筛选）- 保持在主线程
            commentViewModel.init(
                aid = info.aid,
                upMid = info.owner.mid,
                preferredSortMode = preferredCommentSortMode,
                expectedReplyCount = info.stat.reply
            )

            playerState.updateMediaMetadata(
                title = info.title,
                artist = info.owner.name,
                coverUrl = info.pic
            )

            // 📱 [双重验证] 从 API dimension 字段设置预判断值
            info.dimension?.let { dim ->
                playerState.setApiDimension(dim.width, dim.height, dim.rotate)
            }

            //  同步视频信息到小窗管理器（为小窗模式做准备）
            //  🚀 [性能优化] 将繁重的序列化和缓存操作移至后台线程，防止主线程卡顿
            // 🔧 [性能优化] 只有首次加载或视频切换时才缓存 MiniPlayer 信息
            val shouldCacheMiniPlayer = lastCachedMiniPlayerBvid != currentBvid

            if (miniPlayerManager != null && shouldCacheMiniPlayer && isVisible) {
                lastCachedMiniPlayerBvid = currentBvid

                // PiP 判断依赖 MiniPlayerManager 的 active/player/playing/bvid 状态。
                // 这一步必须先于后台缓存完成，避免竖屏全屏快速进入 PiP 时拿到旧状态而暂停。
                miniPlayerManager.setVideoInfo(
                    bvid = currentBvid,
                    title = info.title,
                    cover = info.pic,
                    owner = info.owner.name,
                    cid = info.cid,  //  传递 cid 用于弹幕加载
                    aid = info.aid,
                    externalPlayer = playerState.player,
                    fromLeft = com.android.purebilibili.core.util.CardPositionManager.isCardOnLeft  //  传递入场方向
                )

                launch(Dispatchers.Default) {
                    com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "🔄 [Background] Caching MiniPlayer UI state...")

                    // 序列化缓存 (Heavy Operation)
                    miniPlayerManager.cacheUiState(success)
                    com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "✅ [Background] MiniPlayer info cached")
                }
            } else if (miniPlayerManager == null) {
                android.util.Log.w("VideoDetailScreen", " miniPlayerManager 是 null!")
            }
        } else if (uiState is VideoPlaybackUiState.Loading) {
            playerState.updateMediaMetadata(
                title = "加载中...",
                artist = "",
                coverUrl = coverUrl
            )
        }
    }

    //  弹幕加载逻辑已移至 VideoPlayerState 内部处理
    // 避免在此处重复消耗 InputStream

    // 辅助函数：切换全屏状态
    val toggleFullscreen = {
        val activity = context.findActivity()
        toggleVideoDetailFullscreen(
            activity = activity,
            isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
            isLandscape = isLandscape,
            isFullscreenMode = isFullscreenMode,
            isCompactDevice = windowSizeClass.isCompactDevice,
            fullscreenMode = fullscreenMode,
            isVerticalVideo = isVerticalVideo,
            portraitExperienceEnabled = portraitExperienceEnabled,
            onEnterPortraitFullscreen = { enterPortraitFullscreen() },
            onUserRequestedFullscreenChange = { requested -> userRequestedFullscreen = requested },
            onManualPortraitHoldActiveChange = { active -> manualPortraitHoldActive = active }
        )
    }

    val localBackTarget = resolveVideoDetailLocalBackTarget(
        isLandscapeFullscreen = isFullscreenMode,
        isPortraitFullscreen = isPortraitFullscreen,
    )
    val localBackEventState = rememberNavigationEventState(NavigationEventInfo.None)
    val predictiveBackGestureEnabled = LocalPredictiveBackGestureEnabled.current
    NavigationBackHandler(
        state = localBackEventState,
        isBackEnabled = localBackTarget != VideoDetailLocalBackTarget.NAVIGATE_BACK,
        reportPredictiveProgress = predictiveBackGestureEnabled,
        onBackCompleted = { commitTransition: () -> Unit ->
            when (localBackTarget) {
                VideoDetailLocalBackTarget.EXIT_PORTRAIT_FULLSCREEN -> presentationState.setPortraitFullscreen(false)
                VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN -> toggleFullscreen()
                VideoDetailLocalBackTarget.NAVIGATE_BACK -> Unit
            }
            commitTransition()
        },
    )

    // 以下 BackHandler 会阻止 Compose Navigation 的返回路由动画，由根导航统一处理。
    // 显式点击返回时由 handleBack 提前标记 returning，系统路径仍由 onDispose 兜底标记。
    // BackHandler(enabled = !isFullscreenMode && !isPortraitFullscreen, onBack = handleBack)


    // 清理逻辑（markLeavingByNavigation、restoreStatusBar）已移至 DisposableEffect.onDispose

    // 沉浸式状态栏控制
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }
    val systemBarsVisibilityPolicy = remember(
        isFullscreenMode,
        hideVideoPageStatusBar,
        isPipMode,
        isScreenActive
    ) {
        resolveVideoDetailSystemBarsVisibilityPolicy(
            isFullscreenMode = isFullscreenMode,
            hideVideoPageStatusBar = hideVideoPageStatusBar,
            isInPipMode = isPipMode,
            isScreenActive = isScreenActive
        )
    }
    val systemBarsApplySpec = remember(
        systemBarsVisibilityPolicy,
        useTabletLayout,
        isLightBackground,
        backgroundColor
    ) {
        resolveVideoDetailSystemBarsApplySpec(
            visibilityPolicy = systemBarsVisibilityPolicy,
            useTabletLayout = useTabletLayout,
            isLightBackground = isLightBackground,
            backgroundColor = backgroundColor.toArgb(),
            transparentColor = Color.Transparent.toArgb(),
            blackColor = Color.Black.toArgb(),
            transientBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        )
    }

    VideoDetailSystemBarsEffect(
        view = view,
        window = window,
        insetsController = insetsController,
        isScreenActive = isScreenActive,
        spec = systemBarsApplySpec,
    )

    val uiSuccessState = uiState as? VideoPlaybackUiState.Success
    val videoPlayerSectionTarget = remember(bvid, coverUrl, currentBvid) {
        resolveVideoPlayerSectionTarget(
            routeBvid = bvid,
            routeCoverUrl = coverUrl,
            currentBvid = currentBvid
        )
    }
    val shouldSuppressSubtitleOverlay = useSharedPortraitPlayer &&
        !isPortraitFullscreen &&
        pendingMainReloadBvidAfterPortrait != null &&
        (
            pendingMainReloadBvidAfterPortrait != uiSuccessState?.info?.bvid ||
                (portraitSyncSnapshotCid > 0L && portraitSyncSnapshotCid != (uiSuccessState?.info?.cid ?: 0L))
            )
    // Android 16 ART 曾拒绝校验由 VideoDetailRouteSheetHost 尾随 lambda 生成的超大合成方法
    // （VerifyError: VideoDetailScreen$lambda$N(...BoxScope, Composer, int) 参数过多）。
    // 主布局与覆盖层必须使用两个内容槽，单个局部函数仍会捕获全部状态并生成百参数方法。
    // 分槽后 Compose 编译器会分别生成合成方法，避免一个方法聚合整页状态。
    @Composable
    fun BoxScope.VideoDetailRouteSheetMainContent() {
            // 📐 [平板适配] 全屏模式过渡动画（只有手机横屏才进入全屏）
        if (isFullscreenMode) {
                val showDanmakuDialog by viewModel.showDanmakuDialog.collectAsStateWithLifecycle()
                val isSendingDanmaku by viewModel.isSendingDanmaku.collectAsStateWithLifecycle()
                val composerDrafts by viewModel.composerDrafts.collectAsStateWithLifecycle()
                val useInlineDanmakuComposer =
                    com.android.purebilibili.feature.video.ui.components.shouldUseInlineDanmakuComposer(
                        isFullscreenMode = isFullscreenMode
                    )
                val danmakuSendPreferenceScope = rememberCoroutineScope()
                val rememberedDanmakuSendColor by com.android.purebilibili.core.store.SettingsManager
                    .getDanmakuSendColor(context)
                    .collectAsStateWithLifecycle(initialValue = 16777215)
                val rememberedDanmakuSendMode by com.android.purebilibili.core.store.SettingsManager
                    .getDanmakuSendMode(context)
                    .collectAsStateWithLifecycle(initialValue = 1)
                val rememberedDanmakuSendFontSize by com.android.purebilibili.core.store.SettingsManager
                    .getDanmakuSendFontSize(context)
                    .collectAsStateWithLifecycle(initialValue = 25)
                VideoPlayerSection(
                    playerState = playerState,
                    uiState = uiState,
                    isFullscreen = true,
                    isInPipMode = isPipMode,
                    transitionEnabled = detailChildTransitionEnabled,
                    onToggleFullscreen = { toggleFullscreen() },
                    onQualityChange = { qid -> viewModel.changeQuality(qid) },
                    onBack = { toggleFullscreen() },
                    onHomeClick = {
                        handleTopBarAction(resolveVideoDetailTopBarAction(isHomeButton = true))
                    },
                    onDanmakuInputClick = { viewModel.showDanmakuSendDialog() },
                    danmakuComposerVisible = showDanmakuDialog && useInlineDanmakuComposer,
                    onDismissDanmakuComposer = { viewModel.hideDanmakuSendDialog() },
                    onSendDanmakuComposer = { message, color, mode, fontSize, encourage ->
                        viewModel.sendDanmaku(message, color, mode, fontSize, encourage)
                    },
                    isSendingDanmakuComposer = isSendingDanmaku,
                    danmakuComposerInitialText = composerDrafts.danmaku.text,
                    danmakuComposerInitialAttentionCommand = composerDrafts.danmaku.attentionCommand,
                    danmakuComposerInitialColor = rememberedDanmakuSendColor,
                    danmakuComposerInitialMode = rememberedDanmakuSendMode,
                    danmakuComposerInitialFontSize = rememberedDanmakuSendFontSize,
                    onDanmakuComposerDraftChange = viewModel::updateDanmakuDraft,
                    onDanmakuComposerSelectionChange = { color, mode, fontSize ->
                        danmakuSendPreferenceScope.launch {
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuSendColor(context, color)
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuSendMode(context, mode)
                            com.android.purebilibili.core.store.SettingsManager.setDanmakuSendFontSize(context, fontSize)
                        }
                    },
                    // 🔗 [新增] 分享功能
                    bvid = videoPlayerSectionTarget.bvid,
                    coverUrl = videoPlayerSectionTarget.entryCoverUrl,
                    //  实验性功能：双击点赞
                    onDoubleTapLike = { engagementViewModel.toggleLike() },
                    sponsorSegment = sponsorSegment,
                    showSponsorSkipButton = showSponsorSkipButton,
                    onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                    onSponsorDismiss = { viewModel.dismissSponsorSkipButton() },
                    //  [新增] 重载视频
                    onReloadVideo = { viewModel.reloadVideo() },
                    //  [新增] CDN 线路切换
                    cdnCount = (uiState as? VideoPlaybackUiState.Success)?.cdnCount ?: 1,
                    cdnLineDiagnostics = (uiState as? VideoPlaybackUiState.Success)?.cdnLineDiagnostics.orEmpty(),
                    isCdnProbing = (uiState as? VideoPlaybackUiState.Success)?.isCdnProbing ?: false,
                    onSwitchCdn = { viewModel.switchCdn() },
                    onSwitchCdnTo = { viewModel.switchCdnTo(it) },
                    onProbeCdnCandidates = { viewModel.probeCurrentCdnCandidates() },

                    // [New] Codec & Audio (Fullscreen)
                    currentCodec = codecPreference,
                    onCodecChange = { viewModel.setVideoCodec(it) },
                    currentSecondCodec = secondCodecPreference,
                    onSecondCodecChange = { viewModel.setVideoSecondCodec(it) },
                    currentAudioQuality = audioQualityPreference,
                    onAudioQualityChange = { viewModel.setAudioQuality(it) },
                    onPlaybackSpeedChange = { viewModel.applyPlaybackSpeedFromUi(it) },
                    // [New] Audio Language
                    onAudioLangChange = { viewModel.changeAudioLanguage(it) },

                    //  [新增] 音频模式
                    isAudioOnly = false, // 全屏模式只有视频
                    onAudioOnlyToggle = {
                        viewModel.setAudioMode(true)
                        presentationState.markNavigatingToAudioMode()
                        onNavigateToAudioMode()
                    },

                    //  [新增] 定时关闭
                    sleepTimerMinutes = sleepTimerMinutes,
                    onSleepTimerChange = { viewModel.setSleepTimer(it) },

                    // 🖼️ [新增] 视频预览图数据
                        videoshotData = (uiState as? VideoPlaybackUiState.Success)?.videoshotData,

                    // 📖 [新增] 视频章节数据
                        viewPoints = viewPoints,
                        pbpProgressData = pbpProgressData,
                        sponsorMarkers = sponsorProgressMarkers,
                        onUserSeek = { position -> viewModel.notifyPluginsOfExplicitSeek(position) },
                    // 📱 [新增] 竖屏全屏模式
                    isVerticalVideo = isVerticalVideo && allowStandalonePortraitExperience,
                    isPortraitFullscreen = isPortraitFullscreen,
                    onPortraitFullscreen = {
                        if (allowStandalonePortraitExperience) {
                            if (!isPortraitFullscreen) {
                                if (isFullscreenMode) {
                                    toggleFullscreen()
                                }
                                enterPortraitFullscreen()
                            } else {
                                presentationState.setPortraitFullscreen(false)
                            }
                        }
                    },
                    // 🔁 [新增] 播放模式
                    currentPlayMode = currentPlayMode,
                    onPlayModeClick = { com.android.purebilibili.feature.video.player.PlaylistManager.togglePlayMode() },

                    // [New Actions]
                    onSaveCover = { viewModel.saveCover(context) },
                    onDownloadAudio = { viewModel.downloadAudio(context) },

                    // [新增] 侧边栏抽屉数据与交互
                    relatedVideos = (uiState as? VideoPlaybackUiState.Success)?.related ?: emptyList(),
                    ugcSeason = (uiState as? VideoPlaybackUiState.Success)?.info?.ugc_season,
                    isFollowed = engagementState.isFollowing,
                    isLiked = engagementState.isLiked,
                    isCoined = engagementState.coinCount > 0,
                    isFavorited = engagementState.isFavorited,
                    onToggleFollow = { engagementViewModel.toggleFollow() },
                    onToggleLike = { engagementViewModel.toggleLike() },
                    onDislike = { viewModel.markVideoNotInterested() },
                    onCoin = { engagementViewModel.openCoinDialog() },
                    onToggleFavorite = {
                        openFavoriteFolders(VideoFavoriteEntryPoint.FullscreenOverlay)
                    },
                    onTriple = { engagementViewModel.doTripleAction() },
                    onRelatedVideoClick = navigateToRelatedVideo,
                    onPageSelect = { viewModel.switchPage(it) },
                    hasFavoritePlaylist = isExternalPlaylist &&
                        externalPlaylistSource == ExternalPlaylistSource.FAVORITE &&
                        playlistItems.size > 1,
                    onFavoritePlaylistClick = {
                        showExternalPlaylistQueueSheet = true
                    },
                    forceCoverOnly = forceCoverOnlyForReturn || useResidentCoverForCommittedReturn,
                    useTextureSurfaceForNavigation = transitionEnabled,
                    predictiveBackCancelRecoveryGeneration = predictiveBackCancelRecoveryGeneration,
                    allowLivePlayerSharedElement = true,
                    sourceRouteForSharedElement = sourceRouteForSharedElement,
                    suppressSubtitleOverlay = shouldSuppressSubtitleOverlay,
                    subtitleDisplayModePreferenceOverride = subtitleDisplayModeOverride,
                    onSubtitleDisplayModePreferenceOverrideChange = { subtitleDisplayModeOverride = it },
                    onSubtitleTrackSelected = viewModel::selectSubtitleTrack
                )
            } else {
                    //  沉浸式布局：视频延伸到状态栏 + 内容区域
                    //  📐 [大屏适配] 仅 Expanded 使用分栏布局

                    //  📐 [大屏适配] 根据设备类型选择布局
                    if (useTabletLayout) {
                        // 🖥️ 平板：左右分栏布局（视频+信息 | 评论/推荐）
                        TabletCinemaLayout(
                            playerState = playerState,
                            uiState = uiState,
                            commentState = commentState,
                            engagementState = engagementState,
                            subReplyState = subReplyState,
                            downloadProgress = downloadProgress,
                            tabletCommentPanelWidthPreset = tabletCommentPanelWidthPreset,
                            commentMemberDecorationsEnabled = commentMemberDecorationsEnabled,
                            videoAiSummaryEntryEnabled = videoAiSummaryEntryEnabled,
                            videoNoteEnabled = videoNoteEnabled,
                            videoNoteDefaultCollapsed = videoNoteDefaultCollapsed,
                            playbackActions = playbackActions,
                            engagementActions = engagementActions,
                            commentActions = commentActions,
                            configuration = configuration,
                            isVerticalVideo = isVerticalVideo,
                            sleepTimerMinutes = sleepTimerMinutes,

                            viewPoints = viewPoints,
                            pbpProgressData = pbpProgressData,
                            bvid = bvid,
                            coverUrl = coverUrl,
                            onBack = {
                                com.android.purebilibili.core.util.Logger.d(
                                    "VideoDetailScreen",
                                    "📱 Calling handleBack()"
                                )
                                handleBack()
                            },
                            onUpClick = navigateToUserSpaceFromVideo,
                            onBgmClick = onBgmClick,
                            onNavigateToAudioMode = {
                                presentationState.markNavigatingToAudioMode()
                                onNavigateToAudioMode()
                            },
                            onToggleFullscreen = { toggleFullscreen() },  // 📺 平板全屏切换
                            isInPipMode = isPipMode,
                            onPipClick = handlePipClick,
                            isPortraitFullscreen = isPortraitFullscreen,
                            onHomeClick = {
                                handleTopBarAction(resolveVideoDetailTopBarAction(isHomeButton = true))
                            },

                            transitionEnabled = detailChildTransitionEnabled,  //  传递过渡动画开关
                            // [New] Codec & Audio
                            currentCodec = codecPreference,
                            onCodecChange = { viewModel.setVideoCodec(it) },
                            currentSecondCodec = secondCodecPreference,
                            onSecondCodecChange = { viewModel.setVideoSecondCodec(it) },
                            currentAudioQuality = audioQualityPreference,
                            onAudioQualityChange = { viewModel.setAudioQuality(it) },
                            onRelatedVideoClick = navigateToRelatedVideo,
                            showUpBadge = homeUpBadgesVisible,
                            onSearchKeywordClick = navigateToSearchKeywordFromVideo,
                            onOpenBilibiliLink = onOpenBilibiliLink,
                            // 🔁 [新增] 播放模式
                            currentPlayMode = currentPlayMode,
                            onPlayModeClick = { com.android.purebilibili.feature.video.player.PlaylistManager.togglePlayMode() },
                            forceCoverOnlyOnReturn =
                                forceCoverOnlyForReturn || useResidentCoverForCommittedReturn,
                            predictiveBackCancelRecoveryGeneration = predictiveBackCancelRecoveryGeneration
                        )
                    } else {
                        // 📱 手机竖屏：原有单列布局
                        val stableStatusBarHeight = resolveVideoDetailStableStatusBarHeightDp(
                            visibleStatusBarHeightDp = WindowInsets.statusBars
                                .asPaddingValues()
                                .calculateTopPadding()
                                .value,
                            statusBarIgnoringVisibilityHeightDp = WindowInsets.statusBarsIgnoringVisibility
                                .asPaddingValues()
                                .calculateTopPadding()
                                .value,
                            hideStatusBars = systemBarsVisibilityPolicy.hideStatusBars
                        ).dp
                        val playerTopInset = resolveVideoDetailPortraitPlayerTopInsetDp(
                            stableStatusBarHeightDp = stableStatusBarHeight.value,
                            hideStatusBars = systemBarsVisibilityPolicy.hideStatusBars
                        ).dp
                        val screenWidthDp = configuration.screenWidthDp.dp
                        val screenHeightDp = configuration.screenHeightDp.dp
                        val videoHeight = screenWidthDp * 9f / 16f  // 16:9 比例
                        val uiPreset = LocalUiPreset.current
                        val videoContentTabSwitchAnimationSpec = remember(uiPreset) {
                            resolveVideoContentTabSwitchAnimationSpec(uiPreset)
                        }

                        //  读取竖屏播放器滚动缩小模式
                        val portraitPlayerCollapseMode by com.android.purebilibili.core.store.SettingsManager
                            .getPortraitPlayerCollapseMode(context)
                            .collectAsStateWithLifecycle(initialValue = PortraitPlayerCollapseMode.OFF
            )
                        val inlinePortraitScrollEnabled = shouldEnableInlinePortraitScrollTransform(
                            collapseMode = portraitPlayerCollapseMode,
                            selectedTabIndex = selectedVideoContentTabIndex,
                            isVerticalVideo = isVerticalVideo,
                            isPlaybackPaused = isPlaybackPaused
                        )
                        // 父层只关心折叠阈值，避免列表每个像素的滚动都触发整页重组。
                        var introScrollPastCollapseThreshold by rememberSaveable(currentBvid) {
                            mutableStateOf(
                                isVideoDetailIntroScrollPastCollapseThreshold(
                                    firstVisibleItemIndex = introListState.firstVisibleItemIndex,
                                    firstVisibleItemScrollOffset = introListState.firstVisibleItemScrollOffset
                                )
                            )
                        }
                        val inlinePlayerCollapseState = rememberInlinePortraitPlayerCollapseState(currentBvid)
                        val compactInlinePlayerForCommentTab =
                            shouldUseCompactInlinePortraitPlayerForCommentTab(
                                useOfficialInlinePortraitDetailExperience = useOfficialInlinePortraitDetailExperience,
                                selectedTabIndex = selectedVideoContentTabIndex,
                                isPortraitFullscreen = isPortraitFullscreen,
                                isCommentThreadVisible = subReplyState.visible,
                                collapseMode = portraitPlayerCollapseMode,
                                isVerticalVideo = isVerticalVideo,
                                isPlaybackPaused = isPlaybackPaused
                            )
                        val compactInlinePlayerForIntroScroll =
                            shouldUseCompactInlinePortraitPlayerForIntroScroll(
                                useOfficialInlinePortraitDetailExperience = useOfficialInlinePortraitDetailExperience,
                                selectedTabIndex = selectedVideoContentTabIndex,
                                isPortraitFullscreen = isPortraitFullscreen,
                                firstVisibleItemIndex = if (introScrollPastCollapseThreshold) 1 else 0,
                                firstVisibleItemScrollOffset = 0,
                                collapseMode = portraitPlayerCollapseMode,
                                isVerticalVideo = isVerticalVideo,
                                isPlaybackPaused = isPlaybackPaused
                            )

                        // 📏 [Collapsing Player] 上滑隐藏播放器逻辑
                        val expandedPortraitInlineSpec = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
                            resolvePortraitInlinePlayerLayoutSpec(
                                screenWidthDp = configuration.screenWidthDp.toFloat(),
                                screenHeightDp = configuration.screenHeightDp.toFloat(),
                                isCollapsed = false
                            )
                        }
                        val collapsedPortraitInlineSpec = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
                            resolvePortraitInlinePlayerLayoutSpec(
                                screenWidthDp = configuration.screenWidthDp.toFloat(),
                                screenHeightDp = configuration.screenHeightDp.toFloat(),
                                isCollapsed = true
                            )
                        }
                        val collapseRangePx = with(LocalDensity.current) {
                            if (useOfficialInlinePortraitDetailExperience) {
                                (expandedPortraitInlineSpec.heightDp.dp - collapsedPortraitInlineSpec.heightDp.dp)
                                    .toPx()
                                    .coerceAtLeast(0f)
                            } else {
                                videoHeight.toPx()
                            }
                        }
                        LaunchedEffect(
                            selectedVideoContentTabIndex,
                            compactInlinePlayerForCommentTab,
                            compactInlinePlayerForIntroScroll,
                            portraitPlayerCollapseMode
                        ) {
                            if (!compactInlinePlayerForCommentTab && !compactInlinePlayerForIntroScroll) {
                                inlinePlayerCollapseState.reset()
                            }
                        }
                        TrackJankStateFlag(
                            stateName = "video_detail:player_swipe_collapse",
                            isActive = inlinePortraitScrollEnabled &&
                                abs(inlinePlayerCollapseState.offsetPx) > 0.5f
                        )
                        val isPlayerCollapsed by remember(inlinePortraitScrollEnabled, collapseRangePx) {
                            derivedStateOf {
                                resolveIsPlayerCollapsed(
                                    swipeHidePlayerEnabled = inlinePortraitScrollEnabled,
                                    playerHeightOffsetPx = inlinePlayerCollapseState.offsetPx,
                                    videoHeightPx = collapseRangePx
                                )
                            }
                        }

                        // 当设置关闭时，重置高度
                        LaunchedEffect(inlinePortraitScrollEnabled) {
                            if (!inlinePortraitScrollEnabled) inlinePlayerCollapseState.reset()
                        }

                        val nestedScrollConnection = remember(
                            inlinePortraitScrollEnabled,
                            isPortraitFullscreen,
                            inlinePlayerCollapseState
                        ) {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (available.y != 0f) inlinePlayerCollapseState.beginScroll()
                                    val scrollUpdate = reduceVideoDetailPreScroll(
                                        currentOffsetPx = inlinePlayerCollapseState.offsetPx,
                                        deltaPx = available.y,
                                        minOffsetPx = -collapseRangePx,
                                        inlinePortraitScrollEnabled = inlinePortraitScrollEnabled,
                                        isPortraitFullscreen = isPortraitFullscreen
                                    ) ?: return Offset.Zero
                                    inlinePlayerCollapseState.updateOffset(scrollUpdate.nextOffsetPx)
                                    return Offset(0f, scrollUpdate.consumedDeltaPx)
                                }

                                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                    if (available.y != 0f) inlinePlayerCollapseState.beginScroll()
                                    val scrollUpdate = reduceVideoDetailPostScroll(
                                        currentOffsetPx = inlinePlayerCollapseState.offsetPx,
                                        deltaPx = available.y,
                                        minOffsetPx = -collapseRangePx,
                                        inlinePortraitScrollEnabled = inlinePortraitScrollEnabled,
                                        isPortraitFullscreen = isPortraitFullscreen
                                    ) ?: return Offset.Zero
                                    inlinePlayerCollapseState.updateOffset(scrollUpdate.nextOffsetPx)
                                    return Offset(0f, scrollUpdate.consumedDeltaPx)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                        ) {

                        //  播放器隐藏状态（用于动画）
                        //  播放器隐藏状态（用于动画）
                        //  当 playerHeightOffsetPx 为 -videoHeightPx 时，高度只剩 statusBarHeight
                        //  [Fix] 竖屏全屏模式下强制高度不受偏移影响
                        val playerHeightOffset = if (isPortraitFullscreen) {
                            0f
                        } else {
                            inlinePlayerCollapseState.offsetPx
                        }
                        val collapseProgress = resolveVideoDetailCollapseProgress(
                            playerHeightOffsetPx = playerHeightOffset,
                            collapseRangePx = collapseRangePx,
                            isPortraitFullscreen = isPortraitFullscreen
                        )
                        val commentTabCollapseProgress by animateFloatAsState(
                            targetValue = if (compactInlinePlayerForCommentTab || compactInlinePlayerForIntroScroll) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = resolveInlinePortraitPlayerCommentCollapseDurationMillis(
                                    videoContentTabSwitchAnimationSpec
                                ),
                                easing = FastOutSlowInEasing
                            ),
                            label = "inline_portrait_comment_tab_collapse"
                        )
                        val effectiveCollapseProgress = resolveInlinePortraitPlayerCollapseProgress(
                            manualCollapseProgress = collapseProgress,
                            compactForCommentTabProgress = commentTabCollapseProgress,
                            restoreRequested = inlinePlayerCollapseState.restoreRequested
                        )
                        val expandedViewportHeight = if (useOfficialInlinePortraitDetailExperience) {
                            expandedPortraitInlineSpec.heightDp.dp
                        } else {
                            videoHeight
                        }
                        val collapsedViewportHeight = if (useOfficialInlinePortraitDetailExperience) {
                            collapsedPortraitInlineSpec.heightDp.dp
                        } else {
                            0.dp
                        }
                        val animatedViewportHeight = lerp(
                            expandedViewportHeight,
                            collapsedViewportHeight,
                            effectiveCollapseProgress
                        )
                        val expandedViewportWidth = if (useOfficialInlinePortraitDetailExperience) {
                            expandedPortraitInlineSpec.widthDp.dp
                        } else {
                            screenWidthDp
                        }
                        val collapsedViewportWidth = if (useOfficialInlinePortraitDetailExperience) {
                            collapsedPortraitInlineSpec.widthDp.dp
                        } else {
                            screenWidthDp
                        }
                        val animatedViewportWidth = lerp(
                            expandedViewportWidth,
                            collapsedViewportWidth,
                            effectiveCollapseProgress
                        )
                        val animatedPlayerHeight = animatedViewportHeight + playerTopInset

                        //  注意：移除了状态栏黑色 Spacer
                        // 播放器将延伸到状态栏下方，共享元素过渡更流畅

                        //  注意：移除了状态栏黑色 Spacer
                        // 播放器将延伸到状态栏下方，共享元素过渡更流畅

                        //  视频播放器区域：状态栏可见时避让，隐藏时让画面沉浸到顶部。
                        //  尝试获取共享元素作用域
                        val sharedTransitionScope = LocalSharedTransitionScope.current
                        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                        val coverSharedElementSourceRoute = resolveForcedReturnCoverSharedElementSourceRoute(
                            sourceRouteForSharedElement
                        )

                        //  为播放器容器添加共享元素标记（封面 ↔ 播放器区域映射）
                        val isFullscreenTarget = activeVideoSharedTransitionVisualSpec.fillTargetViewport
                        val playerContainerModifier = if (
                            shouldEnableVideoCoverSharedTransition(
                                transitionEnabled = detailChildTransitionEnabled,
                                hasSharedTransitionScope = sharedTransitionScope != null,
                                hasAnimatedVisibilityScope = animatedVisibilityScope != null
                            ) &&
                            activeVideoSharedTransitionVisualSpec.useCoverSharedBounds &&
                            videoSharedPlaybackIntent == VideoSharedTransitionPlaybackIntent.ImmediatePlayback &&
                            !forceCoverOnlyForReturn
                        ) {
                            with(requireNotNull(sharedTransitionScope)) {
                                Modifier
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(
                                            key = com.android.purebilibili.core.ui.transition.videoCoverSharedElementKey(
                                                bvid,
                                                sourceRoute = coverSharedElementSourceRoute
                                            )
                                        ),
                                        animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                                        boundsTransform = { initialBounds, targetBounds ->
                                            val duration = if (
                                                homeSharedTransitionMotionSpec.enabled && isFullscreenTarget
                                            ) {
                                                homeSharedTransitionMotionSpec.fullscreenDurationMillis
                                            } else {
                                                homeSharedTransitionMotionSpec.durationMillis
                                            }
                                            videoSharedElementBoundsTransformSpec(
                                                motion = homeSharedTransitionMotionSpec,
                                                initialBounds = initialBounds,
                                                targetBounds = targetBounds,
                                                durationMillis = duration
                                            )
                                        },
                                        clipInOverlayDuringTransition = OverlayClip(
                                            RoundedCornerShape(activeVideoSharedTransitionVisualSpec.targetCornerDp.dp)
                                        )
                                    )
                            }
                        } else {
                            Modifier
                        }

                        val isLeaving = useReturningVideoDetailVisualState
                        val crossfadeCoverUrl = remember(coverUrl) {
                            if (coverUrl.isNotBlank()) {
                                val url = coverUrl.trim()
                                when {
                                    url.startsWith("https://") -> url
                                    url.startsWith("http://") -> url.replace("http://", "https://")
                                    url.startsWith("//") -> "https:$url"
                                    else -> url
                                }
                            } else {
                                ""
                            }
                        }
                        val sharedCoverCacheKey = remember(bvid) {
                            resolveVideoSharedCoverCacheKey(bvid)
                        }
                        val residentCoverImageRequest = remember(
                            context,
                            crossfadeCoverUrl,
                            sharedCoverCacheKey,
                        ) {
                            if (crossfadeCoverUrl.isBlank()) {
                                null
                            } else {
                                coil.request.ImageRequest.Builder(context)
                                    .data(crossfadeCoverUrl)
                                    .placeholderMemoryCacheKey(sharedCoverCacheKey)
                                    .crossfade(false)
                                    .memoryCacheKey(sharedCoverCacheKey)
                                    .diskCacheKey(sharedCoverCacheKey)
                                    .build()
                            }
                        }

                        //  播放器容器按当前顶部避让高度计算，避免隐藏状态栏后留下黑边。
                        //  [修复] 始终保持播放器在 Composition 中，避免隐藏时重新创建导致重载
                        Box(
                            modifier = playerContainerModifier
                                .fillMaxWidth()
                                .height(animatedPlayerHeight)  //  使用动画高度（包含0高度）
                                .background(Color.Black)  // 黑色背景
                                .clipToBounds()
                                //  [PiP修复] 捕获视频播放器在屏幕上的位置
                                .onGloballyPositioned { layoutCoordinates ->
                                    val position = layoutCoordinates.positionInWindow()
                                    val rootPosition = layoutCoordinates.positionInRoot()
                                    val size = layoutCoordinates.size
                                    val nextBounds = android.graphics.Rect(
                                        position.x.toInt(),
                                        position.y.toInt(),
                                        position.x.toInt() + size.width,
                                        position.y.toInt() + size.height
                                    )
                                    val nextRootBottomPx = (rootPosition.y + size.height).roundToInt()
                                    if (
                                        videoPlayerRootBottomPx == 0 ||
                                        abs(videoPlayerRootBottomPx - nextRootBottomPx) > 3
                                    ) {
                                        videoPlayerRootBottomPx = nextRootBottomPx
                                    }
                                    if (!hasMeaningfulVideoPlayerBoundsChange(videoPlayerBounds, nextBounds)) {
                                        return@onGloballyPositioned
                                    }
                                    videoPlayerBounds = nextBounds
                                }
                        ) {
                            // 常驻封面叠层：CoverFirst / 非 live morph 时改 alpha；
                            // ImmediatePlayback live morph 保持 cover=0、player=1 一镜到底。
                            if (residentCoverImageRequest != null) {
                                AsyncImage(
                                    model = residentCoverImageRequest,
                                    contentDescription = "cover",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            alpha = resolveVideoDetailReturnCoverAlpha(
                                                transitionProgress = detailTransitionProgress.value,
                                                isCommittedCardReturn = isLeaving,
                                                hasResidentCover = hasResidentReturnCover,
                                                liveReturnMorph = liveReturnMorph,
                                            )
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = playerTopInset)
                                    .graphicsLayer {
                                        alpha = resolveVideoDetailReturnPlayerAlpha(
                                            transitionProgress = detailTransitionProgress.value,
                                            isCommittedCardReturn = isLeaving,
                                            hasResidentCover = hasResidentReturnCover,
                                            liveReturnMorph = liveReturnMorph,
                                        )
                                    }
                            ) {
                            PortraitInlineVideoPlayerHost(
                                modifier = Modifier.align(Alignment.TopCenter),
                                animatedViewportWidth = animatedViewportWidth,
                                animatedViewportHeight = animatedViewportHeight,
                                inlinePlayerAlpha = inlinePlayerAlpha,
                                inlinePlayerScale = inlinePlayerScale,
                                playerState = playerState,
                                uiState = uiState,
                                isPipMode = isPipMode,
                                transitionEnabled = detailChildTransitionEnabled,
                                onToggleFullscreen = { toggleFullscreen() },
                                playbackActions = playbackActions,
                                onDoubleTapLike = engagementViewModel::toggleLike,
                                onBack = handleBack,
                                onHomeClick = {
                                    handleTopBarAction(resolveVideoDetailTopBarAction(isHomeButton = true))
                                },
                                videoPlayerSectionTarget = videoPlayerSectionTarget,
                                sponsorSegment = sponsorSegment,
                                showSponsorSkipButton = showSponsorSkipButton,
                                sleepTimerMinutes = sleepTimerMinutes,
                                viewPoints = viewPoints,
                                pbpProgressData = pbpProgressData,
                                sponsorProgressMarkers = sponsorProgressMarkers,
                                isVerticalVideo = isVerticalVideo && (allowStandalonePortraitExperience || useOfficialInlinePortraitDetailExperience),
                                onPortraitFullscreen = {
                                    when (
                                        resolvePortraitFullscreenButtonAction(
                                            useOfficialInlinePortraitDetailExperience = useOfficialInlinePortraitDetailExperience
                                        )
                                    ) {
                                        PortraitFullscreenButtonAction.ENTER_PORTRAIT_FULLSCREEN -> {
                                            enterPortraitFullscreen()
                                        }
                                    }
                                },
                                isPortraitFullscreen = isPortraitFullscreen,
                                onPipClick = handlePipClick,
                                codecPreference = codecPreference,
                                secondCodecPreference = secondCodecPreference,
                                audioQualityPreference = audioQualityPreference,
                                onNavigateToAudioMode = {
                                    viewModel.setAudioMode(true)
                                    presentationState.markNavigatingToAudioMode()
                                    onNavigateToAudioMode()
                                },
                                forceCoverOnly = forceCoverOnlyForReturn ||
                                    useResidentCoverForCommittedReturn ||
                                    shouldForceBackPreviewPlayerCover(
                                        keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
                                        bindLivePlayerForBackPreview = bindLivePlayerForBackPreview
                                    ),
                                liveBackPreview = bindLivePlayerForBackPreview,
                                useTextureSurfaceForNavigation = transitionEnabled,
                                predictiveBackCancelRecoveryGeneration = predictiveBackCancelRecoveryGeneration,
                                allowLivePlayerSharedElement = true,
                                sourceRouteForSharedElement = sourceRouteForSharedElement,
                                suppressSubtitleOverlay = shouldSuppressSubtitleOverlay,
                                subtitleDisplayModePreferenceOverride = subtitleDisplayModeOverride,
                                onSubtitleDisplayModePreferenceOverrideChange = { subtitleDisplayModeOverride = it }
                            )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .graphicsLayer {
                                    alpha = resolveVideoDetailReturnContentAlpha(
                                        transitionProgress = detailTransitionProgress.value,
                                        isCommittedCardReturn = isLeaving,
                                        holdFullyOpaqueAfterBackPreview =
                                            suppressEnterFadeAfterBackPreview && !isLeaving,
                                        liveReturnMorph = liveReturnMorph,
                                    )
                                }
                                // .nestedScroll(nestedScrollConnection) // [Remove] 移除嵌套滚动，确保 Tabs 正常滑动
                        ) {
                            when (uiState) {
                                is VideoPlaybackUiState.Loading -> {
                                    val loadingState = uiState as VideoPlaybackUiState.Loading
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        //  显示重试进度
                                        if (loadingState.retryAttempt > 0) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    //  iOS 风格加载
                                                    CupertinoActivityIndicator()
                                                    Spacer(Modifier.height(16.dp))
                                                    Text(
                                                        text = "正在重试 ${loadingState.retryAttempt}/${loadingState.maxAttempts}...",
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        } else {
                                            VideoDetailSkeleton()
                                        }
                                    }
                                }

                                is VideoPlaybackUiState.Success -> {
                                    val success = uiState as VideoPlaybackUiState.Success
                                    VideoDetailPhoneSuccessContentLayer(
                                        success = success,
                                        introListState = introListState,
                                        commentListState = commentListState,
                                        videoContentPagerState = videoContentPagerState,
                                        commentState = commentState,
                                        engagementState = engagementState,
                                        androidNativeLiquidGlassEnabled =
                                            homeSettings.androidNativeLiquidGlassEnabled,
                                        commentMemberDecorationsEnabled = commentMemberDecorationsEnabled,
                                        playbackActions = playbackActions,
                                        engagementActions = engagementActions,
                                        commentActions = commentActions,
                                        context = context,
                                        sortPreferenceScope = sortPreferenceScope,
                                        playerState = playerState,
                                        motionSpec = motionSpec,
                                        hazeState = hazeState,
                                        isTransitionFinished = isTransitionFinished ||
                                            keepLoadedContentForBackPreview ||
                                            suppressEnterFadeAfterBackPreview,
                                        isLeaving = isLeaving,
                                        rootTransitionOwnsContentAlpha = detailShellSharedBoundsEnabled,
                                        keepContentVisibleAfterBackPreview =
                                            suppressEnterFadeAfterBackPreview ||
                                                keepLoadedContentForBackPreview,
                                        shouldShowExternalPlaylistQueueBar = shouldShowExternalPlaylistQueueBar,
                                        selectedVideoContentTabIndex = selectedVideoContentTabIndex,
                                        useTabletLayout = useTabletLayout,
                                        isFullscreenMode = isFullscreenMode,
                                        isPortraitFullscreen = isPortraitFullscreen,
                                        showCommentInput = showCommentInput,
                                        isCommentThreadVisible = subReplyState.visible,
                                        showFavoriteFolderDialog = showFavoriteFolderDialog,
                                        downloadProgress = downloadProgress,
                                        danmakuEnabledForDetail = danmakuEnabledForDetail,
                                        isQuickReturnLimitedForSharedElements =
                                            isReturningFromDetail && isQuickReturningFromDetail,
                                        transitionEnabled = detailChildTransitionEnabled,
                                        sourceRouteForSharedElement = sourceRouteForSharedElement,
                                        isPlayerCollapsed = isPlayerCollapsed,
                                        onRestorePlayer = inlinePlayerCollapseState::restore,
                                        onBgmClick = onBgmClick,
                                        homeUpBadgesVisible = homeUpBadgesVisible,
                                        isVideoPlaying = isVideoPlaying,
                                        onSelectedTabChange = presentationState::selectTab,
                                        onIntroScrollThresholdChange = {
                                            introScrollPastCollapseThreshold = it
                                        },
                                        openFavoriteFolders = openFavoriteFolders,
                                        navigateToUserSpaceFromVideo = navigateToUserSpaceFromVideo,
                                        navigateToRelatedVideo = navigateToRelatedVideo,
                                        openCommentUrl = openCommentUrl,
                                        onOpenBilibiliLink = onOpenBilibiliLink,
                                        onShareVideo = { payload -> pendingVideoShare = payload },
                                        externalPlaylistQueueTitle = externalPlaylistQueueTitle,
                                        playlistItems = playlistItems,
                                        onShowExternalPlaylistQueueSheet = {
                                            showExternalPlaylistQueueSheet = true
                                        }
                                    )
                            } // End of Success block

                                is VideoPlaybackUiState.Error -> {
                                    val errorState = uiState as VideoPlaybackUiState.Error
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(32.dp)
                                        ) {
                                            //  根据错误类型显示不同图标
                                            Text(
                                                text = when (errorState.error) {
                                                    is com.android.purebilibili.data.model.VideoLoadError.NetworkError -> "📡"
                                                    is com.android.purebilibili.data.model.VideoLoadError.VideoNotFound -> "🔍"
                                                    is com.android.purebilibili.data.model.VideoLoadError.RegionRestricted -> "🌐"
                                                    is com.android.purebilibili.data.model.VideoLoadError.RateLimited -> "⏳"
                                                    is com.android.purebilibili.data.model.VideoLoadError.GlobalCooldown -> ""
                                                    is com.android.purebilibili.data.model.VideoLoadError.PlayUrlEmpty -> "⚡"
                                                    else -> ""
                                                },
                                                fontSize = 48.sp
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                text = errorState.msg,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 16.sp,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )

                                            //  针对风控错误显示额外建议
                                            when (errorState.error) {
                                                is com.android.purebilibili.data.model.VideoLoadError.GlobalCooldown,
                                                is com.android.purebilibili.data.model.VideoLoadError.PlayUrlEmpty -> {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = " 建议：切换 WiFi/移动数据 或 清除缓存后重试",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 13.sp,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                                is com.android.purebilibili.data.model.VideoLoadError.RateLimited -> {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = " 该视频可能暂时不可用，请尝试其他视频",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 13.sp,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                                else -> {}
                                            }

                                            //  只有可重试的错误才显示重试按钮（或者风控错误允许强制重试）
                                            val showRetryButton = errorState.canRetry ||
                                                errorState.error is com.android.purebilibili.data.model.VideoLoadError.RateLimited ||
                                                errorState.error is com.android.purebilibili.data.model.VideoLoadError.PlayUrlEmpty
                                            if (showRetryButton) {
                                                Spacer(Modifier.height(24.dp))
                                                Button(
                                                    onClick = { viewModel.retry() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Text(
                                                        text = when (errorState.error) {
                                                            is com.android.purebilibili.data.model.VideoLoadError.RateLimited -> "强制重试"
                                                            is com.android.purebilibili.data.model.VideoLoadError.GlobalCooldown -> "清除冷却并重试"
                                                            else -> "重试"
                                                        }
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                    }
                    }  // 📱 手机竖屏布局结束（Column）
                    }  // Box with nested scroll
                }  // else shouldUseSplitLayout
            }  // else targetIsLandscape
    }

    @Composable
    fun BoxScope.VideoDetailRouteSheetOverlayContent() {
        VideoDetailPortraitOverlayAdapter(
            uiState = uiState,
            portraitExperienceEnabled = portraitExperienceEnabled,
            isPortraitFullscreen = isPortraitFullscreen,
            useOfficialInlinePortraitDetailExperience = useOfficialInlinePortraitDetailExperience,
            isLandscape = isLandscape,
            shouldAnimatePortraitPager = shouldAnimatePortraitPager,
            motionSpec = portraitPagerMotionSpec,
            initialBvidOverride = pendingMainReloadBvidAfterPortrait,
            initialStartPositionMs = portraitSyncSnapshotPositionMs,
            playbackViewModel = viewModel,
            engagementViewModel = engagementViewModel,
            sharedPlayer = if (useSharedPortraitPlayer) playerState.player else null,
            onBack = { presentationState.setPortraitFullscreen(false) },
            onHomeClick = {
                presentationState.setPortraitFullscreen(false)
                handleTopBarAction(resolveVideoDetailTopBarAction(isHomeButton = true))
            },
            onVideoChange = { portraitPendingSelectionBvid = it },
            onProgressUpdate = { updatedBvid, positionMs, updatedCid ->
                portraitPendingSelectionBvid = updatedBvid
                portraitSyncSnapshotBvid = updatedBvid
                portraitSyncSnapshotCid = updatedCid
                portraitSyncSnapshotPositionMs = positionMs.coerceAtLeast(0L)
                if (shouldMirrorPortraitProgressToMainPlayer) {
                    hasPendingPortraitSync = true
                    if (tryApplyPortraitProgressSync(updatedBvid, portraitSyncSnapshotPositionMs)) {
                        hasPendingPortraitSync = false
                    }
                }
            },
            onExitSnapshot = { updatedBvid, positionMs, updatedCid ->
                presentationState.switchVideo(updatedBvid, updatedCid)
                portraitPendingSelectionBvid = updatedBvid
                portraitSyncSnapshotBvid = updatedBvid
                portraitSyncSnapshotCid = updatedCid
                portraitSyncSnapshotPositionMs = positionMs.coerceAtLeast(0L)
                pendingMainReloadBvidAfterPortrait = updatedBvid
                if (shouldMirrorPortraitProgressToMainPlayer) {
                    hasPendingPortraitSync = true
                    if (tryApplyPortraitProgressSync(updatedBvid, portraitSyncSnapshotPositionMs)) {
                        hasPendingPortraitSync = false
                    }
                }
            },
            onSearchClick = {
                hasDeferredPortraitRestoreAfterExternalNavigation =
                    com.android.purebilibili.feature.video.ui.pager
                        .shouldDeferPortraitRestoreUntilForegroundResume(
                            isPortraitFullscreen = isPortraitFullscreen,
                            isExternalNavigation = true,
                        )
                if (com.android.purebilibili.feature.video.ui.pager
                        .shouldExitPortraitForExternalNavigation(isPortraitFullscreen)
                ) {
                    presentationState.setPortraitFullscreen(false)
                }
                navigateToSearchFromVideo()
            },
            onUserClick = { mid ->
                val anchorBvid = portraitPendingSelectionBvid
                    ?: pendingMainReloadBvidAfterPortrait
                    ?: portraitSyncSnapshotBvid
                    ?: (uiState as? VideoPlaybackUiState.Success)?.info?.bvid
                if (!anchorBvid.isNullOrBlank()) {
                    val anchorCid = if (anchorBvid == portraitSyncSnapshotBvid) {
                        portraitSyncSnapshotCid
                    } else {
                        0L
                    }
                    presentationState.switchVideo(anchorBvid, anchorCid)
                    pendingMainReloadBvidAfterPortrait = anchorBvid
                }
                hasDeferredPortraitRestoreAfterExternalNavigation =
                    com.android.purebilibili.feature.video.ui.pager
                        .shouldDeferPortraitRestoreUntilForegroundResume(
                            isPortraitFullscreen = isPortraitFullscreen,
                            isExternalNavigation = true,
                        )
                if (com.android.purebilibili.feature.video.ui.pager
                        .shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen)
                ) {
                    presentationState.setPortraitFullscreen(false)
                }
                navigateToUserSpaceFromVideo(mid)
            },
            onRotateToLandscape = {
                presentationState.setPortraitFullscreen(false)
                val hostActivity = context.findActivity()
                val targetOrientation = resolvePortraitRotateTargetOrientation(
                    isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
                    manualPortraitHoldActive = manualPortraitHoldActive,
                )
                if (hostActivity != null && targetOrientation != null) {
                    userRequestedFullscreen = true
                    manualPortraitHoldActive = false
                    hostActivity.requestedOrientation = targetOrientation
                } else {
                    toggleFullscreen()
                }
            },
        )

        VideoDetailCommonOverlayAdapter(
            interactiveChoicePanel = interactiveChoicePanel,
            engagementState = engagementState,
            playbackViewModel = viewModel,
            engagementViewModel = engagementViewModel,
            queueVisible = shouldShowExternalPlaylistQueueBar && showExternalPlaylistQueueSheet,
            queueTitle = externalPlaylistQueueTitle,
            playlist = playlistItems,
            playlistCurrentIndex = playlistCurrentIndex,
            hazeState = hazeState,
            queuePresentation = externalPlaylistQueueSheetPresentation,
            pendingVideoShare = pendingVideoShare,
            player = playerState.player,
            onDismissQueue = { showExternalPlaylistQueueSheet = false },
            onVideoSelected = { index, item ->
                PlaylistManager.playAt(index)
                showExternalPlaylistQueueSheet = false
                switchVideoInCurrentDetailPage(
                    targetBvid = item.bvid,
                    targetCid = 0L,
                    autoPlay = true,
                )
            },
            onDismissShare = { pendingVideoShare = null },
        )

        val inputOverlayLayoutInfo = VideoDetailInputOverlayAdapter(
            context = context,
            configuration = configuration,
            viewModel = viewModel,
            commentState = commentState,
            showCommentInput = showCommentInput,
            isLandscape = isLandscape,
            isFullscreenMode = isFullscreenMode,
            isPortraitFullscreen = isPortraitFullscreen,
            videoPlayerRootBottomPx = videoPlayerRootBottomPx,
            hideStatusBars = systemBarsVisibilityPolicy.hideStatusBars,
            currentVideoPositionMsProvider = {
                playerState.player.currentPosition.coerceAtLeast(0L)
            },
        )
        val screenHeightPx = inputOverlayLayoutInfo.screenHeightPx
        val danmakuDialogTopReservePx = inputOverlayLayoutInfo.topReservedPx

        VideoDetailDownloadOverlayAdapter(
            viewModel = viewModel,
            uiState = uiState,
        )

        val successState = uiState as? VideoPlaybackUiState.Success
        DetachedVideoCommentThreadHost(
            visible = shouldShowDetachedVideoCommentThreadHost(useTabletLayout = useTabletLayout),
            successState = successState,
            commentState = commentState,
            commentViewModel = commentViewModel,
            forceInitialize = shouldForceInitializeDetachedCommentThreadHostForRoute(
                routeCommentRootRpid = openCommentRootRpidFromRoute,
                aid = successState?.info?.aid ?: 0L,
                hasHandledRouteComment = hasHandledCommentRootFromRoute
            ),
            viewModel = viewModel,
            onUpClick = navigateToUserSpaceFromVideo,
            onNavigateToRelatedVideo = { targetVideoId ->
                navigateToRelatedVideo(targetVideoId, null)
            },
            onSearchKeywordClick = navigateToSearchKeywordFromVideo,
            onOpenBilibiliLink = onOpenBilibiliLink,
            screenHeightPx = screenHeightPx,
            topReservedPx = danmakuDialogTopReservePx,
            onTimestampClick = { positionMs ->
                seekPlayerFromUserAction(playerState.player, positionMs)
                commentViewModel.closeSubReply()
            }
        )

        VideoDetailFavoriteFolderOverlayAdapter(
            visible = showFavoriteFolderDialog,
            viewModel = viewModel,
        )

        VideoDetailFeedbackOverlayAdapter(
            playbackViewModel = viewModel,
            engagementViewModel = engagementViewModel,
            engagementState = engagementState,
            playbackEventState = playbackEventState,
            hazeState = hazeState,
            isFullscreenMode = isFullscreenMode,
            isLandscape = isLandscape,
            reducedMotion = isReducedActionMotion,
        )

        VideoDetailPlayerSettingsOverlayAdapter(
            context = context,
            viewModel = viewModel,
            isFullscreenMode = isFullscreenMode,
            danmakuManager = danmakuManager,
        )
    }

    VideoDetailScreenContent(
        transitionState = transitionState,
        routeSheetMotion = routeSheetMotion,
        isFullscreenMode = isFullscreenMode,
        backgroundColor = MaterialTheme.colorScheme.background,
        modifier = detailShellModifier,
        mainContent = { VideoDetailRouteSheetMainContent() },
        overlayContent = { VideoDetailRouteSheetOverlayContent() }
    )
}
