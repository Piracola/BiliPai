package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiDefaultPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiDisabledPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiSharedElementPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackAnimationHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun classicCardRoute_doesNotInstallTargetSpecificPredictiveHandler() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
        )

        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun targetSpecificPredictiveHandler_isNotPresent() {
        val sourceRoot = listOf(File("app/src/main"), File("src/main")).first { it.exists() }
        assertFalse(sourceRoot.walkTopDown().any { it.name == "BiliPaiVideoDetailTargetPredictiveBackAnimation.kt" })
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
    fun sharedElementPredictivePop_pinsUnderlyingSourcePageAgainstHorizontalDrift() {
        val source = sharedElementPredictiveBackSource()
        assertTrue(source.contains("skipToLookaheadPosition()"))
        assertTrue(source.contains("isUnderlyingSourcePage"))
        assertTrue(source.contains("translationX = 0f"))
    }

    @Test
    fun classicCardRoute_usesNavigationDefaults() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
        )
        assertTrue(handler is BiliPaiDefaultPredictiveBackAnimation)
    }

    @Test
    fun defaultPredictivePop_keepsTargetPageFullScreen() {
        val source = defaultPredictiveBackSource()
        val function = source.substringAfter(
            "override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPredictivePopTransitionSpec"
        ).substringBefore(
            "override fun AnimatedContentTransitionScope<Scene<BiliPaiNavKey>>.onPopTransitionSpec"
        )

        assertTrue(function.contains("targetContentEnter = EnterTransition.None"))
        assertTrue(function.contains("initialContentExit = slideOutHorizontally"))
        assertFalse(function.contains("defaultPredictivePopTransitionSpec"))
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
    fun disabledPredictivePreview_keepsRelatedDetailSharedElementPop() {
        val from = BiliPaiNavKey.VideoDetail("BV_B", sourceRoute = "video/BV_A")
        val to = BiliPaiNavKey.VideoDetail("BV_A")

        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
                predictiveBackEnabled = false,
            ) is BiliPaiDisabledPredictiveBackAnimation
        )
        assertEquals(
            BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            resolveBiliPaiNavDisplayPopRouteTransition(
                cardTransitionEnabled = true,
                sourceMetadata = BiliPaiNavSourceMetadata(),
                fromKey = from,
                toKey = to,
            )
        )
        assertEquals(
            BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            resolveBiliPaiNavEntryPopRouteTransition(
                defaultTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
                fromRoute = from.routeBase,
                toRoute = to.routeBase,
                cardTransitionEnabled = true,
                sharedElementPopReady = true,
                sourceMetadata = BiliPaiNavSourceMetadata(),
            )
        )
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

    private fun defaultPredictiveBackSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiDefaultPredictiveBackAnimation.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiDefaultPredictiveBackAnimation.kt")
        ).first { it.exists() }.readText()
    }

}
