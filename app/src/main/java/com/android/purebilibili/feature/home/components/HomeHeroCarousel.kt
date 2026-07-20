package com.android.purebilibili.feature.home.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.feature.home.components.cards.videoCardShellReturnChromeAlpha
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.home.HomeHeroCarouselCardTransform
import com.android.purebilibili.feature.home.HOME_HERO_CAROUSEL_SIDE_PEEK_DP
import com.android.purebilibili.feature.home.resolveHomeHeroCarouselAspectRatio
import com.android.purebilibili.feature.home.resolveHomeHeroCarouselCardTransform
import com.android.purebilibili.feature.home.resolveHomeHeroCarouselItemKey
import com.android.purebilibili.feature.home.resolveHomeHeroCarouselItemOrNull
import com.android.purebilibili.feature.home.resolveHomeHeroCarouselPreviewAlpha
import com.android.purebilibili.feature.home.resolveHomeHeroCarouselWidthDp
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeHeroCarousel(
    videos: List<VideoItem>,
    autoplayEnabled: Boolean,
    onVideoClick: (VideoItem) -> Unit,
    onGetPreviewUrl: suspend (String, Long) -> String?,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) return

    val pagerState = rememberPagerState { videos.size }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val sidePeek = HOME_HERO_CAROUSEL_SIDE_PEEK_DP.dp
        val carouselWidth = resolveHomeHeroCarouselWidthDp(maxWidth.value).dp
        val pageWidth = (carouselWidth - sidePeek * 2).coerceAtLeast(0.dp)
        val aspectRatio = resolveHomeHeroCarouselAspectRatio(carouselWidth.value)
        HorizontalPager(
            state = pagerState,
            key = { page ->
                resolveHomeHeroCarouselItemKey(videos, page, VideoItem::bvid)
            },
            pageSize = PageSize.Fixed(pageWidth),
            pageSpacing = 0.dp,
            contentPadding = PaddingValues(horizontal = sidePeek),
            modifier = Modifier
                .width(carouselWidth)
                .align(Alignment.Center)
        ) { page ->
            val video = resolveHomeHeroCarouselItemOrNull(videos, page)
                ?: return@HorizontalPager
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).coerceIn(-1f, 1f)
            val transform = resolveHomeHeroCarouselCardTransform(pageOffset)
            val activeForPlayback = autoplayEnabled &&
                pagerState.currentPage == page &&
                pageOffset.absoluteValue < 0.12f
            HomeHeroCarouselCard(
                video = video,
                transform = transform,
                activeForPlayback = activeForPlayback,
                aspectRatio = aspectRatio,
                onVideoClick = { onVideoClick(video) },
                onGetPreviewUrl = onGetPreviewUrl
            )
        }

        Row(
            modifier = Modifier
                .width(carouselWidth)
                .align(Alignment.BottomCenter)
                .padding(start = 28.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            videos.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 11.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.46f)
                            }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeHeroCarouselCard(
    video: VideoItem,
    transform: HomeHeroCarouselCardTransform,
    activeForPlayback: Boolean,
    aspectRatio: Float,
    onVideoClick: () -> Unit,
    onGetPreviewUrl: suspend (String, Long) -> String?
) {
    var previewUrl by remember(video.bvid, video.cid) { mutableStateOf<String?>(null) }
    LaunchedEffect(activeForPlayback, video.bvid, video.cid) {
        if (activeForPlayback && previewUrl == null && video.bvid.isNotBlank() && video.cid > 0L) {
            previewUrl = onGetPreviewUrl(video.bvid, video.cid)
        }
    }

    // —— ???????? ——
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val useCardShellSharedBounds = LocalSharedTransitionEnabled.current &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null &&
        video.bvid.isNotBlank() &&
        !sourceRoute.isNullOrBlank()
    val cardShellMotionSpec = remember(
        sourceRoute,
        useCardShellSharedBounds,
        sharedTransitionSpeedSettings
    ) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRoute,
            transitionEnabled = useCardShellSharedBounds,
            speedSettings = sharedTransitionSpeedSettings
        )
    }

    // ??????? CardPositionManager ????????
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx: Float
    val screenHeightPx: Float
    val densityValue: Float
    remember(configuration.screenWidthDp, configuration.screenHeightDp, density) {
        Triple(
            with(density) { configuration.screenWidthDp.dp.toPx() },
            with(density) { configuration.screenHeightDp.dp.toPx() },
            density.density
        )
    }.let { (w, h, d) ->
        screenWidthPx = w
        screenHeightPx = h
        densityValue = d
    }

    // ????????????????????
    val cardCoordsRef = remember { object { var value: LayoutCoordinates? = null } }

    val cardShape = AppShapes.container(ContainerLevel.Card)

    // ????????????????
    val clickAction: () -> Unit = {
        cardCoordsRef.value?.takeIf { it.isAttached }?.boundsInRoot()?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = densityValue
            )
        }
        onVideoClick()
    }

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = (transform.shadowElevationFraction * 10f).dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                motionSpec = cardShellMotionSpec,
                clipShape = cardShape
            )
            .zIndex(transform.zIndex)
            .graphicsLayer {
                transformOrigin = TransformOrigin(transform.pivotFractionX, 0.5f)
                translationX = transform.translationXFraction * size.width
                scaleX = transform.scale
                scaleY = transform.scale
                alpha = transform.alpha
            }
            .clip(cardShape)
            .onGloballyPositioned { coordinates ->
                cardCoordsRef.value = coordinates
            }
            .clickable(onClick = clickAction)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val normalizedCoverUrl = remember(video.pic) { FormatUtils.fixImageUrl(video.pic) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = transform.contentParallaxFraction * size.width
                        scaleX = transform.contentScale
                        scaleY = transform.contentScale
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(normalizedCoverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (activeForPlayback && previewUrl != null) {
                    MutedHeroVideoPlayer(url = previewUrl.orEmpty())
                }
            }
            if (transform.edgeShadeAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (transform.edgeShadeStartFromLeft) {
                                Brush.horizontalGradient(
                                    0f to Color.Black.copy(alpha = transform.edgeShadeAlpha),
                                    0.48f to Color.Transparent,
                                    1f to Color.Transparent
                                )
                            } else {
                                Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    0.52f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = transform.edgeShadeAlpha)
                                )
                            }
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.54f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.76f)
                        )
                    )
            )
            // ????????????????????
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, bottom = 14.dp)
                    .videoCardShellReturnChromeAlpha(
                        enabled = useCardShellSharedBounds,
                        bvid = video.bvid,
                        sourceRoute = sourceRoute,
                    )
            ) {
                // ??
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeForPlayback) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = video.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // ???????????
                if (video.duration > 0 || video.stat.view > 0 || video.stat.danmaku > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                    var separatorNeeded = false
                    // ??
                    if (video.duration > 0) {
                        Text(
                            text = FormatUtils.formatDuration(video.duration),
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.wrapContentSize()
                        )
                        separatorNeeded = true
                    }
                    // ???
                    if (video.stat.view > 0) {
                        if (separatorNeeded) Text(
                            " · ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = FormatUtils.formatStat(video.stat.view.toLong()) + "??",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.wrapContentSize()
                        )
                        separatorNeeded = true
                    }
                    // ??
                    if (video.stat.danmaku > 0) {
                        if (separatorNeeded) Text(
                            " · ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = FormatUtils.formatStat(video.stat.danmaku.toLong()) + "??",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MutedHeroVideoPlayer(url: String) {
    val context = LocalContext.current
    var hasRenderedFirstFrame by remember(url) { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    LaunchedEffect(url) {
        hasRenderedFirstFrame = false
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = resolveHomeHeroCarouselPreviewAlpha(hasRenderedFirstFrame)
            }
    )
}
