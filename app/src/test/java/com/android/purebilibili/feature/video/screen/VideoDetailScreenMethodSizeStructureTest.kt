package com.android.purebilibili.feature.video.screen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailScreenMethodSizeStructureTest {

    @Test
    fun publicVideoDetailScreenRemainsAThinEntryPoint() {
        val source = loadSource("VideoDetailScreen.kt")

        assertTrue(source.lineSequence().count() <= 350)
        assertTrue(source.contains("fun VideoDetailScreen("))
        assertTrue(source.contains("VideoDetailScreenStateHolder("))
        assertFalse(source.contains("collectAsStateWithLifecycle"))
        assertFalse(source.contains("LaunchedEffect("))
        assertFalse(source.contains("VideoDetailRouteSheetHost("))
    }

    @Test
    fun renderingEntrypointsDoNotAcceptViewModels() {
        listOf(
            "VideoDetailScreenContent.kt",
            "VideoDetailPlayerTransitionHost.kt",
            "VideoDetailPhoneContent.kt",
            "TabletVideoLayout.kt",
            "TabletCinemaLayout.kt"
        ).forEach { name ->
            val source = loadSource(name)
            assertFalse(source.contains("ViewModel"), "$name must remain ViewModel-free")
            assertFalse(source.contains("collectAsStateWithLifecycle"), "$name must not collect business state")
        }
    }

    @Test
    fun playerAndDetailContentShareTheRootTransitionProgress() {
        val holder = loadSource("VideoDetailScreenStateHolder.kt")
        val transitionHost = loadSource("VideoDetailTransitionHost.kt")
        val content = loadSource("VideoDetailScreenContent.kt")

        assertTrue(transitionHost.contains("label = \"video-detail-shared-transition-progress\""))
        assertTrue(holder.contains("val detailTransitionProgress = transitionState.progress"))
        assertTrue(holder.contains("resolveVideoDetailReturnCoverAlpha("))
        assertTrue(holder.contains("resolveVideoDetailReturnPlayerAlpha("))
        assertTrue(holder.contains("resolveVideoDetailReturnContentAlpha("))
        assertTrue(content.contains("transitionState.routeSheetFrameProvider"))
    }

    @Test
    fun transitionConstantsLiveInTheTransitionPolicy() {
        val policy = loadSource("VideoDetailTransitionPolicy.kt")
        val host = loadSource("VideoDetailTransitionHost.kt")
        val entry = loadSource("VideoDetailScreen.kt")

        assertTrue(policy.contains("HOME_VIDEO_ROUTE_SHEET_MAIN_DURATION_MILLIS = 320"))
        assertTrue(policy.contains("HOME_VIDEO_ROUTE_SHEET_SETTLE_DURATION_MILLIS"))
        assertFalse(host.contains("HOME_VIDEO_ROUTE_SHEET_MAIN_DURATION_MILLIS"))
        assertFalse(entry.contains("HOME_VIDEO_ROUTE_SHEET_MAIN_DURATION_MILLIS"))
    }

    @Test
    fun largeDialogsAndPlayerHostLiveOutsideTheStateHolder() {
        val holder = loadSource("VideoDetailScreenStateHolder.kt")
        val overlays = loadSource("VideoDetailOverlayHost.kt")
        val playerHost = loadSource("VideoDetailPlayerTransitionHost.kt")

        assertFalse(holder.contains("internal fun VideoDetailFollowGroupDialog("))
        assertFalse(holder.contains("internal fun PortraitInlineVideoPlayerHost("))
        assertTrue(overlays.contains("internal fun VideoDetailFollowGroupDialog("))
        assertTrue(overlays.contains("internal fun VideoDetailPlaybackEndedDialog("))
        assertTrue(playerHost.contains("internal fun PortraitInlineVideoPlayerHost("))
    }

    @Test
    fun platformAndPlaybackEffectsHaveDedicatedBoundedHosts() {
        val platformEffects = loadSource("VideoDetailPlatformEffectsHost.kt")
        val playbackEffects = loadSource("VideoDetailPlaybackSessionEffects.kt")
        val holder = loadSource("VideoDetailScreenStateHolder.kt")

        assertTrue(platformEffects.lineSequence().count() <= 700)
        assertTrue(playbackEffects.lineSequence().count() <= 700)
        assertTrue(platformEffects.contains("VideoDetailPipParamsEffect("))
        assertTrue(platformEffects.contains("VideoDetailKeepScreenOnEffect("))
        assertTrue(platformEffects.contains("VideoDetailSystemBarsEffect("))
        assertTrue(playbackEffects.contains("LaunchedEffect(viewModel, state)"))
        assertFalse(holder.contains("LaunchedEffect(videoPlayerBounds, pipModeEnabled)"))
        assertFalse(holder.contains("DisposableEffect(window, shouldKeepVideoScreenAwake)"))
    }

    @Test
    fun inlinePlayerAnimationUsesDeferredGraphicsLayerReads() {
        val holder = loadSource("VideoDetailScreenStateHolder.kt")
        val playerHost = loadSource("VideoDetailPlayerTransitionHost.kt")

        assertTrue(holder.contains("val inlinePlayerAlpha = animateFloatAsState("))
        assertTrue(holder.contains("val inlinePlayerScale = animateFloatAsState("))
        assertTrue(playerHost.contains("inlinePlayerAlpha: State<Float>"))
        assertTrue(playerHost.contains("inlinePlayerScale: State<Float>"))
        assertTrue(playerHost.contains("alpha = inlinePlayerAlpha.value"))
        assertTrue(playerHost.contains("scaleX = inlinePlayerScale.value"))
        assertFalse(playerHost.contains(".alpha(inlinePlayerAlpha)"))
    }

    @Test
    fun inputAndDownloadOverlayFlowsLiveInBoundedAdapters() {
        val holder = loadSource("VideoDetailScreenStateHolder.kt")
        val inputAdapter = loadSource("VideoDetailInputOverlayAdapter.kt")
        val downloadAdapter = loadSource("VideoDetailDownloadOverlayAdapter.kt")

        assertTrue(inputAdapter.lineSequence().count() <= 700)
        assertTrue(downloadAdapter.lineSequence().count() <= 700)
        assertTrue(inputAdapter.contains("VideoDetailDanmakuInputOverlayContent("))
        assertTrue(inputAdapter.contains("VideoDetailCommentInputOverlayContent("))
        assertTrue(inputAdapter.contains("snapshot: DanmakuInputSnapshot"))
        assertTrue(inputAdapter.contains("snapshot: CommentInputSnapshot"))
        assertEquals(
            1,
            holder.split("viewModel.showDanmakuDialog.collectAsStateWithLifecycle()").size - 1,
            "only the fullscreen inline composer may collect this state in the holder",
        )
        assertFalse(holder.contains("viewModel.isSendingComment.collectAsStateWithLifecycle()"))
        assertFalse(holder.contains("viewModel.showDownloadDialog.collectAsStateWithLifecycle()"))
        assertTrue(holder.contains("VideoDetailInputOverlayAdapter("))
        assertTrue(holder.contains("VideoDetailDownloadOverlayAdapter("))
    }

    @Test
    fun portraitPagerOverlayLivesInItsAdapterWithOriginalMotionSpec() {
        val holder = loadSource("VideoDetailScreenStateHolder.kt")
        val adapter = loadSource("VideoDetailPortraitOverlayAdapter.kt")

        assertTrue(adapter.lineSequence().count() <= 350)
        assertTrue(adapter.contains("PortraitVideoPager("))
        assertTrue(adapter.contains("motionSpec.enterDurationMillis"))
        assertTrue(adapter.contains("motionSpec.exitDurationMillis"))
        assertTrue(adapter.contains("motionSpec.exitScaleTarget"))
        assertTrue(adapter.contains("motionSpec.exitTranslateUpFraction"))
        assertFalse(holder.contains("PortraitVideoPager("))
        assertTrue(holder.contains("VideoDetailPortraitOverlayAdapter("))
    }

    @Test
    fun commonOverlayOrchestrationLivesOutsideTheStateHolder() {
        val holder = loadSource("VideoDetailScreenStateHolder.kt")
        val adapter = loadSource("VideoDetailCommonOverlayAdapter.kt")

        assertTrue(adapter.lineSequence().count() <= 350)
        assertTrue(adapter.contains("InteractiveChoiceOverlay("))
        assertTrue(adapter.contains("ExternalPlaylistQueueSheet("))
        assertTrue(adapter.contains("VideoShareSheet("))
        assertTrue(adapter.contains("VideoDetailPlaybackEndedDialog("))
        assertFalse(holder.contains("InteractiveChoiceOverlay("))
        assertFalse(holder.contains("VideoShareSheet("))
        assertTrue(holder.contains("VideoDetailCommonOverlayAdapter("))
    }

    private fun loadSource(name: String): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/screen/$name"),
            File("app/src/main/java/com/android/purebilibili/feature/video/screen/$name")
        )
        return candidates.first { it.exists() }.readText()
    }
}
