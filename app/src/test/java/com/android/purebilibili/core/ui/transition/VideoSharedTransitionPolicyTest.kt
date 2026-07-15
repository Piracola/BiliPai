package com.android.purebilibili.core.ui.transition

import com.android.purebilibili.core.ui.motion.AppMotionEasing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VideoSharedTransitionPolicyTest {

    @Test
    fun videoSharedTransitionUsesMaterialEmphasizedEnterEasing() {
        assertSame(AppMotionEasing.EmphasizedEnter, resolveVideoCardSharedTransitionEasing())
    }

    @Test
    fun coverSharedTransition_enabled_whenTransitionAndScopesAreReady() {
        assertTrue(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun metadataSharedTransition_keepsDefaultForNonHomeCallers() {
        assertEquals(VideoSharedTransitionProfile.COVER_AND_METADATA, resolveVideoSharedTransitionProfile())
        assertTrue(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = false
            )
        )
    }

    @Test
    fun metadataSharedTransition_staysEnabledWhenQuickReturnLimitedForNonHomeCallers() {
        assertTrue(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = true
            )
        )
    }

    @Test
    fun metadataSharedTransition_disabledWhenCardContainerOwnsSharedBounds() {
        assertFalse(
            shouldEnableVideoMetadataSharedTransition(
                coverSharedEnabled = true,
                isQuickReturnLimited = false,
                useCardContainerSharedBounds = true
            )
        )
    }

    @Test
    fun homeVideoTransition_usesWholeCardShellWithoutMetadataBounds() {
        val policy = resolveVideoSharedTransitionOwnership(
            sourceRoute = "home",
            coverSharedEnabled = true,
            isQuickReturnLimited = false
        )

        assertTrue(policy.useCoverSharedBounds)
        assertTrue(policy.useCardContainerSharedBounds)
        assertFalse(policy.useMetadataSharedBounds)
    }

    @Test
    fun videoCardShellKey_keepsSourceRouteDistinctFromCoverKey() {
        val shellKey = videoCardShellSharedElementKey(
            bvid = "BV1",
            sourceRoute = "history"
        )
        val coverKey = videoCoverSharedElementKey(
            bvid = "BV1",
            sourceRoute = "history"
        )

        assertEquals(VideoSharedElement.CARD_SHELL, shellKey.element)
        assertEquals("history", shellKey.sourceRoute)
        assertFalse(shellKey == coverKey)
    }

    @Test
    fun homeCategoryVideoCardSourceKeyKeepsCategoryRoute() {
        val homeCardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        ).readText()

        assertTrue(homeCardSource.contains("normalizeVideoCardSourceRouteForKey(effectiveSharedElementSourceRoute)"))
        assertFalse(homeCardSource.contains("val normalizedRoute = effectiveSharedElementSourceRoute\n                ?.substringBefore(\"?\")"))
    }

    @Test
    fun nonHomeVideoTransition_usesWholeCardShellWithoutMetadataBounds() {
        val policy = resolveVideoSharedTransitionOwnership(
            sourceRoute = "search",
            coverSharedEnabled = true,
            isQuickReturnLimited = false
        )

        assertTrue(policy.useCoverSharedBounds)
        assertTrue(policy.useCardContainerSharedBounds)
        assertFalse(policy.useMetadataSharedBounds)
    }

    @Test
    fun videoCardShellSharedBounds_includesRelatedDetailSources() {
        assertTrue(shouldUseVideoCardShellSharedBounds("home", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("dynamic", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("watch_later", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("partition", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("space", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("video", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("video/BV_A", transitionEnabled = true))
        assertFalse(shouldUseVideoCardShellSharedBounds("home", transitionEnabled = false))
        assertFalse(shouldUseVideoCardShellSharedBounds(null, transitionEnabled = true))
    }

    @Test
    fun videoCardShellContainerTransform_usesCoverOnlyForSpaceSources() {
        assertTrue(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "home",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertTrue(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "video/BV_A",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertTrue(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "home?tab=recommend",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertTrue(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "search",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertTrue(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "dynamic_detail/123",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "space/42",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "settings",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "video",
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "home",
                transitionEnabled = false,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldUseVideoCardShellContainerTransform(
                sourceRoute = "home",
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
    }

    @Test
    fun cardShellSharedBoundsHelperUsesCardShellKeyNotCoverKey() {
        val helperSource = File(
            "src/main/java/com/android/purebilibili/core/ui/transition/VideoCardShellSharedBounds.kt"
        ).readText()

        assertTrue(helperSource.contains("videoCardShellSharedElementKey("))
        assertFalse(helperSource.contains("videoCoverSharedElementKey("))
    }

    @Test
    fun videoDetailRootProvidesGlobalCardShellSharedBoundsTarget() {
        val detailSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt"
        ).readText()

        assertTrue(detailSource.contains("shouldUseVideoCardShellContainerTransform("))
        assertTrue(detailSource.contains("detailShellSharedBoundsEnabled"))
        assertTrue(detailSource.contains("videoCardShellSharedBoundsOrEmpty("))
    }

    @Test
    fun videoCardSharedTransitionMotion_usesStandardCoverPrimaryTimelineByDefault() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )

        assertTrue(motion.enabled)
        assertEquals(460, motion.durationMillis)
        assertEquals(540, motion.fullscreenDurationMillis)
        assertEquals(40, motion.contentDelayMillis)
        assertEquals(276, motion.contentDurationMillis)
        assertEquals(14, motion.contentSlideOffsetDp)
        assertEquals(0.985f, motion.contentInitialScale, 0.0001f)
        assertTrue(motion.easing.transform(0.35f) > 0.7f)
        assertTrue(motion.easing.transform(0.35f) < 0.9f)
        assertTrue(motion.easing.transform(0.75f) > 0.96f)
    }

    @Test
    fun videoCardSharedTransitionMotion_preservesFastTimelineOption() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(VideoSharedTransitionSpeed.FAST)
        )

        assertEquals(360, motion.durationMillis)
        assertEquals(440, motion.fullscreenDurationMillis)
        assertEquals(220, motion.contentDurationMillis)
    }

    @Test
    fun videoCardSharedTransitionMotion_supportsSlowTimelineOption() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(VideoSharedTransitionSpeed.SLOW)
        )

        assertEquals(560, motion.durationMillis)
        assertEquals(640, motion.fullscreenDurationMillis)
        assertEquals(336, motion.contentDurationMillis)
    }

    @Test
    fun videoCardSharedTransitionMotion_supportsClampedCustomTimeline() {
        val low = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(
                speed = VideoSharedTransitionSpeed.CUSTOM,
                customDurationMillis = 120
            )
        )
        val custom = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(
                speed = VideoSharedTransitionSpeed.CUSTOM,
                customDurationMillis = 620
            )
        )
        val high = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(
                speed = VideoSharedTransitionSpeed.CUSTOM,
                customDurationMillis = 1200
            )
        )

        assertEquals(280, low.durationMillis)
        assertEquals(620, custom.durationMillis)
        assertEquals(700, custom.fullscreenDurationMillis)
        assertEquals(360, custom.contentDurationMillis)
        assertEquals(900, high.durationMillis)
    }

    @Test
    fun videoMetadataSharedTransitionMotion_usesCoverTimingButBoundsFinishEarlier() {
        val coverMotion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )
        val metadataMotion = resolveVideoMetadataSharedTransitionMotionSpec(
            transitionEnabled = true
        )

        assertTrue(metadataMotion.enabled)
        assertEquals(coverMotion.durationMillis, metadataMotion.durationMillis)
        assertEquals(coverMotion.fullscreenDurationMillis, metadataMotion.fullscreenDurationMillis)
        assertEquals(0, metadataMotion.contentDelayMillis)
        assertSame(coverMotion.easing, metadataMotion.easing)
        assertEquals(331, resolveVideoMetadataSharedBoundsDurationMillis(metadataMotion))
        assertTrue(resolveVideoMetadataSharedBoundsDurationMillis(metadataMotion) < metadataMotion.durationMillis)
    }

    @Test
    fun videoCardSources_useWholeCardShellSharedBoundsWithoutMetadataKeys() {
        val homeCardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        ).readText()
        val detailInfoSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/section/VideoInfoSection.kt"
        ).readText()
        val partitionSource = File(
            "src/main/java/com/android/purebilibili/feature/partition/PartitionScreen.kt"
        ).readText()
        val cinematicCardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/CinematicVideoCard.kt"
        ).readText()
        val glassCardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/GlassVideoCard.kt"
        ).readText()
        val dynamicCardSource = File(
            "src/main/java/com/android/purebilibili/feature/dynamic/components/VideoCards.kt"
        ).readText()
        val watchLaterSource = File(
            "src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt"
        ).readText()
        val spaceSource = File(
            "src/main/java/com/android/purebilibili/feature/space/SpaceScreen.kt"
        ).readText()
        val navHostSource = File(
            "src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"
        ).readText()

        assertTrue(homeCardSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(homeCardSource.contains("videoTitleSharedElementKey("))
        assertTrue(detailInfoSource.contains("useCardContainerSharedBounds = useCardContainerSharedBounds"))
        assertTrue(partitionSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(partitionSource.contains("videoTitleSharedElementKey("))
        assertTrue(cinematicCardSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(cinematicCardSource.contains("videoTitleSharedElementKey("))
        assertTrue(glassCardSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(glassCardSource.contains("videoTitleSharedElementKey("))
        assertTrue(dynamicCardSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(dynamicCardSource.contains("videoTitleSharedElementKey("))
        assertTrue(watchLaterSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(watchLaterSource.contains("videoTitleSharedElementKey("))
        assertTrue(spaceSource.contains("videoCardShellSharedBoundsOrEmpty("))
        assertFalse(spaceSource.contains("videoTitleSharedElementKey("))
        assertFalse(navHostSource.contains("VideoSharedTransitionBackdropHost("))
    }

    @Test
    fun videoCardSharedTransitionMotion_keepsTimelineForNonHomeSources() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "search",
            transitionEnabled = true
        )

        assertTrue(motion.enabled)
        assertEquals(460, motion.durationMillis)
    }

    @Test
    fun homeSharedTransitionCornerSpec_softlyConvergesFromCardToPlayer() {
        val corner = resolveHomeVideoSharedTransitionCornerSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )

        assertTrue(corner.enabled)
        assertEquals(16, corner.startCornerDp)
        assertEquals(12, corner.endCornerDp)
    }

    @Test
    fun sharedCoverAspectRatio_defaultsToHomeCardSixteenByTen() {
        assertEquals(1.6f, VIDEO_SHARED_COVER_ASPECT_RATIO, 0.0001f)
    }

    @Test
    fun sharedTransitionVisualSpec_coverFirst_anchorsToInlineCover() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            sourceCornerDp = 12,
            playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
            fullscreen = false,
            autoPortrait = false,
            initialVertical = false,
            isVerticalVideo = false,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.InlineCover, spec.targetMode)
        assertEquals(12, spec.sourceCornerDp)
        assertEquals(12, spec.targetCornerDp)
        assertFalse(spec.fillTargetViewport)
        assertTrue(spec.useCoverSharedBounds)
        assertFalse(spec.suppressCoverFade)
    }

    @Test
    fun sharedTransitionVisualSpec_coverFirstVertical_usesPortraitViewport() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            sourceCornerDp = 12,
            playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
            fullscreen = false,
            autoPortrait = true,
            initialVertical = true,
            isVerticalVideo = true,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.PortraitFullscreen, spec.targetMode)
        assertEquals(0, spec.targetCornerDp)
        assertTrue(spec.fillTargetViewport)
        assertTrue(spec.useCoverSharedBounds)
    }

    @Test
    fun sharedTransitionVisualSpec_immediateLandscapeFullscreen_usesSquareViewport() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "partition",
            sourceCornerDp = 10,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            fullscreen = true,
            autoPortrait = false,
            initialVertical = false,
            isVerticalVideo = false,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.LandscapeFullscreen, spec.targetMode)
        assertEquals(10, spec.sourceCornerDp)
        assertEquals(0, spec.targetCornerDp)
        assertTrue(spec.fillTargetViewport)
    }

    @Test
    fun sharedTransitionVisualSpec_portraitRoute_usesPortraitViewport() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "home",
            sourceCornerDp = 12,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            fullscreen = false,
            autoPortrait = true,
            initialVertical = true,
            isVerticalVideo = true,
            isReturning = false
        )

        assertEquals(VideoSharedTransitionTargetMode.PortraitFullscreen, spec.targetMode)
        assertEquals(0, spec.targetCornerDp)
        assertTrue(spec.fillTargetViewport)
    }

    @Test
    fun sharedTransitionVisualSpec_returnConvergesToRecordedCardCorner() {
        val spec = resolveVideoSharedTransitionVisualSpec(
            sourceRoute = "watch_later",
            sourceCornerDp = 8,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            fullscreen = true,
            autoPortrait = true,
            initialVertical = true,
            isVerticalVideo = true,
            isReturning = true
        )

        assertEquals(VideoSharedTransitionTargetMode.InlineCover, spec.targetMode)
        assertEquals(8, spec.targetCornerDp)
        assertFalse(spec.fillTargetViewport)
        assertTrue(spec.suppressCoverFade)
    }

    @Test
    fun sharedTransitionPlaybackIntent_mapsClickToPlaySetting() {
        assertEquals(
            VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            resolveVideoSharedTransitionPlaybackIntent(clickToPlayEnabled = true)
        )
        assertEquals(
            VideoSharedTransitionPlaybackIntent.CoverFirst,
            resolveVideoSharedTransitionPlaybackIntent(clickToPlayEnabled = false)
        )
        assertEquals(
            VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            resolveVideoSharedTransitionPlaybackIntent(
                clickToPlayEnabled = false,
                forceImmediatePlayback = true
            )
        )
    }

    @Test
    fun detailReturnFade_onlyAppliesToImmediatePlaybackProfile() {
        assertTrue(
            shouldFadePlayerSurfaceOnDetailReturn(
                isLeaving = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback
            )
        )
        assertFalse(
            shouldFadePlayerSurfaceOnDetailReturn(
                isLeaving = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst
            )
        )
        assertFalse(
            shouldFadePlayerSurfaceOnDetailReturn(
                isLeaving = false,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback
            )
        )
        assertTrue(
            shouldUseDetailReturnCoverCrossfade(
                isLeaving = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback
            )
        )
        assertFalse(
            shouldUseDetailReturnCoverCrossfade(
                isLeaving = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst
            )
        )
    }

    @Test
    fun homeVideoCardPropagatesClickToPlayPlaybackIntent() {
        val cardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        ).readText()

        assertTrue(cardSource.contains("resolveVideoSharedTransitionPlaybackIntent("))
        assertTrue(cardSource.contains("SettingsManager.getClickToPlaySync(context)"))
        assertTrue(cardSource.contains("playbackIntent = videoSharedPlaybackIntent"))
    }

    @Test
    fun sharedTransitionSourceCorner_mapsKnownNonHomeSources() {
        assertEquals(10, resolveVideoSharedTransitionSourceCornerDp("dynamic", fallbackCornerDp = 12))
        assertEquals(8, resolveVideoSharedTransitionSourceCornerDp("watch_later", fallbackCornerDp = 12))
        assertEquals(12, resolveVideoSharedTransitionSourceCornerDp("history", fallbackCornerDp = 12))
        assertEquals(12, resolveVideoSharedTransitionSourceCornerDp("partition?from=tab", fallbackCornerDp = 12))
    }
}
