package com.android.purebilibili.feature.video.screen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.LocalVideoCardMorphProgressReporter
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellContainerTransform

@OptIn(ExperimentalSharedTransitionApi::class)
internal data class VideoDetailTransitionState(
    val animatedVisibilityScope: AnimatedVisibilityScope?,
    val sharedTransitionScope: SharedTransitionScope?,
    val isExitTransitionInProgress: Boolean,
    val detailShellSharedBoundsEnabled: Boolean,
    val suppressEnterFadeAfterBackPreview: Boolean,
    val progress: State<Float>,
    val detailChildTransitionEnabled: Boolean,
    val coverSharedBoundsActive: Boolean,
    val sharedBoundsActive: Boolean,
    val routeSheetFrameProvider: () -> VideoDetailRouteSheetFrame,
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberVideoDetailTransitionState(
    bvid: String,
    sourceRoute: String?,
    transitionEnabled: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    motionSpec: VideoSharedTransitionMotionSpec,
    routeSheetMotion: VideoDetailRouteSheetMotion,
): VideoDetailTransitionState {
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val isExitTransitionInProgress =
        animatedVisibilityScope?.transition?.targetState == EnterExitState.PostExit
    val detailShellSharedBoundsEnabled = shouldUseVideoCardShellContainerTransform(
        sourceRoute = sourceRoute,
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null,
    )
    var wasKeptAsBackPreview by rememberSaveable(bvid) { mutableStateOf(false) }
    SideEffect {
        if (keepLoadedContentForBackPreview) wasKeptAsBackPreview = true
    }
    val suppressEnterFadeAfterBackPreview = shouldSuppressVideoDetailEnterFadeAfterBackPreview(
        wasKeptAsBackPreview = wasKeptAsBackPreview,
        keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
    )
    // Shell sharedBounds 路径：根 progress 必须与 boundsTransform 同 duration/easing，
    // 再回灌 VideoCardTransitionClock，形成单时钟。
    val progress = if (
        shouldUseVideoDetailRootTransitionProgress(
            detailShellSharedBoundsEnabled = detailShellSharedBoundsEnabled,
            hasAnimatedVisibilityScope = animatedVisibilityScope != null,
            keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
        )
    ) {
        requireNotNull(animatedVisibilityScope).transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.PostExit) {
                    // 返回：Linear + 满 morph 时长，与 videoSharedElementReturnTweenSpec 一致
                    tween(
                        durationMillis = motionSpec.durationMillis.coerceAtLeast(0),
                        easing = androidx.compose.animation.core.LinearEasing,
                    )
                } else {
                    // 进场：Continuity + 满 morph 时长，与 bounds enter 一致
                    tween(
                        durationMillis = motionSpec.durationMillis.coerceAtLeast(0),
                        easing = motionSpec.enterAlphaEasing,
                    )
                }
            },
            label = "video-detail-shared-morph-clock",
        ) { state ->
            if (state == EnterExitState.Visible) 1f else 0f
        }
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val detailChildTransitionEnabled = transitionEnabled && !detailShellSharedBoundsEnabled
    val coverSharedBoundsActive = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = detailChildTransitionEnabled,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null,
    ) && !sourceRoute.isNullOrBlank()
    val sharedBoundsActive = detailShellSharedBoundsEnabled || coverSharedBoundsActive
    // 单时钟回灌：与 sharedBounds 共用 AVS Transition 的 progress。
    val morphReporter = LocalVideoCardMorphProgressReporter.current
    val morphFraction = progress.value
    val morphActive = detailShellSharedBoundsEnabled && (
        sharedTransitionScope?.isTransitionActive == true ||
            animatedVisibilityScope?.transition?.isRunning == true ||
            isExitTransitionInProgress
        )
    SideEffect {
        if (detailShellSharedBoundsEnabled) {
            morphReporter?.report(
                morphFraction = morphFraction,
                active = morphActive,
            )
        }
    }
    DisposableEffect(detailShellSharedBoundsEnabled, morphReporter) {
        onDispose {
            if (detailShellSharedBoundsEnabled) {
                morphReporter?.report(morphFraction = morphFraction, active = false)
            }
        }
    }
    val routeSheetFrameProvider = rememberVideoDetailRouteSheetFrameProvider(
        motion = routeSheetMotion,
        isExitTransitionInProgress = isExitTransitionInProgress,
        sharedBoundsActive = sharedBoundsActive,
    )
    return VideoDetailTransitionState(
        animatedVisibilityScope = animatedVisibilityScope,
        sharedTransitionScope = sharedTransitionScope,
        isExitTransitionInProgress = isExitTransitionInProgress,
        detailShellSharedBoundsEnabled = detailShellSharedBoundsEnabled,
        suppressEnterFadeAfterBackPreview = suppressEnterFadeAfterBackPreview,
        progress = progress,
        detailChildTransitionEnabled = detailChildTransitionEnabled,
        coverSharedBoundsActive = coverSharedBoundsActive,
        sharedBoundsActive = sharedBoundsActive,
        routeSheetFrameProvider = routeSheetFrameProvider,
    )
}

