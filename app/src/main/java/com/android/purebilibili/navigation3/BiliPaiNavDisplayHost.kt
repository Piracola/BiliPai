package com.android.purebilibili.navigation3

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
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
import com.android.purebilibili.core.ui.ProvideAnimatedVisibilityScope
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.core.ui.transition.LocalPredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.PREDICTIVE_BACK_BACKGROUND_CANCEL_DURATION_MS
import com.android.purebilibili.core.ui.transition.PredictiveBackBackgroundState
import com.android.purebilibili.core.ui.transition.VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackCommitBlurDurationMs
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackGestureBlurProgress
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionBackgroundGestureBlurProgress
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionBackgroundReturnDurationMs
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionReturnFullDurationMillis
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionEnterEasing
import com.android.purebilibili.core.ui.transition.resolveVideoCardTransitionBackgroundReturnClearEasing
import com.android.purebilibili.core.ui.transition.isVideoCardTransitionBackgroundGesturePhase
import com.android.purebilibili.core.ui.transition.shouldApplyPredictiveBackGestureBlur
import com.android.purebilibili.core.ui.transition.shouldShowVideoCardTransitionNavBackdrop
import com.android.purebilibili.core.ui.transition.shouldSnapClearVideoCardDepthBlurOnQuickReturn
import com.android.purebilibili.core.ui.transition.VideoCardTransitionNavBackdrop
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
    val videoCardTransitionBackgroundProgress = remember { Animatable(0f) }
    val predictiveBackBackgroundProgress = remember { Animatable(0f) }
    val isQuickReturnFromDetailUpdated by rememberUpdatedState(isQuickReturnFromDetail)
    var videoCardTransitionBackgroundPhase by remember {
        mutableStateOf(VideoCardTransitionBackgroundPhase.IDLE)
    }
    var videoCardReturnGestureInProgress by remember { mutableStateOf(false) }
    var videoCardBackgroundGestureRestoreInProgress by remember { mutableStateOf(false) }
    var videoCardTransitionSourceRoute by remember { mutableStateOf<String?>(null) }
    var videoCardGestureStartBlurProgress by remember { mutableFloatStateOf(1f) }
    var videoCardGestureProgress by remember { mutableStateOf<Float?>(null) }
    // 景深动画唯一 owner：OPENING / RETURNING / cancel 互斥，避免双路径叠动画。
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
    val videoCardBackgroundProgressProvider = remember(videoCardTransitionBackgroundProgress) {
        {
            val backProgress = videoCardGestureProgress
            if (videoCardReturnGestureInProgress && backProgress != null) {
                resolveVideoCardTransitionBackgroundGestureBlurProgress(
                    phase = VideoCardTransitionBackgroundPhase.OPENING,
                    currentBlurProgress = videoCardGestureStartBlurProgress,
                    backProgress = backProgress,
                )
            } else {
                videoCardTransitionBackgroundProgress.value
            }
        }
    }
    val onVideoCardDepthFrameUpdated by rememberUpdatedState(onVideoCardDepthFrame)
    LaunchedEffect(
        videoCardTransitionBackgroundPhase,
        cardTransitionEnabled,
        videoCardReturnGestureInProgress,
        videoCardBackgroundGestureRestoreInProgress,
    ) {
        if (!cardTransitionEnabled ||
            videoCardTransitionBackgroundPhase == VideoCardTransitionBackgroundPhase.IDLE
        ) {
            onVideoCardDepthFrameUpdated(
                0f,
                VideoCardTransitionBackgroundPhase.IDLE,
                false,
            )
            return@LaunchedEffect
        }
        if (!shouldContinuouslyPublishVideoCardDepthFrames(
                phase = videoCardTransitionBackgroundPhase,
                isReturnGestureInProgress = videoCardReturnGestureInProgress,
                isGestureRestoreInProgress = videoCardBackgroundGestureRestoreInProgress,
            )
        ) {
            onVideoCardDepthFrameUpdated(
                videoCardBackgroundProgressProvider(),
                videoCardTransitionBackgroundPhase,
                videoCardBackgroundGestureRestoreInProgress,
            )
            return@LaunchedEffect
        }
        while (true) {
            onVideoCardDepthFrameUpdated(
                videoCardBackgroundProgressProvider(),
                videoCardTransitionBackgroundPhase,
                videoCardBackgroundGestureRestoreInProgress,
            )
            withFrameNanos { }
        }
    }
    // 系统减弱动画(省电/无障碍/开发者选项)时，过渡背景降级为仅 scrim，跳过全屏 GPU 实时模糊。
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
        val openingSourceRoute = (currentTop as? BiliPaiNavKey.VideoDetail)?.sourceRoute
        val returningSourceRoute = (previousTop as? BiliPaiNavKey.VideoDetail)?.sourceRoute
        val openedVideoDetail = currentTop is BiliPaiNavKey.VideoDetail &&
            safeBackStack.size > previousStack.size &&
            isVideoCardReturnTargetRoute(openingSourceRoute)
        val returnedFromVideoDetail = previousTop is BiliPaiNavKey.VideoDetail &&
            safeBackStack.size < previousStack.size &&
            isVideoCardReturnTargetRoute(returningSourceRoute)
        previousVideoCardTransitionBackStack = safeBackStack

        if (!cardTransitionEnabled) {
            cancelVideoCardDepthAnimation()
            videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
            videoCardReturnGestureInProgress = false
            videoCardGestureProgress = null
            videoCardTransitionBackgroundProgress.snapTo(0f)
            return@LaunchedEffect
        }

        when {
            openedVideoDetail -> {
                videoCardTransitionSourceRoute = openingSourceRoute
                videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING
                videoCardTransitionBackgroundProgress.snapTo(0f)
                // 进场动画跑在独立 Job：返回/打断时可 cancel，禁止补完后强行 HELD。
                launchVideoCardDepthAnimation {
                    videoCardTransitionBackgroundProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = videoSharedTransitionDurationMillis,
                            easing = resolveVideoCardSharedTransitionEnterEasing(),
                        )
                    )
                    // 仅当仍停留在 OPENING（未被返回打断）时才进入 HELD。
                    if (videoCardTransitionBackgroundPhase ==
                        VideoCardTransitionBackgroundPhase.OPENING
                    ) {
                        videoCardTransitionBackgroundPhase =
                            VideoCardTransitionBackgroundPhase.HELD
                    }
                }
            }

            returnedFromVideoDetail -> {
                videoCardTransitionSourceRoute = returningSourceRoute
                if (videoCardTransitionBackgroundPhase != VideoCardTransitionBackgroundPhase.RETURNING) {
                    if (
                        shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                            isQuickReturnFromDetail = isQuickReturnFromDetailUpdated,
                            phase = videoCardTransitionBackgroundPhase,
                        )
                    ) {
                        // 快速返回：shared 落位常快于景深消糊，立刻清零避免封面带模糊闪一下。
                        cancelVideoCardDepthAnimation()
                        videoCardTransitionBackgroundProgress.snapTo(0f)
                        videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
                    } else {
                        val remainingBlur = videoCardTransitionBackgroundProgress.value
                        videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.RETURNING
                        val fullDurationMs = resolveVideoCardTransitionReturnFullDurationMillis(
                            baseDurationMillis = videoSharedTransitionDurationMillis,
                        )
                        launchVideoCardDepthAnimation {
                            videoCardTransitionBackgroundProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = resolveVideoCardTransitionBackgroundReturnDurationMs(
                                        startProgress = remainingBlur,
                                        fullDurationMs = fullDurationMs,
                                    ),
                                    easing = resolveVideoCardTransitionBackgroundReturnClearEasing(),
                                ),
                            )
                            val parentSourceRoute =
                                (currentTop as? BiliPaiNavKey.VideoDetail)?.sourceRoute
                            if (isVideoCardReturnTargetRoute(parentSourceRoute)) {
                                videoCardTransitionSourceRoute = parentSourceRoute
                                videoCardTransitionBackgroundProgress.snapTo(1f)
                                videoCardTransitionBackgroundPhase =
                                    VideoCardTransitionBackgroundPhase.HELD
                            } else if (
                                videoCardTransitionBackgroundPhase ==
                                VideoCardTransitionBackgroundPhase.RETURNING
                            ) {
                                videoCardTransitionBackgroundPhase =
                                    VideoCardTransitionBackgroundPhase.IDLE
                            }
                        }
                    }
                }
            }

            currentTop !is BiliPaiNavKey.VideoDetail -> {
                launchVideoCardDepthAnimation {
                    videoCardTransitionBackgroundProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
                            easing = FastOutLinearInEasing
                        )
                    )
                    videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
                }
            }
        }
    }
    val popRouteTransition = remember(cardTransitionEnabled, sourceMetadata, safeBackStack) {
        resolveBiliPaiNavDisplayPopRouteTransition(
            cardTransitionEnabled = cardTransitionEnabled,
            sourceMetadata = sourceMetadata,
            fromKey = safeBackStack.lastOrNull(),
            toKey = safeBackStack.getOrNull(safeBackStack.lastIndex - 1)
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
        isVideoCardTransitionBackgroundGesturePhase(videoCardTransitionBackgroundPhase) &&
        currentBackKey is BiliPaiNavKey.VideoDetail &&
        targetBackKey != null &&
        isVideoCardReturnTargetRoute(currentBackKey.sourceRoute)
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
                    videoCardTransitionBackgroundPhase == VideoCardTransitionBackgroundPhase.HELD ||
                        videoCardTransitionBackgroundPhase == VideoCardTransitionBackgroundPhase.OPENING
                    ) &&
                currentBackKey is BiliPaiNavKey.VideoDetail &&
                isVideoCardReturnTargetRoute(currentBackKey.sourceRoute)
            // 先切断 OPENING Job，禁止进场补完后写入 HELD。
            if (isVideoCardActiveReturn) {
                cancelVideoCardDepthAnimation()
            }
            val videoBlurFadeJob = if (isVideoCardActiveReturn) {
                videoCardTransitionSourceRoute = currentBackKey.sourceRoute
                val quickReturnForDepthClear = onPrepareVideoCardSharedReturn()
                if (
                    shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                        isQuickReturnFromDetail = quickReturnForDepthClear,
                        phase = videoCardTransitionBackgroundPhase,
                    )
                ) {
                    videoCardTransitionBackgroundProgress.snapTo(0f)
                    videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE
                    null
                } else {
                    val blurAtCommit = videoCardBackgroundProgressProvider()
                    videoCardTransitionBackgroundProgress.snapTo(blurAtCommit)
                    videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.RETURNING
                    val fullDurationMs = resolveVideoCardTransitionReturnFullDurationMillis(
                        baseDurationMillis = videoSharedTransitionDurationMillis,
                    )
                    // 用统一 Job：栈变化触发的 LaunchedEffect 返回路径见 phase==RETURNING 会跳过。
                    launchVideoCardDepthAnimation {
                        videoCardTransitionBackgroundProgress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = resolveVideoCardTransitionBackgroundReturnDurationMs(
                                    startProgress = blurAtCommit,
                                    fullDurationMs = fullDurationMs,
                                ),
                                easing = resolveVideoCardTransitionBackgroundReturnClearEasing(),
                            ),
                        )
                        val parentSourceRoute =
                            (targetBackKey as? BiliPaiNavKey.VideoDetail)?.sourceRoute
                        if (isVideoCardReturnTargetRoute(parentSourceRoute)) {
                            videoCardTransitionSourceRoute = parentSourceRoute
                            videoCardTransitionBackgroundProgress.snapTo(1f)
                            videoCardTransitionBackgroundPhase =
                                VideoCardTransitionBackgroundPhase.HELD
                        } else if (
                            videoCardTransitionBackgroundPhase ==
                            VideoCardTransitionBackgroundPhase.RETURNING
                        ) {
                            videoCardTransitionBackgroundPhase =
                                VideoCardTransitionBackgroundPhase.IDLE
                        }
                    }
                    videoCardDepthAnimationJob
                }
            } else {
                null
            }
            videoCardReturnGestureInProgress = false
            videoCardGestureProgress = null
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
        videoCardTransitionBackgroundProgress,
        videoCardBackgroundProgressProvider,
        predictiveBackBackgroundProgressProvider,
        transitionBackgroundMotionTier,
        isLightBackground,
        quickReturnFromDetailProvider,
    ) {
        { key ->
            val entryRoute = key.toLegacyRoute()
            Box(modifier = Modifier.fillMaxSize()) {
                ProvideAnimatedVisibilityScope(
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                ) {
                    CompositionLocalProvider(
                        LocalVideoCardSharedElementSourceRoute provides entryRoute,
                        LocalVideoCardTransitionBackgroundState provides VideoCardTransitionBackgroundState(
                            progressProvider = videoCardBackgroundProgressProvider,
                            sourceRouteProvider = {
                                videoCardTransitionSourceRoute
                            },
                            phaseProvider = {
                                videoCardTransitionBackgroundPhase
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

    // 预测式返回手势进行中(video → card 返回目标)时，让首页全屏高斯模糊随手势进度实时消退，
    // 与共享元素 morph 同步，避免手势"吃掉"过渡时间轴后、提交返回时封面才在满模糊下补一段收尾。
    // OPENING 阶段同样启用：以当前开场虚化进度为起点线性消退，避免进入未完成即手势返回时背景"卡住"。
    SideEffect {
        val gestureActive = gestureReturningVideoCard && nativeVideoBackProgress != null
        if (gestureActive && !videoCardReturnGestureInProgress) {
            videoCardGestureStartBlurProgress = videoCardTransitionBackgroundProgress.value
        }
        videoCardGestureProgress = nativeVideoBackProgress.takeIf { gestureActive }
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
            videoCardGestureProgress = null
            // 手势取消且详情页仍在栈顶(HELD/OPENING)：把随手势消退的背景虚化平滑复原到满值，
            // 与详情页回弹到全屏保持一致，避免下次手势从残留的中间虚化值起跳。
            if (isVideoCardTransitionBackgroundGesturePhase(videoCardTransitionBackgroundPhase) &&
                cancelledVideoCardBlur < 1f
            ) {
                navigationScope.launch {
                    videoCardBackgroundGestureRestoreInProgress = true
                    try {
                        videoCardTransitionBackgroundProgress.snapTo(cancelledVideoCardBlur)
                        videoCardTransitionBackgroundProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
                                easing = FastOutLinearInEasing
                            )
                        )
                        videoCardTransitionBackgroundPhase = VideoCardTransitionBackgroundPhase.HELD
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
        phase = videoCardTransitionBackgroundPhase,
        isVideoDetailOnStack = currentBackKey is BiliPaiNavKey.VideoDetail,
        isReturningToVideoDetail = targetBackKey is BiliPaiNavKey.VideoDetail,
    )

    Box(modifier = modifier.fillMaxSize()) {
        VideoCardTransitionNavBackdrop(
            visible = showVideoCardNavBackdrop,
            progressProvider = videoCardBackgroundProgressProvider,
            phase = videoCardTransitionBackgroundPhase,
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
