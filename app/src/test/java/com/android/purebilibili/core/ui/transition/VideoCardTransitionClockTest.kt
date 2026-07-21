package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardTransitionClockTest {

    @Test
    fun depthPriority_gestureBeatsSharedBeatsFallback() {
        assertEquals(
            0.4f,
            resolveVideoCardClockDepthProgress(
                gestureBackProgress = 0.6f,
                gestureStartDepth = 1f,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                sharedMorphActive = true,
                sharedMorphFraction = 0.9f,
                fallbackProgress = 0.2f,
            ),
            0.001f,
        )
        assertEquals(
            0.75f,
            resolveVideoCardClockDepthProgress(
                gestureBackProgress = null,
                gestureStartDepth = 1f,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                sharedMorphActive = true,
                sharedMorphFraction = 0.75f,
                fallbackProgress = 0.1f,
            ),
            0.001f,
        )
        assertEquals(
            0.33f,
            resolveVideoCardClockDepthProgress(
                gestureBackProgress = null,
                gestureStartDepth = 1f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                sharedMorphActive = false,
                sharedMorphFraction = 0.9f,
                fallbackProgress = 0.33f,
            ),
            0.001f,
        )
    }

    @Test
    fun preferSharedOnlyWhenActiveAndHasFraction() {
        assertTrue(
            shouldPreferSharedMorphProgress(
                sharedMorphActive = true,
                hasSharedFraction = true,
                gestureActive = false,
            ),
        )
        assertFalse(
            shouldPreferSharedMorphProgress(
                sharedMorphActive = true,
                hasSharedFraction = true,
                gestureActive = true,
            ),
        )
        assertFalse(
            shouldPreferSharedMorphProgress(
                sharedMorphActive = false,
                hasSharedFraction = true,
                gestureActive = false,
            ),
        )
    }

    @Test
    fun morphFractionToSettle_isOneMinusMorph() {
        assertEquals(0f, morphFractionToReturnSettle(1f), 0.0001f)
        assertEquals(1f, morphFractionToReturnSettle(0f), 0.0001f)
        assertEquals(0.3f, morphFractionToReturnSettle(0.7f), 0.0001f)
    }

    @Test
    fun fallbackDuration_scalesWithDepthSpan() {
        assertEquals(
            360,
            resolveMorphAlignedFallbackDurationMs(
                timelineDurationMs = 360,
                startDepth = 1f,
                targetDepth = 0f,
            ),
        )
        assertEquals(
            180,
            resolveMorphAlignedFallbackDurationMs(
                timelineDurationMs = 360,
                startDepth = 0.5f,
                targetDepth = 0f,
            ),
        )
        assertEquals(
            0,
            resolveMorphAlignedFallbackDurationMs(
                timelineDurationMs = 360,
                startDepth = 0f,
                targetDepth = 0f,
            ),
        )
    }

    @Test
    fun clock_reportSharedMorph_drivesDepthWhileActive() {
        val clock = VideoCardTransitionClock()
        clock.beginOpening("home")
        clock.reportSharedMorphProgress(morphFraction = 0.4f, active = true)
        assertEquals(0.4f, clock.depthProgress(), 0.0001f)
        clock.reportSharedMorphProgress(morphFraction = 1f, active = false)
        assertEquals(VideoCardTransitionBackgroundPhase.HELD, clock.phase)
    }

    @Test
    fun timelineSpec_returnIsLinearEnterIsContinuity() {
        val spec = resolveVideoCardTimelineSpec(360)
        assertEquals(360, spec.durationMillis)
        assertEquals(0.5f, spec.returnEasing.transform(0.5f), 0.001f)
        // Continuity at 0.5 is not 0.5
        assertTrue(spec.enterEasing.transform(0.5f) > 0.5f)
    }
}
