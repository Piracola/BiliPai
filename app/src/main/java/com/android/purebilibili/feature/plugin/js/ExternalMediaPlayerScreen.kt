package com.android.purebilibili.feature.plugin.js

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.plugin.js.ExternalMediaLaunchStore
import com.android.purebilibili.core.ui.rememberAppBackIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalMediaPlayerScreen(
    launchId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val request = remember(launchId) { ExternalMediaLaunchStore.get(launchId) }
    var selectedIndex by remember(request) { mutableIntStateOf(request?.selectedStreamIndex ?: 0) }
    val stream = request?.streams?.getOrNull(selectedIndex)
    val dataSourceFactory = remember(stream?.headers) {
        DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(stream?.headers.orEmpty())
        )
    }
    val player = remember(context, dataSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(stream?.url) {
        val current = stream ?: return@LaunchedEffect
        val mediaItem = MediaItem.Builder()
            .setUri(current.url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(request?.title.orEmpty())
                    .build()
            )
            .setMimeType(resolveExternalMediaMimeType(current.url, current.contentType))
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(request?.title ?: "外部媒体") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (request == null || request.streams.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "播放请求已失效，请从插件内容重新打开",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = player
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                request.streams.forEachIndexed { index, mediaStream ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { Text(mediaStream.title.ifBlank { "线路 ${index + 1}" }) }
                    )
                }
            }
        }
    }
}

private fun resolveExternalMediaMimeType(
    url: String,
    contentType: String?
): String? {
    val declared = contentType?.lowercase()?.takeIf { it.isNotBlank() }
    return when {
        declared?.contains("mpegurl") == true || declared?.contains("hls") == true -> MimeTypes.APPLICATION_M3U8
        url.substringBefore("?").endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        url.substringBefore("?").endsWith(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
        else -> null
    }
}
