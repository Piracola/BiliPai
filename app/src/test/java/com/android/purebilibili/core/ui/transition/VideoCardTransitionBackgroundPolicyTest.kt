package com.android.purebilibili.core.ui.transition

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardTransitionBackgroundPolicyTest {

    @Test
    fun snapshotBlur_isEnabledForActivePhasesOnApi31Plus() {
        assertTrue(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                motionTier = MotionTier.Normal,
                sdkInt = 35,
            )
        )
        assertTrue(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.HELD,
                motionTier = MotionTier.Normal,
                sdkInt = 31,
            )
        )
        assertTrue(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                motionTier = MotionTier.Normal,
                sdkInt = 35,
            )
        )
    }

    @Test
    fun snapshotBlur_isDisabledForIdleReducedOrLegacyApi() {
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.IDLE,
                motionTier = MotionTier.Normal,
                sdkInt = 35,
            )
        )
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                motionTier = MotionTier.Reduced,
                sdkInt = 35,
            )
        )
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                motionTier = MotionTier.Normal,
                sdkInt = 30,
            )
        )
    }

    @Test
    fun snapshotBlur_respectsRealtimeBlurSetting() {
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = false,
                sdkInt = 35,
            )
        )
    }

    @Test
    fun navBackdrop_isHiddenWhenPredictiveReturnTargetsAnotherVideoDetail() {
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                isVideoDetailOnStack = true,
                isReturningToVideoDetail = true,
            )
        )
    }

    @Test
    fun reducedMotionTierSkipsRealtimeBlurAndDepthScaleButKeepsScrim() {
        val opening = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Reduced,
            sdkInt = 35
        )
        val returning = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Reduced,
            sdkInt = 35
        )

        assertEquals(0f, opening.blurRadiusPx)
        assertTrue(opening.scrimAlpha > 0f)
        assertEquals(1f, opening.contentScale)
        assertEquals(0f, returning.blurRadiusPx)
        assertTrue(returning.scrimAlpha > 0f)
    }

    @Test
    fun api35OpeningFrameUsesCalibratedBlurStrengthAndScrim() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            isLightBackground = false,
            sdkInt = 35
        )

        assertEquals(20f, frame.blurRadiusPx)
        assertEquals(0f, frame.blurRadiusPx % 1f)
        assertEquals(0.22f, frame.scrimAlpha)
        assertFalse(frame.useLightScrimTint)
        assertEquals(0.96f, frame.contentScale, 0.0001f)
    }

    @Test
    fun lightOpeningUsesReducedScrimAndWarmTint() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            isLightBackground = true,
            sdkInt = 35
        )

        assertEquals(20f, frame.blurRadiusPx)
        assertEquals(0.11f, frame.scrimAlpha)
        assertTrue(frame.useLightScrimTint)
    }

    @Test
    fun lightReducedMotionUsesMinimalOpeningScrimWithoutBlur() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Reduced,
            isLightBackground = true,
            sdkInt = 35
        )

        assertEquals(0f, frame.blurRadiusPx)
        assertEquals(0.07f, frame.scrimAlpha)
        assertTrue(frame.useLightScrimTint)
    }

    @Test
    fun returningScrimMatchesHeldIntensityAtTheSameProgress() {
        val held = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            sdkInt = 35,
        )
        val returning = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35,
        )

        assertEquals(held.scrimAlpha, returning.scrimAlpha)
    }

    @Test
    fun softClearDepthKeepsMoreBlurThroughMidReturn() {
        assertEquals(1f, softClearVideoCardTransitionDepth(1f), 0.0001f)
        assertEquals(0.75f, softClearVideoCardTransitionDepth(0.5f), 0.0001f)
        assertEquals(0f, softClearVideoCardTransitionDepth(0f), 0.0001f)
        assertTrue(
            softClearVideoCardTransitionDepth(0.5f) >
                resolveVideoCardTransitionDepthProgress(
                    progress = 0.5f,
                    phase = VideoCardTransitionBackgroundPhase.OPENING,
                ),
        )
    }

    @Test
    fun returningFrameFadesBlurAndScrimWithSharedElementProgress() {
        val start = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )
        val middle = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )
        val end = resolveVideoCardTransitionBackgroundFrame(
            progress = 0f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )

        assertEquals(20f, start.blurRadiusPx)
        // soft-clear：progress=0.5 时 depth≈0.75，blur 仍约 15px，中段不清空。
        assertEquals(15f, middle.blurRadiusPx)
        assertTrue(middle.blurRadiusPx in 1f..<start.blurRadiusPx)
        assertEquals(0f, end.blurRadiusPx)
        assertTrue(start.scrimAlpha > middle.scrimAlpha)
        assertTrue(middle.scrimAlpha > 0f)
        assertEquals(0f, end.scrimAlpha)
        assertEquals(0.96f, start.contentScale, 0.0001f)
        assertEquals(0.97f, middle.contentScale, 0.0001f)
        assertEquals(1f, end.contentScale)
    }

    @Test
    fun detailToDetailSourceBlursOnlyTheExactPreviousDetailEntry() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "video/BV_A",
                sourceRoute = "video/BV_A",
                activeMainHostRoute = "home"
            )
        )
    }

    @Test
    fun heldFrameKeepsBackgroundBlurDepthAndScrimForStableIosLikeStack() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            isLightBackground = false,
            sdkInt = 35
        )

        assertEquals(20f, frame.blurRadiusPx)
        // HELD 保留与满进度开场一致的压暗，避免详情停留时景深断裂。
        assertEquals(0.22f, frame.scrimAlpha)
        assertEquals(0.96f, frame.contentScale, 0.0001f)
    }

    @Test
    fun gestureRestoreSmoothlyReturnsBackgroundToHeldDepth() {
        val openingScale = resolveVideoCardTransitionContentScale(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            isGestureRestoreInProgress = false,
        )
        val restoreScale = resolveVideoCardTransitionContentScale(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            motionTier = MotionTier.Normal,
            isGestureRestoreInProgress = true,
        )

        assertEquals(0.96f, openingScale, 0.0001f)
        assertEquals(0.98f, restoreScale, 0.0001f)
    }

    @Test
    fun idleFrameClearsBackgroundEffect() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.IDLE,
            sdkInt = 35
        )

        assertEquals(0f, frame.blurRadiusPx)
        assertEquals(0f, frame.scrimAlpha)
        assertEquals(1f, frame.contentScale)
    }

    @Test
    fun androidBeforeSDisablesRealtimeBlurButKeepsOpeningScrim() {
        val opening = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            sdkInt = 30
        )
        val returning = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 30
        )

        assertEquals(0f, opening.blurRadiusPx)
        assertTrue(opening.scrimAlpha > 0f)
        assertEquals(0f, returning.blurRadiusPx)
        assertTrue(returning.scrimAlpha > 0f)
    }

    @Test
    fun midReturnProgressStillKeepsFadingBackgroundEffect() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.25f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )

        assertTrue(frame.blurRadiusPx > 0f)
        assertTrue(frame.scrimAlpha > 0f)
        // soft-clear：p=0.25 → depth=0.4375 → scale=1-0.04*0.4375
        assertEquals(0.9825f, frame.contentScale, 0.0001f)
    }

    @Test
    fun snapshotRecording_staysFrozenForAllActivePhases_toProtectFrameBudget() {
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.HELD,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.IDLE,
            )
        )
    }

    @Test
    fun routeMatcherTargetsOnlyRecordedSourceEntryOrActiveMainHostPage() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home",
                activeMainHostRoute = "dynamic"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "search",
                sourceRoute = "search",
                activeMainHostRoute = "home"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "space/123",
                sourceRoute = "space/123?from=archive",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "settings",
                sourceRoute = "home",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "video/BV1",
                sourceRoute = "video",
                activeMainHostRoute = "home"
            )
        )
    }

    @Test
    fun routeMatcherTreatsHomeCategoryAsActiveHomePageForRealtimeBlur() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home?category=RECOMMEND",
                activeMainHostRoute = "home"
            )
        )
    }

    @Test
    fun gestureProgressMapsBackGestureToDecreasingBlurStartingFromFull() {
        // 手势起点保持满虚化，与 HELD 衔接；拖到底背景清晰；中途单调递减。
        assertEquals(1f, resolveVideoCardTransitionBackgroundGestureProgress(0f))
        assertEquals(0.5f, resolveVideoCardTransitionBackgroundGestureProgress(0.5f))
        assertEquals(0f, resolveVideoCardTransitionBackgroundGestureProgress(1f))
    }

    @Test
    fun gestureProgressClampsOutOfRangeBackProgress() {
        assertEquals(1f, resolveVideoCardTransitionBackgroundGestureProgress(-0.5f))
        assertEquals(0f, resolveVideoCardTransitionBackgroundGestureProgress(1.5f))
    }

    @Test
    fun openingGestureProgress_fadesFromCurrentOpeningBlurLinearly() {
        assertEquals(0.6f, resolveVideoCardTransitionBackgroundOpeningGestureProgress(
            openingBlurProgress = 0.6f,
            backProgress = 0f,
        ))
        assertEquals(0.3f, resolveVideoCardTransitionBackgroundOpeningGestureProgress(
            openingBlurProgress = 0.6f,
            backProgress = 0.5f,
        ))
        assertEquals(0f, resolveVideoCardTransitionBackgroundOpeningGestureProgress(
            openingBlurProgress = 0.6f,
            backProgress = 1f,
        ))
    }

    @Test
    fun resolveGestureBlurProgress_routesHeldAndOpeningPhases() {
        assertEquals(
            0.5f,
            resolveVideoCardTransitionBackgroundGestureBlurProgress(
                phase = VideoCardTransitionBackgroundPhase.HELD,
                currentBlurProgress = 1f,
                backProgress = 0.5f,
            )
        )
        assertEquals(
            0.4f * (1f - 0.4f),
            resolveVideoCardTransitionBackgroundGestureBlurProgress(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                currentBlurProgress = 0.4f,
                backProgress = 0.4f,
            )
        )
    }

    @Test
    fun gesturePhase_includesHeldAndOpeningOnly() {
        assertTrue(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.HELD))
        assertTrue(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.OPENING))
        assertFalse(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.IDLE))
        assertFalse(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.RETURNING))
    }

    @Test
    fun returnDurationScalesWithRemainingBlurButKeepsMinimumFloor() {
        // 未消解(startProgress=1)时用完整时长；手势已消解一半则约减半；接近清晰时不低于取消时长下限。
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS,
            resolveVideoCardTransitionBackgroundReturnDurationMs(1f)
        )
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS / 2,
            resolveVideoCardTransitionBackgroundReturnDurationMs(0.5f)
        )
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
            resolveVideoCardTransitionBackgroundReturnDurationMs(0f)
        )
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
            resolveVideoCardTransitionBackgroundReturnDurationMs(0.05f)
        )
    }

    @Test
    fun returnFullDurationKeepsTheSharedMasterTimeline() {
        assertEquals(
            460,
            resolveVideoCardTransitionReturnFullDurationMillis(
                baseDurationMillis = 460,
            ),
        )
        assertEquals(0, resolveVideoCardTransitionReturnFullDurationMillis(-1))
    }

    @Test
    fun openingPhaseIsInterruptedOnReturn() {
        assertTrue(
            shouldInterruptVideoCardOpeningOnReturn(VideoCardTransitionBackgroundPhase.OPENING)
        )
        assertFalse(
            shouldInterruptVideoCardOpeningOnReturn(VideoCardTransitionBackgroundPhase.HELD)
        )
        assertFalse(
            shouldInterruptVideoCardOpeningOnReturn(VideoCardTransitionBackgroundPhase.RETURNING)
        )
    }

    @Test
    fun navBackdropStaysVisibleThroughTheWholeVideoCardTransition() {
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                isVideoDetailOnStack = true,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                isVideoDetailOnStack = true,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                isVideoDetailOnStack = true,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                isVideoDetailOnStack = false,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = false,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                isVideoDetailOnStack = true,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
                isVideoDetailOnStack = false,
            )
        )
    }

    @Test
    fun navBackdropFrameTracksBlurStrengthDuringHeldAndOpening() {
        val heldFull = resolveVideoCardTransitionNavBackdropFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            isLightBackground = true,
        )
        val heldHalf = resolveVideoCardTransitionNavBackdropFrame(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            isLightBackground = true,
        )
        val openingFull = resolveVideoCardTransitionNavBackdropFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            isLightBackground = false,
        )

        assertEquals(0.11f, heldFull.scrimAlpha)
        assertTrue(heldHalf.scrimAlpha < heldFull.scrimAlpha)
        assertEquals(0.22f, openingFull.scrimAlpha)
        assertTrue(heldFull.useLightScrimTint)
        assertFalse(openingFull.useLightScrimTint)
    }

    @Test
    fun navBackdropColorLerpsFromBaseBackgroundTowardScrimTint() {
        val base = androidx.compose.ui.graphics.Color.White
        val frame = VideoCardTransitionNavBackdropFrame(
            scrimAlpha = 0.10f,
            useLightScrimTint = true,
        )
        val blended = resolveVideoCardTransitionNavBackdropColor(
            baseBackgroundColor = base,
            frame = frame,
        )

        assertTrue(blended != base)
        assertTrue(blended.alpha > 0f)
    }

    @Test
    fun scaleGapFillKeepsOpaqueGrayFloorSoPredictiveBackEdgesDoNotReadAsWhiteBars() {
        val lightHeld = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.11f,
        )
        val lightMidGesture = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.03f,
        )
        val darkHeld = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = false,
            scrimAlpha = 0.22f,
        )

        assertEquals(1f, lightHeld.alpha)
        assertEquals(1f, lightMidGesture.alpha)
        assertTrue(lightHeld.red < 0.92f)
        assertTrue(lightMidGesture.red < 0.92f)
        assertEquals(lightHeld, lightMidGesture)
        assertTrue(darkHeld.red < 0.35f)
        assertTrue(
            shouldDrawVideoCardTransitionScaleGapFill(contentScale = 0.96f)
        )
        assertFalse(
            shouldDrawVideoCardTransitionScaleGapFill(contentScale = 1f)
        )
    }

    @Test
    fun navBackdropColorUsesScaleGapFloorDuringLowProgressGesture() {
        val base = androidx.compose.ui.graphics.Color.White
        val lowProgress = resolveVideoCardTransitionNavBackdropColor(
            baseBackgroundColor = base,
            frame = VideoCardTransitionNavBackdropFrame(
                scrimAlpha = 0.02f,
                useLightScrimTint = true,
            ),
        )
        val gapFill = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.02f,
        )

        assertEquals(gapFill, lowProgress)
        assertTrue(lowProgress.red < 0.92f)
    }
}
