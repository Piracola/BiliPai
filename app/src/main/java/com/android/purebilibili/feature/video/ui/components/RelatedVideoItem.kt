package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.feature.home.components.cards.videoCardShellReturnChromeAlpha
import com.android.purebilibili.core.ui.transition.videoCoverSharedElementKey
import com.android.purebilibili.core.ui.transition.videoSharedElementBoundsTransformSpec
import com.android.purebilibili.feature.video.ui.FollowBadgeTone
import com.android.purebilibili.feature.video.ui.resolveVideoFollowVisualPolicy
import com.android.purebilibili.navigation.VideoRoute

/**
 * Related Video Components
 * 
 * Contains components for displaying related videos:
 * - RelatedVideosHeader: Section header
 * - RelatedVideoItem: Individual video card
 * 
 * Requirement Reference: AC3.3 - Related video components in dedicated file
 */

/**
 * Related Videos Header
 */
@Composable
fun RelatedVideosHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent // 🎨 [修复] 让标题直接显示在背景上，不显示为独立的色块
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u66f4\u591a\u63a8\u8350",
                style = MaterialTheme.typography.titleMedium, // Should be ~17sp SemiBold "Body/Headline"
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveRelatedVideoCardPressScaleTarget(
    isPressed: Boolean,
    transitionEnabled: Boolean
): Float {
    // Keep related card bounds stable to reduce list jank and transition drift.
    // Intentionally avoid press squeeze in this list.
    return 1f
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldEnableRelatedVideoCoverCrossfade(
    transitionEnabled: Boolean
): Boolean {
    // Avoid image crossfade competing with list scrolling and shared transition.
    return false
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldTriggerRelatedVideoPressHaptic(
    isPressed: Boolean,
    transitionEnabled: Boolean
): Boolean {
    return false
}

internal fun resolveRelatedVideoSharedElementSourceRoute(sourceRoute: String?): String {
    return sourceRoute
        ?.substringBefore("?")
        ?.takeIf { it.isNotBlank() }
        ?: VideoRoute.base
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldEnableRelatedVideoMetadataSharedBounds(
    transitionEnabled: Boolean
): Boolean {
    // Metadata shared bounds are expensive in long lists and can cause return misalignment.
    return false
}

/**
 * Related Video Item (iOS style optimized)
 * 
 * @param video 相关视频数据
 * @param isFollowed 是否已关注
 * @param transitionEnabled 🔗 是否启用共享元素过渡动画
 * @param onClick 点击回调
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RelatedVideoItem(
    video: RelatedVideo, 
    isFollowed: Boolean = false,
    transitionEnabled: Boolean = false,  // 🔗 [新增] 共享元素过渡开关
    showUpBadge: Boolean = true,
    onClick: () -> Unit
) {
    // 🔗 获取共享元素作用域 (用于过渡动画)
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = transitionEnabled &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
    val metadataSharedEnabled = shouldEnableRelatedVideoMetadataSharedBounds(transitionEnabled)
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
    val useCardShellSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = sourceRoute,
        transitionEnabled = coverSharedEnabled
    )
    val relatedCoverShape = RoundedCornerShape(12.dp)
    val relatedCoverSharedBoundsModifier = if (coverSharedEnabled && !useCardShellSharedBounds) {
        with(requireNotNull(sharedTransitionScope)) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = videoCoverSharedElementKey(
                        bvid = video.bvid,
                        sourceRoute = sourceRoute
                    )
                ),
                animatedVisibilityScope = requireNotNull(animatedVisibilityScope),
                boundsTransform = { initialBounds, targetBounds ->
                    videoSharedElementBoundsTransformSpec(
                        motion = cardSharedTransitionMotionSpec,
                        initialBounds = initialBounds,
                        targetBounds = targetBounds
                    )
                },
                clipInOverlayDuringTransition = OverlayClip(relatedCoverShape)
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp) // Spacing between items
    ) {
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
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
                .onGloballyPositioned { coordinates ->
                    cardBoundsRef.value = coordinates.boundsInRoot()
                }
                .clickable(
                    onClick = triggerRelatedVideoClick
                )
        ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp) // Internal padding
        ) {
            val relatedCoverWidth = 130.dp
            val relatedCoverHeight = relatedCoverWidth / VIDEO_SHARED_COVER_ASPECT_RATIO
            
            // Video cover
            Box(
                modifier = Modifier
                    .width(relatedCoverWidth)
                    .height(relatedCoverHeight)
                    .then(relatedCoverSharedBoundsModifier)
                    .clip(relatedCoverShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.resolveVideoCoverUrl(video.pic, useLowQuality = false))
                        .crossfade(shouldEnableRelatedVideoCoverCrossfade(transitionEnabled))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Duration label - Plain text with shadow, no background (Apple style)
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
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Video info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = relatedCoverHeight)
                    .videoCardShellReturnChromeAlpha(
                        enabled = useCardShellSharedBounds,
                        bvid = video.bvid,
                        sourceRoute = sourceRoute,
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium.copy( // 15sp regular/medium
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    // UP owner info row
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                        },
                                        modifier = Modifier.wrapContentSize()
                                    )
                                }
                            } else null,
                            leadingContent = if (video.owner.face.isNotEmpty()) {
                                {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(FormatUtils.fixImageUrl(video.owner.face))
                                            .crossfade(shouldEnableRelatedVideoCoverCrossfade(transitionEnabled))
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            } else null,
                            nameStyle = MaterialTheme.typography.labelMedium,
                            nameColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            badgeBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            showUpBadge = showUpBadge,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Stats row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            StatItem(icon = CupertinoIcons.Filled.Play, text = FormatUtils.formatStat(video.stat.view.toLong()))
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Box {
                            StatItem(icon = CupertinoIcons.Filled.BubbleLeft, text = FormatUtils.formatStat(video.stat.danmaku.toLong()))
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline, // System Gray 3 or similar
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
