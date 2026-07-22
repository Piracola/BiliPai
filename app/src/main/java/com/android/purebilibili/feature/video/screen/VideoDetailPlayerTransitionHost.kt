package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.data.model.response.ViewPoint
import com.android.purebilibili.feature.video.progress.PbpProgressData
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.ui.section.VideoPlayerSection
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import kotlin.math.roundToInt

@Composable
internal fun PortraitInlineVideoPlayerHost(
    modifier: Modifier,
    animatedViewportWidth: Dp,
    animatedViewportHeight: Dp,
    inlinePlayerAlpha: State<Float>,
    inlinePlayerScale: State<Float>,
    playerState: VideoPlayerState,
    uiState: VideoPlaybackUiState,
    isPipMode: Boolean,
    transitionEnabled: Boolean,
    onToggleFullscreen: () -> Unit,
    playbackActions: VideoDetailPlaybackActions,
    onDoubleTapLike: () -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    videoPlayerSectionTarget: VideoPlayerSectionTarget,
    sponsorSegment: com.android.purebilibili.data.model.response.SponsorSegment?,
    showSponsorSkipButton: Boolean,
    sleepTimerMinutes: Int?,
    viewPoints: List<ViewPoint>,
    pbpProgressData: PbpProgressData?,
    sponsorProgressMarkers: List<com.android.purebilibili.data.model.response.SponsorProgressMarker>,
    isVerticalVideo: Boolean,
    onPortraitFullscreen: () -> Unit,
    isPortraitFullscreen: Boolean,
    onPipClick: () -> Unit,
    codecPreference: String,
    secondCodecPreference: String,
    audioQualityPreference: Int,
    onNavigateToAudioMode: () -> Unit,
    forceCoverOnly: Boolean,
    preserveCurrentFrameOnFullscreenChange: Boolean,
    liveBackPreview: Boolean,
    useTextureSurfaceForNavigation: Boolean,
    predictiveBackCancelRecoveryGeneration: Int,
    allowLivePlayerSharedElement: Boolean,
    sourceRouteForSharedElement: String?,
    suppressSubtitleOverlay: Boolean,
    subtitleDisplayModePreferenceOverride: SubtitleDisplayMode?,
    onSubtitleDisplayModePreferenceOverrideChange: (SubtitleDisplayMode) -> Unit
) {
    val successState = uiState as? VideoPlaybackUiState.Success

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedViewportHeight)
            .graphicsLayer {
                alpha = inlinePlayerAlpha.value
                scaleX = inlinePlayerScale.value
                scaleY = inlinePlayerScale.value
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
    ) {
        VideoPlayerSection(
            playerState = playerState,
            uiState = uiState,
            isFullscreen = false,
            isInPipMode = isPipMode,
            transitionEnabled = transitionEnabled,
            onToggleFullscreen = onToggleFullscreen,
            onQualityChange = { qid -> playbackActions.changeQuality(qid) },
            onBack = onBack,
            onHomeClick = onHomeClick,
            onDanmakuInputClick = { playbackActions.showDanmakuSendDialog() },
            bvid = videoPlayerSectionTarget.bvid,
            coverUrl = videoPlayerSectionTarget.entryCoverUrl,
            onDoubleTapLike = onDoubleTapLike,
            sponsorSegment = sponsorSegment,
            showSponsorSkipButton = showSponsorSkipButton,
            onSponsorSkip = { playbackActions.skipSponsorSegment() },
            onSponsorDismiss = { playbackActions.dismissSponsorSkipButton() },
            onReloadVideo = { playbackActions.reloadVideo() },
            currentCdnIndex = successState?.currentCdnIndex ?: 0,
            cdnCount = successState?.cdnCount ?: 1,
            cdnLineDiagnostics = successState?.cdnLineDiagnostics.orEmpty(),
            isCdnProbing = successState?.isCdnProbing ?: false,
            onSwitchCdn = { playbackActions.switchCdn() },
            onSwitchCdnTo = { playbackActions.switchCdnTo(it) },
            onProbeCdnCandidates = { playbackActions.probeCdnCandidates() },
            isAudioOnly = false,
            onAudioOnlyToggle = onNavigateToAudioMode,
            sleepTimerMinutes = sleepTimerMinutes,
            onSleepTimerChange = { playbackActions.setSleepTimer(it) },
            videoshotData = successState?.videoshotData,
            viewPoints = viewPoints,
            pbpProgressData = pbpProgressData,
            sponsorMarkers = sponsorProgressMarkers,
            onUserSeek = { position -> playbackActions.notifyExplicitSeek(position) },
            isVerticalVideo = isVerticalVideo,
            onPortraitFullscreen = onPortraitFullscreen,
            isPortraitFullscreen = isPortraitFullscreen,
            viewportWidthDpOverride = animatedViewportWidth.value.roundToInt(),
            onPipClick = onPipClick,
            currentCodec = codecPreference,
            onCodecChange = { playbackActions.setVideoCodec(it) },
            currentSecondCodec = secondCodecPreference,
            onSecondCodecChange = { playbackActions.setVideoSecondCodec(it) },
            currentAudioQuality = audioQualityPreference,
            onAudioQualityChange = { playbackActions.setAudioQuality(it) },
            onPlaybackSpeedChange = { playbackActions.applyPlaybackSpeed(it) },
            onAudioLangChange = { playbackActions.changeAudioLanguage(it) },
            onSaveCover = { playbackActions.saveCover() },
            onDownloadAudio = { playbackActions.downloadAudio() },
            forceCoverOnly = forceCoverOnly,
            preserveCurrentFrameOnFullscreenChange = preserveCurrentFrameOnFullscreenChange,
            liveBackPreview = liveBackPreview,
            useTextureSurfaceForNavigation = useTextureSurfaceForNavigation,
            predictiveBackCancelRecoveryGeneration = predictiveBackCancelRecoveryGeneration,
            allowLivePlayerSharedElement = allowLivePlayerSharedElement,
            sourceRouteForSharedElement = sourceRouteForSharedElement,
            suppressSubtitleOverlay = suppressSubtitleOverlay,
            subtitleDisplayModePreferenceOverride = subtitleDisplayModePreferenceOverride,
            onSubtitleDisplayModePreferenceOverrideChange = onSubtitleDisplayModePreferenceOverrideChange,
            onSubtitleTrackSelected = playbackActions.selectSubtitleTrack
        )
    }
}
