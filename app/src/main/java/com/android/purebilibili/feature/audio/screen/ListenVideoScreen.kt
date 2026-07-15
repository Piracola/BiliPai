package com.android.purebilibili.feature.audio.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.feature.audio.library.ListenVideoAlbum
import com.android.purebilibili.feature.audio.library.ListenVideoArtist
import com.android.purebilibili.feature.audio.library.ListenVideoPlaylist
import com.android.purebilibili.feature.audio.library.ListenVideoSection
import com.android.purebilibili.feature.audio.library.ListenVideoTrack
import com.android.purebilibili.feature.audio.viewmodel.ListenVideoUiState
import com.android.purebilibili.feature.audio.viewmodel.ListenVideoViewModel
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal data class ListenVideoNowPlaying(
    val bvid: String,
    val title: String,
    val artist: String,
    val coverUrl: String
)

@Composable
internal fun ListenVideoRoute(
    onPlayTracks: (List<ListenVideoTrack>, String) -> Unit,
    onNowPlayingClick: (bvid: String, coverUrl: String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListenVideoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistState by PlaylistManager.uiState.collectAsStateWithLifecycle(
        initialValue = com.android.purebilibili.feature.video.player.PlaylistUiState()
    )
    val nowPlaying = remember(playlistState.playlist, playlistState.currentIndex) {
        playlistState.playlist.getOrNull(playlistState.currentIndex)?.let { item ->
            ListenVideoNowPlaying(
                bvid = item.bvid,
                title = item.title,
                artist = item.owner,
                coverUrl = item.cover
            )
        }
    }

    LaunchedEffect(viewModel) {
        if (viewModel.uiState.value.generation == 0L) {
            viewModel.refresh()
        }
    }

    ListenVideoScreen(
        state = state,
        nowPlaying = nowPlaying,
        onNowPlayingClick = onNowPlayingClick,
        onRefresh = viewModel::refresh,
        onSectionSelected = viewModel::selectSection,
        onPlaylistSelected = viewModel::openPlaylist,
        onAlbumSelected = viewModel::openAlbum,
        onArtistSelected = viewModel::openArtist,
        onCloseDetail = viewModel::closeDetail,
        onRetryDetail = viewModel::retryDetail,
        onRetryIndex = viewModel::retryFailedIndex,
        onTrackSelected = onPlayTracks,
        onLogin = onLogin,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListenVideoScreen(
    state: ListenVideoUiState,
    nowPlaying: ListenVideoNowPlaying?,
    onNowPlayingClick: (bvid: String, coverUrl: String) -> Unit,
    onRefresh: () -> Unit,
    onSectionSelected: (ListenVideoSection) -> Unit,
    onPlaylistSelected: (ListenVideoPlaylist) -> Unit,
    onAlbumSelected: (ListenVideoAlbum) -> Unit,
    onArtistSelected: (ListenVideoArtist) -> Unit,
    onCloseDetail: () -> Unit,
    onRetryDetail: () -> Unit,
    onRetryIndex: () -> Unit,
    onTrackSelected: (List<ListenVideoTrack>, String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sectionLabels = remember { listOf("播放列表", "专辑", "歌手") }
    val pagerState = rememberPagerState(
        initialPage = state.section.ordinal,
        pageCount = { ListenVideoSection.entries.size }
    )
    val scope = rememberCoroutineScope()
    val selectionBackdrop = rememberLayerBackdrop()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> onSectionSelected(ListenVideoSection.entries[page]) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
    ) {
        val layout = resolveListenVideoLayout(maxWidth.value.toInt())
        Column(modifier = Modifier.fillMaxSize()) {
            ListenVideoHeader(
                nowPlaying = nowPlaying,
                onNowPlayingClick = onNowPlayingClick,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
            BottomBarLiquidSegmentedControl(
                items = sectionLabels,
                selectedIndex = pagerState.currentPage,
                onSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                height = 52.dp,
                indicatorHeight = 46.dp,
                forceLiquidChrome = true,
                preferInlineContentStyle = false,
                backdrop = selectionBackdrop,
                indicatorPositionProvider = {
                    pagerState.currentPage + pagerState.currentPageOffsetFraction
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .layerBackdrop(selectionBackdrop)
            ) { page ->
                ListenVideoPage(
                    section = ListenVideoSection.entries[page],
                    state = state,
                    layout = layout,
                    onPlaylistSelected = onPlaylistSelected,
                    onAlbumSelected = onAlbumSelected,
                    onArtistSelected = onArtistSelected,
                    onRetryIndex = onRetryIndex,
                    onLogin = onLogin,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (state.selectedTitle.isNotBlank()) {
        ModalBottomSheet(
            onDismissRequest = onCloseDetail,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ) {
            ListenVideoTrackSheet(
                title = state.selectedTitle,
                tracks = state.selectedTracks,
                isLoading = state.isDetailLoading,
                error = state.detailError,
                onClose = onCloseDetail,
                onRetry = onRetryDetail,
                onTrackSelected = onTrackSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ListenVideoHeader(
    nowPlaying: ListenVideoNowPlaying?,
    onNowPlayingClick: (bvid: String, coverUrl: String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "听视频",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "收藏夹是播放列表，视频集是专辑，UP 主是歌手",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新音乐资料")
            }
        }
        nowPlaying?.let { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNowPlayingClick(item.bvid, item.coverUrl) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "正在播放",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Filled.MusicNote, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ListenVideoPage(
    section: ListenVideoSection,
    state: ListenVideoUiState,
    layout: ListenVideoLayout,
    onPlaylistSelected: (ListenVideoPlaylist) -> Unit,
    onAlbumSelected: (ListenVideoAlbum) -> Unit,
    onArtistSelected: (ListenVideoArtist) -> Unit,
    onRetryIndex: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        !state.isLoggedIn -> ListenVideoMessage(
            title = "登录后查看音乐资料",
            actionLabel = "去登录",
            onAction = onLogin,
            modifier = modifier
        )

        state.isLoading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.error != null && state.playlists.isEmpty() -> ListenVideoMessage(
            title = state.error,
            actionLabel = "重试",
            onAction = onRetryIndex,
            modifier = modifier
        )

        else -> when (section) {
            ListenVideoSection.PLAYLISTS -> ListenVideoPlaylistPage(
                playlists = state.playlists,
                layout = layout,
                onSelected = onPlaylistSelected,
                modifier = modifier
            )

            ListenVideoSection.ALBUMS -> ListenVideoAlbumPage(
                albums = state.albums,
                layout = layout,
                isIndexing = state.isIndexing,
                indexedCount = state.indexedFolderCount,
                totalCount = state.totalFolderCount,
                onSelected = onAlbumSelected,
                modifier = modifier
            )

            ListenVideoSection.ARTISTS -> ListenVideoArtistPage(
                artists = state.artists,
                layout = layout,
                isIndexing = state.isIndexing,
                indexedCount = state.indexedFolderCount,
                totalCount = state.totalFolderCount,
                failedFolderCount = state.failedFolderIds.size,
                onRetryIndex = onRetryIndex,
                onSelected = onArtistSelected,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ListenVideoPlaylistPage(
    playlists: List<ListenVideoPlaylist>,
    layout: ListenVideoLayout,
    onSelected: (ListenVideoPlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) {
        ListenVideoMessage("还没有可播放的收藏夹", modifier = modifier)
        return
    }
    if (layout == ListenVideoLayout.WIDE_GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(220.dp),
            modifier = modifier,
            contentPadding = libraryContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(playlists, key = ListenVideoPlaylist::mediaId) { playlist ->
                MusicEntityCard(
                    title = playlist.title,
                    subtitle = "${playlist.trackCount} 首 · 播放列表",
                    coverUrl = playlist.coverUrl,
                    fallbackIcon = Icons.Filled.LibraryMusic,
                    onClick = { onSelected(playlist) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = libraryContentPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(playlists, key = ListenVideoPlaylist::mediaId) { playlist ->
                MusicEntityCard(
                    title = playlist.title,
                    subtitle = "${playlist.trackCount} 首 · 播放列表",
                    coverUrl = playlist.coverUrl,
                    fallbackIcon = Icons.Filled.LibraryMusic,
                    onClick = { onSelected(playlist) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ListenVideoAlbumPage(
    albums: List<ListenVideoAlbum>,
    layout: ListenVideoLayout,
    isIndexing: Boolean,
    indexedCount: Int,
    totalCount: Int,
    onSelected: (ListenVideoAlbum) -> Unit,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty() && !isIndexing) {
        ListenVideoMessage("收藏夹中还没有视频合集", modifier = modifier)
        return
    }
    Column(modifier = modifier) {
        ListenVideoIndexProgress(isIndexing, indexedCount, totalCount)
        if (layout == ListenVideoLayout.WIDE_GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier.weight(1f),
                contentPadding = libraryContentPadding(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums, key = ListenVideoAlbum::seasonId) { album ->
                    MusicEntityCard(
                        title = album.title,
                        subtitle = album.artistName.ifBlank { "${album.trackCount} 首 · 专辑" },
                        coverUrl = album.coverUrl,
                        fallbackIcon = Icons.Filled.Album,
                        onClick = { onSelected(album) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = libraryContentPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(albums, key = ListenVideoAlbum::seasonId) { album ->
                    MusicEntityCard(
                        title = album.title,
                        subtitle = album.artistName.ifBlank { "${album.trackCount} 首 · 专辑" },
                        coverUrl = album.coverUrl,
                        fallbackIcon = Icons.Filled.Album,
                        onClick = { onSelected(album) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ListenVideoArtistPage(
    artists: List<ListenVideoArtist>,
    layout: ListenVideoLayout,
    isIndexing: Boolean,
    indexedCount: Int,
    totalCount: Int,
    failedFolderCount: Int,
    onRetryIndex: () -> Unit,
    onSelected: (ListenVideoArtist) -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty() && !isIndexing) {
        ListenVideoMessage(
            title = if (failedFolderCount > 0) "部分收藏夹索引失败" else "收藏夹中还没有可识别的 UP 主",
            actionLabel = if (failedFolderCount > 0) "重试" else null,
            onAction = onRetryIndex,
            modifier = modifier
        )
        return
    }
    Column(modifier = modifier) {
        ListenVideoIndexProgress(isIndexing, indexedCount, totalCount)
        if (layout == ListenVideoLayout.WIDE_GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier.weight(1f),
                contentPadding = libraryContentPadding(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(artists, key = ListenVideoArtist::mid) { artist ->
                    MusicEntityCard(
                        title = artist.name,
                        subtitle = "${artist.tracks.size} 首 · 歌手",
                        coverUrl = artist.avatarUrl,
                        fallbackIcon = Icons.Filled.Person,
                        circularCover = true,
                        onClick = { onSelected(artist) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = libraryContentPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(artists, key = ListenVideoArtist::mid) { artist ->
                    MusicEntityCard(
                        title = artist.name,
                        subtitle = "${artist.tracks.size} 首 · 歌手",
                        coverUrl = artist.avatarUrl,
                        fallbackIcon = Icons.Filled.Person,
                        circularCover = true,
                        onClick = { onSelected(artist) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ListenVideoIndexProgress(
    isIndexing: Boolean,
    indexedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    if (!isIndexing) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = "正在整理收藏夹 $indexedCount/$totalCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MusicEntityCard(
    title: String,
    subtitle: String,
    coverUrl: String,
    fallbackIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circularCover: Boolean = false
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(if (circularCover) CircleShape else RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
        }
    }
}

@Composable
private fun ListenVideoMessage(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        actionLabel?.let { label ->
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction, modifier = Modifier.height(48.dp)) {
                Text(label)
            }
        }
    }
}

@Composable
private fun ListenVideoTrackSheet(
    title: String,
    tracks: List<ListenVideoTrack>,
    isLoading: Boolean,
    error: String?,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onTrackSelected: (List<ListenVideoTrack>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.navigationBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "关闭曲目列表")
            }
        }
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null -> ListenVideoMessage(
                title = error,
                actionLabel = "重试",
                onAction = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            tracks.isEmpty() -> ListenVideoMessage(
                title = "这个列表中没有可播放的视频",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(tracks, key = ListenVideoTrack::bvid) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackSelected(tracks, track.bvid) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                track.artistName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.PlayArrow, contentDescription = "播放 ${track.title}")
                    }
                }
            }
        }
    }
}

private fun libraryContentPadding(): PaddingValues {
    return PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 120.dp)
}
