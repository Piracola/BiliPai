package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.navigation3.BiliPaiNavKey

internal class BiliPaiSharedElementPredictiveBackAnimation : BiliPaiPredictiveBackAnimationHandler {
    override suspend fun onBackPressed(
        transitionState: NavigationEventTransitionState?,
        currentPageKey: BiliPaiNavKey?,
    ) = Unit

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Modifier.predictiveBackAnimationDecorator(
        transitionState: NavigationEventTransitionState?,
        contentPageKey: Any,
        currentPageKey: BiliPaiNavKey?,
    ): Modifier {
        val gestureInProgress = transitionState is InProgress
        val isUnderlyingSourcePage = gestureInProgress &&
            currentPageKey != null &&
            contentPageKey != currentPageKey
        if (!isUnderlyingSourcePage) {
            return this
        }
        // 底层来源页（首页等）：钉住 lookahead 位置，去掉预测返回 seek 带来的横移。
        val sharedTransitionScope = LocalSharedTransitionScope.current
        return if (sharedTransitionScope != null) {
            with(sharedTransitionScope) {
                this@predictiveBackAnimationDecorator.skipToLookaheadPosition()
            }
        } else {
            graphicsLayer { translationX = 0f }
        }
    }

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPredictivePopTransitionSpec(
        swipeEdge: Int,
    ): ContentTransform = noOpSharedElementContentTransform()

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec(): ContentTransform =
        noOpSharedElementContentTransform()

    private fun noOpSharedElementContentTransform(): ContentTransform =
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
            sizeTransform = null,
        )

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<BiliPaiNavKey>().invoke(this)
}
