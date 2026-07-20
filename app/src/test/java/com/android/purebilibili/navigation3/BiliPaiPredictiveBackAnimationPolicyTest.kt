package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.BiliPaiAospPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiClassicPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiDisabledPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiMiuixPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiScalePredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.BiliPaiSharedElementPredictiveBackAnimation
import com.android.purebilibili.navigation3.predictiveback.resolveBiliPaiPredictiveBackAnimationHandler
import com.android.purebilibili.navigation3.predictiveback.resolveEffectivePredictiveBackAnimationStyle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiPredictiveBackAnimationPolicyTest {

    @Test
    fun effectiveStyle_blocksAospWhenCardTransitionEnabled() {
        assertEquals(
            BiliPaiPredictiveBackAnimationStyle.SCALE,
            resolveEffectivePredictiveBackAnimationStyle(
                style = BiliPaiPredictiveBackAnimationStyle.AOSP,
                cardTransitionEnabled = true,
            ),
        )
        assertEquals(
            BiliPaiPredictiveBackAnimationStyle.AOSP,
            resolveEffectivePredictiveBackAnimationStyle(
                style = BiliPaiPredictiveBackAnimationStyle.AOSP,
                cardTransitionEnabled = false,
            ),
        )
    }

    @Test
    fun cardTransitionEnabled_forcesAospHandlerToScale() {
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.FALLBACK,
                style = BiliPaiPredictiveBackAnimationStyle.AOSP,
                cardTransitionEnabled = true,
            ) is BiliPaiScalePredictiveBackAnimation
        )
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.FALLBACK,
                style = BiliPaiPredictiveBackAnimationStyle.AOSP,
                cardTransitionEnabled = false,
            ) is BiliPaiAospPredictiveBackAnimation
        )
    }

    @Test
    fun noneStyle_disablesPredictivePreviewHandler() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.FALLBACK,
            style = BiliPaiPredictiveBackAnimationStyle.NONE,
        )
        assertTrue(handler is BiliPaiDisabledPredictiveBackAnimation)
    }

    @Test
    fun styleSelection_resolvesInstallerAlignedHandlers() {
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.FALLBACK,
                style = BiliPaiPredictiveBackAnimationStyle.AOSP,
            ) is BiliPaiAospPredictiveBackAnimation
        )
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.FALLBACK,
                style = BiliPaiPredictiveBackAnimationStyle.MIUIX,
            ) is BiliPaiMiuixPredictiveBackAnimation
        )
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.FALLBACK,
                style = BiliPaiPredictiveBackAnimationStyle.SCALE,
            ) is BiliPaiScalePredictiveBackAnimation
        )
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.FALLBACK,
                style = BiliPaiPredictiveBackAnimationStyle.CLASSIC,
            ) is BiliPaiClassicPredictiveBackAnimation
        )
    }

    @Test
    fun settingsRoute_followsSelectedStyleInsteadOfForcedIosHandler() {
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
                style = BiliPaiPredictiveBackAnimationStyle.SCALE,
            ) is BiliPaiScalePredictiveBackAnimation
        )
        assertTrue(
            resolveBiliPaiPredictiveBackAnimationHandler(
                routeTransition = BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
                style = BiliPaiPredictiveBackAnimationStyle.MIUIX,
            ) is BiliPaiMiuixPredictiveBackAnimation
        )
    }

    @Test
    fun displayPop_settingsToMainHostWithSettingsTab_usesSettingsIos() {
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            resolveBiliPaiNavDisplayPopRouteTransition(
                cardTransitionEnabled = true,
                sourceMetadata = BiliPaiNavSourceMetadata(),
                fromKey = BiliPaiNavKey.AppearanceSettings,
                toKey = BiliPaiNavKey.MainHost,
                activeMainHostRoute = "settings",
            )
        )
        assertEquals(
            BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP,
            resolveBiliPaiNavDisplayPopRouteTransition(
                cardTransitionEnabled = true,
                sourceMetadata = BiliPaiNavSourceMetadata(),
                fromKey = BiliPaiNavKey.BottomBarSettings,
                toKey = BiliPaiNavKey.MainHost,
                activeMainHostRoute = "settings",
            )
        )
    }

    @Test
    fun sharedElementRoute_usesSharedElementHandler() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT,
            style = BiliPaiPredictiveBackAnimationStyle.AOSP,
        )
        assertTrue(handler is BiliPaiSharedElementPredictiveBackAnimation)
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
    fun aospAndScale_disableRouteLayerPredictiveTransform() {
        val aosp = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiAospPredictiveBackAnimation.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiAospPredictiveBackAnimation.kt"),
        ).first { it.exists() }.readText()
        val scale = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiScalePredictiveBackAnimation.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiScalePredictiveBackAnimation.kt"),
        ).first { it.exists() }.readText()
        assertTrue(aosp.contains("targetContentEnter = EnterTransition.None"))
        assertTrue(aosp.contains("initialContentExit = ExitTransition.None"))
        assertTrue(scale.contains("targetContentEnter = EnterTransition.None"))
        assertTrue(scale.contains("initialContentExit = ExitTransition.None"))
    }

    @Test
    fun classic_usesSameTransformForPredictiveAndPop() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiClassicPredictiveBackAnimation.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiClassicPredictiveBackAnimation.kt"),
        ).first { it.exists() }.readText()
        assertTrue(source.contains("classicPopTransform()"))
        assertTrue(source.contains("scaleOut(targetScale = 0.9f)"))
    }

    @Test
    fun miuix_usesDefaultPredictivePopTransitionSpec() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixPredictiveBackAnimation.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixPredictiveBackAnimation.kt"),
        ).first { it.exists() }.readText()
        assertTrue(source.contains("defaultPredictivePopTransitionSpec"))
    }

    @Test
    fun disabledPreference_suppressesGlobalPredictivePreview() {
        val handler = resolveBiliPaiPredictiveBackAnimationHandler(
            routeTransition = BiliPaiNavRouteTransition.CLASSIC_CARD,
            predictiveBackEnabled = false,
            style = BiliPaiPredictiveBackAnimationStyle.SCALE,
        )
        assertTrue(handler is BiliPaiDisabledPredictiveBackAnimation)
    }

    @Test
    fun styleStorage_acceptsLegacyClassicAlias() {
        assertEquals(
            BiliPaiPredictiveBackAnimationStyle.CLASSIC,
            BiliPaiPredictiveBackAnimationStyle.fromStorageValue("classic"),
        )
        assertEquals(
            BiliPaiPredictiveBackAnimationStyle.MIUIX,
            BiliPaiPredictiveBackAnimationStyle.fromStorageValue("default"),
        )
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
