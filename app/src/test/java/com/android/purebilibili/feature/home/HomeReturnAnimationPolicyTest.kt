package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeReturnAnimationPolicyTest {

    @Test
    fun quickReturn_withTransition_usesSharedElementSoftLandingSuppressionOnPhone() {
        assertEquals(
            580L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
            )
        )
    }

    @Test
    fun quickReturn_withTransition_usesSharedElementSoftLandingSuppressionOnTablet() {
        assertEquals(
            620L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
            )
        )
    }

    @Test
    fun normalReturn_usesUnifiedSharedTransitionDuration() {
        assertEquals(
            580L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
            )
        )
        assertEquals(
            220L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = false,
                cardTransitionEnabled = false,
            )
        )
    }

    @Test
    fun sharedReturnSuppressionUsesTheConfiguredTransitionDuration() {
        // 520 + 40 buffer + 180 spring settle
        assertEquals(
            740L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = true,
                sharedTransitionDurationMillis = 520,
            )
        )
    }

    @Test
    fun nonSharedReturn_usesShorterSuppressionDurations() {
        assertEquals(
            240L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = false,
                cardAnimationEnabled = true,
                cardTransitionEnabled = false,
            )
        )
        assertEquals(
            220L,
            resolveReturnAnimationSuppressionDurationMs(
                isTabletLayout = true,
                cardAnimationEnabled = true,
                cardTransitionEnabled = false,
            )
        )
    }

    @Test
    fun contentInteractionRestore_doesNotWaitForSharedElementSuppression() {
        assertEquals(
            0L,
            resolveHomeContentInteractionRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = false
            )
        )
        assertEquals(
            0L,
            resolveHomeContentInteractionRestoreDelayMs(
                cardTransitionEnabled = true,
                isQuickReturnFromDetail = true
            )
        )
        assertEquals(
            0L,
            resolveHomeContentInteractionRestoreDelayMs(
                cardTransitionEnabled = false,
                isQuickReturnFromDetail = false
            )
        )
    }

}
