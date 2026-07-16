package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.android.purebilibili.core.player.PlayerVolumeController
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.feature.audio.player.MusicPlayerUiState
import com.android.purebilibili.feature.audio.player.MusicLyricCandidateUi
import com.android.purebilibili.feature.audio.player.MusicQueueItemUi
import com.android.purebilibili.feature.audio.screen.MusicPlayerContent
import com.android.purebilibili.feature.audio.viewmodel.MusicViewModel
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class AudioPlaybackSnapshot(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

internal fun resolveAudioModeTrackTitle(
    videoTitle: String,
    currentCid: Long,
    pages: List<Page>
): String {
    return pages.firstOrNull { it.cid == currentCid }
        ?.part
        ?.takeIf { it.isNotBlank() }
        ?: videoTitle
}

internal data class AudioModeLyricMetadata(
    val title: String,
    val artist: String
)

internal fun resolveAudioModeLyricMetadata(
    trackTitle: String,
    fallbackArtist: String
): AudioModeLyricMetadata {
    val match = Regex(
        """^\s*(?:P?\d{1,4}\s*[.、:：_-]\s*)?(.+?)\s+[-–—]\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    ).matchEntire(trackTitle)
    return AudioModeLyricMetadata(
        title = match?.groupValues?.getOrNull(1)?.trim().orEmpty().ifBlank { trackTitle },
        artist = match?.groupValues?.getOrNull(2)?.trim().orEmpty().ifBlank { fallbackArtist }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioModeMusicPlayer(
    viewModel: VideoPlaybackViewModel,
    successState: VideoPlaybackUiState.Success?,
    player: Player?,
    onBack: () -> Unit,
    onVideoModeClick: (String, Long) -> Unit,
    isInPipMode: Boolean,
    showPipButton: Boolean,
    onEnterPip: () -> Unit,
    sleepTimerMinutes: Int?,
    titleOverride: String?,
    liquidGlassEffectsEnabled: Boolean
) {
    if (successState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val context = LocalContext.current
    val info = successState.info
    val displayTitle = resolveAudioModeTrackTitle(
        videoTitle = titleOverride?.takeIf { it.isNotBlank() } ?: info.title,
        currentCid = info.cid,
        pages = info.pages
    )
    val playlist by PlaylistManager.playlist.collectAsStateWithLifecycle()
    val playlistIndex by PlaylistManager.currentIndex.collectAsStateWithLifecycle()
    val playMode by PlaylistManager.playMode.collectAsStateWithLifecycle()
    val playback = rememberAudioPlaybackSnapshot(player)
    val lyricsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<MusicViewModel>(
        key = "audio_mode_lyrics"
    )
    val lyricsState by lyricsViewModel.uiState.collectAsStateWithLifecycle()
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showPageSelector by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val metadataDurationMs = info.pages
        .firstOrNull { it.cid == info.cid }
        ?.duration
        ?.times(1_000L)
        ?: 0L
    val lyricsDurationMs = playback.durationMs.takeIf { it > 0L } ?: metadataDurationMs
    val lyricMetadata = remember(displayTitle, info.owner.name) {
        resolveAudioModeLyricMetadata(displayTitle, info.owner.name)
    }

    LaunchedEffect(Unit) {
        lyricsViewModel.initPlayer(context)
    }
    LaunchedEffect(info.bvid, info.cid, displayTitle, info.owner.name, lyricsDurationMs) {
        lyricsViewModel.loadLyricsForVideo(
            title = lyricMetadata.title,
            artist = lyricMetadata.artist,
            bvid = info.bvid,
            cid = info.cid,
            durationMs = lyricsDurationMs
        )
    }

    val queue = if (playlist.isEmpty()) {
        listOf(
            MusicQueueItemUi(
                stableId = "video:${info.bvid}:${info.cid}",
                title = displayTitle,
                artist = info.owner.name,
                coverUrl = FormatUtils.fixImageUrl(info.pic)
            )
        )
    } else {
        playlist.mapIndexed { index, item ->
            MusicQueueItemUi(
                stableId = "video:${item.bvid}:$index",
                title = item.title,
                artist = item.owner,
                coverUrl = FormatUtils.fixImageUrl(item.cover)
            )
        }
    }
    val currentIndex = playlistIndex.takeIf { it in queue.indices } ?: 0
    val coverUrl = queue.getOrNull(currentIndex)?.coverUrl ?: FormatUtils.fixImageUrl(info.pic)

    MusicPlayerContent(
        state = MusicPlayerUiState(
            title = displayTitle,
            artist = info.owner.name,
            coverUrl = coverUrl,
            isLoading = player == null,
            isPlaying = playback.isPlaying,
            isBuffering = playback.isBuffering,
            positionMs = playback.positionMs,
            durationMs = playback.durationMs.takeIf { it > 0L } ?: metadataDurationMs,
            lyrics = lyricsState.lyricsDocument,
            lyricsError = lyricsState.lyricsError,
            lyricCandidates = lyricsState.lyricCandidates.map {
                MusicLyricCandidateUi(it.title, it.artist, it.source.name)
            },
            isLyricsSearching = lyricsState.isLyricsSearching,
            queue = queue,
            currentQueueIndex = currentIndex,
            playMode = playMode
        ),
        onBack = onBack,
        onPlayPause = { player?.handleAudioModePlayPause() },
        onSeek = { positionMs ->
            player?.seekTo(positionMs)
            player?.let(PlayerVolumeController::applyPreferredVolume)
        },
        onPrevious = { viewModel.playPreviousAudioModeTrack() },
        onNext = { viewModel.playNextAudioModeTrack() },
        onQueueItemSelected = { index ->
            PlaylistManager.playAt(index)?.let {
                viewModel.loadVideo(
                    bvid = it.bvid,
                    autoPlay = resolveAudioModePageSwitchAutoPlay()
                )
            }
        },
        onPlayModeChange = PlaylistManager::setPlayMode,
        onLyricsOffsetChange = lyricsViewModel::adjustLyricsOffset,
        onLyricsRetry = lyricsViewModel::retryLyrics,
        onLyricsSearch = lyricsViewModel::searchLyrics,
        onLyricsCandidateSelected = lyricsViewModel::selectLyricsCandidate,
        onVideoModeClick = { onVideoModeClick(info.bvid, info.cid) },
        onCollectionClick = when {
            info.pages.size > 1 -> ({ showPageSelector = true })
            info.ugc_season != null -> ({ showCollectionSheet = true })
            else -> null
        },
        onSleepTimerClick = { showSleepTimerDialog = true },
        sleepTimerLabel = formatAudioModeSleepTimerButtonLabel(sleepTimerMinutes),
        onPipClick = if (showPipButton) onEnterPip else null,
        isInPipMode = isInPipMode,
        liquidGlassEffectsEnabled = liquidGlassEffectsEnabled
    )

    if (showPageSelector && info.pages.size > 1) {
        ModalBottomSheet(onDismissRequest = { showPageSelector = false }) {
            PagesSelector(
                pages = info.pages,
                currentPageIndex = info.pages.indexOfFirst { it.cid == info.cid }.coerceAtLeast(0),
                forceGridMode = true,
                onDismissRequest = { showPageSelector = false },
                onPageSelect = { index ->
                    info.pages.getOrNull(index)?.let { page ->
                        showPageSelector = false
                        viewModel.loadVideo(
                            bvid = info.bvid,
                            cid = page.cid,
                            autoPlay = resolveAudioModePageSwitchAutoPlay()
                        )
                    }
                }
            )
        }
    }

    info.ugc_season?.let { season ->
        if (showCollectionSheet) {
            CollectionSheet(
                ugcSeason = season,
                currentBvid = info.bvid,
                currentCid = info.cid,
                onDismiss = { showCollectionSheet = false },
                onEpisodeClick = { episode ->
                    showCollectionSheet = false
                    viewModel.loadVideo(
                        bvid = episode.bvid,
                        cid = episode.cid,
                        autoPlay = resolveAudioModeCollectionSwitchAutoPlay()
                    )
                }
            )
        }
    }

    if (showSleepTimerDialog) {
        AudioModeSleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            onDismiss = { showSleepTimerDialog = false },
            onSelectPreset = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onConfirmCustom = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimerDialog = false
            }
        )
    }
}

@Composable
private fun rememberAudioPlaybackSnapshot(player: Player?): AudioPlaybackSnapshot {
    var snapshot by remember(player) { mutableStateOf(player.readAudioPlaybackSnapshot()) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = player.readAudioPlaybackSnapshot()
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (isActive && player != null) {
            snapshot = player.readAudioPlaybackSnapshot()
            delay(250L)
        }
    }
    return snapshot
}

private fun Player?.readAudioPlaybackSnapshot(): AudioPlaybackSnapshot {
    val player = this ?: return AudioPlaybackSnapshot()
    return AudioPlaybackSnapshot(
        isPlaying = player.isPlaying,
        isBuffering = player.playbackState == Player.STATE_BUFFERING,
        positionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.coerceAtLeast(0L)
    )
}
