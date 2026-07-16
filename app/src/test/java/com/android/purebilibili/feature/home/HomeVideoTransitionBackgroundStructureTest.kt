package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeVideoTransitionBackgroundStructureTest {

    @Test
    fun homeRootNoLongerOwnsVideoTransitionBackgroundBlurAndScrim() {
        val source = homeScreenSource()

        assertFalse(source.contains("homeVideoTransitionBackgroundProgress"))
        assertFalse(source.contains("HomeVideoTransitionBackgroundPhase"))
        assertFalse(source.contains("homeVideoTransitionBackgroundEffect"))
    }

    @Test
    fun navigationHostOnlyProvidesVideoTransitionBackgroundState() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("VideoCardTransitionBackgroundPhase.OPENING"))
        assertTrue(source.contains("VideoCardTransitionBackgroundPhase.RETURNING"))
        assertTrue(source.contains("LocalVideoCardTransitionBackgroundState provides"))
        assertFalse(source.contains("videoCardTransitionBackgroundEffect("))
    }

    @Test
    fun appNavigationAppliesVideoTransitionBackgroundToRealRouteContent() {
        val source = appNavigationSource()

        assertTrue(source.contains("VideoCardTransitionBackgroundRouteContent("))
        assertTrue(source.contains("videoCardTransitionBackgroundEffect("))
        assertTrue(source.contains("shouldApplyVideoCardTransitionBackgroundToRoute("))
        assertTrue(source.contains("RenderNavigationContent(key)"))
        assertTrue(source.contains("sourceRoute = backgroundState.sourceRouteProvider()"))
        assertFalse(source.contains("val predictiveBlurProgress = predictiveBackState.progressProvider()"))
    }

    @Test
    fun videoCardTransitionBackgroundUsesFrozenSnapshotLayerForDynamicBlur() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionBackgroundPolicy.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionBackgroundPolicy.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("rememberGraphicsLayer()"))
        assertTrue(source.contains("shouldUseVideoCardTransitionSnapshotBlur"))
        assertTrue(source.contains("freezeRecording"))
        assertTrue(source.contains("contentLayer.record"))
        assertTrue(source.contains("BlurEffect("))
        // 冻结后不得每帧对 live content 再挂 android RenderEffect
        assertFalse(source.contains("android.graphics.RenderEffect"))
        assertFalse(source.contains("createBlurEffect("))
    }

    private fun homeScreenSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        ).first { it.exists() }.readText()
    }

    private fun navDisplayHostSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
        ).first { it.exists() }.readText()
    }

    private fun appNavigationSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }.readText()
    }
}