@Composable
internal fun rememberVideoDetailRouteSheetFrameProvider(
    motion: VideoDetailRouteSheetMotion,
    isExitTransitionInProgress: Boolean,
    sharedBoundsActive: Boolean = false
): () -> VideoDetailRouteSheetFrame {
    // shell sharedBounds 接管整张详情壳的 morph 时，sheet 自身的 scale/translation/corner/scrim
    // 必须全部停摆——否则会与共享元素同时形变导致撕裂。等价于 motion.enabled = false。
    val effectiveMotion = if (sharedBoundsActive) motion.copy(enabled = false) else motion
    val routeSheetProgress = remember(effectiveMotion.enabled) {
        Animatable(if (effectiveMotion.enabled) 0f else 1f)
    }
    val routeSheetSettleProgress = remember(effectiveMotion.enabled) {
        Animatable(0f)
    }
    var settleDirection by remember {
        mutableStateOf(VideoDetailRouteSheetSettleDirection.None)
    }

    LaunchedEffect(
        effectiveMotion.enabled,
        effectiveMotion.mainDurationMillis,
        effectiveMotion.settleDurationMillis,
        effectiveMotion.enterEasing,
        effectiveMotion.returnEasing,
        isExitTransitionInProgress
    ) {
        if (!effectiveMotion.enabled) {
            settleDirection = VideoDetailRouteSheetSettleDirection.None
            routeSheetSettleProgress.snapTo(0f)
            routeSheetProgress.snapTo(1f)
            return@LaunchedEffect
        }

        settleDirection = VideoDetailRouteSheetSettleDirection.None
        routeSheetSettleProgress.snapTo(0f)
        val targetProgress = if (isExitTransitionInProgress) 0f else 1f
        routeSheetProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(
                durationMillis = effectiveMotion.mainDurationMillis,
                easing = if (isExitTransitionInProgress) {
                    effectiveMotion.returnEasing
                } else {
                    effectiveMotion.enterEasing
                }
            )
        )
        settleDirection = if (isExitTransitionInProgress) {
            VideoDetailRouteSheetSettleDirection.Return
        } else {
            VideoDetailRouteSheetSettleDirection.Enter
        }
        routeSheetSettleProgress.snapTo(1f)
        routeSheetSettleProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = effectiveMotion.settleDurationMillis,
                easing = if (isExitTransitionInProgress) {
                    effectiveMotion.returnEasing
                } else {
                    effectiveMotion.enterEasing
                }
            )
        )
        settleDirection = VideoDetailRouteSheetSettleDirection.None
    }

    return remember(effectiveMotion, routeSheetProgress, routeSheetSettleProgress) {
        {
            resolveVideoDetailRouteSheetFrame(
                rawProgress = routeSheetProgress.value,
                settleProgress = routeSheetSettleProgress.value,
                settleDirection = settleDirection,
                motion = effectiveMotion
            )
        }
    }
}

@Composable
internal fun VideoDetailRouteSheetHost(
    frameProvider: () -> VideoDetailRouteSheetFrame,
    motion: VideoDetailRouteSheetMotion,
    isFullscreenMode: Boolean,
    backgroundColor: Color,
    backgroundAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                val frame = frameProvider()
                if (frame.backgroundScrimAlpha > 0.001f) {
                    drawRect(Color.Black.copy(alpha = frame.backgroundScrimAlpha))
                }
                drawContent()
            }
            .graphicsLayer {
                val frame = frameProvider()
                scaleX = frame.scale
                scaleY = frame.scale
                translationY = with(density) {
                    frame.translationYDp.dp.toPx()
                }
                transformOrigin = TransformOrigin(0.5f, 0f)
                clip = motion.enabled && frame.cornerDp > 0.01f
                shape = RoundedCornerShape(frame.cornerDp.dp)
            }
            .background(
                if (isFullscreenMode) Color.Black.copy(alpha = backgroundAlpha)
                else backgroundColor.copy(alpha = backgroundAlpha)
            ),
    ) {
        content()
        overlayContent()
    }
}
