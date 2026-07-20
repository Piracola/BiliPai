package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.android.purebilibili.core.ui.util.rememberDeviceCornerRadius
import com.android.purebilibili.navigation3.BiliPaiNavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** InstallerX AOSP：AnimatedContent predictive 置空，graphicsLayer 跟手，commit 播完再 pop。 */
internal class BiliPaiAospPredictiveBackAnimation(
    private val exitDirection: BiliPaiPredictiveBackExitDirection =
        BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
) : BiliPaiPredictiveBackAnimationHandler {
    private var exitingPageKey: String? = null
    private val exitAnimatable = Animatable(0f)
    private var inPredictiveBackAnimation = false

    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: BiliPaiNavKey?,
    ) {
        val isInterruptingEnter = transitionState is InProgress && !inPredictiveBackAnimation
        if (!isInterruptingEnter) {
            exitingPageKey = currentPageKey.toString()
            exitAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 150, easing = LinearEasing),
            )
        }
    }

    override fun onPagePop(contentPageKey: Any, animationScope: CoroutineScope) {
        if (exitingPageKey == contentPageKey.toString()) {
            exitingPageKey = null
            animationScope.launch { exitAnimatable.snapTo(0f) }
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: BiliPaiNavKey?,
    ): Modifier = composed {
        val windowInfo = LocalWindowInfo.current
        val navContent = LocalNavAnimatedContentScope.current
        val transition = navContent.transition
        val containerHeightPx = windowInfo.containerSize.height
        val pageKey = contentPageKey.toString()
        val deviceCornerRadius = rememberDeviceCornerRadius()
        val enteringStartOffsetPx = with(LocalDensity.current) { 96.dp.toPx() }
        val linearProgress = exitAnimatable.value
        val emphasizedProgress = CubicBezierEasing(0.2f, 0f, 0f, 1f).transform(linearProgress)
        val progressInProgress = transitionState as? InProgress
        val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
        val touchY = progressInProgress?.latestEvent?.touchY
        val gestureProgress = progressInProgress?.latestEvent?.progress ?: 0f
        val animatedScale by transition.animateFloat(
            transitionSpec = { tween(300) },
            label = "BiliPaiAospPredictiveScale",
        ) { state ->
            when (state) {
                EnterExitState.PostExit -> 0.85f
                else -> 1f
            }
        }

        if (pageKey == currentPageKey.toString()) {
            inPredictiveBackAnimation = animatedScale != 1f
        }

        val directionMultiplier = when (exitDirection) {
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE -> if (edge == EDGE_LEFT) 1f else -1f
            BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
            BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT -> -1f
        }
        val isExitingPage = exitingPageKey != null && exitingPageKey == pageKey
        val isCurrentNavTarget = exitingPageKey == null && pageKey == currentPageKey.toString()
        val maxScale = 0.85f
        val dragScale = 1f - (1f - maxScale) * gestureProgress
        val currentPivotY = if (touchY != null && containerHeightPx > 0) {
            (touchY / containerHeightPx).coerceIn(0.1f, 0.9f)
        } else {
            0.5f
        }
        val currentPivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f
        val needsClip =
            (transitionState is InProgress && inPredictiveBackAnimation) || exitingPageKey != null

        this
            .graphicsLayer {
                if (transitionState is InProgress && !inPredictiveBackAnimation && exitingPageKey == null) {
                    return@graphicsLayer
                }
                if (transitionState is InProgress) {
                    transformOrigin = TransformOrigin(currentPivotX, currentPivotY)
                }
                when {
                    isExitingPage -> {
                        val computedScale = dragScale + (maxScale - dragScale) * emphasizedProgress
                        scaleX = computedScale
                        scaleY = computedScale
                        translationX = enteringStartOffsetPx * directionMultiplier * emphasizedProgress
                        alpha = if (linearProgress >= 0.2f) {
                            0f
                        } else {
                            (1f - linearProgress * 5f).coerceAtLeast(0f)
                        }
                    }
                    isCurrentNavTarget -> {
                        scaleX = dragScale
                        scaleY = dragScale
                        translationX = 0f
                        alpha = 1f
                    }
                    else -> {
                        val initialTranslationX = -enteringStartOffsetPx * directionMultiplier
                        if (exitingPageKey != null) {
                            scaleX = dragScale + (1f - dragScale) * emphasizedProgress
                            scaleY = dragScale + (1f - dragScale) * emphasizedProgress
                            translationX = initialTranslationX * (1f - emphasizedProgress)
                            alpha = 1f
                        } else if (transitionState is InProgress) {
                            scaleX = dragScale
                            scaleY = dragScale
                            translationX = initialTranslationX
                            alpha = 1f
                        }
                    }
                }
            }
            .clip(
                if (needsClip) RoundedCornerShape(deviceCornerRadius) else RoundedCornerShape(0.dp),
            )
            .zIndex(if (isExitingPage || isCurrentNavTarget) 1f else 0f)
    }

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int,
    ): ContentTransform = ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = null,
    )

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec(): ContentTransform =
        ContentTransform(
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null,
        )

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<BiliPaiNavKey>().invoke(this)
}
