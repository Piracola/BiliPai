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
