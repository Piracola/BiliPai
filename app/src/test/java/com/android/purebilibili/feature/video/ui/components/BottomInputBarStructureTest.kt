package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.calculateContrastRatio
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomInputBarStructureTest {

    @Test
    fun bottomInputBar_keepsSolidDockedPathWhenLiquidReuseOff() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("MaterialTheme.colorScheme.surface"))
        assertTrue(source.contains("MaterialTheme.colorScheme.surfaceContainerHighest"))
        assertTrue(source.contains("DockedSolidBottomInputBar("))
        assertTrue(!source.contains("surfaceVariant.copy(alpha = 0.65f)"))
        assertTrue(!source.contains("liquidGlassBackground"))
    }

    @Test
    fun bottomInputBar_reusesHomeFloatingLiquidDockWhenReuseEnabled() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/components/BottomInputBar.kt")
            .readText()

        assertTrue(source.contains("shouldUseFloatingLiquidBottomInputBar("))
        assertTrue(source.contains("resolveGlobalLiquidGlassReuseEnabled"))
        assertTrue(source.contains("FloatingLiquidBottomInputBar("))
        assertTrue(source.contains("FloatingLiquidBottomInputBarContentRow("))
        assertTrue(source.contains(".kernelSuFloatingDockSurface("))
        assertTrue(source.contains("drawShellLens = false"))
        assertTrue(source.contains("resolveSharedBottomBarCapsuleShape()"))
        assertTrue(source.contains("resolveAndroidNativeFloatingBottomBarContainerColor("))
        assertTrue(source.contains("backdrop: Backdrop? = null"))
        assertTrue(
            source.contains("Same liquid dock surface as home bottom-bar search capsule"),
            "Comment placeholder should reuse liquid dock surface, not a solid chip"
        )
    }

    @Test
    fun floatingLiquidGate_followsGlobalReuseMasterOnly() {
        assertTrue(shouldUseFloatingLiquidBottomInputBar(androidNativeLiquidGlassEnabled = true))
        assertFalse(shouldUseFloatingLiquidBottomInputBar(androidNativeLiquidGlassEnabled = false))
    }

    @Test
    fun contentBottomPadding_growsWhenFloatingLiquidChrome() {
        assertEquals(
            112.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = true,
                floatingLiquidGlass = true,
                showActionButtonsFallback = true
            )
        )
        assertEquals(
            96.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = true,
                floatingLiquidGlass = false,
                showActionButtonsFallback = true
            )
        )
        assertEquals(
            84.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = false,
                floatingLiquidGlass = true,
                showActionButtonsFallback = true
            )
        )
        assertEquals(
            12.dp,
            resolveBottomInputBarContentBottomPadding(
                showBar = false,
                floatingLiquidGlass = false,
                showActionButtonsFallback = false
            )
        )
    }

    @Test
    fun bottomInputBarPlaceholderTextKeepsReadableContrastInLightTheme() {
        val inputContainerColor = Color(0xFFE6E0E9)
        val textColor = resolveBottomInputBarPlaceholderTextColor(
            inputContainerColor = inputContainerColor,
            onSurfaceColor = Color(0xFF1D1B20),
            onSurfaceVariantColor = Color(0xFF49454F)
        )

        assertTrue(
            calculateContrastRatio(textColor, inputContainerColor) >=
                BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
        )
    }

    @Test
    fun bottomInputBarPlaceholderTextKeepsReadableContrastInDarkTheme() {
        val inputContainerColor = Color(0xFF36343B)
        val textColor = resolveBottomInputBarPlaceholderTextColor(
            inputContainerColor = inputContainerColor,
            onSurfaceColor = Color(0xFFE6E1E5),
            onSurfaceVariantColor = Color(0xFFCAC4D0)
        )

        assertTrue(
            calculateContrastRatio(textColor, inputContainerColor) >=
                BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
        )
    }
}
