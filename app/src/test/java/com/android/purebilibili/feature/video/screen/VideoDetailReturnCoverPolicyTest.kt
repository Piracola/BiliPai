package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoSharedTransitionPlaybackIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VideoDetailReturnCoverPolicyTest {

    @Test
    fun `immediate video back target keeps secondary content visible`() {
        assertFalse(
            shouldUseVideoDetailRootTransitionProgress(
                detailShellSharedBoundsEnabled = true,
                hasAnimatedVisibilityScope = true,
                keepLoadedContentForBackPreview = true,
            )
        )
    }

    @Test
    fun `related detail return suppresses enter fade flash while keeping exit fade available`() {
        assertTrue(
            shouldSuppressVideoDetailEnterFadeAfterBackPreview(
                wasKeptAsBackPreview = true,
                keepLoadedContentForBackPreview = false,
            )
        )
        assertFalse(
            shouldSuppressVideoDetailEnterFadeAfterBackPreview(
                wasKeptAsBackPreview = false,
                keepLoadedContentForBackPreview = false,
            )
        )
        assertEquals(
            1f,
            resolveVideoDetailReturnContentAlpha(
                transitionProgress = 0.2f,
                isCommittedCardReturn = false,
                holdFullyOpaqueAfterBackPreview = true,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveVideoDetailReturnContentAlpha(
                transitionProgress = 0.2f,
                isCommittedCardReturn = true,
                holdFullyOpaqueAfterBackPreview = true,
            ),
            0.0001f,
        )
    }

    @Test
    fun `normal card detail transition still animates secondary content`() {
        assertTrue(
            shouldUseVideoDetailRootTransitionProgress(
                detailShellSharedBoundsEnabled = true,
                hasAnimatedVisibilityScope = true,
                keepLoadedContentForBackPreview = false,
            )
        )
    }

    @Test
    fun `predictive cancel keeps the cover until the detail exit transition has settled`() {
        assertTrue(
            shouldTreatVideoDetailCardExitAsReturning(
                isExitTransitionInProgress = true,
                sharedBoundsActive = true,
            )
        )
        assertFalse(
            shouldTreatVideoDetailCardExitAsReturning(
                isExitTransitionInProgress = false,
                sharedBoundsActive = true,
            )
        )
    }

    @Test
    fun `detail used as immediate back target keeps its loaded player and controls`() {
        assertFalse(
            shouldTreatVideoDetailCardExitAsReturning(
                isExitTransitionInProgress = true,
                sharedBoundsActive = true,
                keepLoadedContentForBackPreview = true,
            )
        )
    }

    @Test
    fun `video target without player ownership uses its own cover behind the live outgoing video`() {
        assertTrue(
            shouldForceBackPreviewPlayerCover(
                keepLoadedContentForBackPreview = true,
                bindLivePlayerForBackPreview = false
            )
        )
        assertFalse(
            shouldForceBackPreviewPlayerCover(
                keepLoadedContentForBackPreview = true,
                bindLivePlayerForBackPreview = true
            )
        )
    }

    @Test
    fun `immediate back target mounts the live inline player`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val inlinePlayerCall = source
            .substringAfter("PortraitInlineVideoPlayerHost(", "")
            .substringBefore("allowLivePlayerSharedElement = true")

        assertTrue(inlinePlayerCall.contains("liveBackPreview = bindLivePlayerForBackPreview"))
    }

    @Test
    fun `detail route does not manually fade its background during return`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()

        assertFalse(source.contains("resolveVideoDetailShellBackgroundAlphaTarget"))
        assertFalse(source.contains("shellBackgroundAlpha"))
    }

    @Test
    fun `force cover becomes active when explicit return flag is true`() {
        assertTrue(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = true
            )
        )
    }

    @Test
    fun `global returning state does not force the target detail cover`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false
            )
        )
    }

    @Test
    fun `detail shell shared bounds does not disable return cover visual`() {
        assertTrue(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = true
            )
        )
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailTransitionPolicy.kt")
            .readText()
        val policyBlock = source
            .substringAfter("internal fun resolveForceCoverOnlyForReturn(")
            .substringBefore("internal fun shouldUseReturningVideoDetailVisualState")
        assertFalse(policyBlock.contains("detailShellSharedBoundsEnabled"))
    }

    @Test
    fun `force cover stays disabled when shared transition is disabled`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = true,
                transitionEnabled = false
            )
        )
    }

    @Test
    fun `force cover stays disabled when only exit transition is in progress`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false
            )
        )
    }

    @Test
    fun `predictive card return keeps the live player instead of forcing cover`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false,
                isCardReturnExitInProgress = true
            )
        )
    }

    @Test
    fun `force cover stays disabled during predictive card return exit when transition disabled`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false,
                transitionEnabled = false,
                isCardReturnExitInProgress = true
            )
        )
    }

    @Test
    fun `force cover stays disabled when no return state is active`() {
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false
            )
        )
    }

    @Test
    fun `idle detail does not use returning visual without exit or force cover`() {
        assertFalse(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = false,
                isCardReturnExitInProgress = false,
                isSessionReturningToCard = false,
            )
        )
    }

    @Test
    fun `committed card return exit enables cover handoff without forceCoverOnly`() {
        assertTrue(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = false,
                isCardReturnExitInProgress = true,
            )
        )
        assertTrue(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = false,
                isSessionReturningToCard = true,
            )
        )
        assertFalse(
            resolveForceCoverOnlyForReturn(
                forceCoverOnlyOnReturn = false,
                isCardReturnExitInProgress = true,
            )
        )
    }

    @Test
    fun `live return morph keeps player visible instead of handing to resident cover`() {
        assertEquals(
            0f,
            resolveVideoDetailReturnCoverAlpha(0.8f, true, true, liveReturnMorph = true),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoDetailReturnPlayerAlpha(0.8f, true, true, liveReturnMorph = true),
            0.0001f,
        )
        // 整页收缩：提交后正文仍保持可见，随 shell bounds 形变
        assertEquals(
            1f,
            resolveVideoDetailReturnContentAlpha(0.8f, true, liveReturnMorph = true),
            0.0001f,
        )
    }

    @Test
    fun `cover-first committed return still hands visual ownership to resident cover`() {
        assertEquals(
            1f,
            resolveVideoDetailReturnCoverAlpha(0.8f, true, true, liveReturnMorph = false),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveVideoDetailReturnPlayerAlpha(0.8f, true, true, liveReturnMorph = false),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveVideoDetailReturnContentAlpha(0.8f, true, liveReturnMorph = false),
            0.0001f,
        )
    }

    @Test
    fun `uncommitted predictive live morph keeps player and detail content fully visible`() {
        assertEquals(
            0f,
            resolveVideoDetailReturnCoverAlpha(0.8f, false, true, liveReturnMorph = true),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoDetailReturnPlayerAlpha(0.8f, false, true, liveReturnMorph = true),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoDetailReturnContentAlpha(0.8f, false, liveReturnMorph = true),
            0.0001f,
        )
    }

    @Test
    fun `uncommitted predictive cover-first return keeps following transition progress`() {
        assertEquals(
            0.2f,
            resolveVideoDetailReturnCoverAlpha(0.8f, false, true, liveReturnMorph = false),
            0.0001f,
        )
        assertEquals(
            0.8f,
            resolveVideoDetailReturnPlayerAlpha(0.8f, false, true, liveReturnMorph = false),
            0.0001f,
        )
        assertEquals(
            0.8f,
            resolveVideoDetailReturnContentAlpha(0.8f, false, liveReturnMorph = false),
            0.0001f,
        )
    }

    @Test
    fun `live return morph gate requires immediate playback and active shared bounds`() {
        assertTrue(
            shouldUseLiveReturnMorph(
                transitionEnabled = true,
                sharedBoundsActive = true,
                keepLoadedContentForBackPreview = false,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
                detailContentReady = true,
            )
        )
        assertFalse(
            shouldUseLiveReturnMorph(
                transitionEnabled = true,
                sharedBoundsActive = true,
                keepLoadedContentForBackPreview = false,
                playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
                detailContentReady = true,
            )
        )
        assertFalse(
            shouldUseLiveReturnMorph(
                transitionEnabled = true,
                sharedBoundsActive = true,
                keepLoadedContentForBackPreview = true,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
                detailContentReady = true,
            )
        )
        assertFalse(
            shouldUseLiveReturnMorph(
                transitionEnabled = true,
                sharedBoundsActive = false,
                keepLoadedContentForBackPreview = false,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
                detailContentReady = true,
            )
        )
    }

    @Test
    fun `live return morph disabled while detail content still loading to avoid skeleton in card`() {
        assertFalse(
            shouldTreatVideoDetailContentReadyForLiveReturnMorph(
                hasSuccessfulDetailContent = false,
            )
        )
        assertTrue(
            shouldTreatVideoDetailContentReadyForLiveReturnMorph(
                hasSuccessfulDetailContent = true,
            )
        )
        assertFalse(
            shouldUseLiveReturnMorph(
                transitionEnabled = true,
                sharedBoundsActive = true,
                keepLoadedContentForBackPreview = false,
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
                detailContentReady = false,
            )
        )
    }

    @Test
    fun `resident cover ownership is suppressed during live return morph`() {
        assertFalse(
            shouldHandVisualOwnershipToResidentCover(
                useReturningVisualState = true,
                hasResidentCover = true,
                liveReturnMorph = true,
            )
        )
        assertTrue(
            shouldHandVisualOwnershipToResidentCover(
                useReturningVisualState = true,
                hasResidentCover = true,
                liveReturnMorph = false,
            )
        )
        assertFalse(
            shouldHandVisualOwnershipToResidentCover(
                useReturningVisualState = true,
                hasResidentCover = false,
                liveReturnMorph = false,
            )
        )
    }

    @Test
    fun `detail return cover ownership table matches timeline contract`() {
        val live = resolveVideoDetailReturnCoverOwnership(
            transitionEnabled = true,
            sharedBoundsActive = true,
            keepLoadedContentForBackPreview = false,
            playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
            detailContentReady = true,
            hasResidentCover = true,
        )
        assertTrue(isLiveReturnMorphFromOwnership(live))
        assertFalse(
            shouldHandResidentCoverFromOwnership(
                ownership = live,
                useReturningVisualState = true,
                hasResidentCover = true,
            )
        )

        val coverFirst = resolveVideoDetailReturnCoverOwnership(
            transitionEnabled = true,
            sharedBoundsActive = true,
            keepLoadedContentForBackPreview = false,
            playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
            detailContentReady = true,
            hasResidentCover = true,
        )
        assertFalse(isLiveReturnMorphFromOwnership(coverFirst))
        assertTrue(
            shouldHandResidentCoverFromOwnership(
                ownership = coverFirst,
                useReturningVisualState = true,
                hasResidentCover = true,
            )
        )
    }

    @Test
    fun `missing return cover keeps player visible instead of revealing black`() {
        assertEquals(0f, resolveVideoDetailReturnCoverAlpha(0.2f, true, false), 0.0001f)
        assertEquals(1f, resolveVideoDetailReturnPlayerAlpha(0.2f, true, false), 0.0001f)
        assertEquals(0f, resolveVideoDetailReturnContentAlpha(0.2f, true), 0.0001f)
    }

    @Test
    fun `return cover player and content read one shared transition progress`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()

        assertTrue(source.contains("val detailTransitionProgress ="))
        assertTrue(source.contains("alpha = resolveVideoDetailReturnCoverAlpha("))
        assertTrue(source.contains("alpha = resolveVideoDetailReturnPlayerAlpha("))
        assertTrue(source.contains("alpha = resolveVideoDetailReturnContentAlpha("))
        assertFalse(source.contains("val coverCrossfadeAlpha ="))
        assertFalse(source.contains("val playerFadeAlpha ="))
    }

    @Test
    fun `shared shell keeps detail content mounted while root transition owns alpha`() {
        assertTrue(
            shouldShowVideoDetailContent(
                isTransitionFinished = true,
                isLeaving = true,
                rootTransitionOwnsContentAlpha = true,
            )
        )
        assertFalse(
            shouldShowVideoDetailContent(
                isTransitionFinished = true,
                isLeaving = true,
                rootTransitionOwnsContentAlpha = false,
            )
        )
    }

    @Test
    fun `related back preview keeps parent content mounted during return morph`() {
        assertTrue(
            shouldShowVideoDetailContent(
                isTransitionFinished = false,
                isLeaving = false,
                rootTransitionOwnsContentAlpha = true,
                keepContentVisibleAfterBackPreview = true,
            )
        )
        assertFalse(
            shouldShowVideoDetailContent(
                isTransitionFinished = false,
                isLeaving = true,
                rootTransitionOwnsContentAlpha = false,
                keepContentVisibleAfterBackPreview = true,
            )
        )
    }

    @Test
    fun `explicit force cover still switches detail into returning visual state`() {
        assertTrue(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = true,
                isCardReturnExitInProgress = false,
            )
        )
    }

    @Test
    fun `returning visual is wired from exit progress and session for handoff`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val transitionHostSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailTransitionHost.kt"
        ).readText()
        val call = source
            .substringAfter("val useReturningVideoDetailVisualState = shouldUseReturningVideoDetailVisualState(")
            .substringBefore("val handleTopBarAction")
        assertTrue(call.contains("isCardReturnExitInProgress = isCardReturnExitInProgress"))
        assertTrue(call.contains("isSessionReturningToCard = isReturningFromDetail"))
        assertTrue(transitionHostSource.contains("video-detail-shared-transition-progress"))
    }

    @Test
    fun `resident cover starts return without a pre-navigation dead frame`() {
        assertEquals(0L, resolveCoverTakeoverDelayBeforeBackNavigationMillis())
    }

    @Test
    fun `resident return cover reuses the home card image cache`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val requestBlock = source
            .substringAfter("val residentCoverImageRequest =")
            .substringBefore("//  播放器容器按当前顶部避让高度计算")

        assertTrue(requestBlock.contains(".crossfade(false)"))
        assertTrue(requestBlock.contains(".placeholderMemoryCacheKey(sharedCoverCacheKey)"))
        assertTrue(requestBlock.contains(".memoryCacheKey(sharedCoverCacheKey)"))
        assertTrue(requestBlock.contains(".diskCacheKey(sharedCoverCacheKey)"))
    }

    @Test
    fun `navigation actions do not switch the loaded player to a cover`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val actionBlock = source
            .substringAfter("action@{ action: VideoDetailTopBarAction ->")
            .substringBefore("val handleBack =")

        assertFalse(actionBlock.contains("forceCoverOnlyOnReturn = true"))
        assertTrue(source.contains("useTextureSurfaceForNavigation = transitionEnabled"))
    }

    @Test
    fun `player container shared bounds are disabled during return to avoid cover key conflict`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val playerContainerBlock = source
            .substringAfter("val playerContainerModifier = if (")
            .substringBefore(") {")
        assertTrue(
            "Player container must not claim the cover shared bounds during return; the forced return cover overlay owns that key.",
            playerContainerBlock.contains("!forceCoverOnlyForReturn")
        )
    }

    @Test
    fun `related child transition suppresses parent detail shell shared bounds`() {
        assertTrue(
            shouldSuppressDetailShellSharedBoundsForRelatedChildTransition(
                detailBvid = "BV_A",
                lastClickedVideoSourceKey = "video/BV_A:BV_B",
                isSharedTransitionActive = true,
            )
        )
        assertFalse(
            shouldSuppressDetailShellSharedBoundsForRelatedChildTransition(
                detailBvid = "BV_A",
                lastClickedVideoSourceKey = "video/BV_A:BV_B",
                isSharedTransitionActive = false,
            )
        )
        assertFalse(
            shouldSuppressDetailShellSharedBoundsForRelatedChildTransition(
                detailBvid = "BV_A",
                lastClickedVideoSourceKey = "home:BV_A",
                isSharedTransitionActive = true,
            )
        )
        assertFalse(
            shouldSuppressDetailShellSharedBoundsForRelatedChildTransition(
                detailBvid = "BV_A",
                lastClickedVideoSourceKey = "video/BV_OTHER:BV_B",
                isSharedTransitionActive = true,
            )
        )
    }
}
