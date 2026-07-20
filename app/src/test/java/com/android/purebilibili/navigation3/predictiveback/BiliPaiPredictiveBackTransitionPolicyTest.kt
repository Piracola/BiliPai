package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BiliPaiPredictiveBackTransitionPolicyTest {

    @Test
    fun aospAndScale_ownDecoratorLayer() {
        assertEquals(
            BiliPaiPredictiveBackTransitionOwnership.DECORATOR,
            resolveBiliPaiPredictiveBackTransitionOwnership(
                style = BiliPaiPredictiveBackAnimationStyle.AOSP,
                predictiveBackEnabled = true,
            ),
        )
        assertEquals(
            BiliPaiPredictiveBackTransitionOwnership.DECORATOR,
            resolveBiliPaiPredictiveBackTransitionOwnership(
                style = BiliPaiPredictiveBackAnimationStyle.SCALE,
                predictiveBackEnabled = true,
            ),
        )
    }

    @Test
    fun decoratorStyles_suppressSettingsIosEntryPopTransform() {
        val transform = resolveBiliPaiEntryPopContentTransformForPredictiveStyle(
            routeTransition = BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            style = BiliPaiPredictiveBackAnimationStyle.AOSP,
        )
        assertEquals(EnterTransition.None, transform.targetContentEnter)
        assertEquals(ExitTransition.None, transform.initialContentExit)
    }

    @Test
    fun miuixStyle_doesNotKeepSettingsIosParallaxOnPop() {
        val transform = resolveBiliPaiEntryPopContentTransformForPredictiveStyle(
            routeTransition = BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            style = BiliPaiPredictiveBackAnimationStyle.MIUIX,
        )
        // Miuix 对齐 InstallerX：不用设置树 iOS 视差，避免与 defaultPredictivePop 打架。
        assertTrue(transform.targetContentEnter != EnterTransition.None)
        assertTrue(transform.initialContentExit != ExitTransition.None)
    }

    @Test
    fun disabledPredictive_keepsSettingsIosPop() {
        val transform = resolveBiliPaiEntryPopContentTransformForPredictiveStyle(
            routeTransition = BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            style = BiliPaiPredictiveBackAnimationStyle.AOSP,
            predictiveBackEnabled = false,
        )
        // 关闭跟手后仍走路由语义（设置 iOS pop），不强制 None。
        assertTrue(transform.initialContentExit != ExitTransition.None)
    }

    @Test
    fun sharedElementPop_isNotOverriddenByDecoratorStyle() {
        val transform = resolveBiliPaiEntryPopContentTransformForPredictiveStyle(
            routeTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            style = BiliPaiPredictiveBackAnimationStyle.SCALE,
        )
        assertEquals(EnterTransition.None, transform.targetContentEnter)
        assertEquals(ExitTransition.None, transform.initialContentExit)
    }
}
