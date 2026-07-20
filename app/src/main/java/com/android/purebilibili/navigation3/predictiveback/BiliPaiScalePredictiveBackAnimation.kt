package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEvent.Companion.EDGE_LEFT
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.android.purebilibili.core.ui.util.rememberDeviceCornerRadius
import com.android.purebilibili.navigation3.BiliPaiNavKey
import kotlinx.coroutines.CoroutineScope

/** InstallerX Scale：顶页缩放开角，底层 dim；commit 时滑出再 pop。 */
internal class BiliPaiScalePredictiveBackAnimation(
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
        // 与 AOSP 对齐：手势 commit 与工具栏返回都要播完 exit，再让栈 pop。
        val isInterruptingEnter = transitionState is InProgress && !inPredictiveBackAnimation
        if (!isInterruptingEnter) {
            exitingPageKey = currentPageKey.toString()
            exitAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            )
            exitAnimatable.snapTo(0f)
        }
    }

    override fun onPagePop(contentPageKey: Any, animationScope: CoroutineScope) {
        if (exitingPageKey == contentPageKey.toString()) {
            exitingPageKey = null
        }
    }

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: BiliPaiNavKey?,
    ): Modifier {
        val windowInfo = LocalWindowInfo.current
        val navContent = LocalNavAnimatedContentScope.current
        val containerHeightPx = windowInfo.containerSize.height
        val containerWidthPx = windowInfo.containerSize.width.toFloat()
        val pageKey = contentPageKey.toString()
        val transition = navContent.transition
        val deviceCornerRadius = rememberDeviceCornerRadius()

        return if (pageKey == currentPageKey.toString() || exitingPageKey == pageKey) {
            val animatedScale by transition.animateFloat(
                transitionSpec = { tween(300) },
                label = "BiliPaiScalePredictiveScale",
            ) { state ->
                when (state) {
                    EnterExitState.PostExit -> 0.85f
                    else -> 1f
                }
            }
            inPredictiveBackAnimation = animatedScale != 1f

            val progressInProgress = transitionState as? InProgress
            val edge = progressInProgress?.latestEvent?.swipeEdge ?: 0
            val touchY = progressInProgress?.latestEvent?.touchY
            val currentPivotY = if (touchY != null && containerHeightPx > 0) {
                (touchY / containerHeightPx).coerceIn(0.1f, 0.9f)
            } else {
                0.5f
            }
            val currentPivotX = if (edge == EDGE_LEFT) 0.8f else 0.2f
            val directionMultiplier = when (exitDirection) {
                BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE ->
                    if (edge == EDGE_LEFT) 1f else -1f
                BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT -> 1f
                BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT -> -1f
            }
            val exitProgress =
                if (pageKey != currentPageKey.toString()) 1f else exitAnimatable.value
            val needsClip = inPredictiveBackAnimation || exitingPageKey != null

            graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = containerWidthPx * exitProgress * directionMultiplier
                transformOrigin = TransformOrigin(currentPivotX, currentPivotY)
            }.clip(
                if (needsClip) RoundedCornerShape(deviceCornerRadius) else RoundedCornerShape(0.dp),
            ).zIndex(1f)
        } else if (transitionState is InProgress) {
            val progress = if (!inPredictiveBackAnimation) 1f else exitAnimatable.value
            val dynamicAlpha = 0.5f * (1f - progress)
            drawWithContent {
                drawContent()
                drawRect(color = Color.Black.copy(alpha = dynamicAlpha))
            }
        } else {
            this
        }
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
            targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn(),
            initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
            sizeTransform = null,
        )

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<BiliPaiNavKey>().invoke(this)
}
