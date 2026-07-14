package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiDefaultPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiDisabledPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiSharedElementPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackAnimationHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiPredictiveBackAnimationPolicyTest {

    @Test
    fun sharedElementRoute_usesSharedElementHandler() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            style = BiliPaiPredictiveBackAnimationStyle.AOSP,
        )
        assertTrue(handler is BiliPaiSharedElementPredictiveBackAnimation)
    }

    @Test
    fun relatedDetailRoute_usesNavigationDefaultHandler() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.FALLBACK,
        )

        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun sharedElementPredictivePop_keepsRouteLayerStillForCardReturn() {
        val function = sharedElementPredictivePopFunction()

        assertFalse(function.contains("slideOutHorizontally"))
        assertFalse(function.contains("slideInHorizontally"))
        assertTrue(function.contains("noOpSharedElementContentTransform()"))
        assertFalse(function.contains("initialContentExit = fadeOut("))
    }

    @Test
    fun classicCardRoute_usesNavigationDefaults() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun classicCardRoute_defaultStyle_delegatesToNavEngineDefaults() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            style = BiliPaiPredictiveBackAnimationStyle.DEFAULT,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun legacyScaleStyle_doesNotOverrideNavigationDefaults() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            style = BiliPaiPredictiveBackAnimationStyle.SCALE,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun legacyAospStyle_doesNotOverrideNavigationDefaults() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            style = BiliPaiPredictiveBackAnimationStyle.AOSP,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun legacyClassicStyle_doesNotOverrideNavigationDefaults() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            style = BiliPaiPredictiveBackAnimationStyle.CLASSIC,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun disabledPreference_suppressesGlobalPredictivePreview() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            predictiveBackEnabled = false,
        )
        assertTrue(handler is BiliPaiDisabledPredictiveBackAnimation)
    }

    @Test
    fun disabledSharedElementRoute_suppressesPredictivePreviewBeforeRoutePolicy() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            predictiveBackEnabled = false,
        )
        assertTrue(handler is BiliPaiDisabledPredictiveBackAnimation)
    }

    @Test
    fun cardWithoutSharedSource_usesNavigationDefaultFallback() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun cardWithoutSharedSourceIgnoresLegacyDirection() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun fallbackRoute_usesDefaultHandler() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.FALLBACK,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    private fun sharedElementPredictivePopFunction(): String {
        val source = sharedElementPredictiveBackSource()
        val functionStart = source.indexOf("override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPredictivePopTransitionSpec")
        val functionEnd = source.indexOf("override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec")
        return source.substring(functionStart, functionEnd)
    }

    private fun sharedElementPredictiveBackSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiSharedElementPredictiveBackAnimation.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiSharedElementPredictiveBackAnimation.kt")
        ).first { it.exists() }.readText()
    }

}
