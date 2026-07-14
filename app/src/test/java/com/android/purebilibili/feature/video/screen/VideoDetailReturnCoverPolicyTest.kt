package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VideoDetailReturnCoverPolicyTest {

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
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val inlinePlayerCall = source
            .substringAfter("PortraitInlineVideoPlayerHost(", "")
            .substringAfter("PortraitInlineVideoPlayerHost(", "")
            .substringBefore("allowLivePlayerSharedElement = true")

        assertTrue(inlinePlayerCall.contains("liveBackPreview = bindLivePlayerForBackPreview"))
    }

    @Test
    fun `detail route does not manually fade its background during return`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
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
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
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
    fun `predictive exit alone does not switch detail into returning visual state`() {
        assertFalse(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = false
            )
        )
    }

    @Test
    fun `explicit return state switches detail into returning visual state`() {
        assertTrue(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = true
            )
        )
        assertFalse(
            shouldUseReturningVideoDetailVisualState(
                forceCoverOnlyForReturn = false
            )
        )
    }

    @Test
    fun `cover takeover delay keeps a one-frame budget before back navigation`() {
        assertEquals(16L, resolveCoverTakeoverDelayBeforeBackNavigationMillis())
    }

    @Test
    fun `navigation actions do not switch the loaded player to a cover`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val actionBlock = source
            .substringAfter("action@{ action: VideoDetailTopBarAction ->")
            .substringBefore("val handleBack =")

        assertFalse(actionBlock.contains("forceCoverOnlyOnReturn = true"))
        assertTrue(source.contains("useTextureSurfaceForNavigation = transitionEnabled"))
    }

    @Test
    fun `player container shared bounds are disabled during return to avoid cover key conflict`() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val playerContainerBlock = source
            .substringAfter("val playerContainerModifier = if (")
            .substringBefore(") {")
        assertTrue(
            "Player container must not claim the cover shared bounds during return; the forced return cover overlay owns that key.",
            playerContainerBlock.contains("!forceCoverOnlyForReturn")
        )
    }
}
