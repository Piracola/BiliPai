package com.android.purebilibili.navigation3

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigationevent.NavigationEventTransitionState
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ProvideAnimatedVisibilityScope
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.core.ui.transition.LocalPredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.LocalVideoCardMorphProgressReporter
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionClock
import com.android.purebilibili.core.ui.transition.PREDICTIVE_BACK_BACKGROUND_CANCEL_DURATION_MS
import com.android.purebilibili.core.ui.transition.PredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS
import com.android.purebilibili.core.ui.transition.VideoCardMorphProgressReporter
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.VideoCardTransitionClock
import com.android.purebilibili.core.ui.transition.resolveMorphAlignedFallbackDurationMs
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackCommitBlurDurationMs
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackGestureBlurProgress
import com.android.purebilibili.core.ui.transition.resolveVideoCardTimelineSpec
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionReturnFullDurationMillis
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedMorphRemainingDurationMs
import com.android.purebilibili.core.ui.transition.isVideoCardTransitionBackgroundGesturePhase
import com.android.purebilibili.core.ui.transition.shouldApplyPredictiveBackGestureBlur
import com.android.purebilibili.core.ui.transition.shouldShowVideoCardTransitionNavBackdrop
import com.android.purebilibili.core.ui.transition.shouldSnapClearVideoCardDepthBlurOnQuickReturn
import com.android.purebilibili.core.ui.transition.VideoCardTransitionNavBackdrop
import com.android.purebilibili.feature.settings.isSettingsSubtreeNavKey
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationHandler
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiAutoPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackAnimationHandler
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackExitDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun shouldContinuouslyPublishVideoCardDepthFrames(
    phase: VideoCardTransitionBackgroundPhase,
    isReturnGestureInProgress: Boolean,
    isGestureRestoreInProgress: Boolean,
): Boolean {
    return phase == VideoCardTransitionBackgroundPhase.OPENING ||
        phase == VideoCardTransitionBackgroundPhase.RETURNING ||
        isReturnGestureInProgress ||
        isGestureRestoreInProgress
}

