package com.android.purebilibili.core.ui.transition.native

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeVideoCardTransitionPolicyTest {

    private val sourceRect = NativeVideoTransitionRect(
        left = 24f,
        top = 120f,
        right = 224f,
        bottom = 240f
    )
    private val targetRect = NativeVideoTransitionRect(
        left = 0f,
        top = 88f,
        right = 360f,
        bottom = 290f
    )
    private val spec = NativeVideoCardTransitionSpec(
        sourceRect = sourceRect,
        targetRect = targetRect,
        sourceCornerRadiusPx = 18f,
        targetCornerRadiusPx = 4f,
        maxBlurRadiusPx = 28f
    )

    @Test
    fun progressInterpolatesSourceAndTargetRects() {
        val start = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )
        val middle = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0.5f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )
        val end = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 1f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )

        assertEquals(sourceRect, start.cardRect)
        assertEquals(NativeVideoTransitionRect(12f, 104f, 292f, 265f), middle.cardRect)
        assertEquals(targetRect, end.cardRect)
    }

    @Test
    fun progressInterpolatesCornerRadius() {
        val start = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )
        val middle = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0.5f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )
        val end = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 1f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )

        assertEquals(18f, start.cornerRadiusPx)
        assertEquals(11f, middle.cornerRadiusPx)
        assertEquals(4f, end.cornerRadiusPx)
    }

    @Test
    fun api31OpeningBlurBuildsUpAndStaysForNavigationHandoff() {
        val start = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 31
        )
        val middle = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0.5f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 31
        )
        val end = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 1f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 31
        )

        assertEquals(0f, start.blurRadiusPx)
        assertTrue(middle.blurRadiusPx > start.blurRadiusPx)
        assertEquals(28f, end.blurRadiusPx, 0.0001f)
        assertTrue(middle.scrimAlpha > start.scrimAlpha)
        assertTrue(middle.contentScale < start.contentScale)
    }

    @Test
    fun api31ClosingBlurClearsAsCardReturnsToSource() {
        val start = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0f,
            phase = NativeVideoCardTransitionPhase.Closing,
            sdkInt = 31
        )
        val middle = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0.5f,
            phase = NativeVideoCardTransitionPhase.Closing,
            sdkInt = 31
        )
        val end = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 1f,
            phase = NativeVideoCardTransitionPhase.Closing,
            sdkInt = 31
        )

        assertEquals(28f, start.blurRadiusPx, 0.0001f)
        assertTrue(middle.blurRadiusPx < start.blurRadiusPx)
        assertEquals(0f, end.blurRadiusPx, 0.0001f)
    }

    @Test
    fun api30KeepsBlurDisabledButStillAppliesScrimAndScale() {
        val middle = resolveNativeVideoCardTransitionFrame(
            spec = spec,
            progress = 0.5f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 30
        )

        assertEquals(0f, middle.blurRadiusPx)
        assertTrue(middle.scrimAlpha > 0f)
        assertTrue(middle.contentScale < 1f)
    }

    @Test
    fun defaultMidProgressCreatesReferenceLikeBackgroundCompression() {
        val middle = resolveNativeVideoCardTransitionFrame(
            spec = NativeVideoCardTransitionSpec(
                sourceRect = sourceRect,
                targetRect = targetRect,
                sourceCornerRadiusPx = 18f,
                targetCornerRadiusPx = 4f
            ),
            progress = 0.5f,
            phase = NativeVideoCardTransitionPhase.Opening,
            sdkInt = 35
        )

        assertTrue(middle.blurRadiusPx >= 44f)
        assertTrue(middle.scrimAlpha >= 0.3f)
        assertTrue(middle.contentScale <= 0.94f)
    }
}
