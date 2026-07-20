package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState
import com.android.purebilibili.navigation3.BiliPaiNavKey

/** InstallerX Classic：predictive 与 pop 同一套 slide + scaleOut。 */
internal class BiliPaiClassicPredictiveBackAnimation : BiliPaiPredictiveBackAnimationHandler {
    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: BiliPaiNavKey?,
    ) = Unit

    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: BiliPaiNavKey?,
    ): Modifier = this

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int,
    ): ContentTransform = classicPopTransform()

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec(): ContentTransform =
        classicPopTransform()

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<BiliPaiNavKey>().invoke(this)

    private fun classicPopTransform(): ContentTransform = ContentTransform(
        targetContentEnter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
        initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
        sizeTransform = null,
    )
}