@Composable
internal fun BiliPaiNavDisplayHost(
    backStack: List<BiliPaiNavKey>,
    cardTransitionEnabled: Boolean = true,
    videoSharedTransitionDurationMillis: Int,
    predictiveBackEnabled: Boolean = true,
    predictiveBackAnimationStyle: BiliPaiPredictiveBackAnimationStyle = BiliPaiPredictiveBackAnimationStyle.SCALE,
    predictiveBackExitDirectionOverride: String = "auto",
    sourceMetadata: BiliPaiNavSourceMetadata,
    onBack: () -> Unit,
    onNativeVideoBackProgress: (currentKey: BiliPaiNavKey?, targetKey: BiliPaiNavKey?, progress: Float) -> Unit = { _, _, _ -> },
    onNativeVideoBackCancelled: (currentKey: BiliPaiNavKey?, targetKey: BiliPaiNavKey?) -> Unit = { _, _ -> },
    /**
     * 把卡片景深帧同步给 App 层全局壁纸等外部层（progress / phase / gestureRestore）。
     * Animatable 在 draw 期驱动时不一定每帧重组，这里用 withFrameNanos 桥接。
     */
    onVideoCardDepthFrame: (
        progress: Float,
        phase: VideoCardTransitionBackgroundPhase,
        gestureRestore: Boolean,
    ) -> Unit = { _, _, _ -> },
    isQuickReturnFromDetail: Boolean = false,
    /**
     * 系统/预测返回在启动景深收尾前调用：标记返回会话并返回是否快速返回。
     * performBack 里 onBack 更晚，不能只靠 [isQuickReturnFromDetail] 快照。
     */
    onPrepareVideoCardSharedReturn: () -> Boolean = { isQuickReturnFromDetail },
    /**
     * 从相关推荐详情 pop 回父详情后回调：恢复进入 related 前的列表来源 session/key。
     */
    onRelatedVideoDetailReturned: () -> Unit = {},
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    visibleBottomBarRoutes: Set<String> = emptySet(),
    activeMainHostRoute: String? = null,
    isLightBackground: Boolean = false,
    content: @Composable (BiliPaiNavKey) -> Unit
) {
    val safeBackStack = remember(backStack) {
        backStack.ifEmpty { listOf(BiliPaiNavKey.MainHost) }
    }
    val application = LocalContext.current.applicationContext as Application
    var navigationEventState: NavigationEventState<SceneInfo<BiliPaiNavKey>>? = null
    val navigationScope = rememberCoroutineScope()
    // 单时钟：景深 / chrome / 壁纸只读 clock.depthProgress()；shared morph 回灌优先。
    val videoCardClock = remember { VideoCardTransitionClock() }
    val timelineSpec = remember(videoSharedTransitionDurationMillis) {
        resolveVideoCardTimelineSpec(videoSharedTransitionDurationMillis)
    }
    val predictiveBackBackgroundProgress = remember { Animatable(0f) }
    val isQuickReturnFromDetailUpdated by rememberUpdatedState(isQuickReturnFromDetail)
    var videoCardReturnGestureInProgress by remember { mutableStateOf(false) }
    var videoCardBackgroundGestureRestoreInProgress by remember { mutableStateOf(false) }
    // fallback Animatable 唯一 owner：OPENING / RETURNING / cancel 互斥。
    var videoCardDepthAnimationJob by remember { mutableStateOf<Job?>(null) }
    fun cancelVideoCardDepthAnimation() {
        videoCardDepthAnimationJob?.cancel()
        videoCardDepthAnimationJob = null
    }
    fun launchVideoCardDepthAnimation(block: suspend () -> Unit) {
        cancelVideoCardDepthAnimation()
        var job: Job? = null
        job = navigationScope.launch {
            try {
                block()
            } finally {
                if (videoCardDepthAnimationJob === job) {
                    videoCardDepthAnimationJob = null
                }
            }
        }
        videoCardDepthAnimationJob = job
    }
    val morphProgressReporter = remember(videoCardClock) {
        VideoCardMorphProgressReporter { morphFraction, active ->
            videoCardClock.reportSharedMorphProgress(
                morphFraction = morphFraction,
                active = active,
            )
        }
    }
    val videoCardBackgroundProgressProvider = remember(videoCardClock) {
        { videoCardClock.depthProgress() }
    }
    val onVideoCardDepthFrameUpdated by rememberUpdatedState(onVideoCardDepthFrame)
    LaunchedEffect(
        videoCardClock.phase,
        cardTransitionEnabled,
        videoCardReturnGestureInProgress,
        videoCardBackgroundGestureRestoreInProgress,
    ) {
        if (!cardTransitionEnabled ||
            videoCardClock.phase == VideoCardTransitionBackgroundPhase.IDLE
        ) {
            onVideoCardDepthFrameUpdated(
                0f,
                VideoCardTransitionBackgroundPhase.IDLE,
                false,
            )
            return@LaunchedEffect
        }
        if (!shouldContinuouslyPublishVideoCardDepthFrames(
                phase = videoCardClock.phase,
                isReturnGestureInProgress = videoCardReturnGestureInProgress,
                isGestureRestoreInProgress = videoCardBackgroundGestureRestoreInProgress,
            )
        ) {
            onVideoCardDepthFrameUpdated(
                videoCardBackgroundProgressProvider(),
                videoCardClock.phase,
                videoCardBackgroundGestureRestoreInProgress,
            )
            return@LaunchedEffect
        }
        while (true) {
            onVideoCardDepthFrameUpdated(
                videoCardBackgroundProgressProvider(),
                videoCardClock.phase,
                videoCardBackgroundGestureRestoreInProgress,
            )
            withFrameNanos { }
        }
    }
    // 仅系统减弱动画时降为 scrim-only；不按机型降级，保证完整 20px 景深观感。
    val transitionBackgroundMotionTier =
        if (rememberSystemReduceMotion()) MotionTier.Reduced else MotionTier.Normal
    var previousVideoCardTransitionBackStack by remember {
        mutableStateOf(safeBackStack)
    }
    LaunchedEffect(
        safeBackStack,
        cardTransitionEnabled,
        videoSharedTransitionDurationMillis,
    ) {
        val previousStack = previousVideoCardTransitionBackStack
        val previousTop = previousStack.lastOrNull()
        val currentTop = safeBackStack.lastOrNull()
        val openingSourceRoute = resolveCardMorphDestinationSourceRoute(currentTop)
        val returningSourceRoute = resolveCardMorphDestinationSourceRoute(previousTop)
        val openedVideoDetail = isCardMorphDestinationNavKey(currentTop) &&
            safeBackStack.size > previousStack.size
        val returnedFromVideoDetail = isCardMorphDestinationNavKey(previousTop) &&
            safeBackStack.size < previousStack.size
        previousVideoCardTransitionBackStack = safeBackStack

        if (!cardTransitionEnabled) {
            cancelVideoCardDepthAnimation()
            videoCardReturnGestureInProgress = false
            videoCardClock.endGesture()
            videoCardClock.snapClearAndIdle()
            return@LaunchedEffect
        }

        when {
            openedVideoDetail -> {
                videoCardClock.beginOpening(openingSourceRoute)
                // fallback：shared 回灌接管前的保底；与 bounds enter 同 duration/easing
                launchVideoCardDepthAnimation {
                    videoCardClock.snapFallback(0f)
                    videoCardClock.animateFallbackTo(
                        target = 1f,
                        durationMillis = timelineSpec.durationMillis,
                        easing = timelineSpec.enterEasing,
                    )
                    if (videoCardClock.phase == VideoCardTransitionBackgroundPhase.OPENING) {
                        videoCardClock.markHeld()
                        videoCardClock.snapFallback(1f)
                    }
                }
            }

            returnedFromVideoDetail -> {
                if (
                    isRelatedVideoDetailReturn(
                        fromKey = previousTop as? BiliPaiNavKey.VideoDetail,
                        toKey = currentTop,
                    )
                ) {
                    onRelatedVideoDetailReturned()
                }
                if (videoCardClock.phase != VideoCardTransitionBackgroundPhase.RETURNING) {
                    if (
                        shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                            isQuickReturnFromDetail = isQuickReturnFromDetailUpdated,
                            phase = videoCardClock.phase,
                        )
                    ) {
                        cancelVideoCardDepthAnimation()
                        videoCardClock.snapClearAndIdle()
                    } else {
                        val startDepth = videoCardClock.depthProgress()
                        videoCardClock.beginReturning(returningSourceRoute)
                        val fullDurationMs = resolveVideoCardTransitionReturnFullDurationMillis(
                            baseDurationMillis = timelineSpec.durationMillis,
                        )
                        val morphRemainingMs = resolveVideoCardSharedMorphRemainingDurationMs(
                            seekFraction = 0f,
                            fullDurationMs = fullDurationMs,
                        )
                        val clearDurationMs = resolveMorphAlignedFallbackDurationMs(
                            timelineDurationMs = morphRemainingMs,
                            startDepth = startDepth,
                            targetDepth = 0f,
                        )
                        launchVideoCardDepthAnimation {
                            videoCardClock.snapFallback(startDepth)
                            // shared 回灌优先；fallback 与 morph 同 Linear 满长墙钟
                            videoCardClock.animateFallbackTo(
                                target = 0f,
                                durationMillis = clearDurationMs,
                                easing = timelineSpec.returnEasing,
                            )
                            val parentSourceRoute =
                                resolveCardMorphDestinationSourceRoute(currentTop)
                            if (isVideoCardReturnTargetRoute(parentSourceRoute)) {
                                videoCardClock.sourceRoute = parentSourceRoute
                                videoCardClock.snapFallback(1f)
                                videoCardClock.markHeld()
                            } else if (
                                videoCardClock.phase ==
                                VideoCardTransitionBackgroundPhase.RETURNING
                            ) {
                                videoCardClock.markIdle()
                            }
                        }
                    }
                }
            }

            !isCardMorphDestinationNavKey(currentTop) -> {
                launchVideoCardDepthAnimation {
                    val start = videoCardClock.depthProgress()
                    videoCardClock.snapFallback(start)
                    videoCardClock.animateFallbackTo(
                        target = 0f,
                        durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
                        easing = FastOutLinearInEasing,
                    )
                    videoCardClock.markIdle()
                }
            }
        }
    }
    val popRouteTransition = remember(
        cardTransitionEnabled,
        sourceMetadata,
        safeBackStack,
        activeMainHostRoute,
    ) {
        resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = cardTransitionEnabled,
            sourceMetadata = sourceMetadata,
            fromKey = safeBackStack.lastOrNull(),
            toKey = safeBackStack.getOrNull(safeBackStack.lastIndex - 1),
            activeMainHostRoute = activeMainHostRoute,
        )
    }
    val autoPredictiveBackExitDirection = remember(popRouteTransition, sourceMetadata.cardSourceDirection) {
        resolveBiliPaiAutoPredictiveBackExitDirection(
            popRouteTransition = popRouteTransition,
            cardSourceDirection = sourceMetadata.cardSourceDirection,
        )
    }
    val predictiveBackExitDirection = remember(
        autoPredictiveBackExitDirection,
        predictiveBackExitDirectionOverride,
    ) {
        resolveBiliPaiPredictiveBackExitDirection(
            storageValue = predictiveBackExitDirectionOverride,
            autoDerived = autoPredictiveBackExitDirection,
        )
    }
    val predictiveBackHandler: BiliPaiPredictiveBackAnimationHandler = remember(
        popRouteTransition,
        predictiveBackEnabled,
        predictiveBackAnimationStyle,
        predictiveBackExitDirection,
    ) {
        resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = popRouteTransition,
            predictiveBackEnabled = predictiveBackEnabled,
            style = predictiveBackAnimationStyle,
            exitDirection = predictiveBackExitDirection,
        )
    }
    val currentBackKey = safeBackStack.lastOrNull()
    val targetBackKey = safeBackStack.getOrNull(safeBackStack.lastIndex - 1)
    val gestureReturningVideoCard = predictiveBackEnabled &&
        cardTransitionEnabled &&
        isVideoCardTransitionBackgroundGesturePhase(videoCardClock.phase) &&
        isCardMorphDestinationNavKey(currentBackKey) &&
        targetBackKey != null &&
        isVideoCardReturnTargetRoute(resolveCardMorphDestinationSourceRoute(currentBackKey))
    val predictiveBackGestureBlurEnabled = shouldApplyPredictiveBackGestureBlur(
        routeTransition = popRouteTransition,
        predictiveBackEnabled = predictiveBackEnabled,
        gestureReturningVideoCard = gestureReturningVideoCard,
        motionTier = transitionBackgroundMotionTier,
    )
    val predictiveBackBackgroundProgressProvider = remember(
        predictiveBackBackgroundProgress,
        predictiveBackGestureBlurEnabled,
        popRouteTransition,
    ) {
        {
            val liveBackProgress =
                (navigationEventState?.transitionState as? NavigationEventTransitionState.InProgress)
                    ?.latestEvent
                    ?.progress
            if (predictiveBackGestureBlurEnabled && liveBackProgress != null) {
                resolvePredictiveBackGestureBlurProgress(
                    backProgress = liveBackProgress,
                    routeTransition = popRouteTransition,
                )
            } else {
                predictiveBackBackgroundProgress.value
            }
        }
    }
    val performBack: (() -> Unit) -> Unit = { commitTransitionCallBack ->
        navigationScope.launch {
            val predictiveBlurAtCommit = predictiveBackBackgroundProgressProvider()
            val shouldFadePredictiveBlur = shouldApplyPredictiveBackGestureBlur(
                routeTransition = popRouteTransition,
                predictiveBackEnabled = predictiveBackEnabled,
                gestureReturningVideoCard = false,
                motionTier = transitionBackgroundMotionTier,
            ) && predictiveBlurAtCommit > 0f
            val predictiveBlurFadeJob = if (shouldFadePredictiveBlur) {
                launch {
                    predictiveBackBackgroundProgress.snapTo(predictiveBlurAtCommit)
                    predictiveBackBackgroundProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = resolvePredictiveBackCommitBlurDurationMs(
                                predictiveBlurAtCommit
                            ),
                            easing = FastOutLinearInEasing,
                        ),
                    )
                }
            } else {
                null
            }
            predictiveBackHandler.onBackPressed(
                transitionState = navigationEventState?.transitionState,
                currentPageKey = safeBackStack.lastOrNull(),
            )
            predictiveBlurFadeJob?.join()
            val isVideoCardActiveReturn = cardTransitionEnabled &&
                (
                    videoCardClock.phase == VideoCardTransitionBackgroundPhase.HELD ||
                        videoCardClock.phase == VideoCardTransitionBackgroundPhase.OPENING
                    ) &&
                isCardMorphDestinationNavKey(currentBackKey)
            if (isVideoCardActiveReturn) {
                cancelVideoCardDepthAnimation()
            }
            val videoBlurFadeJob = if (isVideoCardActiveReturn) {
                val morphSource = resolveCardMorphDestinationSourceRoute(currentBackKey)
                val quickReturnForDepthClear = onPrepareVideoCardSharedReturn()
                if (
                    shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                        isQuickReturnFromDetail = quickReturnForDepthClear,
                        phase = videoCardClock.phase,
                    )
                ) {
                    videoCardClock.snapClearAndIdle()
                    null
                } else {
                    val gestureFractionAtCommit = videoCardClock.gestureBackProgress
                    val blurAtCommit = videoCardClock.depthProgress()
                    videoCardClock.beginReturning(morphSource)
                    val fullDurationMs = resolveVideoCardTransitionReturnFullDurationMillis(
                        baseDurationMillis = timelineSpec.durationMillis,
                    )
                    val morphRemainingMs = resolveVideoCardSharedMorphRemainingDurationMs(
                        seekFraction = gestureFractionAtCommit ?: 0f,
                        fullDurationMs = fullDurationMs,
                    )
                    val clearDurationMs = resolveMorphAlignedFallbackDurationMs(
                        timelineDurationMs = morphRemainingMs,
                        startDepth = blurAtCommit,
                        targetDepth = 0f,
                    )
                    launchVideoCardDepthAnimation {
                        videoCardClock.snapFallback(blurAtCommit)
                        videoCardClock.animateFallbackTo(
                            target = 0f,
                            durationMillis = clearDurationMs,
                            easing = timelineSpec.returnEasing,
                        )
                        val parentSourceRoute =
                            resolveCardMorphDestinationSourceRoute(targetBackKey)
                        if (isVideoCardReturnTargetRoute(parentSourceRoute)) {
                            videoCardClock.sourceRoute = parentSourceRoute
                            videoCardClock.snapFallback(1f)
                            videoCardClock.markHeld()
                        } else if (
                            videoCardClock.phase ==
                            VideoCardTransitionBackgroundPhase.RETURNING
                        ) {
                            videoCardClock.markIdle()
                        }
                    }
                    videoCardDepthAnimationJob
                }
            } else {
                null
            }
            videoCardReturnGestureInProgress = false
            videoCardClock.endGesture()
            commitTransitionCallBack()
            onBack()
            videoBlurFadeJob?.join()
            predictiveBackBackgroundProgress.snapTo(0f)
        }
    }
    val quickReturnFromDetailProvider = remember {
        { isQuickReturnFromDetailUpdated }
    }
    val scopedContent: @Composable (BiliPaiNavKey) -> Unit = remember(
        content,
        application,
        safeBackStack,
        videoCardClock,
        videoCardBackgroundProgressProvider,
        predictiveBackBackgroundProgressProvider,
        transitionBackgroundMotionTier,
        isLightBackground,
        quickReturnFromDetailProvider,
        morphProgressReporter,
    ) {
        { key ->
            val entryRoute = key.toLegacyRoute()
            Box(modifier = Modifier.fillMaxSize()) {
                ProvideAnimatedVisibilityScope(
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                ) {
                    CompositionLocalProvider(
                        LocalVideoCardSharedElementSourceRoute provides entryRoute,
                        LocalVideoCardTransitionClock provides videoCardClock,
                        LocalVideoCardMorphProgressReporter provides morphProgressReporter,
                        LocalVideoCardTransitionBackgroundState provides VideoCardTransitionBackgroundState(
                            progressProvider = videoCardBackgroundProgressProvider,
                            sourceRouteProvider = {
                                videoCardClock.sourceRoute
                            },
                            phaseProvider = {
                                videoCardClock.phase
                            },
                            isReturnGestureInProgressProvider = {
                                videoCardReturnGestureInProgress
                            },
                            isGestureRestoreInProgressProvider = {
                                videoCardBackgroundGestureRestoreInProgress
                            },
                            isQuickReturnFromDetailProvider = quickReturnFromDetailProvider,
                            motionTierProvider = {
                                transitionBackgroundMotionTier
                            },
                            isLightBackgroundProvider = {
                                isLightBackground
                            },
                        ),
                        LocalPredictiveBackBackgroundState provides PredictiveBackBackgroundState(
                            progressProvider = predictiveBackBackgroundProgressProvider,
                            targetKeyProvider = {
                                safeBackStack.getOrNull(safeBackStack.lastIndex - 1)
                            },
                            motionTierProvider = {
                                transitionBackgroundMotionTier
                            },
                            isLightBackgroundProvider = {
                                isLightBackground
                            },
                        ),
                    ) {
                        ProvideNavigation3ViewModelApplicationExtras(application) {
                            content(key)
                        }
                    }
                }
            }
        }
    }
    val entryProvider = remember(sourceMetadata, cardTransitionEnabled, visibleBottomBarRoutes, activeMainHostRoute, scopedContent) {
        biliPaiNavEntryProvider(
            sourceMetadata = sourceMetadata,
            cardTransitionEnabled = cardTransitionEnabled,
            visibleBottomBarRoutes = visibleBottomBarRoutes,
            activeMainHostRoute = activeMainHostRoute,
            content = scopedContent
        )
    }
    val entries = rememberDecoratedNavEntries(
        backStack = safeBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            NavEntryDecorator(
                onPop = { key ->
                    predictiveBackHandler.onPagePop(
                        contentPageKey = key,
                        animationScope = navigationScope,
                    )
                }
            ) { entry ->
                with(predictiveBackHandler) {
                    Box(
                        modifier = Modifier.predictiveBackAnimationDecorator(
                            transitionState = navigationEventState?.transitionState,
                            contentPageKey = entry.contentKey,
                            currentPageKey = safeBackStack.lastOrNull(),
                        )
                    ) {
                        entry.Content()
                    }
                }
            }
        ),
        entryProvider = entryProvider
    )
    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = sharedTransitionScope,
        onBack = { performBack { } }
    )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
    navigationEventState = rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos
    )
    val transitionState = navigationEventState.transitionState
    val inProgressState = transitionState as? NavigationEventTransitionState.InProgress
    val nativeVideoBackProgress = inProgressState?.latestEvent?.progress
    SideEffect {
        if (nativeVideoBackProgress != null) {
            onNativeVideoBackProgress(currentBackKey, targetBackKey, nativeVideoBackProgress)
        }
    }

    // 预测手势：最高优先级写入 clock，与 shared seek 同一 depth 读口。
    SideEffect {
        val gestureProgress = nativeVideoBackProgress
        val gestureActive = gestureReturningVideoCard && gestureProgress != null
        if (gestureActive && gestureProgress != null) {
            videoCardClock.beginGesture(gestureProgress)
        } else if (videoCardReturnGestureInProgress) {
            videoCardClock.endGesture()
        }
        videoCardReturnGestureInProgress = gestureActive
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        // 关闭全局预测性返回时不向 NavDisplay 上报 InProgress，避免 seek 跟手预览；
        // 松手后仍走 performBack + 普通 popTransitionSpec。
        reportPredictiveProgress = predictiveBackEnabled,
        onBackCompleted = performBack,
        onBackCancelled = { commitTransition ->
            onNativeVideoBackCancelled(currentBackKey, targetBackKey)
            val cancelledVideoCardBlur = videoCardBackgroundProgressProvider()
            val cancelledPredictiveBlur = predictiveBackBackgroundProgressProvider()
            videoCardReturnGestureInProgress = false
            videoCardClock.endGesture()
            // 手势取消：depth 复原到满值，与详情回弹一致。
            if (isVideoCardTransitionBackgroundGesturePhase(videoCardClock.phase) &&
                cancelledVideoCardBlur < 1f
            ) {
                navigationScope.launch {
                    videoCardBackgroundGestureRestoreInProgress = true
                    try {
                        videoCardClock.snapFallback(cancelledVideoCardBlur)
                        videoCardClock.animateFallbackTo(
                            target = 1f,
                            durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
                            easing = FastOutLinearInEasing,
                        )
                        videoCardClock.markHeld()
                    } finally {
                        videoCardBackgroundGestureRestoreInProgress = false
                    }
                }
            }
            if (predictiveBackGestureBlurEnabled && cancelledPredictiveBlur > 0f) {
                navigationScope.launch {
                    predictiveBackBackgroundProgress.snapTo(cancelledPredictiveBlur)
                    predictiveBackBackgroundProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = PREDICTIVE_BACK_BACKGROUND_CANCEL_DURATION_MS,
                            easing = FastOutLinearInEasing,
                        ),
                    )
                }
            }
            commitTransition()
        },
    )

    val showVideoCardNavBackdrop = shouldShowVideoCardTransitionNavBackdrop(
        cardTransitionEnabled = cardTransitionEnabled,
        phase = videoCardClock.phase,
        isVideoDetailOnStack = isCardMorphDestinationNavKey(currentBackKey),
        isReturningToVideoDetail = isCardMorphDestinationNavKey(targetBackKey),
    )

    Box(modifier = modifier.fillMaxSize()) {
        val settingsSubtreeBackdrop =
            (currentBackKey != null && isSettingsSubtreeNavKey(currentBackKey)) ||
                (targetBackKey != null && isSettingsSubtreeNavKey(targetBackKey))
        if (settingsSubtreeBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppSurfaceTokens.groupedListContainer()),
            )
        }
        VideoCardTransitionNavBackdrop(
            visible = showVideoCardNavBackdrop,
            progressProvider = videoCardBackgroundProgressProvider,
            phase = videoCardClock.phase,
            isLightBackground = isLightBackground,
        )
        NavDisplay(
            sceneState = sceneState,
            navigationEventState = navigationEventState,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
            sizeTransform = null,
            transitionEffects = NavDisplayTransitionEffects(blockInputDuringTransition = true),
            transitionSpec = {
                with(predictiveBackHandler) {
                    onTransitionSpec()
                }
            },
            popTransitionSpec = {
                with(predictiveBackHandler) {
                    onPopTransitionSpec()
                }
            },
            predictivePopTransitionSpec = { swipeEdge ->
                with(predictiveBackHandler) {
                    onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
                }
            },
        )
    }
}

@Composable
private fun ProvideNavigation3ViewModelApplicationExtras(
    application: Application,
    content: @Composable () -> Unit
) {
    val navEntryOwner = LocalViewModelStoreOwner.current
    if (navEntryOwner == null) {
        content()
        return
    }

    val patchedOwner = remember(navEntryOwner, application) {
        buildNavigation3ViewModelStoreOwner(navEntryOwner, application)
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides patchedOwner) {
        content()
    }
}

private fun buildNavigation3ViewModelStoreOwner(
    navEntryOwner: ViewModelStoreOwner,
    application: Application
): ViewModelStoreOwner {
    val defaultFactoryOwner = navEntryOwner as? HasDefaultViewModelProviderFactory
    val defaultCreationExtras = defaultFactoryOwner?.defaultViewModelCreationExtras
        ?: CreationExtras.Empty
    val patchedCreationExtras = MutableCreationExtras(defaultCreationExtras).apply {
        set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, application)
    }

    return object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore = navEntryOwner.viewModelStore
        override val defaultViewModelProviderFactory =
            defaultFactoryOwner?.defaultViewModelProviderFactory
                ?: ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        override val defaultViewModelCreationExtras: CreationExtras = patchedCreationExtras
    }
}
