// 文件路径: feature/home/components/cards/GlassVideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.iOSCardTapEffect
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.components.UpBadgeName
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionEnabled
import com.android.purebilibili.core.ui.transition.LocalVideoCardSharedElementSourceRoute
import com.android.purebilibili.core.ui.transition.LocalVideoSharedTransitionSpeedSettings
import com.android.purebilibili.core.ui.transition.VIDEO_SHARED_COVER_ASPECT_RATIO
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoSharedCoverCacheKey
import com.android.purebilibili.core.ui.transition.shouldUseVideoCardShellSharedBounds
import com.android.purebilibili.core.ui.transition.videoCardShellSharedBoundsOrEmpty
import com.android.purebilibili.feature.home.resolveHomeCardEnterAnimationEnabledAtMount
import kotlin.math.roundToInt
import com.android.purebilibili.feature.home.rememberHomeGlassPillColors
import com.android.purebilibili.feature.home.resolveHomeGlassCoverPillBaseColor

/**
 *  玻璃拟态卡片 - Vision Pro 风格 (性能优化版)
 * 
 * 特点：
 * - 彩虹渐变边框
 * - 无封面阴影，滚动时保持平面稳定
 * - 悬浮播放按钮
 * 
 *  性能优化：移除了昂贵的 blur() 和阴影层
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassVideoCard(
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
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    // [新增] 获取圆角缩放比例
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val cardCornerRadius = iOSCornerRadius.ExtraLarge * cornerRadiusScale  // 20.dp * scale
    val coverCornerRadius = iOSCornerRadius.Large * cornerRadiusScale + 2.dp  // 16.dp * scale
    val tagCornerRadius = iOSCornerRadius.Small * cornerRadiusScale  // 10.dp * scale
    val smallTagRadius = iOSCornerRadius.ExtraSmall * cornerRadiusScale  // 6.dp * scale
    val durationBadgeStyle = remember { resolveVideoCardDurationBadgeVisualStyle() }
    val durationText = remember(video.duration) { FormatUtils.formatDuration(video.duration) }
    val durationBadgeMinWidth = remember(durationText, durationBadgeStyle) {
        resolveVideoCardDurationBadgeMinWidthDp(
            durationText = durationText,
            style = durationBadgeStyle
        ).dp
    }
    val coverPillColors = rememberHomeGlassPillColors(
        glassEnabled = true,
        blurEnabled = true,
        emphasized = false,
        baseColor = resolveHomeGlassCoverPillBaseColor()
    )
    val emphasizedCoverPillColors = rememberHomeGlassPillColors(
        glassEnabled = true,
        blurEnabled = true,
        emphasized = true,
        baseColor = resolveHomeGlassCoverPillBaseColor()
    )
    val inlinePillColors = rememberHomeGlassPillColors(
        glassEnabled = true,
        blurEnabled = true,
        emphasized = false,
        baseColor = MaterialTheme.colorScheme.surface
    )
    val badgeStylePolicy = remember(showCoverGlassBadges, showInfoGlassBadges) {
        resolveHomeVideoGlassBadgeStylePolicy(
            showCoverGlassBadges = showCoverGlassBadges,
            showInfoGlassBadges = showInfoGlassBadges
        )
    }
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    
    val useLowQualityCover = isDataSaverActive && preferLowQualityCover
    val coverUrl = remember(video.bvid, useLowQualityCover) {
        FormatUtils.resolveVideoCoverUrl(
            if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic,
            useLowQuality = useLowQualityCover
        )
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    //  玻璃背景色 - 使用系统主题色自动适配
    val glassBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    
    //  获取屏幕尺寸用于计算归一化坐标
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    //  记录卡片位置（非 Compose State，避免滚动时触发高频重组）
    val cardBoundsRef = remember { object { var value: androidx.compose.ui.geometry.Rect? = null } }
    val localSharedElementSourceRoute = LocalVideoCardSharedElementSourceRoute.current
    val effectiveSharedElementSourceRoute = remember(sharedElementSourceRoute, localSharedElementSourceRoute) {
        sharedElementSourceRoute ?: localSharedElementSourceRoute
    }
    val effectiveTransitionEnabled = transitionEnabled && LocalSharedTransitionEnabled.current
    val sharedTransitionSpeedSettings = LocalVideoSharedTransitionSpeedSettings.current
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
    val triggerCardClick = {
        cardBoundsRef.value?.let { bounds ->
            CardPositionManager.recordVideoCardPosition(
                bvid = video.bvid,
                sourceRoute = effectiveSharedElementSourceRoute,
                bounds = bounds,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                sourceCornerDp = cardCornerRadius.value.roundToInt()
            )
        }
        onClick(video.bvid, 0)
    }
    
    //  尝试获取共享元素作用域
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val coverSharedEnabled = effectiveTransitionEnabled &&
        sharedTransitionScope != null &&
        animatedVisibilityScope != null
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
        isScrollInProgress = false,
        isReturningFromDetail = isReturningFromVideoDetail,
        useCoverSharedBounds = useCardShellSharedBounds,
        isSharedReturnTarget = isSharedReturnTarget,
    )
    val coverCacheKey = remember(video.bvid, useLowQualityCover) {
        resolveVideoSharedCoverCacheKey(video.bvid, useLowQualityCover)
    }
    // 🌈 彩虹渐变边框色
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF6B6B),  // 珊瑩红
            Color(0xFFFF8E53),  // 橙色
            Color(0xFFFFD93D),  // 金黄
            Color(0xFF6BCB77),  // 翠绿
            Color(0xFF4D96FF),  // 天蓝
            Color(0xFF9B59B6),  // 紫色
            Color(0xFFFF6B6B)   // 循环回红色
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

    Box(
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
            .padding(6.dp)
            .animateEnter(
                index = index,
                key = Unit,
                animationEnabled = enterAnimationEnabledAtMount,
                motionTier = motionTier,
                coordinateWithSharedTransition = coordinateEnterWithTransition
            )
            //  [新增] 记录卡片位置
            .onGloballyPositioned { coordinates ->
                cardBoundsRef.value = coordinates.boundsInRoot()
            }
    ) {
        //  [性能优化] 移除 blur() 层，改用静态渐变色
        // 原：blur(radius = 20.dp) 成本很高
        // 新：单层轻量阴影
        
        //  玻璃卡片主体
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cardCornerRadius))
                // 彩虹渐变边框
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        colors = rainbowColors.map { it.copy(alpha = 0.6f) }
                    ),
                    shape = RoundedCornerShape(cardCornerRadius)
                )
                // 单层轻量阴影
                .background(glassBackground)
                //  [新增] 长按手势检测
                .pointerInput(onDismiss) {
                    if (onDismiss != null) {
                        detectTapGestures(
                            onLongPress = {
                                haptic(HapticType.HEAVY)
                                showDismissMenu = true
                            },
                            onTap = {
                                triggerCardClick()
                            }
                        )
                    }
                }
                .then(
                    if (onDismiss == null) {
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                //  封面区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(VIDEO_SHARED_COVER_ASPECT_RATIO)
                        .padding(10.dp)
                ) {
                    // 封面图片 - 圆角内嵌
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(coverCornerRadius))
                    ) {
                        // 由 AsyncImage 根据卡片布局约束选择解码尺寸。
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .placeholderMemoryCacheKey(coverCacheKey)
                                .crossfade(coverCrossfadeEnabled)
                                .memoryCacheKey(coverCacheKey)
                                .diskCacheKey(coverCacheKey)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        //  底部渐变遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                        
                        //  已删除悬浮播放按钮
                        //  时长标签 - 玻璃胶囊
                        if (badgeStylePolicy.coverStyle == HomeVideoBadgeStyle.GLASS) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp),
                                color = emphasizedCoverPillColors.containerColor,
                                border = BorderStroke(0.8.dp, emphasizedCoverPillColors.borderColor),
                                shape = RoundedCornerShape(tagCornerRadius)
                            ) {
                                Text(
                                    text = durationText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .widthIn(min = durationBadgeMinWidth)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(20.dp, 0.dp, 20.dp, 16.dp),
                                color = Color.Black.copy(alpha = durationBadgeStyle.backgroundAlpha),
                                shape = RoundedCornerShape(tagCornerRadius)
                            ) {
                                Text(
                                    text = durationText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .widthIn(min = durationBadgeMinWidth)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        
                        //  [新增] 竖屏标签 - 左上角显示
                        if (video.isVertical && badgeStylePolicy.coverStyle == HomeVideoBadgeStyle.GLASS) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                                color = Color(0xFF00D1B2).copy(alpha = 0.82f),
                                border = BorderStroke(0.8.dp, coverPillColors.borderColor),
                                shape = RoundedCornerShape(smallTagRadius)
                            ) {
                                Text(
                                    text = "竖屏",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        } else if (video.isVertical) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                                color = Color(0xFF00D1B2).copy(alpha = 0.82f),
                                shape = RoundedCornerShape(smallTagRadius)
                            ) {
                                Text(
                                    text = "竖屏",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                
                //  信息区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp)
                        .videoCardShellReturnChromeAlpha(
                            enabled = useCardShellSharedBounds,
                            bvid = video.bvid,
                            sourceRoute = effectiveSharedElementSourceRoute,
                            isReturningFromDetail = isReturningFromVideoDetail,
                            isQuickReturnFromDetail = isQuickReturningFromVideoDetail,
                        )
                ) {
                    Text(
                        text = video.title,
                        color = onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 数据行
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UpBadgeName(
                            name = video.owner.name,
                            leadingContent = if (video.owner.face.isNotBlank()) {
                                {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(FormatUtils.fixImageUrl(video.owner.face))
                                            .crossfade(100)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else null,
                            nameStyle = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            nameColor = MaterialTheme.colorScheme.primary,
                            badgeTextColor = onSurfaceVariant.copy(alpha = 0.85f),
                            badgeBackgroundColor = onSurfaceVariant.copy(alpha = 0.12f),
                            showUpBadge = showUpBadge,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 播放量 -  [修复] 只在有播放量时显示
                        if (video.stat.view > 0) {
                            if (badgeStylePolicy.infoStyle == HomeVideoBadgeStyle.GLASS) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = inlinePillColors.containerColor,
                                    border = BorderStroke(0.8.dp, inlinePillColors.borderColor)
                                ) {
                                    Text(
                                        text = "${FormatUtils.formatStat(video.stat.view.toLong())}播放",
                                        color = onSurfaceVariant.copy(alpha = 0.78f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "${FormatUtils.formatStat(video.stat.view.toLong())}播放",
                                    color = onSurfaceVariant.copy(alpha = 0.78f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            
            //  顶部高光线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.White.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
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
