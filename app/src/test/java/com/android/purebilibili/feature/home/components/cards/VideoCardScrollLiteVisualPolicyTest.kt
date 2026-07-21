package com.android.purebilibili.feature.home.components.cards

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardScrollLiteVisualPolicyTest {

    @Test
    fun `normal mode removes cover gradient behind compact stats`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertTrue(policy.showHistoryProgressBar)
        assertTrue(policy.showCompactStatsOnCover)
        assertFalse(policy.showSecondaryStatsRow)
    }

    @Test
    fun `normal mode removes cover gradient when stats move below cover`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertTrue(policy.showHistoryProgressBar)
        assertFalse(policy.showCompactStatsOnCover)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode keeps stats without cover shadow`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true,
            compactStatsOnCover = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertFalse(policy.showHistoryProgressBar)
        assertTrue(policy.showCompactStatsOnCover)
        assertFalse(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode keeps secondary row when cover stats are disabled`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true,
            compactStatsOnCover = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCompactStatsOnCover)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `normal mode keeps story card secondary stats row`() {
        val policy = resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode removes story card shadows but keeps stats`() {
        val policy = resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `home video card variants do not attach shadow modifiers`() {
        listOf(
            "VideoCard.kt",
            "StoryVideoCard.kt",
            "GlassVideoCard.kt",
            "CinematicVideoCard.kt"
        ).forEach { fileName ->
            val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/$fileName")
                .readText()

            assertFalse("$fileName should not draw video cover shadows", source.contains(".shadow("))
        }
    }

    @Test
    fun `elegant video card clips static cover container to cover shape`() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()

        assertTrue(
            "首页视频封面本体必须裁剪 coverShape，不能只依赖 sharedBounds 的 overlay 裁剪。",
            source.contains(
                ".aspectRatio(coverAspectRatio)\n" +
                    "                .clip(coverShape)"
            )
        )
    }

    @Test
    fun `cover stats do not claim separate shared bounds when shell owns morph`() {
        // CARD_SHELL 容器已接管共享元素；封面上的播放量/弹幕不再挂独立 sharedBounds，
        // 避免与 shell morph / 冻结景深叠层抢 key。
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()
        val coverStatsBlock = source
            .substringAfter("if (scrollLitePolicy.showCompactStatsOnCover) {")
            .substringBefore("//  时长标签")

        assertTrue(coverStatsBlock.contains("BoxWithConstraints("))
        assertFalse(coverStatsBlock.contains("videoViewsSharedElementKey"))
        assertFalse(coverStatsBlock.contains("sharedBounds("))
        assertTrue(source.contains("videoCardShellSharedBoundsOrEmpty("))
    }

    @Test
    fun `return target cover disables crossfade during and after shared return`() {
        // 返回过程中
        assertFalse(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = true,
                useCoverSharedBounds = true,
                isSharedReturnTarget = true
            )
        )
        // clearReturning 之后仍是 lastClicked 目标：必须继续关 crossfade，否则会再闪一次
        assertFalse(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = false,
                useCoverSharedBounds = true,
                isSharedReturnTarget = true
            )
        )
        // 非返回目标可正常 crossfade
        assertTrue(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = false,
                useCoverSharedBounds = true,
                isSharedReturnTarget = false
            )
        )
    }

    @Test
    fun `video card cover request remembers crossfade to avoid rebuild flash`() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()
        assertTrue(source.contains("val coverImageRequest = remember("))
        assertTrue(source.contains("coverCrossfadeEnabled"))
        assertTrue(source.contains("pinnedSharedReturnCover"))
        assertTrue(source.contains(".placeholderMemoryCacheKey(requestCoverCacheKey)"))
        assertTrue(source.contains("model = coverImageRequest"))
    }

    @Test
    fun `shared return target pins cover source to avoid mid-return swap flash`() {
        assertTrue(shouldPinVideoCardCoverForSharedReturn(isSharedReturnTarget = true))
        assertFalse(shouldPinVideoCardCoverForSharedReturn(isSharedReturnTarget = false))
    }

    @Test
    fun `non return target cover keeps crossfade`() {
        // 同屏其它卡：返回会话中仍可 crossfade
        assertTrue(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = true,
                useCoverSharedBounds = true,
                isSharedReturnTarget = false
            )
        )
        // lastClicked 返回目标：clear 后仍关 crossfade（防落位闪）
        assertFalse(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = false,
                useCoverSharedBounds = true,
                isSharedReturnTarget = true
            )
        )
    }

    @Test
    fun `scrolling disables cover crossfade`() {
        assertFalse(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = true,
                isReturningFromDetail = false,
                useCoverSharedBounds = false,
                isSharedReturnTarget = false
            )
        )
    }

    @Test
    fun `home video metadata uses on surface colors for readable up and publish text`() {
        val onSurface = Color(0xFF1D1B20)
        val colors = resolveHomeVideoCardMetadataColors(onSurface)

        assertEquals(onSurface, colors.upNameColor)
        assertEquals(onSurface.copy(alpha = 0.82f), colors.upMetaColor)
        assertEquals(onSurface.copy(alpha = 0.68f), colors.upBadgeTextColor)
        assertEquals(onSurface.copy(alpha = 0.10f), colors.upBadgeBackgroundColor)
        assertEquals(onSurface.copy(alpha = 0.72f), colors.publishTimeColor)
    }

    @Test
    fun `home video card reserves followed badge height to align publish rows`() {
        val cardSource = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()
        val upBadgeSource = File("src/main/java/com/android/purebilibili/core/ui/components/UpBadgeName.kt")
            .readText()

        assertTrue(cardSource.contains("trailingSlotMinHeight = 20.dp"))
        assertTrue(upBadgeSource.contains(".heightIn(min = trailingSlotMinHeight)"))
    }

    @Test
    fun homeCardCover_neverHidesDuringShellMorph() {
        // OPENING 也不再藏封面：快速返回打断时避免落位露出 surfaceVariant。
        assertFalse(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING,
                isVideoCardReturnGestureInProgress = false,
            )
        )
        assertFalse(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = true,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.RETURNING,
                isVideoCardReturnGestureInProgress = false,
            )
        )
        assertFalse(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE,
                isVideoCardReturnGestureInProgress = false,
            )
        )
        assertFalse(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING,
                isVideoCardReturnGestureInProgress = true,
            )
        )
    }

    @Test
    fun homeCardChrome_staysInSyncWithWholeCardShellOnOpenAndReturn() {
        // 整卡一起飞：进场/返回 morph 中标题 alpha 恒为 1，禁止封面先落位、字晚出。
        assertEquals(
            1f,
            resolveHomeCardChromeAlphaDuringShellReturnMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING,
                isSharedTransitionActive = true,
                transitionBackgroundProgress = 0.4f,
            ),
            0.001f,
        )
        assertEquals(
            1f,
            resolveHomeCardChromeAlphaDuringShellReturnMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = true,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.RETURNING,
                isSharedTransitionActive = true,
                transitionBackgroundProgress = 1f,
            ),
            0.001f,
        )
        assertEquals(
            1f,
            resolveHomeCardChromeAlphaDuringShellReturnMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = true,
                isSharedTransitionActive = true,
                isVideoCardReturnGestureInProgress = true,
                transitionBackgroundProgress = 0.7f,
            ),
            0.001f,
        )
        assertFalse(
            shouldSuppressHomeCardVisualDuringShellReturnMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = true,
                isSharedTransitionActive = true,
                transitionBackgroundProgress = 1f,
            )
        )
        // revealStart=0：任意 settle 进度标题都满显（与壳同步）
        assertEquals(1f, resolveHomeCardChromeEarlyRevealAlpha(settleProgress = 0f), 0.001f)
        assertEquals(1f, resolveHomeCardChromeEarlyRevealAlpha(settleProgress = 0.1f), 0.001f)
        assertEquals(1f, resolveHomeCardChromeEarlyRevealAlpha(settleProgress = 1f), 0.001f)
        assertTrue(
            isVideoCardSharedReturnTarget(
                bvid = "BV1xx",
                sourceRoute = "home?category=1",
                lastClickedVideoSourceKey = "home?category=1:BV1xx",
            )
        )
    }
}
