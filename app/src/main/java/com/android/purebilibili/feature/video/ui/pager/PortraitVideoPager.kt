package com.android.purebilibili.feature.video.ui.pager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import com.android.purebilibili.feature.video.ui.section.shouldKeepVideoPlaybackAwake
import com.android.purebilibili.feature.video.usecase.seekPlayerFromUserAction
import com.android.purebilibili.feature.video.usecase.togglePlayerPlaybackFromUserAction
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.store.DanmakuSettings
import com.android.purebilibili.core.util.NetworkUtils
import com.android.purebilibili.feature.plugin.PlaybackCdnPlugin
import com.android.purebilibili.core.store.DanmakuSettingsScope
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.core.store.PortraitDanmakuDisplayAreaMode
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.rememberIsolatedDanmakuManager
import com.android.purebilibili.feature.video.playback.session.PlaybackSeekSessionState
import com.android.purebilibili.feature.video.playback.session.SEEK_PLAYBACK_RECOVERY_DELAY_MS
import com.android.purebilibili.feature.video.playback.session.shouldAttemptPlaybackRecoveryAfterSeek
import com.android.purebilibili.feature.video.playback.session.shouldShowPlaybackRecoveryUiAfterSeek
import com.android.purebilibili.feature.video.playback.session.cancelPlaybackSeekInteraction
import com.android.purebilibili.feature.video.playback.session.finishPlaybackSeekInteraction
import com.android.purebilibili.feature.video.playback.session.shouldUsePlaybackSeekSessionPosition
import com.android.purebilibili.feature.video.playback.session.startPlaybackSeekInteraction
import com.android.purebilibili.feature.video.playback.session.syncPlaybackSeekSession
import com.android.purebilibili.feature.video.playback.session.updatePlaybackSeekInteraction
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.components.AspectRatioMenu
import com.android.purebilibili.feature.video.ui.components.QualitySelectionMenu
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenuDialog
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.components.resolveSafeVideoAspectRatio
import com.android.purebilibili.feature.video.ui.overlay.PortraitFullscreenOverlay
import com.android.purebilibili.feature.video.player.resolveHandleAudioFocusByPolicy
import com.android.purebilibili.feature.video.ui.section.FOREGROUND_SURFACE_RECOVERY_DELAY_MS
import com.android.purebilibili.feature.video.ui.section.resolveLongPressPlaybackParameters
import com.android.purebilibili.feature.video.ui.section.rebindPlayerSurfaceIfNeeded
import com.android.purebilibili.feature.video.ui.section.shouldKickPlaybackAfterSurfaceRecovery
import com.android.purebilibili.feature.video.viewmodel.PlaybackEndAction
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoComposerViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoEngagementUiState
import com.android.purebilibili.feature.video.viewmodel.VideoSupplementViewModel
import com.android.purebilibili.feature.video.usecase.TripleActionResult
import com.android.purebilibili.feature.video.viewmodel.toEngagementSeed
import com.android.purebilibili.feature.video.viewmodel.toSupplementSeed
import com.android.purebilibili.feature.video.viewmodel.withEngagementUiState
import com.android.purebilibili.feature.video.viewmodel.resolvePlaybackCompletionRepeatMode
import com.android.purebilibili.feature.video.viewmodel.resolvePlaybackEndAction
import com.bytedance.danmaku.render.engine.DanmakuView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal data class PortraitVideoInteractionOverride(
    val isLiked: Boolean? = null,
    val isFavorited: Boolean? = null,
    val likeCount: Int? = null,
    val favoriteCount: Int? = null
)

internal data class PortraitVideoInteractionUiState(
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val likeCount: Int,
    val favoriteCount: Int
)

internal enum class PortraitFavoriteAction {
    OpenFavoriteFolders
}

internal enum class PortraitDanmakuSurfaceMode {
    VideoViewport,
    Page
}

/**
 * 竖屏无缝滑动播放页面 (TikTok Style)
 * 
 * @param initialBvid 初始视频 BVID
 * @param initialInfo 初始视频详情
 * @param recommendations 推荐视频列表
 * @param onBack 返回回调
 * @param onVideoChange 切换视频回调 (当滑动到新视频时通知外部)
 */

