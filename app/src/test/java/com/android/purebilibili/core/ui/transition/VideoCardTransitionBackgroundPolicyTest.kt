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
        assertEquals(0f, opening.cornerRadiusPx)
        assertEquals(0f, returning.blurRadiusPx)
        assertTrue(returning.scrimAlpha > 0f)
        assertEquals(0f, returning.cornerRadiusPx)
    }

    @Test
    fun api35OpeningFrameUsesCalibratedBlurStrengthAndScrim() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Enhanced,
            isLightBackground = false,
            sdkInt = 35
        )

        // density=1 时 12dp → 12px；真实机型由 DrawScope.density 换算。
        assertEquals(12f, frame.blurRadiusPx)
        assertEquals(0f, frame.blurRadiusPx % 1f)
        assertEquals(0.28f, frame.scrimAlpha)
        assertFalse(frame.useLightScrimTint)
        // 页面层禁止缩放/圆角，避免整页缩进黑边
        assertEquals(1f, frame.contentScale, 0.0001f)
        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun normalAndEnhancedShareFullBlurBudget_noDeviceTierDowngrade() {
        val normal = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
        )
        val enhanced = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35,
        )

        assertEquals(12f, normal.blurRadiusPx)
        assertEquals(12f, enhanced.blurRadiusPx)
        assertEquals(12f, resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Normal))
        assertEquals(12f, resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Enhanced))
        assertEquals(0f, resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Reduced))
        assertEquals(1f, resolveVideoCardTransitionBlurQuantumPx(MotionTier.Normal))
        assertEquals(1f, resolveVideoCardTransitionBlurQuantumPx(MotionTier.Enhanced))
    }

    @Test
    fun blurRadiusScalesWithDensity_forConsistentDpFeel() {
        val phone = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
            density = 2.75f,
        )
        val tablet = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
            density = 1.5f,
        )

        // 12dp × 2.75 ≈ 33px；12dp × 1.5 = 18px
        assertEquals(33f, phone.blurRadiusPx, 0.51f)
        assertEquals(18f, tablet.blurRadiusPx, 0.51f)
        // 页面层缩放关闭时圆角恒为 0
        assertEquals(0f, phone.cornerRadiusPx, 0.0001f)
        assertEquals(0f, tablet.cornerRadiusPx, 0.0001f)
        assertEquals(
            33f,
            resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Normal, density = 2.75f),
            0.01f,
        )
    }

    @Test
    fun backgroundCornerUsesDeviceRadiusWhenLargerThanFallback() {
        // 页面缩放关闭时，页面圆角解析恒为 0（避免四角啃边）。
        assertEquals(
            0f,
            resolveVideoCardTransitionBackgroundCornerRadiusPx(
                depthProgress = 1f,
                motionTier = MotionTier.Normal,
                density = 2.75f,
                deviceCornerRadiusPx = 80f,
            ),
            0.0001f,
        )
        // dp 策略仍保留：max(设备, 24dp 兜底)，供整页缩放重新开启时复用。
        assertEquals(
            80f / 2.75f,
            resolveVideoCardTransitionBackgroundCornerRadiusDp(
                deviceCornerRadiusPx = 80f,
                density = 2.75f,
            ),
            0.0001f,
        )
        assertEquals(
            24f,
            resolveVideoCardTransitionBackgroundCornerRadiusDp(
                deviceCornerRadiusPx = 40f,
                density = 2.75f,
            ),
            0.0001f,
        )
        assertEquals(
            24f,
            resolveVideoCardTransitionBackgroundCornerRadiusDp(
                deviceCornerRadiusPx = 0f,
                density = 2.75f,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveDeviceDisplayCornerRadiusPx(
                rootWindowInsets = null,
                sdkInt = 35,
            ),
        )
        assertEquals(
            0f,
            resolveDeviceDisplayCornerRadiusPx(
                rootWindowInsets = null,
                sdkInt = 30,
            ),
        )
    }

    @Test
    fun siblingCardsShrinkWithDepth_butMorphSourceStaysOne() {
        assertEquals(
            0.92f,
            resolveVideoCardSiblingDepthScale(
                depthProgress = 1f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                isSharedMorphSourceCard = false,
                motionTier = MotionTier.Normal,
            ),
            0.0001f,
        )
        assertEquals(
            0.96f,
            resolveVideoCardSiblingDepthScale(
                depthProgress = 0.5f,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
                isSharedMorphSourceCard = false,
                motionTier = MotionTier.Normal,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoCardSiblingDepthScale(
                depthProgress = 1f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                isSharedMorphSourceCard = true,
                motionTier = MotionTier.Normal,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoCardSiblingDepthScale(
                depthProgress = 1f,
                phase = VideoCardTransitionBackgroundPhase.IDLE,
                isSharedMorphSourceCard = false,
                motionTier = MotionTier.Normal,
            ),
            0.0001f,
        )
    }

    @Test
    fun openingEarlyProgressStillAppliesBlurForVisualQuality() {
        val early = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
        )
        // 0.1 × 12dp = 1.2 → 量化到 1px
        assertEquals(1f, early.blurRadiusPx, 1f)
        assertTrue(early.scrimAlpha > 0f)
        assertTrue(early.blurRadiusPx > 0f)
        assertEquals(0f, early.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun lightOpeningUsesReducedScrimAndWarmTint() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Enhanced,
            isLightBackground = true,
            sdkInt = 35
        )

        assertEquals(12f, frame.blurRadiusPx)
        assertEquals(0.14f, frame.scrimAlpha)
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
        assertEquals(0.08f, frame.scrimAlpha)
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
    fun returningDepthProgressIsLinearToLockstepWithMorph() {
        // RETURNING 与 OPENING 同源线性，禁止 soft-clear 二次映射拖糊。
        assertEquals(
            0.5f,
            resolveVideoCardTransitionDepthProgress(
                progress = 0.5f,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            resolveVideoCardTransitionDepthProgress(
                progress = 0.5f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            ),
            0.0001f,
        )
        // 遗留 soft-clear 曲线仍可算，但主路径不再使用。
        assertEquals(0.5647f, softClearVideoCardTransitionDepth(0.5f), 0.01f)
    }

    @Test
    fun morphAlignedDepthClearDuration_matchesMorphRemainingWallClock() {
        assertEquals(
            360,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 360,
                blurStartProgress = 1f,
            ),
        )
        assertEquals(
            180,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 360,
                blurStartProgress = 0.5f,
            ),
        )
        // 禁止 min 160 地板：手势已拖到底后不应再补一段长消糊。
        assertEquals(
            0,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 90,
                blurStartProgress = 0f,
            ),
        )
        assertEquals(
            18,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 90,
                blurStartProgress = 0.2f,
            ),
        )
    }

    @Test
    fun returningFrameFadesBlurAndScrimWithSharedElementProgress() {
        val start = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35
        )
        val middle = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35
        )
        val end = resolveVideoCardTransitionBackgroundFrame(
            progress = 0f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35
        )

        assertEquals(12f, start.blurRadiusPx)
        // 线性锁步：progress=0.5 → blur 6px（density1）
        assertEquals(6f, middle.blurRadiusPx, 1f)
        assertTrue(middle.blurRadiusPx in 1f..<start.blurRadiusPx)
        assertEquals(0f, end.blurRadiusPx)
        assertTrue(start.scrimAlpha > middle.scrimAlpha)
        assertTrue(middle.scrimAlpha > 0f)
        assertEquals(0f, end.scrimAlpha)
        assertEquals(1f, start.contentScale, 0.0001f)
        assertEquals(1f, middle.contentScale, 0.0001f)
        assertEquals(1f, end.contentScale)
        assertEquals(0f, start.cornerRadiusPx, 0.0001f)
        assertEquals(0f, middle.cornerRadiusPx, 0.0001f)
        assertEquals(0f, end.cornerRadiusPx, 0.0001f)
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
            motionTier = MotionTier.Enhanced,
            isLightBackground = false,
            sdkInt = 35
        )

        assertEquals(12f, frame.blurRadiusPx)
        // HELD 保留与满进度开场一致的压暗，避免详情停留时景深断裂。
        assertEquals(0.28f, frame.scrimAlpha)
        assertEquals(1f, frame.contentScale, 0.0001f)
        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
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

        assertEquals(1f, openingScale, 0.0001f)
        assertEquals(1f, restoreScale, 0.002f)
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
        assertEquals(0f, frame.cornerRadiusPx)
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
        assertEquals(1f, frame.contentScale, 0.0001f)
        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
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
    fun routeMatcherAppliesHomeHostDepthForInlinePartitionSource() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "home",
                sourceRoute = "partition",
                activeMainHostRoute = "home"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "partition",
                activeMainHostRoute = "home"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "partition",
                sourceRoute = "partition",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "partition",
                activeMainHostRoute = "dynamic"
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
    fun quickReturnSnapsDepthBlurClearToAvoidCoverBlurFlash() {
        // 打断 OPENING：无论是否已 mark quick，都必须立刻清模糊。
        assertTrue(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = false,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            )
        )
        assertTrue(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
            )
        )
        assertFalse(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = false,
                phase = VideoCardTransitionBackgroundPhase.HELD,
            )
        )
        assertFalse(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = false,
                phase = VideoCardTransitionBackgroundPhase.IDLE,
            )
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

        assertEquals(0.14f, heldFull.scrimAlpha)
        assertTrue(heldHalf.scrimAlpha < heldFull.scrimAlpha)
        assertEquals(0.28f, openingFull.scrimAlpha)
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
            scrimAlpha = 0.14f,
        )
        val lightMidGesture = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.03f,
        )
        val darkHeld = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = false,
            scrimAlpha = 0.28f,
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
