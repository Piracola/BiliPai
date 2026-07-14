package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailRouteSheetPolicyTest {

    @Test
    fun cardReturnSecondaryContent_fadesLateWhilePlayerRemainsVisible() {
        val timing = resolveVideoDetailCardReturnSecondaryContentTiming(fullDurationMillis = 400)

        assertEquals(232, timing.delayMillis)
        assertEquals(128, timing.durationMillis)
    }

    @Test
    fun cardReturnTargetSourcesEnableRouteSheetMotion() {
        listOf(
            "home",
            "dynamic",
            "search",
            "watch_later",
            "dynamic_detail/123",
            "space/42",
            "category/1",
            "season_series_detail/favorite_season/1324105"
        ).forEach { route ->
            val motion = resolveVideoDetailRouteSheetMotion(
                sourceRoute = route,
                transitionEnabled = true
            )

            assertTrue(motion.enabled, "expected route sheet motion for $route")
            assertEquals(416, motion.durationMillis)
            assertEquals(320, motion.mainDurationMillis)
            assertEquals(96, motion.settleDurationMillis)
            assertEquals(0.965f, motion.initialScale)
            assertEquals(28f, motion.initialCornerDp)
            assertTrue(motion.settleScaleDelta in 0f..0.002f)
            assertTrue(motion.settleTranslationDp in 0f..2f)
            assertTrue(motion.easing.transform(0.35f) > 0.7f)
            assertTrue(motion.easing.transform(0.75f) > 0.96f)
        }
    }

    @Test
    fun nonCardSourceDoesNotUseRouteSheetMotion() {
        assertFalse(
            resolveVideoDetailRouteSheetMotion(
                sourceRoute = "settings",
                transitionEnabled = true
            ).enabled
        )
        assertFalse(
            resolveVideoDetailRouteSheetMotion(
                sourceRoute = "video",
                transitionEnabled = true
            ).enabled
        )
        assertFalse(
            resolveVideoDetailRouteSheetMotion(
                sourceRoute = "home",
                transitionEnabled = false
            ).enabled
        )
    }

    @Test
    fun routeSheetFrameStartsAsRoundedFloatingPanelAndEndsFullscreen() {
        val motion = resolveVideoDetailRouteSheetMotion(
            sourceRoute = "home",
            transitionEnabled = true
        )
        val start = resolveVideoDetailRouteSheetFrame(
            rawProgress = 0f,
            settleProgress = 0f,
            settleDirection = VideoDetailRouteSheetSettleDirection.None,
            motion = motion
        )
        val end = resolveVideoDetailRouteSheetFrame(
            rawProgress = 1f,
            settleProgress = 0f,
            settleDirection = VideoDetailRouteSheetSettleDirection.None,
            motion = motion
        )

        assertEquals(0.965f, start.scale)
        assertEquals(56f, start.translationYDp)
        assertEquals(28f, start.cornerDp)
        assertEquals(0.18f, start.backgroundScrimAlpha)

        assertEquals(1f, end.scale)
        assertEquals(0f, end.translationYDp)
        assertEquals(0f, end.cornerDp)
        assertEquals(0f, end.backgroundScrimAlpha)
        assertEquals(0f, end.settleProgress)
    }

    @Test
    fun routeSheetSettleBounceStaysSubtleAtBothEnds() {
        val motion = resolveVideoDetailRouteSheetMotion(
            sourceRoute = "home",
            transitionEnabled = true
        )

        val enterSettle = resolveVideoDetailRouteSheetFrame(
            rawProgress = 1f,
            settleProgress = 1f,
            settleDirection = VideoDetailRouteSheetSettleDirection.Enter,
            motion = motion
        )
        val returnSettle = resolveVideoDetailRouteSheetFrame(
            rawProgress = 0f,
            settleProgress = 1f,
            settleDirection = VideoDetailRouteSheetSettleDirection.Return,
            motion = motion
        )

        assertTrue(enterSettle.scale <= 1.002f)
        assertTrue(enterSettle.translationYDp >= -2f)
        assertTrue(returnSettle.scale >= motion.initialScale - 0.002f)
        assertTrue(returnSettle.translationYDp <= motion.initialTranslationYDp + 2f)
        assertEquals(1f, enterSettle.settleProgress)
        assertEquals(1f, returnSettle.settleProgress)
    }
}