@UnstableApi
@Composable
fun PortraitVideoPager(
    initialBvid: String,
    initialInfo: ViewInfo,
    recommendations: List<RelatedVideo>,
    isActive: Boolean = true,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = onBack,
    onVideoChange: (String) -> Unit,
    viewModel: VideoPlaybackViewModel,
    engagementViewModel: VideoEngagementViewModel,
    sharedPlayer: ExoPlayer? = null,
    initialStartPositionMs: Long = 0L,
    onProgressUpdate: (String, Long, Long) -> Unit = { _, _, _ -> },
    onExitSnapshot: (String, Long, Long) -> Unit = { _, _, _ -> },
    onSearchClick: () -> Unit = {},
    onUserClick: (Long) -> Unit,
    onRotateToLandscape: () -> Unit
) {
    val context = LocalContext.current
    val composerViewModel: VideoComposerViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(key = "portrait_composer_$initialBvid")
    val supplementViewModel: VideoSupplementViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(key = "portrait_supplement_$initialBvid")
    val commentViewModel: VideoCommentViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(
            key = "portrait_comments_$initialBvid"
        )
    val playbackDomainState by viewModel.uiState.collectAsStateWithLifecycle()
    val engagementState by engagementViewModel.uiState.collectAsStateWithLifecycle()
    val subjectSnapshot by viewModel.subjectSnapshot.collectAsStateWithLifecycle()
    val engagementSeed = (playbackDomainState as? VideoPlaybackUiState.Success)?.toEngagementSeed()
    val supplementSeed = (playbackDomainState as? VideoPlaybackUiState.Success)?.toSupplementSeed()
    LaunchedEffect(subjectSnapshot, engagementSeed) {
        val subject = subjectSnapshot ?: return@LaunchedEffect
        val seed = engagementSeed ?: return@LaunchedEffect
        engagementViewModel.bindSubject(subject, seed)
        composerViewModel.bindSubject(subject)
    }
    LaunchedEffect(subjectSnapshot, supplementSeed, isActive) {
        val subject = subjectSnapshot ?: return@LaunchedEffect
        val seed = supplementSeed ?: return@LaunchedEffect
        supplementViewModel.bindSubject(subject, seed)
        supplementViewModel.setVisible(isActive)
    }
    val useSharedPlayer = sharedPlayer != null
    val entryStartPositionMs = remember(initialBvid) { initialStartPositionMs.coerceAtLeast(0L) }
    val scope = rememberCoroutineScope()
    val danmakuManager = rememberIsolatedDanmakuManager(
        sessionKey = "portrait_danmaku_$initialBvid"
    )
    val danmakuScope = DanmakuSettingsScope.PORTRAIT
    val loadedDanmakuSettings by produceState<DanmakuSettings?>(
        initialValue = null,
        context,
        danmakuScope
    ) {
        SettingsManager.getDanmakuSettings(context, danmakuScope).collect { value = it }
    }
    val danmakuSettingsLoaded = loadedDanmakuSettings != null
    val danmakuSettings = loadedDanmakuSettings ?: DanmakuSettings()
    val danmakuEnabled = danmakuSettingsLoaded && danmakuSettings.enabled
    val danmakuOpacity = danmakuSettings.opacity
    val danmakuFontScale = danmakuSettings.fontScale
    val effectiveDanmakuFontScale = resolvePortraitDanmakuReadableFontScale(danmakuFontScale)
    val danmakuSpeed = danmakuSettings.speed
    val danmakuDisplayArea = danmakuSettings.displayArea
    val portraitDanmakuDisplayAreaMode = danmakuSettings.portraitDisplayAreaMode
    val danmakuMergeDuplicates = danmakuSettings.mergeDuplicates
    val danmakuDuplicateMergeWindowMs = danmakuSettings.duplicateMergeWindowMs
    val danmakuDuplicateMergeCountThreshold = danmakuSettings.duplicateMergeCountThreshold
    val danmakuAllowScroll = danmakuSettings.allowScroll
    val danmakuAllowTop = danmakuSettings.allowTop
    val danmakuAllowBottom = danmakuSettings.allowBottom
    val danmakuAllowColorful = danmakuSettings.allowColorful
    val danmakuAllowSpecial = danmakuSettings.allowSpecial
    val danmakuBlockRules = danmakuSettings.blockRules
    val danmakuSmartOcclusion = danmakuSettings.smartOcclusion
    val autoPlayEnabled by SettingsManager
        .getAutoPlay(context)
        .collectAsStateWithLifecycle(initialValue = true
        )
    val externalPlaylistAutoContinueEnabled by SettingsManager
        .getExternalPlaylistAutoContinue(context)
        .collectAsStateWithLifecycle(initialValue = true
        )
    val playbackCompletionBehavior by SettingsManager
        .getPlaybackCompletionBehavior(context)
        .collectAsStateWithLifecycle(initialValue = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC
        )
    val isExternalPlaylist by PlaylistManager.isExternalPlaylist.collectAsStateWithLifecycle()
    val prefetchVideoEnabled by SettingsManager.getPrefetchVideo(context)
        .collectAsStateWithLifecycle(initialValue = false)
    // Align portrait pager playurl qn with detail-page playable default (Wi‑Fi / VIP / auto-highest).
    val portraitDefaultQuality = remember(context) {
        NetworkUtils.getPlayableDefaultQualityId(
            context = context,
            isLoggedIn = !TokenManager.sessDataCache.isNullOrEmpty(),
            isVip = TokenManager.isVipCache
        )
    }
    var portraitSelectedQuality by remember {
        mutableIntStateOf(resolvePortraitPlaybackTargetQuality(portraitDefaultQuality))
    }
    var portraitDisplayedQuality by remember {
        mutableIntStateOf(resolvePortraitPlaybackTargetQuality(portraitDefaultQuality))
    }
    var portraitAvailableQualityIds by remember {
        mutableStateOf(
            listOf(resolvePortraitPlaybackTargetQuality(portraitDefaultQuality))
        )
    }
    val portraitQualityLabel = remember(portraitDisplayedQuality) {
        resolvePortraitQualityLabel(portraitDisplayedQuality)
    }
    var portraitAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    val isPortraitLoggedIn = !TokenManager.sessDataCache.isNullOrEmpty()
    val isPortraitVip = TokenManager.isVipCache
    val portraitMediaSourceFactory = remember(context) {
        buildPortraitCachedMediaSourceFactory(context)
    }
    val portraitPlaybackCdnPlugin = remember {
        PluginManager.getEnabledPlugins(PlaybackCdnPlugin::class).firstOrNull()
    }
    val portraitPrefetchedPlayUrlBvids = remember { mutableSetOf<String>() }
    val recommendationShuffleSeed = remember(initialInfo.bvid, initialInfo.aid) {
        resolvePortraitRecommendationShuffleSeed(
            initialBvid = initialInfo.bvid,
            initialAid = initialInfo.aid
        )
    }

    val baseRecommendations = remember(recommendations, recommendationShuffleSeed) {
        shufflePortraitRecommendations(
            seed = recommendationShuffleSeed,
            recommendations = recommendations
        )
    }
    val initialPageIndex = remember(initialBvid, initialInfo.bvid, baseRecommendations) {
        resolvePortraitInitialPageIndex(
            initialBvid = initialBvid,
            initialInfoBvid = initialInfo.bvid,
            recommendations = baseRecommendations
        )
    }
    val recommendationItems = remember(initialInfo.bvid, baseRecommendations) {
        mutableStateListOf<RelatedVideo>().apply {
            addAll(baseRecommendations)
        }
    }
    val pageItems = remember(initialInfo.bvid, baseRecommendations) {
        mutableStateListOf<Any>().apply {
            add(initialInfo)
            addAll(baseRecommendations)
        }
    }
    val knownVideoAspectRatios = remember(initialInfo.bvid) {
        mutableStateMapOf<String, Float>().apply {
            resolveAspectRatioFromDimension(initialInfo.dimension)?.let { aspectRatio ->
                put(initialInfo.bvid, aspectRatio)
            }
        }
    }
    var watchLaterVideos by remember { mutableStateOf<List<RelatedVideo>>(emptyList()) }
    var isLoadingMoreRecommendations by remember { mutableStateOf(false) }
    val appendedRecommendationSeeds = remember { mutableStateListOf<String>() }
    var recommendationFeedCursor by rememberSaveable(initialInfo.bvid) { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (TokenManager.sessDataCache.isNullOrEmpty()) {
            watchLaterVideos = emptyList()
            return@LaunchedEffect
        }
        runCatching { NetworkModule.api.getWatchLaterList() }
            .onSuccess { response ->
                if (response.code == 0) {
                    watchLaterVideos = response.data?.list
                        .orEmpty()
                        .mapNotNull(::toRelatedVideoFromWatchLater)
                        .distinctBy { it.bvid }
                }
            }
            .onFailure {
                watchLaterVideos = emptyList()
            }
    }

    LaunchedEffect(watchLaterVideos) {
        if (watchLaterVideos.isEmpty()) return@LaunchedEffect
        val existingBvids = withContext(Dispatchers.Main.immediate) {
            snapshotPortraitPageBvids(pageItems)
        }
        val appendItems = watchLaterVideos.filter { it.bvid !in existingBvids }
        if (appendItems.isNotEmpty()) {
            withContext(Dispatchers.Main.immediate) {
                pageItems.addAll(appendItems)
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = initialPageIndex) {
        pageItems.size
    }
    LaunchedEffect(initialInfo.bvid) {
        val discoveryRecommendations = VideoRepository.getHomeVideos(idx = 0)
            .getOrNull()
            .orEmpty()
            .mapNotNull(::toRelatedVideoForPortraitRecommendation)
        if (discoveryRecommendations.isEmpty()) return@LaunchedEffect

        val shuffledDiscoveryRecommendations = shufflePortraitRecommendations(
            seed = resolvePortraitRecommendationAppendSeed(
                baseSeed = recommendationShuffleSeed,
                currentBvid = initialInfo.bvid
            ),
            recommendations = discoveryRecommendations
        )
        val insertion = withContext(Dispatchers.Main.immediate) {
            recommendationFeedCursor = 1
            mergePortraitRecommendationAppendItems(
                currentBvid = initialInfo.bvid,
                existingBvids = snapshotPortraitPageBvids(pageItems),
                existingRecommendations = recommendationItems.toList(),
                fetchedRecommendations = shuffledDiscoveryRecommendations
            )
        }
        if (insertion.isEmpty()) return@LaunchedEffect

        withContext(Dispatchers.Main.immediate) {
            val insertNearHead = pagerState.currentPage == 0 && !pagerState.isScrollInProgress
            if (insertNearHead) {
                recommendationItems.addAll(0, insertion)
                pageItems.addAll(1, insertion)
            } else {
                recommendationItems.addAll(insertion)
                pageItems.addAll(insertion)
            }
        }
    }
    var currentPageScale by remember { mutableFloatStateOf(1f) }

    // [重构] 优先复用主播放器；仅在未传入 sharedPlayer 时才创建页内播放器
    val exoPlayer = sharedPlayer ?: remember(context) {
        val audioFocusEnabled = SettingsManager.getAudioFocusEnabledSync(context)
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                resolveHandleAudioFocusByPolicy(audioFocusEnabled = audioFocusEnabled)
            )
            .build()
            .apply {
                repeatMode = resolvePortraitPagerRepeatMode()
                volume = com.android.purebilibili.core.player.PlayerVolumeController
                    .preferredVolumeSync()
                setPlaybackSpeed(SettingsManager.getPreferredPlaybackSpeedSync(context))
            }
    }
    LaunchedEffect(exoPlayer, playbackCompletionBehavior) {
        exoPlayer.repeatMode = resolvePlaybackCompletionRepeatMode(playbackCompletionBehavior)
    }

    if (!useSharedPlayer) {
        DisposableEffect(exoPlayer) {
            onDispose {
                exoPlayer.release()
            }
        }
    }

    val sharedPlayerHasFrameAtEntry = remember(useSharedPlayer, exoPlayer) {
        useSharedPlayer &&
            exoPlayer.videoSize.width > 0 &&
            exoPlayer.videoSize.height > 0
    }

    val sharedPlayerShouldResumeAtEntry = remember(useSharedPlayer, exoPlayer) {
        useSharedPlayer && (exoPlayer.playWhenReady || exoPlayer.isPlaying)
    }

    // [状态] 当前播放的视频 URL
    var currentPlayingBvid by remember(initialBvid, useSharedPlayer) {
        mutableStateOf(
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = useSharedPlayer,
                initialBvid = initialBvid
            )
        )
    }
    var currentPlayingCid by remember(initialInfo.cid, useSharedPlayer) {
        mutableStateOf(if (useSharedPlayer) initialInfo.cid else 0L)
    }
    var currentPlayingAid by remember(initialInfo.aid, useSharedPlayer) {
        mutableStateOf(if (useSharedPlayer) initialInfo.aid else 0L)
    }
    var isLoading by remember { mutableStateOf(false) }
    var lastCommittedPage by remember(useSharedPlayer) {
        mutableIntStateOf(if (useSharedPlayer) 0 else -1)
    }
    var activeLoadGeneration by remember { mutableIntStateOf(0) }
    var lastSwipePrefetchPage by remember { mutableIntStateOf(-1) }
    var lastEarlyPlaybackPage by remember { mutableIntStateOf(-1) }
    var hasConsumedInitialSeek by remember(useSharedPlayer) { mutableStateOf(useSharedPlayer) }
    var pendingAutoPlayGeneration by remember { mutableIntStateOf(-1) }
    var renderedFirstFrameGeneration by remember(useSharedPlayer, sharedPlayerHasFrameAtEntry) {
        mutableIntStateOf(
            resolvePortraitInitialRenderedFirstFrameGeneration(
                useSharedPlayer = useSharedPlayer,
                sharedPlayerHasFrameAtEntry = sharedPlayerHasFrameAtEntry
            )
        )
    }
    var lastAutoAdvancedBvid by remember { mutableStateOf<String?>(null) }
    var pendingUserSpaceNavigation by rememberSaveable { mutableStateOf(false) }
    var portraitOverlayVisible by rememberSaveable(initialInfo.bvid) { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val isPortraitPlaybackAllowed = shouldAllowPortraitPlayback(
        isCurrentStoryTab = isActive,
        isLifecycleResumed = isLifecycleResumed
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleResumed = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> isLifecycleResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        danmakuManager.attachPlayer(exoPlayer)
        onDispose { }
    }

    LaunchedEffect(exoPlayer, isPortraitPlaybackAllowed) {
        if (isPortraitPlaybackAllowed) return@LaunchedEffect
        pendingAutoPlayGeneration = -1
        exoPlayer.playWhenReady = false
        exoPlayer.pause()
    }

    DisposableEffect(exoPlayer, activeLoadGeneration, isPortraitPlaybackAllowed) {
        val autoPlayListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY &&
                    pendingAutoPlayGeneration == activeLoadGeneration &&
                    isPortraitPlaybackAllowed &&
                    !exoPlayer.isPlaying
                ) {
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && pendingAutoPlayGeneration == activeLoadGeneration) {
                    pendingAutoPlayGeneration = -1
                }
            }

            override fun onRenderedFirstFrame() {
                renderedFirstFrameGeneration = activeLoadGeneration
                isLoading = false
            }
        }
        exoPlayer.addListener(autoPlayListener)
        onDispose {
            exoPlayer.removeListener(autoPlayListener)
        }
    }

    LaunchedEffect(useSharedPlayer, sharedPlayerShouldResumeAtEntry, isPortraitPlaybackAllowed) {
        if (useSharedPlayer && sharedPlayerShouldResumeAtEntry && isPortraitPlaybackAllowed) {
            exoPlayer.playWhenReady = true
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
        }
    }

    LaunchedEffect(pendingUserSpaceNavigation, pagerState.currentPage, pageItems.size) {
        if (!pendingUserSpaceNavigation) return@LaunchedEffect
        val item = pageItems.getOrNull(pagerState.currentPage)
        val expectedBvid = when (item) {
            is ViewInfo -> item.bvid
            is RelatedVideo -> item.bvid
            else -> ""
        }
        val shouldResync = shouldResyncPortraitPagerOnUserSpaceReturn(
            pendingUserSpaceNavigation = pendingUserSpaceNavigation,
            expectedBvid = expectedBvid,
            currentPlayingBvid = currentPlayingBvid,
            currentPlayerMediaId = exoPlayer.currentMediaItem?.mediaId
        )
        pendingUserSpaceNavigation = false
        if (!shouldResync) return@LaunchedEffect

        currentPlayingBvid = null
        currentPlayingCid = 0L
        currentPlayingAid = 0L
        lastCommittedPage = -1
        isLoading = true
    }

    DisposableEffect(lifecycleOwner, pagerState, pageItems, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event != androidx.lifecycle.Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

            val item = pageItems.getOrNull(pagerState.currentPage)
            val expectedBvid = when (item) {
                is ViewInfo -> item.bvid
                is RelatedVideo -> item.bvid
                else -> ""
            }
            val shouldResync = shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = pendingUserSpaceNavigation,
                expectedBvid = expectedBvid,
                currentPlayingBvid = currentPlayingBvid,
                currentPlayerMediaId = exoPlayer.currentMediaItem?.mediaId
            )
            pendingUserSpaceNavigation = false
            if (!shouldResync) return@LifecycleEventObserver

            currentPlayingBvid = null
            currentPlayingCid = 0L
            currentPlayingAid = 0L
            lastCommittedPage = -1
            isLoading = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(
        exoPlayer,
        pagerState,
        pageItems.size,
        currentPlayingBvid,
        autoPlayEnabled,
        externalPlaylistAutoContinueEnabled,
        playbackCompletionBehavior,
        isExternalPlaylist,
        isPortraitPlaybackAllowed
    ) {
        val autoAdvanceListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isPortraitPlaybackAllowed) return
                if (playbackState != Player.STATE_ENDED) return
                val playbackEndAction = resolvePlaybackEndAction(
                    behavior = playbackCompletionBehavior,
                    autoPlayEnabled = autoPlayEnabled,
                    isExternalPlaylist = isExternalPlaylist,
                    externalPlaylistAutoContinueEnabled = externalPlaylistAutoContinueEnabled,
                    hasNextPageOrSeasonTarget = false
                )
                when (playbackEndAction) {
                    PlaybackEndAction.STOP -> return
                    PlaybackEndAction.REPEAT_CURRENT -> {
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                        exoPlayer.play()
                    }
                    PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
                    PlaybackEndAction.AUTO_CONTINUE -> {
                        val playingBvid = currentPlayingBvid ?: return
                        if (lastAutoAdvancedBvid == playingBvid) return
                        lastAutoAdvancedBvid = playingBvid

                        val nextPage = resolveNextPortraitPageAfterPlaybackEnd(
                            action = playbackEndAction,
                            currentPage = pagerState.currentPage,
                            lastPage = pageItems.lastIndex
                        ) ?: return

                        scope.launch {
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }
                    PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP -> {
                        val playingBvid = currentPlayingBvid ?: return
                        if (lastAutoAdvancedBvid == playingBvid) return
                        lastAutoAdvancedBvid = playingBvid

                        val nextPage = resolveNextPortraitPageAfterPlaybackEnd(
                            action = PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP,
                            currentPage = pagerState.currentPage,
                            lastPage = pageItems.lastIndex
                        ) ?: return
                        scope.launch {
                            pagerState.animateScrollToPage(nextPage)
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(autoAdvanceListener)
        onDispose {
            exoPlayer.removeListener(autoAdvanceListener)
        }
    }

    fun requestPortraitPlaybackForPage(
        targetPage: Int,
        applyInitialSeekOnFirstPage: Boolean,
        forceReload: Boolean = false,
        resumePositionMs: Long? = null,
    ): Boolean {
        val item = pageItems.getOrNull(targetPage) ?: return false
        val playbackIdentity = resolvePortraitPagePlaybackIdentity(item) ?: return false
        val bvid = playbackIdentity.bvid
        val aid = playbackIdentity.aid
        val requestedCid = playbackIdentity.cid
        val targetQuality = resolvePortraitPlaybackTargetQuality(portraitSelectedQuality)

        if (!isPortraitPlaybackAllowed) {
            pendingAutoPlayGeneration = -1
            isLoading = false
            return false
        }

        if (
            !forceReload &&
            shouldSkipPortraitReloadForCurrentMedia(
                currentPlayingBvid = currentPlayingBvid,
                targetBvid = bvid,
                currentPlayerMediaId = exoPlayer.currentMediaItem?.mediaId
            )
        ) {
            isLoading = false
            return false
        }

        val seekResumeMs = resumePositionMs
            ?.coerceAtLeast(0L)
            ?: if (forceReload) exoPlayer.currentPosition.coerceAtLeast(0L) else null

        activeLoadGeneration += 1
        val requestGeneration = activeLoadGeneration

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        danmakuManager.clearForVideoChange()
        isLoading = true
        currentPlayingBvid = bvid
        currentPlayingCid = 0L
        currentPlayingAid = 0L
        pendingAutoPlayGeneration = requestGeneration
        renderedFirstFrameGeneration = -1

        scope.launch {
            try {
                val result = VideoRepository.getPortraitPlaybackDetails(
                    bvid = bvid,
                    aid = aid,
                    requestedCid = requestedCid,
                    targetQuality = targetQuality
                )

                result.fold(
                    onSuccess = { (info, playData) ->
                        val streamUrls = resolvePortraitPlaybackStreamUrls(
                            playData = playData,
                            targetQuality = targetQuality
                        ) ?: run {
                                pendingAutoPlayGeneration = -1
                                if (shouldApplyLoadResult(
                                        requestGeneration = requestGeneration,
                                        activeGeneration = activeLoadGeneration,
                                        expectedBvid = bvid,
                                        currentPlayingBvid = currentPlayingBvid
                                    )
                                ) {
                                    isLoading = false
                                }
                                return@fold
                            }
                        val resolvedUrls = resolvePortraitPlaybackCdnUrls(
                            streamUrls = streamUrls,
                            cachedDashVideos = playData.dash?.video.orEmpty(),
                            cachedDashAudios = playData.dash?.audio.orEmpty(),
                            targetQuality = targetQuality,
                            cdnPlugin = portraitPlaybackCdnPlugin
                        )
                        val dashVideoIds = playData.dash?.video?.map { it.id }?.distinct().orEmpty()
                        val availableQualityIds = resolvePortraitAvailableQualityIds(
                            acceptQualities = playData.accept_quality,
                            dashVideoIds = dashVideoIds
                        )
                        if (availableQualityIds.isNotEmpty()) {
                            portraitAvailableQualityIds = availableQualityIds
                        }
                        portraitDisplayedQuality = resolvePortraitDisplayedQualityId(
                            requestedQuality = targetQuality,
                            returnedQuality = playData.quality,
                            dashVideoIds = dashVideoIds
                        )

                        val videoItem = MediaItem.Builder()
                            .setUri(resolvedUrls.videoUrl)
                            .setMediaId(bvid)
                            .build()
                        val videoSource = portraitMediaSourceFactory.createMediaSource(videoItem)
                        val finalSource = resolvedUrls.audioUrl?.takeIf { it.isNotEmpty() }?.let { audioUrl ->
                            val audioItem = MediaItem.Builder()
                                .setUri(audioUrl)
                                .setMediaId("audio_$bvid")
                                .build()
                            val audioSource = portraitMediaSourceFactory.createMediaSource(audioItem)
                            MergingMediaSource(videoSource, audioSource)
                        } ?: videoSource

                        if (!shouldApplyLoadResult(
                                requestGeneration = requestGeneration,
                                activeGeneration = activeLoadGeneration,
                                expectedBvid = bvid,
                                currentPlayingBvid = currentPlayingBvid
                            )
                        ) {
                            com.android.purebilibili.core.util.Logger.d(
                                "PortraitVideoPager",
                                "Discarded stale video load for $bvid (request=$requestGeneration, active=$activeLoadGeneration, current=$currentPlayingBvid)"
                            )
                            return@fold
                        }

                        resolveAspectRatioFromDimension(info.dimension)?.let { aspectRatio ->
                            knownVideoAspectRatios[bvid] = aspectRatio
                        }
                        currentPlayingCid = info.cid
                        currentPlayingAid = info.aid
                        exoPlayer.playWhenReady = isPortraitPlaybackAllowed
                        exoPlayer.setMediaSource(finalSource)
                        exoPlayer.prepare()

                        when {
                            seekResumeMs != null && seekResumeMs > 0L -> {
                                exoPlayer.seekTo(seekResumeMs)
                            }
                            applyInitialSeekOnFirstPage && entryStartPositionMs > 0 && !hasConsumedInitialSeek -> {
                                exoPlayer.seekTo(entryStartPositionMs)
                                hasConsumedInitialSeek = true
                            }
                        }

                        if (isPortraitPlaybackAllowed) {
                            exoPlayer.play()
                        }
                    },
                    onFailure = {
                        pendingAutoPlayGeneration = -1
                        if (shouldApplyLoadResult(
                                requestGeneration = requestGeneration,
                                activeGeneration = activeLoadGeneration,
                                expectedBvid = bvid,
                                currentPlayingBvid = currentPlayingBvid
                            )
                        ) {
                            isLoading = false
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                pendingAutoPlayGeneration = -1
                if (shouldApplyLoadResult(
                        requestGeneration = requestGeneration,
                        activeGeneration = activeLoadGeneration,
                        expectedBvid = bvid,
                        currentPlayingBvid = currentPlayingBvid
                    )
                ) {
                    isLoading = false
                }
            }
        }
        return true
    }

    LaunchedEffect(pagerState, pageItems) {
        snapshotFlow {
            Triple(
                pagerState.isScrollInProgress,
                pagerState.currentPage,
                pagerState.currentPageOffsetFraction
            )
        }.collect { (isScrollInProgress, currentPage, offsetFraction) ->
            if (!isScrollInProgress) {
                lastSwipePrefetchPage = -1
                return@collect
            }

            val targetPage = resolvePortraitSwipePrefetchTargetPage(
                isScrollInProgress = isScrollInProgress,
                currentPage = currentPage,
                currentPageOffsetFraction = offsetFraction,
                lastPageIndex = pageItems.lastIndex
            ) ?: return@collect
            if (targetPage == lastSwipePrefetchPage) return@collect
            lastSwipePrefetchPage = targetPage

            val identity = pageItems.getOrNull(targetPage)
                ?.let(::resolvePortraitPagePlaybackIdentity)
                ?: return@collect
            if (!portraitPrefetchedPlayUrlBvids.add(identity.bvid)) return@collect
            if (identity.cid <= 0L) return@collect

            launch(Dispatchers.IO) {
                runCatching {
                    VideoRepository.preloadPortraitPlayUrl(
                        bvid = identity.bvid,
                        cid = identity.cid,
                        targetQuality = resolvePortraitPlaybackTargetQuality(portraitSelectedQuality)
                    )
                }
            }
        }
    }

    LaunchedEffect(pagerState, pageItems, isPortraitPlaybackAllowed, portraitSelectedQuality) {
        snapshotFlow {
            Triple(
                pagerState.isScrollInProgress,
                pagerState.currentPage,
                pagerState.currentPageOffsetFraction
            )
        }.collect { (isScrollInProgress, currentPage, offsetFraction) ->
            if (!isScrollInProgress) {
                lastEarlyPlaybackPage = -1
                return@collect
            }
            if (!isPortraitPlaybackAllowed) return@collect

            val targetPage = resolvePortraitEarlyPlaybackPage(
                isScrollInProgress = isScrollInProgress,
                currentPage = currentPage,
                currentPageOffsetFraction = offsetFraction,
                lastPageIndex = pageItems.lastIndex
            ) ?: return@collect
            if (targetPage == lastEarlyPlaybackPage) return@collect
            lastEarlyPlaybackPage = targetPage

            requestPortraitPlaybackForPage(
                targetPage = targetPage,
                applyInitialSeekOnFirstPage = false
            )
        }
    }

    // [核心] 页面 settle 后提交评论/推荐切换；播放流在 settle 或高阈值 early playback 时绑定
    LaunchedEffect(pagerState, pageItems, isPortraitPlaybackAllowed) {
        snapshotFlow {
            resolveCommittedPage(
                isScrollInProgress = pagerState.isScrollInProgress,
                currentPage = pagerState.currentPage,
                lastCommittedPage = lastCommittedPage
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { committedPage ->
                lastCommittedPage = committedPage
                val item = pageItems.getOrNull(committedPage) ?: return@collect
                val playbackIdentity = resolvePortraitPagePlaybackIdentity(item) ?: return@collect
                val bvid = playbackIdentity.bvid

                commentViewModel.clearForVideoChange()
                onVideoChange(bvid)

                if (!isPortraitPlaybackAllowed) {
                    pendingAutoPlayGeneration = -1
                    isLoading = false
                    return@collect
                }

                if (
                    shouldLoadMorePortraitRecommendations(
                        committedPage = committedPage,
                        totalItemsCount = pageItems.size,
                        isLoadingMoreRecommendations = isLoadingMoreRecommendations
                    ) &&
                    bvid !in appendedRecommendationSeeds
                ) {
                    appendedRecommendationSeeds.add(bvid)
                    isLoadingMoreRecommendations = true
                    launch {
                        try {
                            val (existingBvids, existingRecommendations, homeFeedCursor) = withContext(Dispatchers.Main.immediate) {
                                Triple(
                                    snapshotPortraitPageBvids(pageItems),
                                    recommendationItems.toList(),
                                    recommendationFeedCursor
                                )
                            }
                            val homeFeedRecommendations = VideoRepository.getHomeVideos(idx = homeFeedCursor)
                                .getOrNull()
                                .orEmpty()
                                .mapNotNull(::toRelatedVideoForPortraitRecommendation)
                            withContext(Dispatchers.Main.immediate) {
                                recommendationFeedCursor = homeFeedCursor + 1
                            }
                            val relatedFallbackRecommendations = if (homeFeedRecommendations.size < 8) {
                                VideoRepository.getRelatedVideos(bvid)
                            } else {
                                emptyList()
                            }
                            val shuffledFetchedRecommendations = shufflePortraitRecommendations(
                                seed = resolvePortraitRecommendationAppendSeed(
                                    baseSeed = recommendationShuffleSeed,
                                    currentBvid = bvid
                                ),
                                recommendations = homeFeedRecommendations + relatedFallbackRecommendations
                            )
                            val appendItems = mergePortraitRecommendationAppendItems(
                                currentBvid = bvid,
                                existingBvids = existingBvids,
                                existingRecommendations = existingRecommendations,
                                fetchedRecommendations = shuffledFetchedRecommendations
                            )
                            if (appendItems.isNotEmpty()) {
                                withContext(Dispatchers.Main.immediate) {
                                    recommendationItems.addAll(appendItems)
                                    pageItems.addAll(appendItems)
                                }
                            }
                        } finally {
                            withContext(Dispatchers.Main.immediate) {
                                isLoadingMoreRecommendations = false
                            }
                        }
                    }
                }

                requestPortraitPlaybackForPage(
                    targetPage = committedPage,
                    applyInitialSeekOnFirstPage = committedPage == 0
                )

                launch(Dispatchers.IO) {
                    val pageSnapshot = pageItems.toList()
                    val preloadCount = resolvePortraitPlayUrlPreloadCount(
                        prefetchVideoEnabled = prefetchVideoEnabled,
                        isWifi = NetworkUtils.isWifi(context),
                        availableTargets = (pageSnapshot.size - committedPage - 1).coerceAtLeast(0)
                    )
                    val preloadTargets = resolvePortraitPlayUrlPreloadTargets(
                        committedPage = committedPage,
                        pageItems = pageSnapshot,
                        preloadCount = preloadCount
                    )
                    preloadTargets.forEach { target ->
                        if (!portraitPrefetchedPlayUrlBvids.add(target.bvid)) return@forEach
                        if (target.cid <= 0L) return@forEach
                        runCatching {
                            VideoRepository.preloadPortraitPlayUrl(
                                bvid = target.bvid,
                                cid = target.cid,
                                targetQuality = resolvePortraitPlaybackTargetQuality(portraitSelectedQuality)
                            )
                        }
                    }
                }
            }
    }

    LaunchedEffect(currentPlayingCid, currentPlayingAid, danmakuEnabled, danmakuSettingsLoaded, exoPlayer) {
        if (shouldLoadPortraitDanmaku(danmakuSettingsLoaded, currentPlayingCid, danmakuEnabled)) {
            danmakuManager.isEnabled = true
            var durationMs = exoPlayer.duration
            var retries = 0
            while (durationMs <= 0 && retries < 50) {
                delay(100)
                durationMs = exoPlayer.duration
                retries++
            }
            danmakuManager.loadDanmaku(currentPlayingCid, currentPlayingAid, durationMs.coerceAtLeast(0L))
        } else {
            danmakuManager.isEnabled = false
        }
    }

    LaunchedEffect(viewModel, danmakuManager) {
        viewModel.danmakuSentEvent.collect { danmakuData ->
            danmakuManager.addLocalDanmaku(
                text = danmakuData.text,
                color = danmakuData.color,
                mode = danmakuData.mode,
                fontSize = danmakuData.fontSize
            )
        }
    }

    LaunchedEffect(
        danmakuOpacity,
        danmakuFontScale,
        effectiveDanmakuFontScale,
        danmakuSpeed,
        danmakuDisplayArea,
        danmakuMergeDuplicates,
        danmakuDuplicateMergeWindowMs,
        danmakuDuplicateMergeCountThreshold,
        danmakuAllowScroll,
        danmakuAllowTop,
        danmakuAllowBottom,
        danmakuAllowColorful,
        danmakuAllowSpecial,
        danmakuBlockRules,
        danmakuSmartOcclusion,
        danmakuSettingsLoaded
    ) {
        if (!danmakuSettingsLoaded) return@LaunchedEffect
        danmakuManager.updateSettings(
            opacity = danmakuOpacity,
            fontScale = effectiveDanmakuFontScale,
            speed = danmakuSpeed,
            displayArea = danmakuDisplayArea,
            mergeDuplicates = danmakuMergeDuplicates,
            duplicateMergeWindowMs = danmakuDuplicateMergeWindowMs,
            duplicateMergeCountThreshold = danmakuDuplicateMergeCountThreshold,
            allowScroll = danmakuAllowScroll,
            allowTop = danmakuAllowTop,
            allowBottom = danmakuAllowBottom,
            allowColorful = danmakuAllowColorful,
            allowSpecial = danmakuAllowSpecial,
            blockedRules = danmakuBlockRules,
            // Mask-only mode: keep lane layout fixed, do not move danmaku tracks.
            smartOcclusion = false
        )
    }

    var portraitCommentOverlayActive by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) {
        portraitCommentOverlayActive = false
    }

    VerticalPager(
        state = pagerState,
        userScrollEnabled = shouldEnablePortraitPagerUserScroll(
            scale = currentPageScale,
            commentOverlayActive = portraitCommentOverlayActive
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { page ->
        val item = pageItems.getOrNull(page)
        val handleUserClick: (Long) -> Unit = { mid ->
            pendingUserSpaceNavigation = true
            onUserClick(mid)
        }
        
        if (item != null) {
            val itemBvid = when (item) {
                is ViewInfo -> item.bvid
                is RelatedVideo -> item.bvid
                else -> ""
            }
            VideoPageItem(
                item = item,
                isCurrentPage = page == pagerState.currentPage,
                onBack = onBack,
                onHomeClick = onHomeClick,
                viewModel = viewModel,
                commentViewModel = commentViewModel,
                engagementState = engagementState,
                onToggleFollow = engagementViewModel::toggleFollow,
                onToggleLike = engagementViewModel::toggleLike,
                onTripleAction = engagementViewModel::doTripleAction,
                onOpenCoinDialog = engagementViewModel::openCoinDialog,
                exoPlayer = exoPlayer, // [核心] 传递共享播放器
                currentPlayingBvid = currentPlayingBvid, // [修复] 传递当前播放的 BVID 用于校验
                currentPlayingCid = currentPlayingCid,
                currentPlayingAid = currentPlayingAid,
                isPortraitPlaybackAllowed = isPortraitPlaybackAllowed,
                isLoading = if (page == pagerState.currentPage) isLoading else false, // 只有当前页显示 Loading
                danmakuManager = danmakuManager,
                danmakuEnabled = danmakuEnabled,
                danmakuSmartOcclusion = danmakuSmartOcclusion,
                portraitDanmakuDisplayAreaMode = portraitDanmakuDisplayAreaMode,
                onExitSnapshot = onExitSnapshot,
                onSearchClick = onSearchClick,
                onUserClick = handleUserClick,
                onRotateToLandscape = onRotateToLandscape,
                onProgressUpdate = onProgressUpdate,
                watchLaterVideos = watchLaterVideos,
                recommendationVideos = recommendationItems,
                knownVideoAspectRatio = knownVideoAspectRatios[itemBvid]
                    ?: (item as? ViewInfo)?.dimension?.let(::resolveAspectRatioFromDimension),
                qualityLabel = portraitQualityLabel,
                selectedQualityId = portraitSelectedQuality,
                availableQualityIds = portraitAvailableQualityIds,
                aspectRatio = portraitAspectRatio,
                isLoggedIn = isPortraitLoggedIn,
                isVip = isPortraitVip,
                onQualitySelected = { qualityId ->
                    if (qualityId <= 0 || qualityId == portraitSelectedQuality) return@VideoPageItem
                    portraitSelectedQuality = qualityId
                    portraitDisplayedQuality = qualityId
                    requestPortraitPlaybackForPage(
                        targetPage = pagerState.currentPage,
                        applyInitialSeekOnFirstPage = false,
                        forceReload = true
                    )
                },
                onAspectRatioChange = { ratio ->
                    // Runtime safety is applied per-page from actual video aspect.
                    portraitAspectRatio = ratio
                },
                hasRenderedFirstFrame = (renderedFirstFrameGeneration == activeLoadGeneration),
                initialProgressPositionMs = resolvePortraitInitialProgressPosition(
                    isFirstPage = page == 0,
                    initialStartPositionMs = entryStartPositionMs
                ),
                onCurrentPageScaleChange = { scale ->
                    if (page == pagerState.currentPage) {
                        currentPageScale = scale
                    }
                },
                onCommentOverlayActiveChange = { active ->
                    if (page == pagerState.currentPage) {
                        portraitCommentOverlayActive = active
                    }
                },
                portraitOverlayVisible = portraitOverlayVisible,
                onPortraitOverlayVisibleChange = { visible ->
                    portraitOverlayVisible = visible
                },
                onRequestVideoChange = { targetBvid ->
                    val targetIndex = pageItems.indexOfFirst { candidate ->
                        when (candidate) {
                            is ViewInfo -> candidate.bvid == targetBvid
                            is RelatedVideo -> candidate.bvid == targetBvid
                            else -> false
                        }
                    }
                    if (targetIndex >= 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(targetIndex)
                        }
                    }
                }
            )
        }
    }

    com.android.purebilibili.feature.video.ui.components.CoinDialog(
        visible = engagementState.coinDialogVisible,
        currentCoinCount = engagementState.coinCount,
        userBalance = engagementState.userCoinBalance,
        onDismiss = { engagementViewModel.setCoinDialogVisible(false) },
        onConfirm = { count, alsoLike ->
            engagementViewModel.doCoin(count = count, alsoLike = alsoLike)
        }
    )
}

@UnstableApi
@Composable
private fun VideoPageItem(
    item: Any,
    isCurrentPage: Boolean,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    viewModel: VideoPlaybackViewModel,
    commentViewModel: VideoCommentViewModel,
    engagementState: VideoEngagementUiState,
    onToggleFollow: (Long?, Boolean?) -> Unit,
    onToggleLike: (Long?, String?, Boolean?, ((Boolean) -> Unit)?) -> Unit,
    onTripleAction: (Long?, String?, Boolean?, Int?, Boolean?, ((TripleActionResult) -> Unit)?) -> Unit,
    onOpenCoinDialog: () -> Unit,
    exoPlayer: ExoPlayer,
    currentPlayingBvid: String?, // [新增]
    currentPlayingCid: Long,
    currentPlayingAid: Long,
    isPortraitPlaybackAllowed: Boolean,
    isLoading: Boolean,
    danmakuManager: DanmakuManager,
    danmakuEnabled: Boolean,
    danmakuSmartOcclusion: Boolean,
    portraitDanmakuDisplayAreaMode: PortraitDanmakuDisplayAreaMode,
    onExitSnapshot: (String, Long, Long) -> Unit,
    onSearchClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    onRotateToLandscape: () -> Unit,
    onProgressUpdate: (String, Long, Long) -> Unit,
    watchLaterVideos: List<RelatedVideo>,
    recommendationVideos: List<RelatedVideo>,
    knownVideoAspectRatio: Float?,
    qualityLabel: String,
    selectedQualityId: Int,
    availableQualityIds: List<Int>,
    aspectRatio: VideoAspectRatio,
    isLoggedIn: Boolean,
    isVip: Boolean,
    onQualitySelected: (Int) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    hasRenderedFirstFrame: Boolean,
    initialProgressPositionMs: Long,
    onCurrentPageScaleChange: (Float) -> Unit,
    onCommentOverlayActiveChange: (Boolean) -> Unit = {},
    portraitOverlayVisible: Boolean,
    onPortraitOverlayVisibleChange: (Boolean) -> Unit,
    onRequestVideoChange: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    val longPressSpeed by SettingsManager
        .getLongPressSpeed(context)
        .collectAsStateWithLifecycle(initialValue = 2.0f
        )
    val currentAudioQuality by viewModel.audioQualityPreference.collectAsStateWithLifecycle(initialValue = -1
        )
    val bvid = if (item is ViewInfo) item.bvid else (item as RelatedVideo).bvid
    val itemAid = if (item is ViewInfo) item.aid else (item as RelatedVideo).aid
    
    // [修复] 手动监听 ExoPlayer 播放状态，确保 UI 及时更新
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPlaybackSpeed by remember(exoPlayer) {
        mutableFloatStateOf(exoPlayer.playbackParameters.speed)
    }
    var showSpeedMenu by rememberSaveable(bvid) { mutableStateOf(false) }
    var showQualityMenu by rememberSaveable(bvid) { mutableStateOf(false) }
    var showRatioMenu by rememberSaveable(bvid) { mutableStateOf(false) }
    var keepPortraitPagerAwake by remember(exoPlayer) {
        mutableStateOf(
            shouldKeepVideoPlaybackAwake(
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                playbackState = exoPlayer.playbackState
            )
        )
    }
    var currentVideoAspect by remember(bvid, currentPlayingBvid, knownVideoAspectRatio) {
        mutableFloatStateOf(
            resolvePortraitInitialVideoAspectRatio(
                itemBvid = bvid,
                currentPlayingBvid = currentPlayingBvid,
                playerVideoWidth = exoPlayer.videoSize.width,
                playerVideoHeight = exoPlayer.videoSize.height,
                knownVideoAspectRatio = knownVideoAspectRatio
            )
        )
    }
    
    DisposableEffect(exoPlayer) {
        fun updateAwakeState() {
            keepPortraitPagerAwake = shouldKeepVideoPlaybackAwake(
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                playbackState = exoPlayer.playbackState
            )
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying_: Boolean) {
                isPlaying = isPlaying_
                updateAwakeState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateAwakeState()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateAwakeState()
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                currentPlaybackSpeed = playbackParameters.speed
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    currentVideoAspect = resolvePortraitRuntimeVideoAspectRatio(
                        knownVideoAspectRatio = knownVideoAspectRatio,
                        playerVideoWidth = videoSize.width,
                        playerVideoHeight = videoSize.height
                    )
                }
            }
        }
        exoPlayer.addListener(listener)
        currentPlaybackSpeed = exoPlayer.playbackParameters.speed
        updateAwakeState()
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // [逻辑] 只有当播放器正在播放当前视频时，才显示 PlayerView
    val isPlayerReadyForThisVideo = bvid == currentPlayingBvid
    val shouldKeepPortraitPagerItemAwake = isPortraitPlaybackAllowed &&
        keepPortraitPagerAwake &&
        isPlayerReadyForThisVideo
    val snapshotCid = if (isPlayerReadyForThisVideo && currentPlayingCid > 0L) {
        currentPlayingCid
    } else {
        0L
    }
    val activeAid = resolvePortraitActiveAid(
        isPlayerReadyForThisVideo = isPlayerReadyForThisVideo,
        itemAid = itemAid,
        currentPlayingAid = currentPlayingAid
    )

    LaunchedEffect(
        playerViewRef,
        isCurrentPage,
        isPlayerReadyForThisVideo,
        currentPlayingBvid,
        exoPlayer.videoSize
    ) {
        val view = playerViewRef ?: return@LaunchedEffect
        if (!shouldRebindSharedPlayerSurfaceOnAttach(
                isCurrentPage = isCurrentPage,
                isPlayerReadyForThisVideo = isPlayerReadyForThisVideo,
                hasPlayerView = true,
                videoWidth = exoPlayer.videoSize.width,
                videoHeight = exoPlayer.videoSize.height
            )
        ) {
            return@LaunchedEffect
        }

        // Force the shared player to hand over its surface to the portrait pager view.
        rebindPlayerSurfaceIfNeeded(playerView = view, player = exoPlayer)
    }

    DisposableEffect(
        lifecycleOwner,
        exoPlayer,
        playerViewRef,
        isCurrentPage,
        isPlayerReadyForThisVideo
    ) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event != androidx.lifecycle.Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val view = playerViewRef
            if (
                !shouldRecoverPortraitPagerSurfaceOnResume(
                    isCurrentPage = isCurrentPage,
                    isPlayerReadyForThisVideo = isPlayerReadyForThisVideo,
                    hasPlayerView = view != null
                )
            ) {
                return@LifecycleEventObserver
            }

            view?.let { playerView ->
                rebindPlayerSurfaceIfNeeded(playerView = playerView, player = exoPlayer)
            }
            scope.launch {
                delay(FOREGROUND_SURFACE_RECOVERY_DELAY_MS)
                val retryView = playerViewRef ?: return@launch
                if (
                    !shouldRecoverPortraitPagerSurfaceOnResume(
                        isCurrentPage = isCurrentPage,
                        isPlayerReadyForThisVideo = isPlayerReadyForThisVideo,
                        hasPlayerView = true
                    )
                ) {
                    return@launch
                }

                rebindPlayerSurfaceIfNeeded(playerView = retryView, player = exoPlayer)
                if (
                    shouldKickPlaybackAfterSurfaceRecovery(
                        playWhenReady = exoPlayer.playWhenReady,
                        isPlaying = exoPlayer.isPlaying,
                        playbackState = exoPlayer.playbackState
                    )
                ) {
                    exoPlayer.play()
                }
                danmakuManager.recoverAfterForeground(
                    positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                    playWhenReady = exoPlayer.playWhenReady,
                    playbackState = exoPlayer.playbackState
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val title = if (item is ViewInfo) item.title else (item as RelatedVideo).title
    val cover = if (item is ViewInfo) item.pic else (item as RelatedVideo).pic
    val seedAuthorName = if (item is ViewInfo) item.owner.name else (item as RelatedVideo).owner.name
    val seedAuthorFace = if (item is ViewInfo) item.owner.face else (item as RelatedVideo).owner.face
    val seedAuthorMid = if (item is ViewInfo) item.owner.mid else (item as RelatedVideo).owner.mid

    // 提取时长
    val initialDuration = if (item is RelatedVideo) {
        item.duration * 1000L
    } else if (item is ViewInfo) {
        (item.pages.firstOrNull()?.duration ?: 0L) * 1000L
    } else {
        0L
    }

    // 互动状态
    var showCommentSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var detailSheetUpOnlyMode by remember { mutableStateOf(false) }
    var commentSheetVisibilityProgress by remember { mutableFloatStateOf(0f) }
    val subReplyState by commentViewModel.subReplyState.collectAsStateWithLifecycle()
    var portraitPageWidthPx by remember { mutableIntStateOf(0) }
    var portraitPageHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val isVerticalContent = currentVideoAspect < 1f
    val portraitPagerFillContainer = resolvePortraitPagerFillContainer(
        aspectRatio = aspectRatio,
        isVerticalContent = isVerticalContent
    )
    val portraitPagerResizeMode = resolvePortraitPagerResizeMode(
        aspectRatio = aspectRatio,
        isVerticalContent = isVerticalContent
    )
    val portraitViewportVerticalOffsetPx = with(density) {
        resolvePortraitVideoViewportVerticalOffsetDp(
            currentVideoAspect = currentVideoAspect,
            fillContainer = portraitPagerFillContainer,
            isLandscape = portraitPageWidthPx > portraitPageHeightPx
        ).dp.toPx()
    }
    val commentExpansionTransform = resolvePortraitCommentPlayerTransform(
        commentVisibilityProgress = commentSheetVisibilityProgress,
        containerWidthPx = portraitPageWidthPx,
        containerHeightPx = portraitPageHeightPx,
        currentVideoAspect = currentVideoAspect,
        viewportVerticalOffsetPx = portraitViewportVerticalOffsetPx,
        fillContainer = portraitPagerFillContainer
    )

    LaunchedEffect(isCurrentPage, bvid) {
        if (!isCurrentPage) {
            showCommentSheet = false
            showDetailSheet = false
            commentSheetVisibilityProgress = 0f
            onCommentOverlayActiveChange(false)
        }
    }

    LaunchedEffect(
        isCurrentPage,
        showCommentSheet,
        subReplyState.visible,
        commentSheetVisibilityProgress
    ) {
        onCommentOverlayActiveChange(
            isCurrentPage && shouldBlockPortraitPagerScrollForCommentOverlay(
                commentSheetVisible = showCommentSheet,
                subReplyVisible = subReplyState.visible,
                commentVisibilityProgress = commentSheetVisibilityProgress
            )
        )
    }

    // 进度状态 (从播放器获取)
    var progressState by remember(bvid, initialDuration, initialProgressPositionMs) {
        mutableStateOf(
            PlayerProgress(
                current = initialProgressPositionMs.coerceAtLeast(0L),
                duration = initialDuration,
                buffered = initialProgressPositionMs.coerceAtLeast(0L)
            )
        )
    }
    var seekSession by remember(bvid, initialProgressPositionMs) {
        mutableStateOf(
            syncPlaybackSeekSession(
                state = PlaybackSeekSessionState(),
                playbackPositionMs = initialProgressPositionMs.coerceAtLeast(0L)
            )
        )
    }
    
    // 如果是当前页，监听播放器进度
    LaunchedEffect(isCurrentPage, exoPlayer, hasRenderedFirstFrame, isPortraitPlaybackAllowed) {
        if (isCurrentPage && isPortraitPlaybackAllowed) {
            while (true) {
                if (isPlayerReadyForThisVideo) {
                    val playerPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val effectivePosition = if (
                        playerPosition == 0L &&
                        progressState.current > 0L &&
                        !hasRenderedFirstFrame
                    ) {
                        progressState.current
                    } else {
                        playerPosition
                    }
                    val realDuration = if (exoPlayer.duration > 0) exoPlayer.duration else initialDuration
                    seekSession = syncPlaybackSeekSession(
                        state = seekSession,
                        playbackPositionMs = effectivePosition,
                        hasPlaybackResumedAfterPendingSeek = exoPlayer.isPlaying
                    )
                    progressState = PlayerProgress(
                        current = seekSession.sliderPositionMs,
                        duration = realDuration,
                        buffered = exoPlayer.bufferedPosition
                    )
                    if (exoPlayer.isPlaying || effectivePosition > 0L) {
                        onProgressUpdate(bvid, effectivePosition, snapshotCid)
                    }
                }
                delay(200)
            }
        }
    }

    LaunchedEffect(
        seekSession.pendingSeekPositionMs,
        exoPlayer.playWhenReady,
        exoPlayer.isPlaying,
        exoPlayer.playbackState,
        isCurrentPage,
        isPortraitPlaybackAllowed
    ) {
        if (!isCurrentPage || !isPortraitPlaybackAllowed) return@LaunchedEffect
        if (!shouldAttemptPlaybackRecoveryAfterSeek(
                state = seekSession,
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                playbackState = exoPlayer.playbackState
            )
        ) {
            return@LaunchedEffect
        }

        delay(SEEK_PLAYBACK_RECOVERY_DELAY_MS)
        if (!shouldAttemptPlaybackRecoveryAfterSeek(
                state = seekSession,
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                playbackState = exoPlayer.playbackState
            )
        ) {
            return@LaunchedEffect
        }

        if (exoPlayer.playbackState == Player.STATE_IDLE && exoPlayer.mediaItemCount > 0) {
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }
    
    // 手势调整进度状态
    var isSeekGesture by remember { mutableStateOf(false) }
    var seekStartPosition by remember { mutableFloatStateOf(0f) }
    var seekTargetPosition by remember { mutableFloatStateOf(0f) }
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressOriginPlaybackParameters by remember { mutableStateOf(PlaybackParameters.DEFAULT) }
    var effectiveLongPressSpeed by remember { mutableFloatStateOf(longPressSpeed) }
    var showLongPressSpeedFeedback by remember { mutableStateOf(false) }
    var scale by remember(bvid) { mutableFloatStateOf(1f) }
    var panX by remember(bvid) { mutableFloatStateOf(0f) }
    var panY by remember(bvid) { mutableFloatStateOf(0f) }

    fun resetViewportTransform() {
        scale = 1f
        panX = 0f
        panY = 0f
    }

    fun applyPortraitTemporaryPlaybackParameters(parameters: PlaybackParameters) {
        // 竖屏长按只是临时倍速，不走详情页 ViewModel，避免用入口视频状态触发音频流重建。
        exoPlayer.playbackParameters = parameters
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            resetViewportTransform()
        }
    }

    LaunchedEffect(isCurrentPage, isLongPressing, longPressOriginPlaybackParameters) {
        if (shouldRestorePortraitLongPressSpeed(isLongPressing = isLongPressing, isCurrentPage = isCurrentPage)) {
            applyPortraitTemporaryPlaybackParameters(longPressOriginPlaybackParameters)
            isLongPressing = false
            showLongPressSpeedFeedback = false
        }
    }

    val latestIsLongPressing by rememberUpdatedState(isLongPressing)
    val latestLongPressOriginPlaybackParameters by rememberUpdatedState(longPressOriginPlaybackParameters)
    DisposableEffect(exoPlayer) {
        onDispose {
            if (latestIsLongPressing) {
                exoPlayer.playbackParameters = latestLongPressOriginPlaybackParameters
            }
        }
    }

    LaunchedEffect(isCurrentPage, scale, onCurrentPageScaleChange) {
        onCurrentPageScaleChange(if (isCurrentPage) scale else 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                portraitPageWidthPx = size.width
                portraitPageHeightPx = size.height
            }
            .pointerInput(isCurrentPage, bvid, commentExpansionTransform.playerGesturesEnabled) {
                if (!isCurrentPage || !commentExpansionTransform.playerGesturesEnabled) return@pointerInput

                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var observedMultiTouch = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount == 0) {
                            break
                        }
                        if (pressedCount < 2) {
                            if (observedMultiTouch) {
                                break
                            }
                            continue
                        }
                        observedMultiTouch = true

                        if (isLongPressing) {
                            applyPortraitTemporaryPlaybackParameters(longPressOriginPlaybackParameters)
                            isLongPressing = false
                            showLongPressSpeedFeedback = false
                        }

                        val pan = event.calculatePan()
                        val zoom = event.calculateZoom()

                        if (zoom != 1f || pan != Offset.Zero) {
                            val updatedScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = updatedScale

                            if (updatedScale > 1f) {
                                val maxPanX = (size.width * updatedScale - size.width) / 2f
                                val maxPanY = (size.height * updatedScale - size.height) / 2f
                                panX = (panX + pan.x * updatedScale).coerceIn(-maxPanX, maxPanX)
                                panY = (panY + pan.y * updatedScale).coerceIn(-maxPanY, maxPanY)
                                onPortraitOverlayVisibleChange(false)
                            } else {
                                panX = 0f
                                panY = 0f
                            }

                            event.changes.forEach { change ->
                                if (change.position != change.previousPosition) {
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(
                longPressSpeed,
                currentAudioQuality,
                isCurrentPage,
                commentExpansionTransform.playerGesturesEnabled,
                portraitOverlayVisible
            ) {
                detectTapGestures(
                    onTap = {
                        if (
                            !commentExpansionTransform.playerGesturesEnabled ||
                            !shouldHandlePortraitTapGesture(scale = scale)
                        ) {
                            return@detectTapGestures
                        }
                        onPortraitOverlayVisibleChange(
                            resolvePortraitOverlayVisibilityAfterTap(portraitOverlayVisible)
                        )
                    },
                    onDoubleTap = {
                        if (
                            !commentExpansionTransform.playerGesturesEnabled ||
                            !shouldHandlePortraitTapGesture(scale = scale)
                        ) {
                            return@detectTapGestures
                        }
                        if (isCurrentPage) {
                            togglePlayerPlaybackFromUserAction(exoPlayer)
                        }
                    },
                    onLongPress = {
                        if (
                            !commentExpansionTransform.playerGesturesEnabled ||
                            !shouldHandlePortraitLongPressGesture(scale = scale)
                        ) {
                            return@detectTapGestures
                        }
                        if (!isCurrentPage) return@detectTapGestures
                        longPressOriginPlaybackParameters = exoPlayer.playbackParameters
                        val longPressPlaybackParameters = resolveLongPressPlaybackParameters(
                            requestedSpeed = longPressSpeed,
                            currentAudioQuality = currentAudioQuality
                        )
                        effectiveLongPressSpeed = longPressPlaybackParameters.speed
                        applyPortraitTemporaryPlaybackParameters(longPressPlaybackParameters)
                        isLongPressing = true
                        showLongPressSpeedFeedback = true
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (isLongPressing) {
                            applyPortraitTemporaryPlaybackParameters(longPressOriginPlaybackParameters)
                            isLongPressing = false
                            showLongPressSpeedFeedback = false
                        }
                    }
                )
            }
            // 进度调整手势
            .pointerInput(
                progressState.duration,
                scale,
                isCurrentPage,
                commentExpansionTransform.playerGesturesEnabled
            ) {
                detectHorizontalDragGestures(
                    onDragStart = { 
                        if (
                            isCurrentPage &&
                            commentExpansionTransform.playerGesturesEnabled &&
                            progressState.duration > 0 &&
                            shouldHandlePortraitSeekGesture(scale = scale)
                        ) {
                            isSeekGesture = true
                            seekStartPosition = exoPlayer.currentPosition.toFloat()
                            seekTargetPosition = seekStartPosition
                        }
                    },
                    onDragEnd = {
                        if (isCurrentPage && isSeekGesture) {
                            val targetPosition = seekTargetPosition.toLong()
                            seekPlayerFromUserAction(exoPlayer, targetPosition)
                            danmakuManager.seekTo(targetPosition)
                            isSeekGesture = false
                        }
                    },
                    onDragCancel = { isSeekGesture = false },
                    onHorizontalDrag = { _, dragAmount ->
                        if (isCurrentPage && isSeekGesture && progressState.duration > 0) {
                            val seekDelta = (dragAmount / size.width) * progressState.duration * 0.75f
                            seekTargetPosition = (seekTargetPosition + seekDelta).coerceIn(0f, progressState.duration.toFloat())
                        }
                    }
                )
            }
    ) {
        val mediaLayerModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = commentExpansionTransform.scale
                scaleY = commentExpansionTransform.scale
                translationY = commentExpansionTransform.translationYPx
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        val danmakuSurfaceMode = resolvePortraitDanmakuSurfaceMode(
            currentVideoAspect = currentVideoAspect,
            displayAreaMode = portraitDanmakuDisplayAreaMode
        )
        val viewportTransformModifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panX
                translationY = panY
            }
        val pageDanmakuModifier = mediaLayerModifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panX
                translationY = panY
            }
        val pageDanmakuTopInset = with(density) {
            WindowInsets.statusBars.getTop(this).toDp()
        }

        // [核心逻辑]
        // 始终保留 AndroidView 以确保 Surface 准备就绪，但只有当播放器加载了当前视频时才将其绑定或显示
        // 否则显示封面
        
        if (isCurrentPage && isPlayerReadyForThisVideo) {
            PortraitVideoViewportContainer(
                currentVideoAspect = currentVideoAspect,
                modifier = mediaLayerModifier,
                fillContainer = portraitPagerFillContainer
            ) {
                key(currentPlayingBvid, bvid) {
                    Box(
                        modifier = viewportTransformModifier
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    playerViewRef = this
                                    player = exoPlayer
                                    useController = false
                                    keepScreenOn = shouldKeepPortraitPagerItemAwake
                                    resizeMode = portraitPagerResizeMode
                                    setKeepContentOnPlayerReset(true)
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                }
                            },
                            update = { view ->
                                playerViewRef = view
                                if (view.player != exoPlayer) {
                                    view.player = exoPlayer
                                }
                                view.keepScreenOn = shouldKeepPortraitPagerItemAwake
                                if (view.resizeMode != portraitPagerResizeMode) {
                                    view.resizeMode = portraitPagerResizeMode
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (danmakuEnabled && danmakuSurfaceMode == PortraitDanmakuSurfaceMode.VideoViewport) {
                            PortraitDanmakuOverlay(
                                danmakuManager = danmakuManager,
                                videoWidth = exoPlayer.videoSize.width,
                                videoHeight = exoPlayer.videoSize.height,
                                resizeMode = playerViewRef?.resizeMode ?: portraitPagerResizeMode,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        if (
            isCurrentPage &&
            isPlayerReadyForThisVideo &&
            danmakuEnabled &&
            danmakuSurfaceMode == PortraitDanmakuSurfaceMode.Page
        ) {
            PortraitDanmakuOverlay(
                danmakuManager = danmakuManager,
                videoWidth = exoPlayer.videoSize.width,
                videoHeight = exoPlayer.videoSize.height,
                resizeMode = playerViewRef?.resizeMode ?: portraitPagerResizeMode,
                modifier = pageDanmakuModifier.then(
                    if (shouldInsetPortraitDanmakuFromStatusBar(danmakuSurfaceMode)) {
                        Modifier.padding(top = pageDanmakuTopInset)
                    } else {
                        Modifier
                    }
                )
            )
        }

        // 封面图 (在加载中、未匹配到视频、或未开始播放时显示)
        val showCover = shouldShowPortraitCover(
            isLoading = isLoading,
            isCurrentPage = isCurrentPage,
            isPlayerReadyForThisVideo = isPlayerReadyForThisVideo,
            hasRenderedFirstFrame = hasRenderedFirstFrame
        )
        
        if (showCover) {
            Box(
                modifier = mediaLayerModifier.background(Color.Black)
            ) {
                if (shouldUseViewportBoundPortraitCover(
                        isCurrentPage = isCurrentPage,
                        isPlayerReadyForThisVideo = isPlayerReadyForThisVideo,
                        hasRenderedFirstFrame = hasRenderedFirstFrame
                    )
                ) {
                    PortraitVideoViewportContainer(
                        currentVideoAspect = resolvePortraitCoverViewportAspect(
                            currentVideoAspect = currentVideoAspect,
                            hasRenderedFirstFrame = hasRenderedFirstFrame
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = FormatUtils.fixImageUrl(cover),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = resolvePortraitCoverContentScale()
                        )
                    }
                } else {
                    AsyncImage(
                        model = FormatUtils.fixImageUrl(cover),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = resolvePortraitCoverContentScale()
                    )
                }

                if (isLoading && isCurrentPage) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
            }
        }

        // 暂停图标 (仅当前页且暂停时显示)
        // [修复] 使用响应式的 isPlaying 状态
        val showPauseIcon = shouldShowPortraitPauseIcon(
            isCurrentPage = isCurrentPage,
            isPlaying = isPlaying,
            playWhenReady = exoPlayer.playWhenReady,
            isLoading = isLoading,
            isSeekGesture = isSeekGesture
        )
        if (showPauseIcon) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Pause",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
        
        // 滑动进度提示
        if (isSeekGesture && progressState.duration > 0) {
            val targetTimeText = FormatUtils.formatDuration(seekTargetPosition.toLong())
            val totalTimeText = FormatUtils.formatDuration(progressState.duration)
            val deltaMs = (seekTargetPosition - seekStartPosition).toLong()
            val deltaText = if (deltaMs >= 0) "+${FormatUtils.formatDuration(deltaMs)}" else "-${FormatUtils.formatDuration(-deltaMs)}"
            
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "$targetTimeText / $totalTimeText",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Text(
                    text = deltaText,
                    color = if (deltaMs >= 0) Color(0xFF66FF66) else Color(0xFFFF6666),
                    fontSize = 14.sp
                )
            }
        }

        // 长按倍速提示（透明背景 + 循环箭头动画，位于视频上方）
        AnimatedVisibility(
            visible = isLongPressing && isCurrentPage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(targetOffsetY = { -it })
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "fast_forward_portrait")
            val arrow1Alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.3f at 0
                        1.0f at 300
                        0.3f at 600
                        0.3f at 900
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "arrow1"
            )
            val arrow2Alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.3f at 0
                        0.3f at 300
                        1.0f at 600
                        0.3f at 900
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "arrow2"
            )
            val arrow3Alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 900
                        0.3f at 0
                        0.3f at 600
                        1.0f at 900
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "arrow3"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val arrowAlphas = listOf(arrow1Alpha, arrow2Alpha, arrow3Alpha)
                arrowAlphas.forEach { alpha ->
                    Canvas(
                        modifier = Modifier.size(14.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = alpha)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${effectiveLongPressSpeed}x",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(1f, 1f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = isCurrentPage && scale > 1.05f,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = {
                    resetViewportTransform()
                    onPortraitOverlayVisibleChange(true)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "还原画面"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "还原画面",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Overlay & Interaction
    val currentUiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val isCurrentModelVideo = (currentUiState as? VideoPlaybackUiState.Success)?.info?.bvid == bvid
    val currentSuccess = (currentUiState as? VideoPlaybackUiState.Success)
        ?.withEngagementUiState(engagementState)
    var portraitInteractionOverride by remember(bvid) {
        mutableStateOf(PortraitVideoInteractionOverride())
    }
    val favoriteSaveEvent by viewModel.favoriteFolderSaveEvent.collectAsStateWithLifecycle()
    var consumedFavoriteSaveEventVersion by remember(bvid) { mutableLongStateOf(0L) }
    val stat = if (item is ViewInfo) item.stat else (item as RelatedVideo).stat
    val resolvedInteractionState = resolvePortraitVideoInteractionUiState(
        targetBvid = bvid,
        fallbackStat = stat,
        sharedState = currentSuccess,
        localOverride = portraitInteractionOverride
    )
    val fallbackDetailInfo = when (item) {
        is ViewInfo -> item
        is RelatedVideo -> toViewInfoForPortraitDetail(item)
        else -> null
    }
    val portraitDetailInfo = if (isCurrentModelVideo && currentSuccess != null) {
        currentSuccess.info
    } else {
        fallbackDetailInfo
    }
    // seed 进场可能没有 owner；加载成功后用 Success.info 补齐 UP 名/头像。
    val authorName = portraitDetailInfo?.owner?.name?.takeIf { it.isNotBlank() } ?: seedAuthorName
    val authorFace = portraitDetailInfo?.owner?.face?.takeIf { it.isNotBlank() } ?: seedAuthorFace
    val authorMid = portraitDetailInfo?.owner?.mid?.takeIf { it > 0L } ?: seedAuthorMid
    val isFollowing = (currentUiState as? VideoPlaybackUiState.Success)?.followingMids?.contains(authorMid) == true
    val detailVideoList = remember(bvid, watchLaterVideos, recommendationVideos) {
        buildPortraitDetailVideoList(
            currentBvid = bvid,
            watchLaterVideos = watchLaterVideos,
            recommendationVideos = recommendationVideos
        )
    }
    val upOnlyVideos = remember(detailVideoList.videos, authorMid, bvid) {
        detailVideoList.videos.filter { candidate ->
            candidate.owner.mid == authorMid && candidate.bvid != bvid
        }
    }
    val detailSheetTitle = remember(detailSheetUpOnlyMode, recommendationVideos.size, upOnlyVideos.size) {
        if (detailSheetUpOnlyMode) {
            if (upOnlyVideos.isEmpty()) "该 UP 暂无可切换视频" else "UP 主视频"
        } else {
            detailVideoList.title
        }
    }
    val detailSheetVideos = remember(detailSheetUpOnlyMode, upOnlyVideos, detailVideoList.videos) {
        if (detailSheetUpOnlyMode) upOnlyVideos else detailVideoList.videos
    }
    val toggleDanmaku: () -> Unit = {
        val next = !danmakuEnabled
        danmakuManager.isEnabled = next
        scope.launch {
            SettingsManager.setDanmakuEnabled(
                context,
                next,
                com.android.purebilibili.core.store.DanmakuSettingsScope.PORTRAIT
            )
        }
        Unit
    }
    val canHandlePortraitInteraction = shouldHandlePortraitVideoInteraction(
        isCurrentPage = isCurrentPage,
        aid = activeAid,
        bvid = bvid
    )

    LaunchedEffect(favoriteSaveEvent?.version, activeAid) {
        val event = favoriteSaveEvent ?: return@LaunchedEffect
        if (event.aid != activeAid || event.version == consumedFavoriteSaveEventVersion) {
            return@LaunchedEffect
        }
        val nextFavoriteCount = (
            resolvedInteractionState.favoriteCount +
                when {
                    event.isFavorited == resolvedInteractionState.isFavorited -> 0
                    event.isFavorited -> 1
                    else -> -1
                }
            ).coerceAtLeast(0)
        portraitInteractionOverride = portraitInteractionOverride.copy(
            isFavorited = event.isFavorited,
            favoriteCount = nextFavoriteCount
        )
        consumedFavoriteSaveEventVersion = event.version
    }

    LaunchedEffect(isCurrentPage, authorMid) {
        if (isCurrentPage && authorMid > 0L) {
            viewModel.ensureFollowStatus(authorMid)
        }
    }

    PortraitFullscreenOverlay(
            title = title,
            authorName = authorName,
            authorFace = authorFace,
            isPlaying = if (isCurrentPage) {
                isPlaying || shouldShowPlaybackRecoveryUiAfterSeek(
                    state = seekSession,
                    playWhenReady = exoPlayer.playWhenReady,
                    isPlaying = exoPlayer.isPlaying,
                    playbackState = exoPlayer.playbackState
                )
            } else {
                false
            },
            progress = progressState,
            
            statView = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.view else stat.view,
            statLike = resolvedInteractionState.likeCount,
            statCoin = if (isCurrentModelVideo) {
                engagementState.coinCount.takeIf { it > 0 }
                    ?: currentSuccess?.info?.stat?.coin
                    ?: stat.coin
            } else {
                stat.coin
            },
            statDanmaku = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.danmaku else stat.danmaku,
            statReply = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.reply else stat.reply,
            statFavorite = resolvedInteractionState.favoriteCount,
            statShare = if(isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.share else stat.share,
            
            isLiked = resolvedInteractionState.isLiked,
            isCoined = isCurrentModelVideo && engagementState.coinCount > 0,
            isFavorited = resolvedInteractionState.isFavorited,
            
            isFollowing = isFollowing,
            onFollowClick = { 
                onToggleFollow(authorMid, isFollowing)
            },
            
            onDetailClick = {
                if (portraitDetailInfo != null) {
                    detailSheetUpOnlyMode = false
                    showDetailSheet = true
                }
            },
            onTitleClick = {
                if (portraitDetailInfo != null) {
                    detailSheetUpOnlyMode = false
                    showDetailSheet = true
                }
            },
            onAuthorClick = {
                if (portraitDetailInfo != null) {
                    detailSheetUpOnlyMode = true
                    showDetailSheet = true
                }
            },
            onLikeClick = {
                if (canHandlePortraitInteraction) {
                    val currentLikeState = resolvedInteractionState.isLiked
                    val currentLikeCount = resolvedInteractionState.likeCount
                    onToggleLike(
                        activeAid,
                        bvid,
                        currentLikeState
                    ) { liked ->
                        val nextLikeCount = (
                            currentLikeCount +
                                when {
                                    liked == currentLikeState -> 0
                                    liked -> 1
                                    else -> -1
                                }
                            ).coerceAtLeast(0)
                        portraitInteractionOverride = portraitInteractionOverride.copy(
                            isLiked = liked,
                            likeCount = nextLikeCount
                        )
                    }
                }
            },
            onLikeLongClick = {
                if (canHandlePortraitInteraction) {
                    val currentInteractionState = resolvedInteractionState
                    onTripleAction(
                        activeAid,
                        bvid,
                        currentInteractionState.isLiked,
                        currentSuccess?.coinCount ?: 0,
                        currentInteractionState.isFavorited
                    ) { result ->
                        portraitInteractionOverride = resolvePortraitTripleActionOverride(
                            currentState = currentInteractionState,
                            likeSuccess = result.likeSuccess,
                            favoriteSuccess = result.favoriteSuccess
                        )
                    }
                }
            },
            onCoinClick = {
                if (canHandlePortraitInteraction) {
                    onOpenCoinDialog()
                }
            },
            onFavoriteClick = {
                if (canHandlePortraitInteraction) {
                    when (resolvePortraitFavoriteAction()) {
                        PortraitFavoriteAction.OpenFavoriteFolders -> {
                            viewModel.showFavoriteFolderDialog(activeAid)
                        }
                    }
                }
            },
            onCommentClick = { showCommentSheet = true },
            onShareClick = {
                val shareText = buildPortraitShareText(title = title, bvid = bvid)
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share too..."))
            },
            
            currentSpeed = currentPlaybackSpeed,
            currentQualityLabel = qualityLabel,
            currentRatio = aspectRatio,
            danmakuEnabled = danmakuEnabled,
            isStatusBarHidden = true,
            videoshotData = currentSuccess?.videoshotData,
            isPlaybackRecovering = isCurrentPage && shouldShowPlaybackRecoveryUiAfterSeek(
                state = seekSession,
                playWhenReady = exoPlayer.playWhenReady,
                isPlaying = exoPlayer.isPlaying,
                playbackState = exoPlayer.playbackState
            ),
            
            onBack = {
                onExitSnapshot(bvid, exoPlayer.currentPosition, snapshotCid)
                onBack()
            },
            onHomeClick = {
                onExitSnapshot(bvid, exoPlayer.currentPosition, snapshotCid)
                onHomeClick()
            },
            onPlayPause = {
                if (isCurrentPage) {
                    togglePlayerPlaybackFromUserAction(exoPlayer)
                }
            },
            onSeek = {
                if (isCurrentPage) {
                    val resolvedDuration = progressState.duration
                        .takeIf { durationMs -> durationMs > 0L }
                        ?: exoPlayer.duration.coerceAtLeast(0L)
                    val targetPosition = resolvePortraitCommittedSeekPosition(
                        requestedPositionMs = it,
                        durationMs = resolvedDuration
                    )
                    val commitResult = finishPlaybackSeekInteraction(
                        updatePlaybackSeekInteraction(
                            state = seekSession,
                            positionMs = targetPosition
                        )
                    )
                    seekSession = commitResult.state
                    progressState = progressState.copy(current = commitResult.committedPositionMs)
                    seekPlayerFromUserAction(
                        player = exoPlayer,
                        positionMs = commitResult.committedPositionMs,
                        shouldResumePlaybackOverride = commitResult.shouldResumePlayback
                    )
                    danmakuManager.seekTo(commitResult.committedPositionMs)
                }
            },
            onSeekStart = { danmakuManager.prepareForSeekScrub() },
            seekPositionMs = seekSession.sliderPositionMs,
            isSeekScrubbing = seekSession.isSliderMoving,
            onSeekDragStart = { position ->
                seekSession = startPlaybackSeekInteraction(
                    state = seekSession,
                    player = exoPlayer,
                    positionMs = position
                )
            },
            onSeekDragUpdate = { position ->
                seekSession = updatePlaybackSeekInteraction(
                    state = seekSession,
                    positionMs = position
                )
            },
            onSeekDragCancel = {
                seekSession = cancelPlaybackSeekInteraction(seekSession)
                danmakuManager.cancelSeekScrub()
            },
            onSpeedClick = {
                if (isCurrentPage) {
                    showSpeedMenu = true
                    onPortraitOverlayVisibleChange(true)
                }
            },
            onQualityClick = {
                if (isCurrentPage) {
                    showQualityMenu = true
                    onPortraitOverlayVisibleChange(true)
                }
            },
            onRatioClick = {
                if (isCurrentPage) {
                    showRatioMenu = true
                    onPortraitOverlayVisibleChange(true)
                }
            },
            onDanmakuToggle = toggleDanmaku,
            onDanmakuInputClick = {
                if (isCurrentPage) {
                    viewModel.showDanmakuSendDialog()
                }
            },
            onToggleStatusBar = { },
            onSearchClick = {
                onExitSnapshot(bvid, exoPlayer.currentPosition, snapshotCid)
                onSearchClick()
            },
            onMoreClick = {
                showDetailSheet = true
            },
            onRotateToLandscape = {
                onExitSnapshot(bvid, exoPlayer.currentPosition, snapshotCid)
                onRotateToLandscape()
            },
            
            showControls = resolvePortraitOverlayControlsVisible(
                portraitOverlayVisible = portraitOverlayVisible,
                showDetailSheet = showDetailSheet
            ),
            commentExpansionProgress = commentSheetVisibilityProgress
        )

        if (showQualityMenu && isCurrentPage) {
            val qualityIds = availableQualityIds.ifEmpty { listOf(selectedQualityId) }
            QualitySelectionMenu(
                qualities = resolvePortraitQualityMenuLabels(qualityIds),
                qualityIds = qualityIds,
                switchableQualityIds = qualityIds,
                currentQuality = qualityLabel,
                isLoggedIn = isLoggedIn,
                isVip = isVip,
                onQualitySelected = { index ->
                    qualityIds.getOrNull(index)?.let(onQualitySelected)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false },
                useDialog = true
            )
        }

        if (showRatioMenu && isCurrentPage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showRatioMenu = false },
                contentAlignment = Alignment.Center
            ) {
                AspectRatioMenu(
                    currentRatio = aspectRatio,
                    onRatioSelected = { ratio ->
                        onAspectRatioChange(ratio)
                        showRatioMenu = false
                    },
                    onDismiss = { showRatioMenu = false }
                )
            }
        }

        if (showSpeedMenu && isCurrentPage) {
            SpeedSelectionMenuDialog(
                currentSpeed = currentPlaybackSpeed,
                onSpeedSelected = { speed ->
                    val normalizedSpeed = speed.coerceAtLeast(0.1f)
                    currentPlaybackSpeed = normalizedSpeed
                    val handledByViewModel = viewModel.applyPlaybackSpeedFromUi(normalizedSpeed)
                    if (!handledByViewModel || exoPlayer.playbackParameters.speed != normalizedSpeed) {
                        exoPlayer.playbackParameters = PlaybackParameters(normalizedSpeed, 1.0f)
                    }
                    scope.launch {
                        SettingsManager.setLastPlaybackSpeed(context, normalizedSpeed)
                    }
                    showSpeedMenu = false
                },
                onDismiss = { showSpeedMenu = false }
            )
        }

        PortraitCommentSheet(
            visible = showCommentSheet,
            active = isCurrentPage,
            onDismiss = {
                showCommentSheet = false
            },
            onVisibilityProgressChange = { progress ->
                commentSheetVisibilityProgress = progress
            },
            commentViewModel = commentViewModel,
            aid = activeAid,
            upMid = authorMid,
            expectedReplyCount = if (isCurrentModelVideo && currentSuccess != null) currentSuccess.info.stat.reply else stat.reply,
            emoteMap = currentSuccess?.emoteMap ?: emptyMap(),
            maxTimestampMs = currentSuccess?.videoDurationMs?.takeIf { it > 0L }
                ?: progressState.duration.takeIf { it > 0L },
            onRootCommentClick = { viewModel.openRootCommentComposer() },
            onReplyClick = { reply ->
                viewModel.setReplyingTo(reply)
                viewModel.showCommentInputDialog()
            },
            onUserClick = onUserClick
        )

        if (isCurrentPage) {
            val showCommentInput by viewModel.showCommentDialog.collectAsStateWithLifecycle()
            val isSendingComment by viewModel.isSendingComment.collectAsStateWithLifecycle()
            val replyingToComment by viewModel.replyingToComment.collectAsStateWithLifecycle()
            val emotePackages by viewModel.emotePackages.collectAsStateWithLifecycle()
            val mentionSearchState by viewModel.commentMentionSearchState.collectAsStateWithLifecycle()
            val composerDrafts by viewModel.composerDrafts.collectAsStateWithLifecycle()
            val commentDraftKey = com.android.purebilibili.feature.video.viewmodel
                .commentComposerDraftKey(replyingToComment?.rpid)
            val commentDraft = composerDrafts.comments[commentDraftKey]
                ?: com.android.purebilibili.feature.video.viewmodel.CommentComposerDraft()
            val commentState by commentViewModel.commentState.collectAsStateWithLifecycle()
            val commentFraudDetectionEnabled by com.android.purebilibili.core.store.SettingsManager
                .getCommentFraudDetectionEnabled(context)
                .collectAsStateWithLifecycle(initialValue = true
        )

            LaunchedEffect(activeAid, commentFraudDetectionEnabled) {
                viewModel.commentSentEvent.collect { reply ->
                    commentViewModel.onExternalCommentSent(
                        aid = activeAid,
                        newReply = reply,
                        fraudDetectionEnabled = commentFraudDetectionEnabled
                    )
                }
            }

            com.android.purebilibili.feature.video.ui.components.CommentInputDialog(
                visible = showCommentInput,
                onDismiss = { viewModel.hideCommentInputDialog() },
                isSending = isSendingComment,
                replyToName = replyingToComment?.member?.uname,
                inputHint = if (replyingToComment != null) commentState.childInputHint else commentState.rootInputHint,
                canUploadImage = commentState.canUploadImage,
                canInputComment = commentState.canInputComment,
                emotePackages = emotePackages,
                mentionUsers = mentionSearchState.users,
                isMentionSearching = mentionSearchState.isLoading,
                mentionSearchError = mentionSearchState.errorMessage,
                onMentionSearchQueryChange = viewModel::searchCommentMentionUsers,
                initialText = commentDraft.text,
                initialImageUris = commentDraft.imageUris,
                initialSyncToDynamic = commentDraft.syncToDynamic,
                onDraftChange = viewModel::updateCommentDraft,
                currentVideoPositionMsProvider = { exoPlayer.currentPosition.coerceAtLeast(0L) },
                onSend = { message, imageUris, syncToDynamic ->
                    viewModel.sendComment(
                        inputMessage = message,
                        imageUris = imageUris,
                        syncToDynamic = syncToDynamic,
                        targetAid = activeAid
                    )
                    viewModel.hideCommentInputDialog()
                }
            )
        }
        
        PortraitDetailSheet(
            visible = showDetailSheet,
            onDismiss = {
                showDetailSheet = false
                detailSheetUpOnlyMode = false
            },
            info = portraitDetailInfo,
            recommendationTitle = detailSheetTitle,
            recommendations = detailSheetVideos,
            onRecommendationClick = { targetBvid ->
                showDetailSheet = false
                detailSheetUpOnlyMode = false
                onRequestVideoChange(targetBvid)
            },
            onAuthorClick = { mid ->
                showDetailSheet = false
                detailSheetUpOnlyMode = false
                onExitSnapshot(bvid, exoPlayer.currentPosition, snapshotCid)
                onUserClick(mid)
            },
            danmakuEnabled = danmakuEnabled,
            onDanmakuToggle = toggleDanmaku
        )
    }
}

internal fun resolvePortraitInitialPageIndex(
    initialBvid: String,
    initialInfoBvid: String,
    recommendations: List<RelatedVideo>
): Int {
    if (initialBvid == initialInfoBvid) return 0
    val recommendationIndex = recommendations.indexOfFirst { it.bvid == initialBvid }
    if (recommendationIndex < 0) return 0
    return recommendationIndex + 1
}

internal fun shouldHandlePortraitVideoInteraction(
    isCurrentPage: Boolean,
    aid: Long,
    bvid: String
): Boolean {
    return isCurrentPage && aid > 0L && bvid.isNotBlank()
}

internal fun resolvePortraitActiveAid(
    isPlayerReadyForThisVideo: Boolean,
    itemAid: Long,
    currentPlayingAid: Long
): Long {
    return if (isPlayerReadyForThisVideo) currentPlayingAid.coerceAtLeast(0L) else itemAid
}

internal fun resolvePortraitOverlayControlsVisible(
    portraitOverlayVisible: Boolean,
    showDetailSheet: Boolean
): Boolean {
    return portraitOverlayVisible && !showDetailSheet
}

internal fun resolvePortraitOverlayVisibilityAfterTap(currentlyVisible: Boolean): Boolean {
    return !currentlyVisible
}

internal fun resolvePortraitFavoriteAction(): PortraitFavoriteAction {
    return PortraitFavoriteAction.OpenFavoriteFolders
}

internal fun resolvePortraitVideoInteractionUiState(
    targetBvid: String,
    fallbackStat: Stat,
    sharedState: VideoPlaybackUiState.Success?,
    localOverride: PortraitVideoInteractionOverride? = null
): PortraitVideoInteractionUiState {
    val currentSharedState = sharedState?.takeIf { it.info.bvid == targetBvid }
    return if (currentSharedState != null) {
        PortraitVideoInteractionUiState(
            isLiked = currentSharedState.isLiked,
            isFavorited = currentSharedState.isFavorited,
            likeCount = currentSharedState.info.stat.like,
            favoriteCount = currentSharedState.info.stat.favorite
        )
    } else {
        PortraitVideoInteractionUiState(
            isLiked = localOverride?.isLiked ?: false,
            isFavorited = localOverride?.isFavorited ?: false,
            likeCount = localOverride?.likeCount ?: fallbackStat.like,
            favoriteCount = localOverride?.favoriteCount ?: fallbackStat.favorite
        )
    }
}

internal fun resolvePortraitTripleActionOverride(
    currentState: PortraitVideoInteractionUiState,
    likeSuccess: Boolean,
    favoriteSuccess: Boolean
): PortraitVideoInteractionOverride {
    val nextLiked = currentState.isLiked || likeSuccess
    val nextFavorited = currentState.isFavorited || favoriteSuccess
    val likeDelta = if (!currentState.isLiked && nextLiked) 1 else 0
    val favoriteDelta = if (!currentState.isFavorited && nextFavorited) 1 else 0
    return PortraitVideoInteractionOverride(
        isLiked = nextLiked,
        isFavorited = nextFavorited,
        likeCount = (currentState.likeCount + likeDelta).coerceAtLeast(0),
        favoriteCount = (currentState.favoriteCount + favoriteDelta).coerceAtLeast(0)
    )
}

@Composable
private fun PortraitDanmakuOverlay(
    danmakuManager: DanmakuManager,
    videoWidth: Int,
    videoHeight: Int,
    resizeMode: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            DanmakuView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                danmakuManager.attachView(this)
            }
        },
        update = { view ->
            val viewportTag = "$videoWidth:$videoHeight:$resizeMode:${view.width}x${view.height}"
            if (view.width > 0 && view.height > 0 && view.tag != viewportTag) {
                view.tag = viewportTag
                danmakuManager.attachView(view)
                }
        },
        modifier = modifier
    )
}

internal fun resolvePortraitPagerRepeatMode(): Int = Player.REPEAT_MODE_OFF

internal fun resolvePortraitDanmakuSurfaceMode(
    currentVideoAspect: Float,
    displayAreaMode: PortraitDanmakuDisplayAreaMode
): PortraitDanmakuSurfaceMode {
    return when (displayAreaMode) {
        PortraitDanmakuDisplayAreaMode.VIDEO_VIEWPORT -> PortraitDanmakuSurfaceMode.VideoViewport
        PortraitDanmakuDisplayAreaMode.SCREEN_TOP -> PortraitDanmakuSurfaceMode.Page
    }
}

internal fun shouldInsetPortraitDanmakuFromStatusBar(
    surfaceMode: PortraitDanmakuSurfaceMode
): Boolean {
    return surfaceMode == PortraitDanmakuSurfaceMode.Page
}

internal fun shouldLoadPortraitDanmaku(
    settingsLoaded: Boolean,
    cid: Long,
    danmakuEnabled: Boolean
): Boolean {
    return settingsLoaded && cid > 0L && danmakuEnabled
}

internal fun resolvePortraitDanmakuReadableFontScale(fontScale: Float): Float {
    return (fontScale * 1.18f).coerceIn(0.3f, 2.0f)
}

internal data class PortraitVideoViewportSize(
    val width: Int,
    val height: Int
)

internal fun resolvePortraitVideoViewportSize(
    containerWidth: Int,
    containerHeight: Int,
    currentVideoAspect: Float,
    fillContainer: Boolean
): PortraitVideoViewportSize {
    val safeWidth = containerWidth.coerceAtLeast(1)
    val safeHeight = containerHeight.coerceAtLeast(1)
    if (fillContainer) {
        return PortraitVideoViewportSize(width = safeWidth, height = safeHeight)
    }
    val safeAspect = currentVideoAspect.coerceAtLeast(0.1f)
    val containerAspect = safeWidth.toFloat() / safeHeight.toFloat()
    return if (safeAspect > containerAspect) {
        PortraitVideoViewportSize(
            width = safeWidth,
            height = (safeWidth / safeAspect).roundToInt().coerceIn(1, safeHeight)
        )
    } else {
        PortraitVideoViewportSize(
            width = (safeHeight * safeAspect).roundToInt().coerceIn(1, safeWidth),
            height = safeHeight
        )
    }
}

internal fun resolvePortraitPagerFillContainer(
    aspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    isVerticalContent: Boolean = true
): Boolean {
    val safe = resolveSafeVideoAspectRatio(
        preferred = aspectRatio,
        isVerticalVideo = isVerticalContent
    )
    return safe == VideoAspectRatio.FILL || safe == VideoAspectRatio.STRETCH
}

internal fun resolvePortraitPagerResizeMode(
    aspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    isVerticalContent: Boolean = true
): Int {
    return resolveSafeVideoAspectRatio(
        preferred = aspectRatio,
        isVerticalVideo = isVerticalContent
    ).playerResizeMode
}

internal fun shouldAllowPortraitPlayback(
    isCurrentStoryTab: Boolean,
    isLifecycleResumed: Boolean
): Boolean {
    return isCurrentStoryTab && isLifecycleResumed
}

internal fun resolvePortraitVideoViewportVerticalOffsetDp(
    currentVideoAspect: Float,
    fillContainer: Boolean,
    isLandscape: Boolean = false
): Int {
    if (fillContainer || isLandscape) return 0
    return if (currentVideoAspect > 1f) -48 else 0
}

@Composable
internal fun PortraitVideoViewportContainer(
    currentVideoAspect: Float,
    modifier: Modifier = Modifier,
    viewportModifier: Modifier = Modifier,
    fillContainer: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val viewportSize = with(density) {
            resolvePortraitVideoViewportSize(
                containerWidth = maxWidth.roundToPx(),
                containerHeight = maxHeight.roundToPx(),
                currentVideoAspect = currentVideoAspect,
                fillContainer = fillContainer
            )
        }
        val isLandscape = maxWidth > maxHeight

        Box(
            modifier = viewportModifier
                .size(
                    width = with(density) { viewportSize.width.toDp() },
                    height = with(density) { viewportSize.height.toDp() }
                )
                .align(Alignment.Center)
                .offset(
                    y = resolvePortraitVideoViewportVerticalOffsetDp(
                        currentVideoAspect = currentVideoAspect,
                        fillContainer = fillContainer,
                        isLandscape = isLandscape
                    ).dp
                )
        ) {
            content()
        }
    }
}

internal fun resolvePortraitInitialVideoAspectRatio(
    itemBvid: String,
    currentPlayingBvid: String?,
    playerVideoWidth: Int,
    playerVideoHeight: Int,
    knownVideoAspectRatio: Float? = null
): Float {
    knownVideoAspectRatio
        ?.takeIf { it > 0f }
        ?.let { return it }
    val hasValidPlayerSize = playerVideoWidth > 0 && playerVideoHeight > 0
    return if (itemBvid == currentPlayingBvid && hasValidPlayerSize) {
        playerVideoWidth.toFloat() / playerVideoHeight.toFloat()
    } else {
        9f / 16f
    }
}

internal fun resolvePortraitRuntimeVideoAspectRatio(
    knownVideoAspectRatio: Float?,
    playerVideoWidth: Int,
    playerVideoHeight: Int
): Float {
    val safeKnownAspect = knownVideoAspectRatio?.takeIf { it > 0f }
    if (playerVideoWidth <= 0 || playerVideoHeight <= 0) {
        return safeKnownAspect ?: (9f / 16f)
    }
    val playerAspect = playerVideoWidth.toFloat() / playerVideoHeight.toFloat()
    if (safeKnownAspect == null) return playerAspect
    val knownPortrait = safeKnownAspect < 1f
    val playerPortrait = playerAspect < 1f
    return if (knownPortrait != playerPortrait) {
        safeKnownAspect
    } else {
        playerAspect
    }
}

internal fun resolvePortraitInitialRenderedFirstFrameGeneration(
    useSharedPlayer: Boolean,
    sharedPlayerHasFrameAtEntry: Boolean
): Int {
    return if (useSharedPlayer && sharedPlayerHasFrameAtEntry) 0 else -1
}

internal fun resolveAspectRatioFromDimension(
    dimension: com.android.purebilibili.data.model.response.Dimension?
): Float? {
    val source = dimension ?: return null
    if (source.width <= 0 || source.height <= 0) return null
    val normalizedRotate = ((source.rotate % 360) + 360) % 360
    val shouldSwap = normalizedRotate == 90 || normalizedRotate == 270
    val effectiveWidth = if (shouldSwap) source.height else source.width
    val effectiveHeight = if (shouldSwap) source.width else source.height
    if (effectiveWidth <= 0 || effectiveHeight <= 0) return null
    return effectiveWidth.toFloat() / effectiveHeight.toFloat()
}

internal fun shouldRebindSharedPlayerSurfaceOnAttach(
    isCurrentPage: Boolean,
    isPlayerReadyForThisVideo: Boolean,
    hasPlayerView: Boolean,
    videoWidth: Int,
    videoHeight: Int
): Boolean {
    return isCurrentPage && isPlayerReadyForThisVideo && hasPlayerView
}
