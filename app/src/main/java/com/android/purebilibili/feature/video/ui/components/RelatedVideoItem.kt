package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.feature.home.components.cards.videoCardShellReturnChromeAlpha
import com.android.purebilibili.feature.video.ui.FollowBadgeTone
import com.android.purebilibili.feature.video.ui.resolveVideoFollowVisualPolicy
import com.android.purebilibili.navigation.VideoRoute
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.BubbleLeft
import io.github.alexzhirkevich.cupertino.icons.filled.Play

/** 与首页 ElegantVideoCard 默认封面比例一致，便于复用整卡 shell。 */
internal const val RELATED_VIDEO_CARD_COVER_ASPECT_RATIO = 4f / 3f

internal const val RELATED_VIDEO_GRID_COLUMNS = 2

/**
 * Related Videos Header
 */
@Composable
fun RelatedVideosHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u66f4\u591a\u63a8\u8350",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveRelatedVideoCardPressScaleTarget(
    isPressed: Boolean,
    transitionEnabled: Boolean
): Float = 1f

@Suppress("UNUSED_PARAMETER")
internal fun shouldEnableRelatedVideoCoverCrossfade(
    transitionEnabled: Boolean
): Boolean = false

@Suppress("UNUSED_PARAMETER")
internal fun shouldTriggerRelatedVideoPressHaptic(
    isPressed: Boolean,
    transitionEnabled: Boolean
): Boolean = false

internal fun resolveRelatedVideoSharedElementSourceRoute(sourceRoute: String?): String {
    return sourceRoute
        ?.substringBefore("?")
        ?.takeIf { it.isNotBlank() }
        ?: VideoRoute.base
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldEnableRelatedVideoMetadataSharedBounds(
    transitionEnabled: Boolean
): Boolean = false

internal fun chunkRelatedVideosForHomeStyleGrid(
    videos: List<RelatedVideo>,
    columns: Int = RELATED_VIDEO_GRID_COLUMNS,
): List<List<RelatedVideo>> {
    val safeColumns = columns.coerceAtLeast(1)
    if (videos.isEmpty()) return emptyList()
    return videos.chunked(safeColumns)
}

/**
 * 相关推荐竖卡：上封面下标题，尺寸形态对齐首页，复用整卡 shell 一镜到底。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RelatedVideoItem(
    video: RelatedVideo,
    isFollowed: Boolean = false,
    transitionEnabled: Boolean = false,
    showUpBadge: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = transitionEnabled &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val densityValue = density.density
    val sourceRoute = resolveRelatedVideoSharedElementSourceRoute(
        LocalVideoCardSharedElementSourceRoute.current
    )
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val cardSharedTransitionMotionSpec = remember(sourceRoute, transitionEnabled, sharedTransitionSpeedSettings) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = sourceRoute,
            transitionEnabled = transitionEnabled,
            speedSettings = sharedTransitionSpeedSettings
        )
    }
    val cardBoundsRef = remember { object { var value: Rect? = null } }
    val triggerRelatedVideoClick = {
        cardBoundsRef.value?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                density = densityValue,
                sourceCornerDp = 12
            )
        }
        onClick()
    }
    val cardShape = RoundedCornerShape(12.dp)
    val coverShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    val useCardShellSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = coverSharedEnabled
    )
    val context = LocalContext.current
    val coverRequest = remember(video.pic, transitionEnabled) {
        ImageRequest.Builder(context)
            .data(FormatUtils.resolveVideoCoverUrl(video.pic, useLowQuality = false))
            .crossfade(shouldEnableRelatedVideoCoverCrossfade(transitionEnabled))
            .build()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = sourceRoute,
                motionSpec = cardSharedTransitionMotionSpec,
                clipShape = cardShape
            )
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .onGloballyPositioned { coordinates ->
                cardBoundsRef.value = coordinates.boundsInRoot()
            }
            .clickable(onClick = triggerRelatedVideoClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(RELATED_VIDEO_CARD_COVER_ASPECT_RATIO)
                .clip(coverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = coverRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = FormatUtils.formatDuration(video.duration),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        blurRadius = 4f,
                        offset = Offset(0f, 1f)
                    )
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .videoCardShellReturnChromeAlpha(
                    enabled = useCardShellSharedBounds,
                    bvid = video.bvid,
                    sourceRoute = sourceRoute,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            UpBadgeName(
                name = video.owner.name,
                badgeTrailingContent = if (isFollowed) {
                    {
                        val followVisualPolicy = resolveVideoFollowVisualPolicy(isFollowing = true)
                        Text(
                            text = "已关注",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = when (followVisualPolicy.relatedBadgeTone) {
                                FollowBadgeTone.PRIMARY -> MaterialTheme.colorScheme.primary
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                } else {
                    null
                },
                leadingContent = if (video.owner.face.isNotEmpty()) {
                    {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(FormatUtils.fixImageUrl(video.owner.face))
                                .crossfade(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                } else {
                    null
                },
                nameStyle = MaterialTheme.typography.labelMedium,
                nameColor = MaterialTheme.colorScheme.onSurfaceVariant,
                badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                badgeBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                showUpBadge = showUpBadge,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatItem(
                    icon = CupertinoIcons.Filled.Play,
                    text = FormatUtils.formatStat(video.stat.view.toLong())
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatItem(
                    icon = CupertinoIcons.Filled.BubbleLeft,
                    text = FormatUtils.formatStat(video.stat.danmaku.toLong())
                )
            }
        }
    }
}

@Composable
fun RelatedVideoGridRow(
    videos: List<RelatedVideo>,
    followingMids: Set<Long> = emptySet(),
    transitionEnabled: Boolean = false,
    showUpBadge: Boolean = true,
    columns: Int = RELATED_VIDEO_GRID_COLUMNS,
    onVideoClick: (RelatedVideo) -> Unit,
) {
    val safeColumns = columns.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        videos.take(safeColumns).forEach { video ->
            RelatedVideoItem(
                video = video,
                isFollowed = video.owner.mid in followingMids,
                transitionEnabled = transitionEnabled,
                showUpBadge = showUpBadge,
                modifier = Modifier.weight(1f),
                onClick = { onVideoClick(video) }
            )
        }
        repeat((safeColumns - videos.size).coerceAtLeast(0)) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
