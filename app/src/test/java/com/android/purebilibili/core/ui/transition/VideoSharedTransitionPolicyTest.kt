package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VideoSharedTransitionPolicyTest {

    @Test
    fun videoSharedTransitionUsesContinuityCurveForEnterAndReturnAlpha() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )
        val enter = motion.enterAlphaEasing
        val returning = motion.returnAlphaEasing

        assertSame(enter, returning)
        assertEquals(
            AppMotionEasing.Continuity.transform(0.5f),
            enter.transform(0.5f),
            0.001f,
        )
        assertTrue(enter.transform(0.5f) > 0.7f)
    }

    @Test
    fun videoSharedTransitionSpatialEasing_isContinuityEaseOut() {
        val easing = resolveVideoCardSharedTransitionSpatialEasing()
        assertEquals(
            AppMotionEasing.Continuity.transform(0.5f),
            easing.transform(0.5f),
            0.001f,
        )
        // 先快后慢：半程进度应明显超过线性 0.5
        assertTrue(easing.transform(0.5f) > 0.7f)
    }

    @Test
    fun videoSharedBoundsUseContinuityEnterAndLinearSeekableReturn() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true
        )
        val cardBounds = Rect(0f, 0f, 160f, 100f)
        val detailBounds = Rect(0f, 0f, 360f, 800f)

        val enter = videoSharedElementBoundsTransformSpec(motion, cardBounds, detailBounds)
        val returning = videoSharedElementBoundsTransformSpec(motion, detailBounds, cardBounds)

        assertTrue(enter is TweenSpec<*>)
        assertTrue(returning is TweenSpec<*>)
        assertEquals(motion.durationMillis, (enter as TweenSpec<*>).durationMillis)
        assertEquals(motion.durationMillis, (returning as TweenSpec<*>).durationMillis)
        // 进场：Continuity 先快后慢、无过冲
        assertEquals(
            AppMotionEasing.Continuity.transform(0.4f),
            enter.easing.transform(0.4f),
            0.001f,
        )
        // 返回：Linear 固定时长，预测返回 seek/松手 remainingDuration 可算，避免一闪落位
        assertSame(LinearEasing, returning.easing)
        assertEquals(
            LinearEasing,
            resolveVideoSharedElementSpatialEasing(detailBounds, cardBounds),
        )
        assertEquals(
            AppMotionEasing.Continuity,
            resolveVideoSharedElementSpatialEasing(cardBounds, detailBounds),
        )
        // settle buffer 仅覆盖主时长后的短收尾，不再为 spring 过冲预留长窗口
        assertEquals(48L, resolveVideoCardReturnSpringSettleBufferMs())
    }

    @Test
    fun videoSharedCoverCacheKeyMatchesTheHomeCardIdentity() {
        assertEquals("cover_BV1ab411_n", resolveVideoSharedCoverCacheKey(" BV1ab411 "))
        assertEquals("cover_BV1ab411_s", resolveVideoSharedCoverCacheKey("BV1ab411", true))
    }

    @Test
    fun videoSharedTransitionDirection_followsBoundsArea() {
        val cardBounds = Rect(0f, 0f, 160f, 100f)
        val detailBounds = Rect(0f, 0f, 360f, 800f)

        assertEquals(
            VideoSharedTransitionDirection.ENTER,
            resolveVideoSharedTransitionDirection(cardBounds, detailBounds)
        )
        assertEquals(
            VideoSharedTransitionDirection.RETURN,
            resolveVideoSharedTransitionDirection(detailBounds, cardBounds)
        )
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
    fun coverRelayTransition_disabledSoShellOwnsRelatedAndPartition() {
        val related = resolveVideoSharedTransitionOwnership(
            sourceRoute = "video/BV_A",
            coverSharedEnabled = true,
            isQuickReturnLimited = false
        )
        // 相关推荐竖卡恢复整卡 shell，与首页同形态。
        assertTrue(related.useCoverSharedBounds)
        assertTrue(related.useCardContainerSharedBounds)
        assertFalse(related.useMetadataSharedBounds)

        val partition = resolveVideoSharedTransitionOwnership(
            sourceRoute = "partition",
            coverSharedEnabled = true,
            isQuickReturnLimited = false
        )
        assertTrue(partition.useCoverSharedBounds)
        assertTrue(partition.useCardContainerSharedBounds)
        assertFalse(partition.useMetadataSharedBounds)
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
    fun videoCardShellKey_stripsSeasonSeriesQueryForStableMatch() {
        val shellKey = videoCardShellSharedElementKey(
            bvid = "BV1",
            sourceRoute = "season_series_detail/favorite_season/123?mid=1&title=%E6%B5%8B%E8%AF%95"
        )
        assertEquals("season_series_detail/favorite_season/123", shellKey.sourceRoute)
    }

    @Test
    fun homeCategoryVideoCardSourceKeyKeepsCategoryRoute() {
        val homeCardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        ).readText()
        val sharedKeySource = File(
            "src/main/java/com/android/purebilibili/core/ui/transition/BiliPaiSharedElementKey.kt"
        ).readText()

        assertTrue(homeCardSource.contains("isVideoCardSharedReturnTarget("))
        assertTrue(sharedKeySource.contains("home?category="))
        assertTrue(homeCardSource.contains("videoCardShellReturnChromeAlpha("))

        val shellKey = videoCardShellSharedElementKey(
            bvid = "BV1",
            sourceRoute = "home?category=动画"
        )
        assertEquals("home?category=动画", shellKey.sourceRoute)
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
    fun videoCoverRelay_isDisabledPendingCoordinateFix() {
        // 详情套详情下 cover relay 飞位易错；相关/分区暂用 shell + 源卡进场淡出。
        assertFalse(shouldUseVideoCoverRelayTransition("partition"))
        assertFalse(shouldUseVideoCoverRelayTransition("video/BV_A"))
        assertFalse(shouldUseVideoCoverRelayTransition("home"))
        assertFalse(shouldUseVideoCoverRelayTransition(null))
    }

    @Test
    fun videoCardShellSharedBounds_includesRelatedAndPartitionSources() {
        assertTrue(shouldUseVideoCardShellSharedBounds("home", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("dynamic", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("watch_later", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("space", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("partition", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("video/BV_A", transitionEnabled = true))
        assertTrue(shouldUseVideoCardShellSharedBounds("video", transitionEnabled = true))
        assertFalse(shouldUseVideoCardShellSharedBounds("home", transitionEnabled = false))
        assertFalse(shouldUseVideoCardShellSharedBounds(null, transitionEnabled = true))
        assertFalse(shouldSkipVideoCardSharedBoundsMorph("video/BV_A"))
        assertFalse(shouldSkipVideoCardSharedBoundsMorph("partition"))
        assertFalse(shouldSkipVideoCardSharedBoundsMorph("home"))
    }

    @Test
    fun videoCardShellContainerTransform_includesSpaceSources() {
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
                sourceRoute = "partition",
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
        assertTrue(
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
    fun cardShellSharedBoundsScalesTowardTopNotCenter() {
        val helperSource = File(
            "src/main/java/com/android/purebilibili/core/ui/transition/VideoCardShellSharedBounds.kt"
        ).readText()

        // 默认详情壳：FillWidth + TopCenter，对齐顶部播放器落点
        assertTrue(helperSource.contains("scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter)"))
        // 竖屏全屏壳：FillBounds/Crop + Center（整卡展开）
        assertTrue(helperSource.contains("scaleToBounds(ContentScale.Crop, Alignment.Center)"))
    }

    @Test
    fun cardShellSharedBoundsKeepsLiveReturnVisibleBeforeSourceCoverFadesIn() {
        assertEquals(
            EnterTransition.None,
            resolveVideoCardShellSharedBoundsEnter(
                role = VideoCardShellSharedBoundsRole.DetailShell,
                transitionDurationMillis = 360,
            )
        )
        assertTrue(shouldDelaySourceCardEnterForLiveReturnMorph("home"))
        assertTrue(shouldDelaySourceCardEnterForLiveReturnMorph("dynamic"))
        // 分区竖卡与首页一样：返回延后淡入源卡。
        assertTrue(shouldDelaySourceCardEnterForLiveReturnMorph("partition"))
        // 相关推荐竖卡与首页一样：返回延后淡入源卡。
        assertTrue(shouldDelaySourceCardEnterForLiveReturnMorph("video/BV_A"))
        // 快速返回：不延后，标题/UP 与封面同步落位。
        assertFalse(
            shouldDelaySourceCardEnterForLiveReturnMorph(
                sourceRoute = "home",
                isQuickReturnFromDetail = true,
            )
        )
        assertEquals(
            EnterTransition.None,
            resolveVideoCardShellSharedBoundsEnter(
                role = VideoCardShellSharedBoundsRole.SourceCard,
                transitionDurationMillis = 360,
                delaySourceCardEnterForLiveReturn = false,
            ),
        )
        // 竖卡：源卡返回 Enter 延后淡入，避免封面盖住实时画面。
        assertTrue(
            resolveVideoCardShellSharedBoundsEnter(
                role = VideoCardShellSharedBoundsRole.SourceCard,
                transitionDurationMillis = 360,
                delaySourceCardEnterForLiveReturn = true,
            ) != EnterTransition.None
        )
        // 360 * 0.38 = 136.8 → 136
        assertEquals(136, resolveVideoCardShellSourceEnterFadeDelayMillis(360))
        assertEquals(
            ExitTransition.None,
            resolveVideoCardShellSharedBoundsExit(
                role = VideoCardShellSharedBoundsRole.DetailShell,
            )
        )
        // 首页 / 相关 / 分区竖卡进场：源卡不淡出。
        assertEquals(
            ExitTransition.None,
            resolveVideoCardShellSharedBoundsExit(
                role = VideoCardShellSharedBoundsRole.SourceCard,
                fadeOutSourceCardOnOpen = false,
            )
        )
        assertFalse(shouldFadeOutShellSourceCardOnOpen("partition"))
        assertFalse(shouldFadeOutShellSourceCardOnOpen("video/BV_A"))
        assertFalse(shouldFadeOutShellSourceCardOnOpen("home"))
        assertTrue(
            resolveVideoCardShellSharedBoundsExit(
                role = VideoCardShellSharedBoundsRole.SourceCard,
                fadeOutSourceCardOnOpen = true,
                transitionDurationMillis = 360,
            ) != ExitTransition.None
        )
        assertEquals(100, resolveVideoCardShellSourceExitFadeDurationMillis(360))
        val detailSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        ).readText()
        assertTrue(detailSource.contains("VideoCardShellSharedBoundsRole.DetailShell"))
        val homeCardSource = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt"
        ).readText()
        // 整卡 alpha=0 会落位黑闪；只对 info 区接 chrome alpha。
        assertTrue(homeCardSource.contains("infoContainerModifier.videoCardShellReturnChromeAlpha"))
        assertFalse(
            Regex(
                """videoCardShellSharedBoundsOrEmpty\([\s\S]{0,400}?\)\s*\.graphicsLayer"""
            ).containsMatchIn(homeCardSource)
        )
        val chromeHelper = File(
            "src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCardShellReturnChrome.kt"
        ).readText()
        assertTrue(chromeHelper.contains("resolveHomeCardChromeAlphaDuringShellReturnMorph("))
        val shellHelper = File(
            "src/main/java/com/android/purebilibili/core/ui/transition/VideoCardShellSharedBounds.kt"
        ).readText()
        assertTrue(shellHelper.contains("shouldDelaySourceCardEnterForLiveReturnMorph("))
        assertTrue(shellHelper.contains("isQuickReturnFromDetail"))
    }

    @Test
    fun videoDetailRootProvidesGlobalCardShellSharedBoundsTarget() {
        val detailSource = listOf(
            "VideoDetailTransitionHost.kt",
            "VideoDetailScreenStateHolder.kt"
        ).joinToString("\n") { name ->
            File("src/main/java/com/android/purebilibili/feature/video/screen/$name").readText()
        }

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
        assertEquals(360, motion.durationMillis)
        assertEquals(360, motion.fullscreenDurationMillis)
        assertEquals(40, motion.contentDelayMillis)
        assertEquals(220, motion.contentDurationMillis)
        assertEquals(14, motion.contentSlideOffsetDp)
        assertEquals(0.985f, motion.contentInitialScale, 0.0001f)
        assertSame(motion.enterAlphaEasing, motion.returnAlphaEasing)
        assertEquals(
            AppMotionEasing.Continuity.transform(0.35f),
            motion.enterAlphaEasing.transform(0.35f),
            0.001f,
        )
        assertEquals(
            AppMotionEasing.Continuity.transform(0.5f),
            resolveVideoCardSharedTransitionEnterEasing().transform(0.5f),
            0.001f,
        )
        assertEquals(
            AppMotionEasing.Continuity.transform(0.5f),
            resolveVideoCardSharedTransitionReturnEasing().transform(0.5f),
            0.001f,
        )
        // 景深返回清晰单独用 SoftClear：中段 fraction 明显低于 Continuity，模糊不会过早掐清。
        assertTrue(
            resolveVideoCardTransitionBackgroundReturnClearEasing().transform(0.5f) <
                AppMotionEasing.Continuity.transform(0.5f) - 0.2f,
        )
    }

    @Test
    fun videoCardSharedTransitionMotion_preservesFastTimelineOption() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(VideoSharedTransitionSpeed.FAST)
        )

        assertEquals(280, motion.durationMillis)
        assertEquals(280, motion.fullscreenDurationMillis)
        assertEquals(220, motion.contentDurationMillis)
    }

    @Test
    fun videoCardSharedTransitionMotion_keepsMasterTimelineForQuickReturn() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            isQuickReturn = true,
        )

        assertEquals(360, motion.durationMillis)
        assertEquals(0, motion.contentDelayMillis)
        assertEquals(220, motion.contentDurationMillis)
        assertEquals(360, motion.fullscreenDurationMillis)
    }

    @Test
    fun videoCardSharedTransitionMotion_supportsSlowTimelineOption() {
        val motion = resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = "home",
            transitionEnabled = true,
            speedSettings = VideoSharedTransitionSpeedSettings(VideoSharedTransitionSpeed.SLOW)
        )

        assertEquals(480, motion.durationMillis)
        assertEquals(480, motion.fullscreenDurationMillis)
        assertEquals(288, motion.contentDurationMillis)
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

        assertEquals(240, low.durationMillis)
        assertEquals(620, custom.durationMillis)
        assertEquals(620, custom.fullscreenDurationMillis)
        assertEquals(360, custom.contentDurationMillis)
        assertEquals(900, high.durationMillis)
    }

    @Test
    fun videoMetadataSharedTransitionMotion_matchesCoverTimeline() {
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
        assertSame(coverMotion.enterAlphaEasing, metadataMotion.enterAlphaEasing)
        assertSame(coverMotion.returnAlphaEasing, metadataMotion.returnAlphaEasing)
        assertEquals(
            coverMotion.durationMillis,
            resolveVideoMetadataSharedBoundsDurationMillis(metadataMotion),
        )
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
        // 分区复用首页 ElegantVideoCard（shell 在 VideoCard.kt），列表本身走竖卡双列。
        assertTrue(partitionSource.contains("ElegantVideoCard("))
        assertTrue(partitionSource.contains("state.videos.chunked(2)"))
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
        assertEquals(360, motion.durationMillis)
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
        assertEquals(16f / 10f, VIDEO_SHARED_COVER_ASPECT_RATIO, 0.0001f)
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
    fun detailReturnFade_immediateKeepsLiveSurface_coverFirstStillUsesCover() {
        assertFalse(
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
        // ImmediatePlayback 一镜到底不叠封面；CoverFirst 仍叠封面
        assertFalse(
            shouldUseDetailReturnCoverCrossfade(
                isLeaving = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback
            )
        )
        assertTrue(
            shouldUseDetailReturnCoverCrossfade(
                isLeaving = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst
            )
        )
        assertFalse(
            shouldUseDetailReturnCoverCrossfade(
                isLeaving = false,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback
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
