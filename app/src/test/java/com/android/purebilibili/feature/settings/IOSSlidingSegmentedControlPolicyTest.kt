package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IOSSlidingSegmentedControlPolicyTest {

    @Test
    fun defaultSettingsIndicatorRenderPolicyMatchesCommentSortIndicator() {
        val policy = resolveIosSlidingSegmentedControlRenderPolicy(
            itemCount = 2,
            hasExternalBackdrop = false
        )

        assertEquals(66, policy.itemWidthDp)
        assertEquals(44, policy.heightDp)
        assertEquals(30, policy.indicatorHeightDp)
        assertEquals(15, policy.labelFontSizeSp)
        assertFalse(policy.liquidGlassEffectsEnabled)
        assertFalse(policy.tapPressRefractionEnabled)
    }

    @Test
    fun denseSegmentLabelsShrinkFontAndPreferFillMaxWidth() {
        assertEquals(
            12f,
            resolveSlidingSegmentedLabelFontSizeSp(
                optionCount = 4,
                longestLabelLength = 4,
            )
        )
        assertEquals(
            13f,
            resolveSlidingSegmentedLabelFontSizeSp(
                optionCount = 3,
                longestLabelLength = 4,
            )
        )
        assertTrue(
            shouldFillMaxWidthSlidingSegmentedControl(
                optionCount = 4,
                longestLabelLength = 4,
            )
        )
        val densePolicy = resolveIosSlidingSegmentedControlRenderPolicy(
            itemCount = 4,
            hasExternalBackdrop = false,
            longestLabelLength = 4,
        )
        assertEquals(12, densePolicy.labelFontSizeSp)
    }

    @Test
    fun iosSlidingSegmentedControlImplAppliesRenderPolicyToBottomBarControl() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/IOSSlidingSegmentedControl.kt"
        )

        assertTrue(source.contains("itemWidth = resolvedItemWidth"))
        assertTrue(source.contains("val resolvedItemWidth = null"))
        assertTrue(source.contains("modifier = modifier.fillMaxWidth()"))
        assertTrue(source.contains("height = resolvedHeight"))
        assertTrue(source.contains("indicatorHeight = resolvedIndicatorHeight"))
        assertTrue(source.contains("labelFontSize = resolvedLabelFontSize"))
        assertTrue(source.contains("liquidGlassEffectsEnabled = renderPolicy.liquidGlassEffectsEnabled"))
        assertTrue(source.contains("tapPressRefractionEnabled = resolvedTapPressRefractionEnabled"))
        assertTrue(source.contains("indicatorIdleSurfaceColorOverride = indicatorIdleSurfaceColorOverride"))
        assertTrue(source.contains("containerColorOverride = containerColorOverride"))
    }

    @Test
    fun forceLiquidIndicatorWithoutExternalBackdropKeepsRequestDelegated() {
        val request = resolveIosSlidingSegmentedLiquidGlassRequest(
            forceLiquidIndicator = true,
            hasExternalBackdrop = false
        )

        assertNull(request)
    }

    @Test
    fun forceLiquidIndicatorWithExternalBackdropRequestsLiquidGlass() {
        val request = resolveIosSlidingSegmentedLiquidGlassRequest(
            forceLiquidIndicator = true,
            hasExternalBackdrop = true
        )

        assertEquals(true, request)
    }

    @Test
    fun inactiveForceLiquidIndicatorKeepsRequestDelegated() {
        val request = resolveIosSlidingSegmentedLiquidGlassRequest(
            forceLiquidIndicator = false,
            hasExternalBackdrop = true
        )

        assertNull(request)
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
