package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.navigationevent.NavigationEventTransitionState
import com.android.purebilibili.core.ui.motion.resolveSettingsIosPushPopContentTransform
import com.android.purebilibili.navigation3.BiliPaiNavKey

/**
 * 设置树预测式返回：手势预览与松手提交共用 iOS push/pop，
 * 避免默认 550ms 全宽滑出与 350ms 视差 pop 在末尾切换造成卡顿。
 */
internal class BiliPaiSettingsIosPredictiveBackAnimation : BiliPaiPredictiveBackAnimationHandler {
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
    ): ContentTransform = resolveSettingsIosPushPopContentTransform()

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec(): ContentTransform =
        resolveSettingsIosPushPopContentTransform()

    override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onTransitionSpec(): ContentTransform =
        defaultTransitionSpec<BiliPaiNavKey>().invoke(this)
}
