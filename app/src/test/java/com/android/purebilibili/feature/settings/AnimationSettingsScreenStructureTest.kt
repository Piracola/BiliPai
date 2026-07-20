package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSettingsScreenStructureTest {

    @Test
    fun animationSettingsScreen_controlsGlobalPredictivePreviewIndependently() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"预测性返回手势\""))
        assertTrue(source.contains("SettingsManager.setPredictiveBackEnabled(context, enabled)"))
        val predictiveItem = source
            .substringAfter("title = \"预测性返回手势\"")
            .substringBefore("IOSDivider()")
        assertFalse(predictiveItem.contains("enabled = state.cardTransitionEnabled"))
    }

    @Test
    fun animationSettingsScreen_exposesInstallerAlignedPredictiveStylePicker() {
        val source = animationSettingsSource()

        assertTrue(source.contains("PREDICTIVE_BACK_EXIT_DIRECTION_TITLE") || source.contains("退出方向"))
        assertTrue(source.contains("setPredictiveBackAnimationStyle"))
        assertTrue(source.contains("setPredictiveBackExitDirection"))
        assertTrue(source.contains("resolvePredictiveBackStyleOptions"))
        assertTrue(source.contains("resolvePredictiveBackExitDirectionOptions"))
        assertTrue(source.contains("PredictiveBackAnimationDialog"))
        assertTrue(source.contains("showExitDirection"))
        assertTrue(source.contains("cardTransitionEnabled = state.cardTransitionEnabled"))
        assertTrue(source.contains("过渡动画开启时 AOSP 不可用") || source.contains("不可使用 AOSP"))
    }

    @Test
    fun animationSettingsScreen_exposesRealtimeTransitionBlurToggle() {
        val source = animationSettingsSource()

        assertTrue(source.contains("title = \"过渡动画实时模糊\""))
        assertTrue(source.contains("checked = videoTransitionRealtimeBlurEnabled"))
        assertTrue(source.contains("toggleVideoTransitionRealtimeBlur"))
    }

    private fun animationSettingsSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt"),
        ).first { it.exists() }.readText()
    }
}
