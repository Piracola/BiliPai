// 文件路径: feature/home/components/cards/StoryVideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.iOSCardTapEffect
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween

import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.store.HomeDurationStyle
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.ui.components.resolveUpStatsText
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.feature.home.resolveHomeCardEnterAnimationEnabledAtMount
import com.android.purebilibili.feature.video.ui.section.resolveCompactPublishTimeRowText
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlin.math.roundToInt

/**
 *  故事卡片 - 影院海报风格
 * 
 * 特点：
 * - 封面比例由首页卡片样式统一配置
 * - 大圆角 (24dp)
 * - 标题叠加在封面底部
 * - 沉浸电影感
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StoryVideoCard(
    video: VideoItem,
    index: Int = 0,  //  [新增] 索引用于动画延迟
    animationEnabled: Boolean = true,  //  卡片动画开关
    motionTier: MotionTier = MotionTier.Normal,
    transitionEnabled: Boolean = false, //  卡片过渡动画开关
    sharedElementSourceRoute: String? = null,
    isReturningFromVideoDetail: Boolean = false,
    isQuickReturningFromVideoDetail: Boolean = false,
    scrollLiteModeEnabled: Boolean = false,
    isDataSaverActive: Boolean = false,
    preferLowQualityCover: Boolean = false,
    showCoverGlassBadges: Boolean = true,
    showInfoGlassBadges: Boolean = true,
    showUpBadge: Boolean = true,
    homeDurationStyle: HomeDurationStyle = HomeDurationStyle.OUTSIDE_COVER,
    coverAspectRatio: Float = 4f / 3f,
    cardHorizontalPadding: Dp = 0.dp,
    compactMetadata: Boolean = true,
    showOnlineCount: Boolean = false,
    showPublishTime: Boolean = false,
    upFollowerCount: Int? = null,
    upVideoCount: Int? = null,
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onUpClick: ((Long) -> Unit)? = null,
    onLongClick: ((VideoItem) -> Unit)? = null, // [修复] 长按预览回调
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    // [新增] 获取圆角缩放比例
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val cardCornerRadius = iOSCornerRadius.ExtraLarge * cornerRadiusScale  // 20.dp * scale
    val coverShape = RoundedCornerShape(cardCornerRadius)
    val smallCornerRadius = iOSCornerRadius.Small * cornerRadiusScale - 2.dp  // 8.dp * scale
    val durationText = remember(video.duration) { FormatUtils.formatDuration(video.duration) }
    val showDurationOnCover = homeDurationStyle == HomeDurationStyle.OVERLAY_TEXT_ONLY
    val coverOverlayTextStyle = remember {
        TextStyle(shadow = resolveVideoCardCoverOverlayTextShadow())
    }
    val showDurationOutside = homeDurationStyle == HomeDurationStyle.OUTSIDE_COVER
    val scrollLitePolicy = remember(scrollLiteModeEnabled) {
        resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = scrollLiteModeEnabled
        )
    }
    val badgeStylePolicy = remember(showCoverGlassBadges, showInfoGlassBadges) {
        resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = showCoverGlassBadges,
            showInfoGlassBadges = showInfoGlassBadges
        )
    }
    val premiumBadgeLabel = remember(video.rights) {
        resolveVideoPremiumBadgeLabel(video.rights)
    }
    val onlineCount = rememberVideoCardOnlineCount(
        video = video,
        showOnlineCount = showOnlineCount
    )
    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverUrl = remember(video.bvid, useLowQualityCover) {
        FormatUtils.resolveVideoCoverUrl(
            if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic,
            useLowQuality = useLowQualityCover
        )
    }
    val publishTimeRowText = remember(showPublishTime, video.pubdate) {
        if (!showPublishTime) {
            ""
        } else {
            resolveCompactPublishTimeRowText(pubdate = video.pubdate)
        }
    }
    val emphasizePublishTime = false
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    
    //  获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val localSharedElementSourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val effectiveSharedElementSourceRoute = remember(sharedElementSourceRoute, localSharedElementSourceRoute) {
        sharedElementSourceRoute ?: localSharedElementSourceRoute
    }
    
    //  记录卡片位置
    var cardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val triggerCardClick = {
        cardBounds?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                isSingleColumn = !transitionEnabled,
                sourceCornerDp = cardCornerRadius.value.roundToInt()
            )
        }
        onClick(video.bvid, 0)
    }
    
    //  尝试获取共享元素作用域
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
    val effectiveTransitionEnabled = transitionEnabled && LocalSharedTransitionEnabled.current
    val coverSharedEnabled = shouldEnableVideoCoverSharedTransition(
        transitionEnabled = effectiveTransitionEnabled,
        hasSharedTransitionScope = sharedTransitionScope != null,
        hasAnimatedVisibilityScope = animatedVisibilityScope != null
    )
    val isQuickReturnLimited = isReturningFromVideoDetail && isQuickReturningFromVideoDetail
    val useCardShellSharedBounds = shouldUseVideoCardShellSharedBounds(
        sourceRoute = effectiveSharedElementSourceRoute,
        transitionEnabled = coverSharedEnabled
    )
    val isSharedReturnTarget = remember(
        video.bvid,
        effectiveSharedElementSourceRoute,
        CardPositionManager.lastClickedVideoSourceKey,
    ) {
        isVideoCardSharedReturnTarget(
            bvid = video.bvid,
            sourceRoute = effectiveSharedElementSourceRoute,
            lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
        )
    }
    val coverCrossfadeEnabled = shouldEnableVideoCardCoverCrossfade(
        isScrollInProgress = scrollLiteModeEnabled,
        isReturningFromDetail = isReturningFromVideoDetail,
        useCoverSharedBounds = useCardShellSharedBounds,
        isSharedReturnTarget = isSharedReturnTarget,
    )
    val cardSharedTransitionMotionSpec = remember(
        effectiveSharedElementSourceRoute,
        effectiveTransitionEnabled,
        sharedTransitionSpeedSettings
    ) {
        resolveVideoCardSharedTransitionMotionSpec(
            sourceRoute = effectiveSharedElementSourceRoute,
            transitionEnabled = effectiveTransitionEnabled,
            speedSettings = sharedTransitionSpeedSettings
        )
    }
    
    val cardShellShape = remember(cardCornerRadius) { RoundedCornerShape(cardCornerRadius) }
    val enterAnimationEnabledAtMount = remember(video.bvid) {
        resolveHomeCardEnterAnimationEnabledAtMount(
            baseAnimationEnabled = animationEnabled,
            isReturningFromDetail = isReturningFromVideoDetail,
            isSwitchingCategory = CardPositionManager.isSwitchingCategory,
            isScrollInProgress = scrollLiteModeEnabled
        )
    }
    val coordinateEnterWithTransition = remember(animationEnabled, transitionEnabled) {
        animationEnabled && transitionEnabled
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .videoCardShellSharedBoundsOrEmpty(
                enabled = useCardShellSharedBounds,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                motionSpec = cardSharedTransitionMotionSpec,
                clipShape = cardShellShape
            )
            .videoCardSiblingDepthScale(
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
            )
            .padding(horizontal = cardHorizontalPadding, vertical = 8.dp)
            .animateEnter(
                index = index,
                key = Unit,
                animationEnabled = enterAnimationEnabledAtMount,
                motionTier = motionTier,
                coordinateWithSharedTransition = coordinateEnterWithTransition
            )
            //  [新增] 记录卡片位置
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInRoot()
            }
            .pointerInput(onDismiss, onLongClick) {
                 val hasLongPressAction = onDismiss != null || onLongClick != null
                 if (hasLongPressAction) {
                     detectTapGestures(
                         onLongPress = {
                             if (onLongClick != null) {
                                 haptic(HapticType.HEAVY)
                                 onLongClick(video)
                             } else if (onDismiss != null) {
                                 haptic(HapticType.HEAVY)
                                 showDismissMenu = true
                             }
                         },
                         onTap = {
                             triggerCardClick()
                         }
                     )
                 }
            }
            .then(
                 if (onDismiss == null && onLongClick == null) {
                     Modifier.iOSCardTapEffect(
                         pressScale = 1f,
                         pressTranslationY = 0f,
                         hapticEnabled = true
                     ) {
                         triggerCardClick()
                     }
                 } else Modifier
            )
    ) {
        // 卡片容器 (封面)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_story_video_cover")
                .aspectRatio(coverAspectRatio)
                .clip(coverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant) // 封面占位色
        ) {
            // 封面比例由首页卡片样式统一配置。
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(coverCrossfadeEnabled)
                    .memoryCacheKey("story_${video.bvid}")
                    .diskCacheKey("story_${video.bvid}")
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (premiumBadgeLabel != null) {
                HomeVideoBadgePill(
                    style = badgeStylePolicy.coverStyle,
                    shape = AppShapes.container(ContainerLevel.Chip),
                    containerColor = BiliPink.copy(alpha = 0.82f),
                    borderColor = Color.White.copy(alpha = 0.24f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = premiumBadgeLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            
            //  时长标签 (保留在封面上)
            if (showDurationOnCover) {
                Text(
                    text = durationText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    style = coverOverlayTextStyle,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier.videoCardShellReturnChromeAlpha(
                enabled = useCardShellSharedBounds,
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                isReturningFromDetail = isReturningFromVideoDetail,
                isQuickReturnFromDetail = isQuickReturningFromVideoDetail,
            )
        ) {
        Spacer(modifier = Modifier.height(if (compactMetadata) 8.dp else 12.dp))
        
        Text(
            text = video.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compactMetadata) 15.sp else 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = if (compactMetadata) 20.sp else 23.sp,
            modifier = Modifier.fillMaxWidth()
        )

        VideoCardDurationPublishRow(
            durationText = durationText.takeIf { showDurationOutside }.orEmpty(),
            publishTimeText = publishTimeRowText,
            emphasizePublishTime = emphasizePublishTime,
            publishTimeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            topSpacingDp = if (compactMetadata) 4 else 8
        )
        
        Spacer(modifier = Modifier.height(if (compactMetadata) 6.dp else 8.dp))
        
        // UP主信息 + 数据
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val upClickMid = video.owner.mid.takeIf { it > 0L && onUpClick != null }
            val upNameModifier = if (upClickMid != null) {
                Modifier.wrapContentSize().clickable { onUpClick?.invoke(upClickMid) }
            } else {
                Modifier.wrapContentSize()
            }
            
            UpBadgeName(
                name = video.owner.name,
                metaText = resolveUpStatsText(
                    followerCount = upFollowerCount,
                    videoCount = upVideoCount
                ),
                leadingContent = if (video.owner.face.isNotEmpty()) {
                    {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(video.owner.face))
                                .crossfade(100)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else null,
                nameStyle = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                nameColor = MaterialTheme.colorScheme.primary,
                metaColor = MaterialTheme.colorScheme.primary,
                badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                badgeBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                showUpBadge = showUpBadge,
                modifier = upNameModifier
            )
            
            // 数据行 (Play & Danmaku)
             //  [重设计] 播放数据行 - 独立展示，精致风格
            if (scrollLitePolicy.showSecondaryStatsRow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 16.dp) // 与 UP 主信息分开
                ) {
                    // 播放量
                    if (video.stat.view > 0) {
                        Box {
                             Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = CupertinoIcons.Outlined.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatStat(video.stat.view.toLong()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 弹幕数 (仅当有播放量时显示，保持逻辑一致)
                    if (video.stat.view > 0 && video.stat.danmaku > 0) {
                         Box {
                             Row(
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.spacedBy(2.dp)
                             ) {
                                 Icon(
                                     imageVector = CupertinoIcons.Outlined.BubbleLeft,
                                     contentDescription = null,
                                     modifier = Modifier.size(12.dp),
                                     tint = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 Text(
                                     text = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                                     fontSize = 11.sp,
                                     fontWeight = FontWeight.Medium
                                 )
                             }
                         }
                    }

                    if (onlineCount.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.Eye,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = onlineCount,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        }
    }
    
    //  [新增] 长按删除菜单
    DropdownMenu(
        expanded = showDismissMenu,
        onDismissRequest = { showDismissMenu = false }
    ) {
        DropdownMenuItem(
            text = { 
                Text(
                    "🚫 不感兴趣",
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            onClick = {
                showDismissMenu = false
                onDismiss?.invoke()
            }
        )
    }
}
