package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationAppearancePolicyTest {

    @Test
    fun mapsBottomBarAndTransitionFlagsFromHomeSettings() {
        val appearance = resolveAppNavigationAppearance(
            HomeSettings(
                isBottomBarFloating = false,
                bottomBarLabelMode = 2,
                isBottomBarBlurEnabled = false,
                cardTransitionEnabled = false
            )
        )

        assertFalse(appearance.cardTransitionEnabled)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(2, appearance.bottomBarLabelMode)
        assertFalse(appearance.bottomBarFloating)
    }

    @Test
    fun keepsDefaultsWithoutRemovedBackPreviewAppearanceState() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertTrue(appearance.cardTransitionEnabled)
        assertTrue(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(0, appearance.bottomBarLabelMode)
        assertTrue(appearance.bottomBarFloating)
    }

    @Test
    fun md3Preset_keepsFloatingBottomBarWhenShellSettingsAreStillDefault() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(),
            uiPreset = UiPreset.MD3
        )

        assertTrue(appearance.bottomBarFloating)
        assertTrue(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(0, appearance.bottomBarLabelMode)
    }

    @Test
    fun md3Material3_keepsDockedBottomBarBlurWhenLiquidGlassIsDisabled() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(
                isBottomBarFloating = false,
                isBottomBarBlurEnabled = true,
                androidNativeLiquidGlassEnabled = false,
            ),
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MATERIAL3,
        )

        assertFalse(appearance.bottomBarFloating)
        assertTrue(appearance.bottomBarBlurEnabled)
    }

    @Test
    fun iosPreset_keepsBottomBarBlurWithDefaultHomeSettings() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(),
            uiPreset = UiPreset.IOS
        )

        assertTrue(appearance.bottomBarBlurEnabled)
    }

    @Test
    fun md3Preset_preservesExplicitBottomBarShellCustomization() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(
                isBottomBarFloating = true,
                bottomBarLabelMode = 1,
                isBottomBarBlurEnabled = false
            ),
            uiPreset = UiPreset.MD3
        )

        assertTrue(appearance.bottomBarFloating)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(1, appearance.bottomBarLabelMode)
    }

    @Test
    fun md3MiuixPreset_keepsFloatingBottomBarWhenShellSettingsAreDefault() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(),
            uiPreset = UiPreset.MD3,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )

        assertTrue(appearance.bottomBarFloating)
        assertTrue(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(0, appearance.bottomBarLabelMode)
    }

    @Test
    fun bottomBarBackdropCapturesGlobalWallpaperBeforeNavDisplayContent() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        // Dock liquid glass samples Miuix LayerBackdrop (InstallerX-aligned), not Kyant.
        val capturedLayerSource = source
            .substringAfter(".miuixLayerBackdrop(bottomBarBackdrop)")
            .substringBefore("// ===== 全局底栏")

        val wallpaperIndex = capturedLayerSource.indexOf("HomeWallpaperBackdrop(")
        val navDisplayIndex = capturedLayerSource.indexOf("BiliPaiNavDisplayHost(")

        assertTrue(wallpaperIndex >= 0)
        assertTrue(navDisplayIndex > wallpaperIndex)
        assertTrue(capturedLayerSource.contains(".then(if (mainHazeState != null) Modifier.hazeSourceCompat(mainHazeState) else Modifier)"))
    }

    @Test
    fun appNavigationPassesSkinDecorationAsReadOnlyBottomBarInput() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("val uiSkinState by rememberUiSkinState(context)"))
        assertTrue(source.contains("val bottomBarUiSkinDecoration = rememberBottomBarUiSkinDecoration(uiSkinState)"))
        assertTrue(source.contains("uiSkinDecoration = bottomBarUiSkinDecoration"))
        assertFalse(source.contains("uiSkinState.copy("))
        assertFalse(source.contains("bottomBarLiquidGlassPreset = uiSkin"))
        assertFalse(source.contains("isBottomBarLiquidGlassEnabled = uiSkin"))
    }

    @Test
    fun appNavigationProvidesGlobalSharedTransitionSwitch() {
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val providerSource = loadSource("app/src/main/java/com/android/purebilibili/core/ui/SharedTransitionProvider.kt")
        val activitySource = loadSource("app/src/main/java/com/android/purebilibili/MainActivity.kt")

        assertTrue(navigationSource.contains("SharedTransitionProvider(enabled = cardTransitionEnabled)"))
        assertTrue(providerSource.contains("val sharedTransitionScope = if (enabled) this else null"))
        assertTrue(providerSource.contains("LocalSharedTransitionScope provides sharedTransitionScope"))
        assertTrue(providerSource.contains("LocalSharedTransitionEnabled provides enabled"))
        assertFalse(activitySource.contains("SharedTransitionProvider"))
    }

    @Test
    fun appNavigationRemovesVideoTransitionRealtimeBlurRuntimePath() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertFalse(source.contains("videoTransitionRealtimeBlurEnabled"))
        assertFalse(source.contains("video_source_background_blur"))
        assertFalse(source.contains("RenderEffect.createBlurEffect"))
    }

    @Test
    fun appNavigationAppearanceDoesNotExposeRemovedBackPreviewState() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigationAppearancePolicy.kt")

        assertFalse(source.contains("Predictive" + "BackAnimationStyle"))
        assertFalse(source.contains("predictive" + "BackAnimationStyle"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
