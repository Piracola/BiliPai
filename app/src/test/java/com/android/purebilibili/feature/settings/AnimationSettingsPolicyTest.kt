package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AnimationSettingsPolicyTest {

    @Test
    fun liquidGlassPreviewUiState_usesContinuousCopy() {
        val clear = resolveLiquidGlassPreviewUiState(progress = 0.1f)
        val frosted = resolveLiquidGlassPreviewUiState(progress = 0.9f)

        assertEquals("通透", clear.modeLabel)
        assertTrue(clear.subtitle.contains("清晰"))
        assertEquals("磨砂", frosted.modeLabel)
        assertTrue(frosted.subtitle.contains("柔和"))
        assertNotEquals("平衡", clear.modeLabel)
        assertNotEquals("平衡", frosted.modeLabel)
    }

    @Test
    fun liquidGlassPreviewUiState_clampsAndFormatsProgress() {
        val state = resolveLiquidGlassPreviewUiState(progress = 1.4f)

        assertEquals(1f, state.normalizedProgress)
        assertEquals("100%", state.strengthLabel)
    }

    @Test
    fun bottomBarLiquidGlassUsesUnifiedMaterialWithoutPresetPicker() {
        val animationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"
        )
        val bottomBarSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/BottomBarSettingsScreen.kt"
        )

        assertFalse(animationSource.contains("当前底栏材质"))
        assertFalse(animationSource.contains("BottomBarLiquidGlassPreset"))
        assertFalse(animationSource.contains("getBottomBarLiquidGlassPreset"))
        assertFalse(animationSource.contains("底栏跟随高光"))
        assertFalse(animationSource.contains("getBottomBarInteractiveHighlightEnabled"))
        assertFalse(animationSource.contains("setBottomBarInteractiveHighlightEnabled"))
        assertFalse(bottomBarSource.contains("BottomBarLiquidGlassPreset"))
        assertFalse(bottomBarSource.contains("底栏跟随高光"))
    }

    @Test
    fun predictiveBackAnimationUiState_requiresGestureToggle() {
        val disabled = resolvePredictiveBackAnimationUiState(
            predictiveBackEnabled = false,
            styleStorageValue = "scale",
            exitDirectionStorageValue = "auto",
        )
        assertFalse(disabled.enabled)
        assertFalse(disabled.showExitDirection)
        assertTrue(disabled.subtitle.contains("预测性返回手势"))

        val scale = resolvePredictiveBackAnimationUiState(
            predictiveBackEnabled = true,
            styleStorageValue = "scale",
            exitDirectionStorageValue = "always_left",
        )
        assertTrue(scale.enabled)
        assertTrue(scale.showExitDirection)
        assertEquals("缩放", scale.selectedStyle.displayName)
        assertEquals("始终向左", scale.selectedExitDirection?.displayName)
    }

    @Test
    fun predictiveBackAnimationUiState_blocksAospWhenCardTransitionEnabled() {
        val blocked = resolvePredictiveBackAnimationUiState(
            predictiveBackEnabled = true,
            styleStorageValue = "aosp",
            exitDirectionStorageValue = "always_right",
            cardTransitionEnabled = true,
        )
        assertTrue(blocked.aospBlockedByCardTransition)
        assertEquals("缩放", blocked.selectedStyle.displayName)
        assertTrue(blocked.subtitle.contains("AOSP"))

        val options = resolvePredictiveBackStyleOptions(cardTransitionEnabled = true)
        assertFalse(options.any { it.displayName == "AOSP" })
        assertTrue(resolvePredictiveBackStyleOptions(cardTransitionEnabled = false).any {
            it.displayName == "AOSP"
        })
    }

    @Test
    fun predictiveBackAnimationEntry_isPresentInAnimationSettings() {
        val animationSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"
        )
        val policySource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/AnimationSettingsPolicy.kt"
        )

        assertTrue(animationSource.contains("PredictiveBackAnimationDialog"))
        assertTrue(animationSource.contains("SettingsIconRole.PREDICTIVE_BACK"))
        assertTrue(animationSource.contains("setPredictiveBackAnimationStyle"))
        assertTrue(policySource.contains("PredictiveBackAnimationUiState"))
        assertTrue(policySource.contains("PREDICTIVE_BACK_ANIMATION_TITLE"))
        assertTrue(policySource.contains("resolvePredictiveBackAnimationUiState"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
