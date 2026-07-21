// 文件路径: feature/home/components/TopBar.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tv

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi // [Added]
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.LocalAndroidNativeVariant
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.feature.home.UserState
import com.android.purebilibili.feature.home.HomeCategory
import com.android.purebilibili.feature.home.resolveHomeTopCategories
import com.android.purebilibili.core.store.BottomBarLiquidGlassPreset
import com.android.purebilibili.core.store.LiquidGlassStyle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.animation.DampedDragAnimationState
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.delay
import com.android.purebilibili.core.ui.motion.BottomBarMotionProfile
import com.android.purebilibili.core.ui.motion.resolveBottomBarMotionSpec
import androidx.compose.foundation.combinedClickable // [Added]
import java.io.File

private const val IOS_TOP_TAB_CONTENT_PADDING_DP = 2f

internal fun resolveFloatingIndicatorStartPaddingPx(
    baseInsetPx: Float,
    leftBiasPx: Float
): Float = (baseInsetPx - leftBiasPx).coerceAtLeast(0f)

internal fun resolveTopTabRowHorizontalPaddingDp(
    isFloatingStyle: Boolean,
    edgeToEdge: Boolean = false,
    labelMode: Int = 0
): Float {
    if (edgeToEdge) return 0f
    if (isFloatingStyle) return 0f
    // Text-only MD3/Miuix: drop the extra 4dp so the first indicator sits closer to the edge.
    return if (normalizeTopTabLabelMode(labelMode) == 2) 0f else 4f
}

// Slightly tighter than before so rest capsule nearly fills the dock (bottom-bar feel),
// while drag scale still overflows the chrome edge.
internal fun resolveTopTabDockIndicatorHorizontalGapDp(hasOuterChromeSurface: Boolean): Float =
    if (hasOuterChromeSurface) 2f else 2f

internal fun resolveTopTabDockIndicatorVerticalGapDp(hasOuterChromeSurface: Boolean): Float = 1f

internal fun resolveTopTabDockIndicatorWidthDp(
    itemWidthDp: Float,
    horizontalGapDp: Float,
    minWidthDp: Float = 0f
): Float {
    if (itemWidthDp <= 0f) return 0f
    val maxWidth = (itemWidthDp - horizontalGapDp.coerceAtLeast(0f) * 2f)
        .coerceAtLeast(0f)
    val minWidth = minWidthDp.coerceIn(0f, itemWidthDp)
    return maxWidth.coerceAtLeast(minWidth)
}

internal fun resolveTopTabDockIndicatorHeightDp(
    rowHeightDp: Float,
    verticalGapDp: Float,
    minHeightDp: Float,
    indicatorWidthDp: Float = Float.POSITIVE_INFINITY
): Float {
    if (rowHeightDp <= 0f) return 0f
    val maxHeight = (rowHeightDp - verticalGapDp.coerceAtLeast(0f) * 2f)
        .coerceAtLeast(0f)
    val minHeight = minHeightDp.coerceIn(0f, rowHeightDp)
    return resolveSegmentedControlIndicatorHeightDp(
        slotWidthDp = indicatorWidthDp,
        indicatorHeightDp = maxHeight
    ).coerceAtLeast(minHeight)
}

/**
 * Preferred per-tab width when the floating dock **wraps content** instead of stretching
 * full width (icon / text density drives dock length).
 */
internal fun resolveTopTabWrapItemWidthDp(
    labelMode: Int,
    isFloatingStyle: Boolean = true
): Float {
    return when (normalizeTopTabLabelMode(labelMode)) {
        0 -> if (isFloatingStyle) 74f else 70f // icon + text
        1 -> if (isFloatingStyle) 56f else 52f // icon only
        else -> if (isFloatingStyle) 66f else 62f // text only
    }
}

/**
 * Whether the top dock should shrink to tab content instead of fillMaxWidth.
 * Floating / bottom-bar-matched docks: wrap so right side isn't empty chrome.
 * Embedded / full-bleed rows keep stretch.
 */
internal fun shouldWrapTopTabDockWidth(
    isFloatingStyle: Boolean,
    hasOuterChromeSurface: Boolean,
    edgeToEdge: Boolean
): Boolean {
    if (edgeToEdge) return false
    return isFloatingStyle || hasOuterChromeSurface
}

/**
 * Dock content width = itemWidth × tabCount (+ optional horizontal content padding).
 * Clamped to [maxWidthDp] so small phones still fill when content is wider.
 */
internal fun resolveTopTabDockWrapWidthDp(
    itemWidthDp: Float,
    categoryCount: Int,
    maxWidthDp: Float,
    contentPaddingHorizontalDp: Float = 0f
): Float {
    if (itemWidthDp <= 0f || categoryCount <= 0) return 0f
    val content = itemWidthDp * categoryCount + contentPaddingHorizontalDp.coerceAtLeast(0f) * 2f
    if (maxWidthDp <= 0f) return content
    return content.coerceIn(0f, maxWidthDp)
}

/**
 * Item width for wrap dock: use content-driven preferred width when it fits;
 * otherwise fall back to dividing the available max width (scrollable denser slots).
 */
internal fun resolveTopTabDockItemWidthDp(
    maxWidthDp: Float,
    categoryCount: Int,
    labelMode: Int,
    isFloatingStyle: Boolean,
    wrapContent: Boolean,
    fillItemWidthDp: Float
): Float {
    if (!wrapContent || categoryCount <= 0) return fillItemWidthDp
    val preferred = resolveTopTabWrapItemWidthDp(labelMode, isFloatingStyle)
    val wrapWidth = resolveTopTabDockWrapWidthDp(
        itemWidthDp = preferred,
        categoryCount = categoryCount,
        maxWidthDp = maxWidthDp
    )
    // Preferred pack fits: use content-driven item width.
    if (wrapWidth <= maxWidthDp + 0.01f && preferred * categoryCount <= maxWidthDp + 0.01f) {
        return preferred
    }
    // Overflow: pack into available width.
    return fillItemWidthDp
}

internal fun resolveTopTabDockIndicatorOffsetPx(
    slotTranslationPx: Float,
    horizontalGapPx: Float
): Float = slotTranslationPx + horizontalGapPx.coerceAtLeast(0f)

internal fun resolveTopTabVisibleSlots(
    categoryCount: Int,
    longestLabelLength: Int = 0
): Int {
    if (categoryCount in 1..3) return categoryCount
    if (categoryCount <= 4) return 4
    if (categoryCount == 6 && longestLabelLength <= 3) return 6
    return if (longestLabelLength >= 8) 4 else 5
}

internal fun resolveMd3TopTabVisibleSlots(): Int = 3

internal fun resolveMd3TopTabLayoutVisibleSlots(
    categoryCount: Int,
    labelMode: Int,
    showPartitionAction: Boolean
): Int {
    val hasSupportedLabelMode = normalizeTopTabLabelMode(labelMode) in 0..2
    return if (!showPartitionAction && hasSupportedLabelMode && categoryCount >= 4) {
        categoryCount.coerceAtMost(6)
    } else {
        resolveMd3TopTabVisibleSlots()
    }
}

internal fun resolveIosTopTabLayoutVisibleSlots(
    categoryCount: Int,
    labelMode: Int
): Int = resolveMd3TopTabLayoutVisibleSlots(
    categoryCount = categoryCount,
    labelMode = labelMode,
    showPartitionAction = false
)

internal fun resolveIosTopTabItemWidthDp(
    containerWidthDp: Float,
    categoryCount: Int,
    labelMode: Int
): Float = resolveMd3TopTabItemWidthDp(
    containerWidthDp = (containerWidthDp - IOS_TOP_TAB_CONTENT_PADDING_DP * 2f)
        .coerceAtLeast(0f),
    visibleSlots = resolveIosTopTabLayoutVisibleSlots(categoryCount, labelMode)
)

internal fun resolveMd3TopTabItemWidthDp(
    containerWidthDp: Float,
    visibleSlots: Int = resolveMd3TopTabVisibleSlots()
): Float {
    if (containerWidthDp <= 0f) return 96f
    if (visibleSlots >= 5) return (containerWidthDp / visibleSlots).coerceIn(52f, 72f)
    return (containerWidthDp / visibleSlots.coerceAtLeast(1)).coerceAtLeast(88f)
}

internal fun resolveMd3TopTabContentPaddingDp(
    containerWidthDp: Float,
    itemWidthDp: Float,
    categoryCount: Int,
    labelMode: Int = 0
): Float {
    if (containerWidthDp <= 0f || itemWidthDp <= 0f || categoryCount <= 0) return 0f
    val contentWidth = itemWidthDp * categoryCount
    val leftover = (containerWidthDp - contentWidth).coerceAtLeast(0f)
    // Multi-tab rows (MD3 / MIUIX / all label modes): lead-align so the first indicator
    // sits at the leading edge. The 72dp item-width cap on 4–6 tabs creates leftover;
    // centering it pushes "推荐" away from the left of the dock.
    // Sparse rows (1–2 tabs): keep residual centered so a single tab is not glued left.
    @Suppress("UNUSED_PARAMETER")
    val ignoredLabelMode = labelMode
    return if (categoryCount >= 3) {
        0f
    } else {
        leftover / 2f
    }
}

internal fun resolveMd3VisibleTabIndices(
    totalCount: Int,
    selectedIndex: Int,
    visibleSlots: Int = resolveMd3TopTabVisibleSlots()
): List<Int> {
    if (totalCount <= 0) return emptyList()
    return List(totalCount) { it }
}

internal fun resolveMd3SelectedVisibleIndex(
    visibleIndices: List<Int>,
    selectedIndex: Int
): Int {
    val resolved = visibleIndices.indexOf(selectedIndex)
    return if (resolved >= 0) resolved else 0
}

internal fun resolveTopTabMinItemWidthDp(isFloatingStyle: Boolean): Float {
    return if (isFloatingStyle) 72f else 64f
}

internal fun resolveTopTabItemWidthDp(
    containerWidthDp: Float,
    categoryCount: Int,
    isFloatingStyle: Boolean,
    longestLabelLength: Int = 0
): Float {
    if (containerWidthDp <= 0f) return resolveTopTabMinItemWidthDp(isFloatingStyle)
    val slots = resolveTopTabVisibleSlots(
        categoryCount = categoryCount,
        longestLabelLength = longestLabelLength
    ).coerceAtLeast(1)
    val baseWidth = containerWidthDp / slots
    return baseWidth.coerceAtLeast(resolveTopTabMinItemWidthDp(isFloatingStyle))
}

internal fun resolveTopTabVisibleCategorySlots(
    categoryCount: Int,
    longestLabelLength: Int = 0
): Int {
    return resolveTopTabVisibleSlots(
        categoryCount = categoryCount,
        longestLabelLength = longestLabelLength
    ).coerceAtMost(categoryCount.coerceAtLeast(1)).coerceAtLeast(1)
}

internal fun resolveTopTabActionSlotWidthDp(
    containerWidthDp: Float,
    categoryCount: Int,
    longestLabelLength: Int = 0
): Float {
    if (containerWidthDp <= 0f) return 0f
    val categorySlots = resolveTopTabVisibleCategorySlots(
        categoryCount = categoryCount,
        longestLabelLength = longestLabelLength
    )
    return containerWidthDp / (categorySlots + 1)
}

internal fun normalizeTopTabLabelMode(mode: Int): Int {
    return when (mode) {
        0, 1, 2 -> mode
        else -> 2
    }
}

internal fun shouldShowTopTabIcon(mode: Int): Boolean {
    val normalized = normalizeTopTabLabelMode(mode)
    return normalized == 0 || normalized == 1
}

internal fun shouldShowTopTabText(mode: Int): Boolean {
    val normalized = normalizeTopTabLabelMode(mode)
    return normalized == 0 || normalized == 2
}

internal fun resolveMd3TopTabLabelMode(requestedLabelMode: Int): Int =
    normalizeTopTabLabelMode(requestedLabelMode)

private fun resolveTopTabCategoryForIcon(categoryKey: String): HomeCategory? {
    val normalizedKey = categoryKey.trim()
    if (normalizedKey.isEmpty()) return null

    return HomeCategory.entries.firstOrNull { category ->
        category.name.equals(normalizedKey, ignoreCase = true) || category.label == normalizedKey
    }
}

internal fun resolveTopTabCategoryIcon(
    categoryKey: String,
    uiPreset: UiPreset = UiPreset.IOS
): ImageVector {
    val category = resolveTopTabCategoryForIcon(categoryKey)
    return when (uiPreset) {
        UiPreset.MD3 -> when (category) {
            HomeCategory.RECOMMEND -> Icons.Outlined.Home
            HomeCategory.FOLLOW -> Icons.Outlined.Person
            HomeCategory.POPULAR -> Icons.AutoMirrored.Outlined.TrendingUp
            HomeCategory.LIVE -> Icons.Outlined.LiveTv
            HomeCategory.ANIME -> Icons.Outlined.Tv
            HomeCategory.GAME -> Icons.Outlined.SportsEsports
            HomeCategory.KNOWLEDGE -> Icons.Outlined.Lightbulb
            HomeCategory.TECH -> Icons.Outlined.SmartToy
            else -> Icons.AutoMirrored.Outlined.MenuOpen
        }
        UiPreset.IOS -> when (category) {
            HomeCategory.RECOMMEND -> CupertinoIcons.Default.House
            HomeCategory.FOLLOW -> CupertinoIcons.Default.PersonCropCircleBadgePlus
            HomeCategory.POPULAR -> CupertinoIcons.Default.ChartBar
            HomeCategory.LIVE -> CupertinoIcons.Default.Video
            HomeCategory.ANIME -> CupertinoIcons.Default.Tv
            HomeCategory.GAME -> CupertinoIcons.Default.PlayCircle
            HomeCategory.KNOWLEDGE -> CupertinoIcons.Default.Lightbulb
            HomeCategory.TECH -> CupertinoIcons.Default.Cpu
            else -> CupertinoIcons.Default.ListBullet
        }
    }
}

internal fun resolveTopTabPartitionIcon(uiPreset: UiPreset): ImageVector {
    return if (uiPreset == UiPreset.MD3) {
        Icons.AutoMirrored.Outlined.MenuOpen
    } else {
        CupertinoIcons.Default.ListBullet
    }
}

internal enum class Md3TopTabRowVariant {
    UNDERLINE_FIXED
}

internal fun resolveMd3TopTabRowVariant(): Md3TopTabRowVariant =
    Md3TopTabRowVariant.UNDERLINE_FIXED

internal fun resolveMd3TopTabActionButtonCorner(
    isFloatingStyle: Boolean,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
    if (isFloatingStyle) 18.dp else 14.dp
} else {
    if (isFloatingStyle) 16.dp else 12.dp
}

internal fun resolveMd3TopTabActionButtonSize(
    isFloatingStyle: Boolean,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
    if (isFloatingStyle) 50.dp else 44.dp
} else {
    if (isFloatingStyle) 48.dp else 42.dp
}

internal fun resolveMd3TopTabActionIconSize(
    isFloatingStyle: Boolean,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
) = if (androidNativeVariant == AndroidNativeVariant.MIUIX) {
    if (isFloatingStyle) 24.dp else 22.dp
} else {
    if (isFloatingStyle) 24.dp else 22.dp
}

internal fun resolveMd3TopTabActionContentBottomPadding(): Dp = 4.dp

internal fun resolveMd3TopTabVerticalLiftDp(): Float = 4f

internal fun resolveMd3TopTabRowVerticalTranslationDp(
    skinPlainStyle: Boolean,
    hasOuterChromeSurface: Boolean
): Float {
    if (skinPlainStyle || hasOuterChromeSurface) return 0f
    return -resolveMd3TopTabVerticalLiftDp()
}

internal fun resolveMd3TopTabIndicatorBottomPadding(): Dp = 8.dp

internal fun resolveHomeSkinTopTabActionButtonSize(): Dp = 44.dp

internal fun resolveHomeSkinTopTabActionIconSize(): Dp = 24.dp

internal fun resolveHomeSkinTopTabIndicatorBottomPadding(): Dp = 4.dp

internal fun resolveTopTabSkinStickerIconSize(showText: Boolean): Dp =
    if (showText) 32.dp else 36.dp

internal fun resolveTopTabSkinPartitionIconSize(): Dp = 32.dp

internal fun resolveTopTabSkinStickerIndicatorWidth(): Dp = 28.dp

internal fun resolveTopTabSkinStickerRowHeight(
    baseRowHeight: Dp,
    hasSkinStickerIcons: Boolean,
    showIcon: Boolean,
    showText: Boolean
): Dp {
    return if (hasSkinStickerIcons && showIcon && showText) {
        baseRowHeight.coerceAtLeast(64.dp)
    } else {
        baseRowHeight
    }
}

internal fun resolveTopTabSkinStickerItemVerticalPadding(showText: Boolean): Dp =
    if (showText) 2.dp else 4.dp

internal fun resolveIosTopTabRowHeight(
    isFloatingStyle: Boolean,
    labelMode: Int = com.android.purebilibili.core.store.SettingsManager.TopTabLabelMode.TEXT_ONLY
): Dp {
    return if (normalizeTopTabLabelMode(labelMode) ==
        com.android.purebilibili.core.store.SettingsManager.TopTabLabelMode.ICON_AND_TEXT
    ) {
        if (isFloatingStyle) 62.dp else 58.dp
    } else {
        // Text-only / icon-only: still taller so the liquid capsule can fill + overflow on drag.
        if (isFloatingStyle) 56.dp else 54.dp
    }
}

internal fun resolveIosTopTabActionButtonSize(isFloatingStyle: Boolean): Dp =
    if (isFloatingStyle) 46.dp else 44.dp

internal fun resolveIosTopTabActionButtonCorner(isFloatingStyle: Boolean): Dp =
    if (isFloatingStyle) 22.dp else 20.dp

internal fun resolveIosTopTabActionIconSize(isFloatingStyle: Boolean): Dp =
    if (isFloatingStyle) 23.dp else 22.dp

internal fun performHomeTopBarTap(
    haptic: (HapticType) -> Unit,
    onClick: () -> Unit,
    hapticType: HapticType = HapticType.LIGHT
) {
    haptic(hapticType)
    onClick()
}

/**
 * Q弹点击效果
 */
fun Modifier.premiumClickable(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "scale"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 *  iOS 风格悬浮顶栏
 * - 不贴边，有水平边距
 * - 圆角 + 毛玻璃效果
 */
@Composable
fun FluidHomeTopBar(
    user: UserState,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        //  悬浮式导航栏容器 - 增强视觉层次
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = AppShapes.borderedContainer(ContainerLevel.Floating),
            color = AppSurfaceTokens.cardContainer(),  //  使用预设感知表面色，适配深色模式
            shadowElevation = 6.dp,  // 添加阴影增加层次感
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp) // 稍微减小高度
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //  左侧：头像
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .premiumClickable { onAvatarClick() }
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    if (user.isLogin && user.face.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(user.face))
                                .crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                //  中间：搜索框
                val searchClickInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(AppShapes.container(ContainerLevel.Pill))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = searchClickInteractionSource,
                            indication = null
                        ) {
                            onSearchClick()
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CupertinoIcons.Default.MagnifyingGlass,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "搜索视频、UP主...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                //  右侧：设置按钮
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        CupertinoIcons.Default.Gearshape,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * [HIG] iOS 风格可滑动分类标签栏。
 * - 所有分类水平平铺，支持系统惯性滚动。
 * - 使用轻量胶囊和文字强调，不再绘制顶部液态玻璃指示器。
 */
internal fun resolveTopTabUnselectedAlpha(): Float = 0.78f

internal fun resolveTopTabUnselectedColor(isLightMode: Boolean): Color {
    return if (isLightMode) {
        Color.Black.copy(alpha = 0.72f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
}

internal fun resolveIosTopTabSelectedContentColor(colorScheme: ColorScheme): Color =
    colorScheme.primary

internal fun resolveIosTopTabCapsuleContainerColor(
    isDarkTheme: Boolean,
    selectionFraction: Float
): Color {
    val selectedAlpha = selectionFraction.coerceIn(0f, 1f)
    val baseColor = resolveBottomBarMovingIndicatorSurfaceColor(isDarkTheme = isDarkTheme)
    return baseColor.copy(alpha = 0.28f * selectedAlpha)
}

internal fun Modifier.homeTopBottomBarMatchedSurface(
    renderMode: HomeTopChromeRenderMode,
    shape: Shape,
    hazeState: HazeState?,
    backdrop: LayerBackdrop?,
    liquidGlassStyle: LiquidGlassStyle,
    liquidGlassTuning: LiquidGlassTuning?,
    liquidGlassPreset: BottomBarLiquidGlassPreset = BottomBarLiquidGlassPreset.BILIPAI_TUNED,
    motionTier: MotionTier,
    isTransitionRunning: Boolean,
    forceLowBlurBudget: Boolean,
    drawShellLens: Boolean = true,
    isScrolling: Boolean = false,
    materialScrollProgress: Float = if (isScrolling) 1f else 0f
): Modifier = composed {
    val isGlassEnabled = renderMode == HomeTopChromeRenderMode.LIQUID_GLASS_BACKDROP ||
        renderMode == HomeTopChromeRenderMode.LIQUID_GLASS_HAZE
    val isBlurEnabled = renderMode != HomeTopChromeRenderMode.PLAIN
    val blurIntensity = currentUnifiedBlurIntensity()
    val isDarkTheme = resolveBottomBarDarkTheme(AppSurfaceTokens.chromeBackground())
    val tuning = resolveAndroidNativeBottomBarTuning(
        blurEnabled = isBlurEnabled || isGlassEnabled,
        darkTheme = isDarkTheme
    )
    // Match floating bottom-bar shell tint exactly (same preset + glass material policy).
    val containerColor = resolveAndroidNativeFloatingBottomBarContainerColor(
        surfaceColor = MaterialTheme.colorScheme.surfaceContainer,
        tuning = tuning,
        glassEnabled = isGlassEnabled,
        blurEnabled = isBlurEnabled,
        blurIntensity = blurIntensity,
        liquidGlassPreset = liquidGlassPreset
    )
    this.kernelSuFloatingDockSurface(
        shape = shape,
        backdrop = backdrop,
        containerColor = containerColor,
        blurEnabled = isBlurEnabled,
        glassEnabled = isGlassEnabled,
        drawShellLens = drawShellLens,
        blurRadius = tuning.shellBlurRadiusDp.dp,
        hazeState = hazeState,
        motionTier = motionTier,
        isTransitionRunning = isTransitionRunning,
        forceLowBlurBudget = forceLowBlurBudget,
        liquidGlassPreset = liquidGlassPreset,
        isScrolling = isScrolling,
        materialScrollProgress = materialScrollProgress
    )
}

@Composable
private fun LightweightHomeTopTabs(
    renderer: HomeTopTabRenderer,
    categories: List<String>,
    categoryKeys: List<String>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit,
    onPartitionClick: () -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState?,
    labelMode: Int,
    isFloatingStyle: Boolean,
    edgeToEdge: Boolean,
    skinPlainStyle: Boolean = false,
    skinPlainContentColor: Color? = null,
    isLiquidGlassEnabled: Boolean = false,
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC,
    liquidGlassTuning: LiquidGlassTuning? = null,
    backdrop: LayerBackdrop? = null,
    topTabSkinIconPaths: Map<String, TopTabSkinIconPaths> = emptyMap(),
    partitionSkinIconPath: String? = null,
    hasOuterChromeSurface: Boolean = false,
    /** When non-null, overrides [shouldWrapTopTabDockWidth] so shell and tabs share one decision. */
    wrapDockWidth: Boolean? = null,
    isTransitionRunning: Boolean = false,
    showPartitionAction: Boolean = true,
    forceMaterialUnderline: Boolean = false
) {
    val uiPreset = LocalUiPreset.current
    val haptic = com.android.purebilibili.core.util.rememberHapticFeedback()
    val scrollChannel = com.android.purebilibili.feature.home.LocalHomeScrollChannel.current
    val normalizedLabelMode = normalizeTopTabLabelMode(labelMode)
    val showIcon = shouldShowTopTabIcon(normalizedLabelMode)
    val showText = shouldShowTopTabText(normalizedLabelMode)
    val effectiveRenderer = if (skinPlainStyle || forceMaterialUnderline) {
        HomeTopTabRenderer.MD3
    } else {
        renderer
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, (categories.size - 1).coerceAtLeast(0))
    val topTabDragMotionSpec = remember { resolveSegmentedControlMotionSpec() }
    var topTabIndicatorDragEngaged by remember { mutableStateOf(false) }
    val topTabDragState = rememberDampedDragAnimationState(
        initialIndex = safeSelectedIndex,
        itemCount = categories.size.coerceAtLeast(1),
        motionSpec = topTabDragMotionSpec,
        holdPressUntilReleaseTargetSettles = true,
        onIndexChanged = { index ->
            if (index in categories.indices) {
                onCategorySelected(index)
            }
        }
    )
    LaunchedEffect(topTabDragState.settledReleaseCount) {
        if (topTabDragState.settledReleaseCount > 0) {
            topTabIndicatorDragEngaged = false
        }
    }
    val baseRowHeight = if (skinPlainStyle) {
        resolveHomeSkinTopTabRowHeight()
    } else when (effectiveRenderer) {
        HomeTopTabRenderer.IOS -> resolveIosTopTabRowHeight(isFloatingStyle, normalizedLabelMode)
        HomeTopTabRenderer.MD3 -> resolveMd3TopTabVisualSpec(
            isFloatingStyle = isFloatingStyle,
            labelMode = normalizedLabelMode
        ).rowHeight
        HomeTopTabRenderer.MIUIX -> resolveMd3TopTabVisualSpec(
            isFloatingStyle = false,
            androidNativeVariant = AndroidNativeVariant.MIUIX,
            labelMode = normalizedLabelMode
        ).rowHeight
    }
    val hasSkinStickerIcons = topTabSkinIconPaths.isNotEmpty() || !partitionSkinIconPath.isNullOrBlank()
    val rowHeight = resolveTopTabSkinStickerRowHeight(
        baseRowHeight = baseRowHeight,
        hasSkinStickerIcons = hasSkinStickerIcons,
        showIcon = showIcon,
        showText = showText
    )
    val actionButtonSize = if (skinPlainStyle) {
        resolveHomeSkinTopTabActionButtonSize()
    } else when (effectiveRenderer) {
        HomeTopTabRenderer.IOS -> resolveIosTopTabActionButtonSize(isFloatingStyle)
        HomeTopTabRenderer.MD3 -> resolveMd3TopTabActionButtonSize(isFloatingStyle)
        HomeTopTabRenderer.MIUIX -> resolveMd3TopTabActionButtonSize(
            isFloatingStyle = false,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
    }
    val actionButtonCorner = if (skinPlainStyle) {
        0.dp
    } else when (effectiveRenderer) {
        HomeTopTabRenderer.IOS -> resolveIosTopTabActionButtonCorner(isFloatingStyle)
        HomeTopTabRenderer.MD3 -> resolveMd3TopTabActionButtonCorner(isFloatingStyle)
        HomeTopTabRenderer.MIUIX -> resolveMd3TopTabActionButtonCorner(
            isFloatingStyle = false,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
    }
    val actionIconSize = if (skinPlainStyle) {
        resolveHomeSkinTopTabActionIconSize()
    } else when (effectiveRenderer) {
        HomeTopTabRenderer.IOS -> resolveIosTopTabActionIconSize(isFloatingStyle)
        HomeTopTabRenderer.MD3 -> resolveMd3TopTabActionIconSize(isFloatingStyle)
        HomeTopTabRenderer.MIUIX -> resolveMd3TopTabActionIconSize(
            isFloatingStyle = false,
            androidNativeVariant = AndroidNativeVariant.MIUIX
        )
    }
    val listState = rememberLazyListState()
    var tabViewportLeftInWindowPx by remember { mutableFloatStateOf(Float.NaN) }
    var selectedItemLeftInWindowPx by remember { mutableFloatStateOf(Float.NaN) }
    val pagerIsDragging = rememberTopTabPagerDragHeld(pagerState)
    val currentPosition by remember(pagerState, selectedIndex) {
        derivedStateOf {
            resolveTopTabIndicatorRenderPosition(
                selectedIndex = selectedIndex,
                pagerCurrentPage = pagerState?.currentPage,
                pagerTargetPage = pagerState?.targetPage,
                pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction,
                pagerIsScrolling = pagerState?.isScrollInProgress == true
            )
        }
    }
    val selectedContentPosition by remember(pagerState, selectedIndex) {
        derivedStateOf {
            resolveTopTabSelectedContentPosition(
                selectedIndex = selectedIndex,
                pagerCurrentPage = pagerState?.currentPage,
                pagerTargetPage = pagerState?.targetPage,
                pagerCurrentPageOffsetFraction = pagerState?.currentPageOffsetFraction,
                pagerIsScrolling = pagerState?.isScrollInProgress == true
            )
        }
    }

    LaunchedEffect(selectedIndex, categories.size) {
        selectedItemLeftInWindowPx = Float.NaN
        if (categories.isNotEmpty()) {
            val targetIndex = selectedIndex.coerceIn(0, categories.lastIndex)
            topTabDragState.updateIndex(targetIndex)
            listState.animateScrollToItem(targetIndex)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(
                horizontal = resolveTopTabRowHorizontalPaddingDp(
                    isFloatingStyle = isFloatingStyle,
                    edgeToEdge = edgeToEdge,
                    labelMode = normalizedLabelMode
                ).dp
            )
    ) {
        val wrapDock = wrapDockWidth ?: shouldWrapTopTabDockWidth(
            isFloatingStyle = isFloatingStyle,
            hasOuterChromeSurface = hasOuterChromeSurface,
            edgeToEdge = edgeToEdge
        )
        val fillItemWidthDp = when (effectiveRenderer) {
            HomeTopTabRenderer.IOS -> resolveIosTopTabItemWidthDp(
                containerWidthDp = maxWidth.value,
                categoryCount = categories.size,
                labelMode = normalizedLabelMode
            )
            HomeTopTabRenderer.MD3,
            HomeTopTabRenderer.MIUIX -> resolveMd3TopTabItemWidthDp(
                containerWidthDp = maxWidth.value,
                visibleSlots = resolveMd3TopTabLayoutVisibleSlots(
                    categoryCount = categories.size,
                    labelMode = normalizedLabelMode,
                    showPartitionAction = showPartitionAction
                )
            )
        }
        val itemWidthDp = resolveTopTabDockItemWidthDp(
            maxWidthDp = maxWidth.value,
            categoryCount = categories.size,
            labelMode = normalizedLabelMode,
            isFloatingStyle = isFloatingStyle,
            wrapContent = wrapDock,
            fillItemWidthDp = fillItemWidthDp
        )
        val itemWidth = itemWidthDp.dp
        // Prefer content-driven dock length; parent chrome also uses this policy so shell + tabs match.
        val dockContentWidthDp = if (wrapDock) {
            resolveTopTabDockWrapWidthDp(
                itemWidthDp = itemWidthDp,
                categoryCount = categories.size,
                maxWidthDp = maxWidth.value
            )
        } else {
            maxWidth.value
        }
        val density = LocalDensity.current
        val isDarkTheme = isSystemInDarkTheme()
        // When dock wraps content, no leftover to center — lead padding is always 0.
        val md3ContentPadding = if (
            (effectiveRenderer == HomeTopTabRenderer.MD3 ||
                effectiveRenderer == HomeTopTabRenderer.MIUIX) &&
            !wrapDock
        ) {
            resolveMd3TopTabContentPaddingDp(
                containerWidthDp = maxWidth.value,
                itemWidthDp = itemWidth.value,
                categoryCount = categories.size,
                labelMode = normalizedLabelMode
            ).dp
        } else {
            0.dp
        }
        val md3IndicatorWidth = if (skinPlainStyle) 30.dp else 28.dp
        val dockIndicatorHorizontalGap = resolveTopTabDockIndicatorHorizontalGapDp(
            hasOuterChromeSurface = hasOuterChromeSurface
        ).dp
        val dockIndicatorVerticalGap = resolveTopTabDockIndicatorVerticalGapDp(
            hasOuterChromeSurface = hasOuterChromeSurface
        ).dp
        val md3TopTabRowVerticalTranslationPx = with(density) {
            resolveMd3TopTabRowVerticalTranslationDp(
                skinPlainStyle = skinPlainStyle,
                hasOuterChromeSurface = hasOuterChromeSurface
            ).dp.toPx()
        }
        val rowScrollOffsetPx by remember(itemWidth, density, listState) {
            derivedStateOf {
                with(density) {
                    listState.firstVisibleItemIndex * itemWidth.toPx() +
                        listState.firstVisibleItemScrollOffset
                }
            }
        }
        val rowScrollStartPadding = with(density) { (-rowScrollOffsetPx).toDp() }
        val pagerIsScrolling = pagerState?.isScrollInProgress == true
        val topTabDragPosition by remember(topTabDragState, categories.size) {
            derivedStateOf {
                topTabDragState.value.coerceIn(0f, (categories.size - 1).coerceAtLeast(0).toFloat())
            }
        }
        val topTabDragActive by remember(topTabDragState, topTabIndicatorDragEngaged) {
            derivedStateOf {
                topTabIndicatorDragEngaged &&
                    (topTabDragState.isDragging || topTabDragState.isRunning || topTabDragState.pressProgress > 0.001f)
            }
        }
        val topTabIndicatorPosition = if (topTabDragActive) topTabDragPosition else currentPosition
        val topTabContentPosition = if (topTabDragActive) {
            topTabDragPosition
        } else if (effectiveRenderer == HomeTopTabRenderer.IOS) {
            selectedContentPosition
        } else {
            currentPosition
        }
        val iosCapsulePosition = if (topTabDragActive) topTabDragPosition else selectedContentPosition
        val indicatorIsInteracting = pagerIsDragging || pagerIsScrolling || topTabDragActive
        val topTabShouldStretchIndicator = (topTabDragActive && topTabDragState.isDragging) ||
            shouldDeformTopTabIndicator(
                position = topTabIndicatorPosition,
                isInMotion = indicatorIsInteracting
            )
        val topTabVelocityPositionTracker = remember { FloatArray(1) { topTabIndicatorPosition } }
        val topTabVelocityTimeTracker = remember { LongArray(1) { System.nanoTime() } }
        val topTabPagerVelocityItemsPerSecond = if (topTabDragActive) {
            0f
        } else {
            resolveTopTabPagerVelocityItemsPerSecond(
                currentPosition = topTabIndicatorPosition,
                previousPosition = topTabVelocityPositionTracker[0],
                elapsedNanos = (System.nanoTime() - topTabVelocityTimeTracker[0]).coerceAtLeast(1L)
            )
        }
        SideEffect {
            topTabVelocityPositionTracker[0] = topTabIndicatorPosition
            topTabVelocityTimeTracker[0] = System.nanoTime()
        }
        val topTabMotionVelocityItemsPerSecond = if (topTabDragActive) {
            topTabDragState.deformationVelocityItemsPerSecond
        } else {
            topTabPagerVelocityItemsPerSecond
        }
        val topTabIndicatorLayerVelocityItemsPerSecond =
            resolveTopTabIndicatorLayerVelocityItemsPerSecond(
                motionVelocityItemsPerSecond = topTabMotionVelocityItemsPerSecond
            )
        val topTabMotionVelocityPxPerSecond = with(density) {
            if (topTabDragActive) {
                topTabDragState.velocityPxPerSecond
            } else {
                topTabMotionVelocityItemsPerSecond * itemWidth.toPx()
            }
        }
        val topTabIndicatorDragScaleProgress = rememberBottomBarIndicatorDragScaleProgress(
            isDragging = topTabDragActive
        )
        val topTabPressProgress = if (topTabDragActive) {
            topTabDragState.pressProgress
        } else {
            0f
        }
        val topTabIndicatorLayerScaleProgress = resolveTopTabIndicatorScaleProgress(
            pagerSliding = !topTabDragActive && topTabShouldStretchIndicator,
            dragScaleProgress = topTabIndicatorDragScaleProgress,
            pressProgress = topTabPressProgress
        )
        // Match home bottom bar: velocity stretch from items/sec only (no dragState.scale compound).
        val topTabIndicatorLayerTransform = resolveBottomBarIndicatorLayerTransform(
            motionProgress = topTabPressProgress,
            velocityItemsPerSecond = topTabIndicatorLayerVelocityItemsPerSecond,
            isDragging = topTabShouldStretchIndicator,
            dragScaleProgress = topTabIndicatorLayerScaleProgress,
            dragScaleTransform = null,
            motionSpec = topTabDragMotionSpec
        )
        val topTabRefractionMotionProfile = resolveBottomBarRefractionMotionProfile(
            position = topTabIndicatorPosition,
            velocity = topTabMotionVelocityPxPerSecond,
            isDragging = indicatorIsInteracting,
            motionSpec = topTabDragMotionSpec
        )
        val topTabMotionProgress = resolveSegmentedControlMotionProgress(
            pressProgress = topTabPressProgress,
            refractionProgress = topTabRefractionMotionProfile.progress,
            tapPressRefractionEnabled = true
        )
        val topTabPanelOffsetPx by remember(density, itemWidth, categories.size, topTabDragState) {
            derivedStateOf {
                val dockWidthPx = with(density) {
                    (itemWidth * categories.size.coerceAtLeast(1)).toPx()
                }.coerceAtLeast(1f)
                val maxOffsetPx = with(density) { 4.dp.toPx() }
                resolveSharedLiquidIndicatorPanelOffsetPx(
                    dragOffsetPx = topTabDragState.dragOffset,
                    dockWidthPx = dockWidthPx,
                    maxOffsetPx = maxOffsetPx
                )
            }
        }
        val topTabBackdropPresetProgress = resolveBottomBarBackdropPresetProgress(
            motionProgress = topTabMotionProgress,
            verticalProgress = 0f,
            pressProgress = topTabPressProgress
        )
        // Swipe/press lens progress so theme-tinted glass follows the capsule.
        val topTabIndicatorLensSpec = resolveBottomBarBackdropPresetIndicatorLens(
            progress = resolveSharedLiquidIndicatorLensProgress(
                pressProgress = topTabPressProgress,
                motionProgress = topTabMotionProgress,
                isDragging = topTabDragActive
            )
        )
        val topTabIndicatorHighlightAlpha = resolveBottomBarLiquidGlassHighlightAlpha(
            motionProgress = topTabBackdropPresetProgress.indicatorProgress
        )
        val topTabIndicatorGlowAlpha = resolveBottomBarIndicatorGlowAlpha(
            glassEnabled = topTabDragActive || isLiquidGlassEnabled,
            pressProgress = topTabPressProgress,
            motionProgress = topTabMotionProgress
        )
        val md3IndicatorTranslationXPx by remember(topTabIndicatorPosition, itemWidth, md3IndicatorWidth, density, listState) {
            derivedStateOf {
                with(density) {
                    resolveMd3TopTabIndicatorTranslationPx(
                        absolutePagerPosition = topTabIndicatorPosition,
                        itemWidthPx = itemWidth.toPx(),
                        rowScrollOffsetPx = rowScrollOffsetPx,
                        indicatorWidthPx = md3IndicatorWidth.toPx(),
                        contentPaddingPx = md3ContentPadding.toPx()
                    )
                }
            }
        }
        val md3LiquidCapsuleWidth = resolveTopTabDockIndicatorWidthDp(
            itemWidthDp = itemWidth.value,
            horizontalGapDp = dockIndicatorHorizontalGap.value,
            minWidthDp = md3IndicatorWidth.value
        ).dp
        val dockIndicatorHeight = resolveTopTabDockIndicatorHeightDp(
            rowHeightDp = rowHeight.value,
            verticalGapDp = dockIndicatorVerticalGap.value,
            // Prefer near-full dock fill at rest (bottom-bar like); drag scale overflows.
            minHeightDp = resolveTopTabVisualTuning().floatingIndicatorHeightDp,
            indicatorWidthDp = md3LiquidCapsuleWidth.value
        ).dp
        val md3LiquidCapsuleTranslationXPx by remember(
            topTabIndicatorPosition,
            itemWidth,
            md3LiquidCapsuleWidth,
            density,
            listState
        ) {
            derivedStateOf {
                with(density) {
                    resolveMd3TopTabIndicatorTranslationPx(
                        absolutePagerPosition = topTabIndicatorPosition,
                        itemWidthPx = itemWidth.toPx(),
                        rowScrollOffsetPx = rowScrollOffsetPx,
                        indicatorWidthPx = md3LiquidCapsuleWidth.toPx(),
                        contentPaddingPx = md3ContentPadding.toPx()
                    )
                }
            }
        }
        val shouldUseMovingIosCapsule = effectiveRenderer == HomeTopTabRenderer.IOS &&
            !skinPlainStyle &&
            !hasSkinStickerIcons
        val shouldUseLiquidGlassIndicator = isLiquidGlassEnabled &&
            !skinPlainStyle &&
            !hasSkinStickerIcons
        val shouldRenderTopTabLiquidGlassIndicator = shouldUseLiquidGlassIndicator &&
            !hasOuterChromeSurface
        val shouldUseMd3LiquidCapsule = effectiveRenderer == HomeTopTabRenderer.MD3 &&
            shouldRenderTopTabLiquidGlassIndicator
        val shouldUseMd3DockBackedCapsule = effectiveRenderer == HomeTopTabRenderer.MD3 &&
            shouldUseLiquidGlassIndicator &&
            hasOuterChromeSurface
        val shouldPrimeTopTabLiquidGlassCapture =
            isLiquidGlassEnabled &&
                !skinPlainStyle &&
                !hasSkinStickerIcons
        val isTopTabIndicatorInteractionActive =
            topTabDragActive || topTabShouldStretchIndicator || topTabPressProgress > 0.001f
        val topTabIndicatorVisualPolicy = resolveTopTabIndicatorVisualPolicy(
            position = topTabIndicatorPosition,
            interacting = indicatorIsInteracting,
            velocityPxPerSecond = topTabMotionVelocityPxPerSecond,
            useNeutralIndicatorTint = shouldUseLiquidGlassIndicator
        )
        val shouldRenderTopTabIndicatorBackdropRaw = shouldRenderBottomBarIndicatorBackdrop(
            glassEnabled = shouldUseLiquidGlassIndicator,
            hasContentBackdrop = true,
            indicatorProgress = topTabMotionProgress,
            isTransitionRunning = isTransitionRunning,
            isBottomBarInteractionActive = isTopTabIndicatorInteractionActive,
            allowIdleGlassEffect = false,
            allowTransitionIndicatorPulse = topTabPressProgress > 0.001f
        )
        // [KSU 对齐] 玻璃开启时指示器采样层常驻，避免 idle ↔ 交互切换时 tabsBackdrop 为空导致折射瞬态。
        val glassLayersAlwaysOn = shouldUseLiquidGlassIndicator
        val shouldRenderTopTabIndicatorBackdrop =
            glassLayersAlwaysOn || shouldRenderTopTabIndicatorBackdropRaw
        val topTabIndicatorBackdropPolicy = resolveTopTabIndicatorBackdropPolicy(
            effectiveLiquidGlassEnabled = shouldUseLiquidGlassIndicator,
            hasBackdrop = backdrop != null,
            indicatorVisualPolicy = topTabIndicatorVisualPolicy
        )
        val topTabContentBackdrop = rememberLayerBackdrop()
        val effectiveTopTabIndicatorContentBackdrop: Backdrop? = when {
            !shouldRenderTopTabIndicatorBackdrop ||
                !topTabIndicatorBackdropPolicy.useIndicatorBackdrop -> null
            topTabIndicatorBackdropPolicy.useCombinedBackdrop && backdrop != null ->
                rememberCombinedBackdrop(backdrop, topTabContentBackdrop)
            else -> topTabContentBackdrop
        }
        val topTabLensProgress = resolveSharedLiquidIndicatorLensProgress(
            pressProgress = topTabPressProgress,
            motionProgress = topTabMotionProgress,
            isDragging = topTabDragActive
        )
        val useTopTabGlassColorPath = resolveSharedLiquidIndicatorUseGlassColorPath(
            liquidGlassEnabled = shouldUseLiquidGlassIndicator,
            lensProgress = topTabLensProgress
        )
        val topTabThemeColor = MaterialTheme.colorScheme.primary
        val topTabExportTintColor = resolveAndroidNativeExportTintColor(
            themeColor = topTabThemeColor,
            darkTheme = isDarkTheme
        )
        val topTabExportMonochromeColor = resolveSharedLiquidExportMonochromeColor(
            darkTheme = isDarkTheme
        )
        val measuredSelectedItemLeftPx by remember(shouldUseMovingIosCapsule) {
            derivedStateOf {
                if (!shouldUseMovingIosCapsule ||
                    tabViewportLeftInWindowPx.isNaN() ||
                    selectedItemLeftInWindowPx.isNaN()
                ) {
                    null
                } else {
                    selectedItemLeftInWindowPx - tabViewportLeftInWindowPx
                }
            }
        }
        val iosCapsuleTargetTranslationXPx by remember(
            iosCapsulePosition,
            measuredSelectedItemLeftPx,
            itemWidth,
            density,
            rowScrollOffsetPx,
            pagerState,
            pagerIsDragging,
            topTabDragActive
        ) {
            derivedStateOf {
                with(density) {
                    resolveIosTopTabCapsuleTargetTranslationPx(
                        measuredSelectedItemLeftPx = measuredSelectedItemLeftPx,
                        absolutePagerPosition = iosCapsulePosition,
                        itemWidthPx = itemWidth.toPx(),
                        rowScrollOffsetPx = rowScrollOffsetPx,
                        contentPaddingPx = IOS_TOP_TAB_CONTENT_PADDING_DP.dp.toPx(),
                        followPagerPosition = pagerIsDragging || pagerIsScrolling || topTabDragActive
                    )
                }
            }
        }
        val shouldAnimateIosCapsule = shouldAnimateIosTopTabCapsule(
            pagerIsDragging = pagerIsDragging,
            pagerIsScrolling = pagerIsScrolling || topTabDragActive
        )
        val animatedIosCapsuleTranslationXPx by animateFloatAsState(
            targetValue = iosCapsuleTargetTranslationXPx,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "iosTopTabCapsuleTranslation"
        )
        val iosCapsuleTranslationXPx = if (shouldAnimateIosCapsule) {
            animatedIosCapsuleTranslationXPx
        } else {
            iosCapsuleTargetTranslationXPx
        }
        Row(
            modifier = Modifier
                .then(
                    if (wrapDock) {
                        Modifier
                            .width(dockContentWidthDp.dp)
                            .fillMaxHeight()
                            .align(Alignment.Center)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .graphicsLayer {
                    translationY = if (effectiveRenderer == HomeTopTabRenderer.MD3) {
                        md3TopTabRowVerticalTranslationPx
                    } else {
                        0f
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { coordinates ->
                        tabViewportLeftInWindowPx = coordinates.boundsInWindow().left
                    }
            ) {
                val topTabHorizontalPadding = if (effectiveRenderer == HomeTopTabRenderer.IOS) {
                    IOS_TOP_TAB_CONTENT_PADDING_DP.dp
                } else {
                    md3ContentPadding
                }
                val topTabContentPadding = PaddingValues(horizontal = topTabHorizontalPadding)
                // Frame-synced export scroll (no second LazyList / LaunchedEffect lag → no ghost).
                val topTabListScrollOffsetPx by remember(density, itemWidth, listState) {
                    derivedStateOf {
                        with(density) {
                            listState.firstVisibleItemIndex * itemWidth.toPx() +
                                listState.firstVisibleItemScrollOffset.toFloat()
                        }
                    }
                }
                val topTabContentPanelOffsetPx =
                    if (shouldUseLiquidGlassIndicator) topTabPanelOffsetPx else 0f
                val topTabHorizontalPaddingPx = with(density) { topTabHorizontalPadding.toPx() }
                // One shared shift for export + visible + capsule avoids double panel offset ghosts.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = topTabContentPanelOffsetPx }
                ) {
                // Hidden monochrome export row: theme tint → pure primary under glass.
                if (shouldPrimeTopTabLiquidGlassCapture) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clearAndSetSemantics {}
                            .alpha(0f)
                            .zIndex(0f)
                            .layerBackdrop(topTabContentBackdrop)
                            .graphicsLayer {
                                // Only mirror LazyRow content origin (padding - scroll). No extra panel offset.
                                translationX = topTabHorizontalPaddingPx - topTabListScrollOffsetPx
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            categories.forEachIndexed { index, category ->
                                val categoryKey = categoryKeys.getOrNull(index) ?: category
                                LightweightTopTabItem(
                                    renderer = effectiveRenderer,
                                    category = category,
                                    categoryKey = categoryKey,
                                    index = index,
                                    selectionFraction = 1f,
                                    selectedIndex = selectedIndex,
                                    showIcon = showIcon,
                                    showText = showText,
                                    itemWidth = itemWidth,
                                    skinPlainStyle = false,
                                    drawContainer = false,
                                    skinIconPaths = null,
                                    hasSkinStickerIcon = false,
                                    useClickIndication = false,
                                    colorMode = TopTabLiquidColorMode.GLASS_EXPORT,
                                    exportMonochromeColor = topTabExportMonochromeColor,
                                    modifier = Modifier.graphicsLayer(
                                        colorFilter = ColorFilter.tint(topTabExportTintColor)
                                    ),
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    contentPadding = topTabContentPadding
                ) {
                    itemsIndexed(
                        items = categories,
                        key = { index, category -> categoryKeys.getOrNull(index) ?: category }
                    ) { index, category ->
                        val categoryKey = categoryKeys.getOrNull(index) ?: category
                        val selectionFraction = (1f - abs(topTabContentPosition - index.toFloat())).coerceIn(0f, 1f)
                        val drawItemContainer = shouldDrawLightweightTopTabItemContainer(
                            renderer = effectiveRenderer,
                            skinPlainStyle = skinPlainStyle,
                            hasSkinStickerIcon = hasSkinStickerIcons
                        )
                        val measuredItemModifier = if (shouldUseMovingIosCapsule && index == selectedIndex) {
                            Modifier.onGloballyPositioned { coordinates ->
                                selectedItemLeftInWindowPx = coordinates.boundsInWindow().left
                            }
                        } else {
                            Modifier
                        }
                        val gestureItemModifier = if (index == safeSelectedIndex) {
                            measuredItemModifier.topTabSelectedItemDrag(
                                dragState = topTabDragState,
                                itemWidthPx = with(density) { itemWidth.toPx() },
                                itemCount = categories.size,
                                onDragEngaged = {
                                    topTabIndicatorDragEngaged = true
                                }
                            )
                        } else {
                            measuredItemModifier
                        }
                        LightweightTopTabItem(
                            renderer = effectiveRenderer,
                            category = category,
                            categoryKey = categoryKey,
                            index = index,
                            selectionFraction = selectionFraction,
                            selectedIndex = selectedIndex,
                            showIcon = showIcon,
                            showText = showText,
                            itemWidth = itemWidth,
                            skinPlainStyle = skinPlainStyle,
                            skinPlainContentColor = skinPlainContentColor,
                            drawContainer = drawItemContainer,
                            skinIconPaths = topTabSkinIconPaths[categoryKey.trim().uppercase()],
                            hasSkinStickerIcon = hasSkinStickerIcons,
                            useClickIndication = shouldUseLightweightTopTabItemClickIndication(
                                renderer = effectiveRenderer,
                                skinPlainStyle = skinPlainStyle,
                                usesCapsuleIndicator = shouldUseMovingIosCapsule ||
                                    shouldUseMd3LiquidCapsule ||
                                    shouldUseMd3DockBackedCapsule
                            ),
                            colorMode = if (useTopTabGlassColorPath) {
                                TopTabLiquidColorMode.GLASS_VISIBLE
                            } else {
                                TopTabLiquidColorMode.NORMAL
                            },
                            modifier = gestureItemModifier,
                            onClick = {
                                performHomeTopBarTap(haptic = haptic, onClick = {
                                    when (resolveTopTabClickAction(index, selectedIndex)) {
                                        TopTabClickAction.SELECT_TAB -> onCategorySelected(index)
                                        TopTabClickAction.SCROLL_TO_TOP -> scrollChannel?.trySend(Unit)
                                    }
                                })
                            }
                        )
                    }
                }
                // Capsule above labels; panel offset is on parent so do NOT add again here.
                // clip=false so drag-scale (88/56) can slightly exceed the dock chrome.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .graphicsLayer { clip = false }
                ) {
                    if (shouldUseMovingIosCapsule) {
                        val capsuleShape = resolveSharedBottomBarCapsuleShape()
                        val indicatorWidth = resolveTopTabDockIndicatorWidthDp(
                            itemWidthDp = itemWidth.value,
                            horizontalGapDp = dockIndicatorHorizontalGap.value
                        ).dp
                        KernelSuBottomBarIndicatorLayer(
                            visible = true,
                            dockContentAlpha = 1f,
                            indicatorTranslationXPx = resolveTopTabDockIndicatorOffsetPx(
                                slotTranslationPx = iosCapsuleTranslationXPx,
                                horizontalGapPx = with(density) {
                                    dockIndicatorHorizontalGap.toPx()
                                }
                            ),
                            indicatorPanelOffsetPx = 0f,
                            indicatorSettleReboundTransform = BottomBarClickPulseTransform(scaleX = 1f),
                            indicatorWidth = indicatorWidth,
                            indicatorHeight = dockIndicatorHeight,
                            shellShape = capsuleShape,
                            liquidGlassPreset = BottomBarLiquidGlassPreset.BILIPAI_TUNED,
                            // Prefer export capture so capsule shows theme-tinted glyphs.
                            contentBackdrop = topTabContentBackdrop,
                            backdrop = effectiveTopTabIndicatorContentBackdrop ?: backdrop,
                            indicatorLensSpec = topTabIndicatorLensSpec,
                            effectivePressProgress = topTabLensProgress,
                            indicatorIdleSurfaceColor = resolveIosTopTabCapsuleContainerColor(
                                isDarkTheme = isDarkTheme,
                                selectionFraction = 1f
                            ),
                            glassEnabled = shouldUseLiquidGlassIndicator,
                            indicatorEffectsEnabled = shouldUseLiquidGlassIndicator,
                            motionProgress = topTabMotionProgress,
                            velocityItemsPerSecond = topTabIndicatorLayerVelocityItemsPerSecond,
                            isDragging = topTabShouldStretchIndicator,
                            indicatorLayerScaleProgress = topTabIndicatorLayerScaleProgress,
                            indicatorLayerScaleTransform = null,
                            bottomBarMotionSpec = topTabDragMotionSpec,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    if (shouldUseMd3DockBackedCapsule) {
                        KernelSuBottomBarIndicatorLayer(
                            visible = true,
                            dockContentAlpha = 1f,
                            indicatorTranslationXPx = md3LiquidCapsuleTranslationXPx,
                            indicatorPanelOffsetPx = 0f,
                            indicatorSettleReboundTransform = BottomBarClickPulseTransform(scaleX = 1f),
                            indicatorWidth = md3LiquidCapsuleWidth,
                            indicatorHeight = dockIndicatorHeight,
                            shellShape = resolveSharedBottomBarCapsuleShape(),
                            liquidGlassPreset = BottomBarLiquidGlassPreset.BILIPAI_TUNED,
                            // Prefer export capture so capsule shows theme-tinted glyphs.
                            contentBackdrop = topTabContentBackdrop,
                            backdrop = effectiveTopTabIndicatorContentBackdrop ?: backdrop,
                            indicatorLensSpec = topTabIndicatorLensSpec,
                            effectivePressProgress = topTabLensProgress,
                            indicatorIdleSurfaceColor = resolveAndroidNativeIdleIndicatorSurfaceColor(
                                darkTheme = isDarkTheme
                            ),
                            glassEnabled = true,
                            motionProgress = topTabMotionProgress,
                            velocityItemsPerSecond = topTabIndicatorLayerVelocityItemsPerSecond,
                            isDragging = topTabShouldStretchIndicator,
                            indicatorLayerScaleProgress = topTabIndicatorLayerScaleProgress,
                            indicatorLayerScaleTransform = null,
                            bottomBarMotionSpec = topTabDragMotionSpec,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    if (shouldUseMd3LiquidCapsule) {
                        val capsuleShape = resolveSharedBottomBarCapsuleShape()
                        KernelSuBottomBarIndicatorLayer(
                            visible = true,
                            dockContentAlpha = 1f,
                            indicatorTranslationXPx = md3LiquidCapsuleTranslationXPx,
                            indicatorPanelOffsetPx = 0f,
                            indicatorSettleReboundTransform = BottomBarClickPulseTransform(scaleX = 1f),
                            indicatorWidth = md3LiquidCapsuleWidth,
                            indicatorHeight = dockIndicatorHeight,
                            shellShape = capsuleShape,
                            liquidGlassPreset = BottomBarLiquidGlassPreset.BILIPAI_TUNED,
                            // Prefer export capture so capsule shows theme-tinted glyphs.
                            contentBackdrop = topTabContentBackdrop,
                            backdrop = effectiveTopTabIndicatorContentBackdrop ?: backdrop,
                            indicatorLensSpec = topTabIndicatorLensSpec,
                            effectivePressProgress = topTabLensProgress,
                            indicatorIdleSurfaceColor = if (isDarkTheme) {
                                Color.White.copy(alpha = 0.1f)
                            } else {
                                Color.Black.copy(alpha = 0.1f)
                            },
                            glassEnabled = true,
                            motionProgress = topTabMotionProgress,
                            velocityItemsPerSecond = topTabIndicatorLayerVelocityItemsPerSecond,
                            isDragging = topTabShouldStretchIndicator,
                            indicatorLayerScaleProgress = topTabIndicatorLayerScaleProgress,
                            indicatorLayerScaleTransform = null,
                            bottomBarMotionSpec = topTabDragMotionSpec,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
                } // shared panel-offset group (export + visible + capsule)

                if (effectiveRenderer == HomeTopTabRenderer.MD3 && !hasSkinStickerIcons) {
                    val indicatorColor = if (skinPlainStyle && skinPlainContentColor != null) {
                        resolveHomeSkinTopTabIndicatorColor(skinPlainContentColor)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    val indicatorBottomPadding = if (skinPlainStyle) {
                        resolveHomeSkinTopTabIndicatorBottomPadding()
                    } else {
                        resolveMd3TopTabIndicatorBottomPadding()
                    }
                    if (!shouldUseMd3DockBackedCapsule && !shouldUseMd3LiquidCapsule) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(bottom = indicatorBottomPadding)
                                .graphicsLayer {
                                    translationX = md3IndicatorTranslationXPx
                                    scaleX = topTabIndicatorLayerTransform.scaleX
                                    scaleY = topTabIndicatorLayerTransform.scaleY
                                }
                                .width(md3IndicatorWidth)
                                .height(2.dp)
                                .clip(AppShapes.container(ContainerLevel.Pill))
                                .background(indicatorColor)
                        )
                    }
                }
            }

            if (showPartitionAction) {
                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(actionButtonSize)
                        .then(
                            if (skinPlainStyle) {
                                Modifier
                            } else {
                                Modifier.clip(RoundedCornerShape(actionButtonCorner))
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current
                        ) {
                            performHomeTopBarTap(haptic = haptic, onClick = onPartitionClick)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!partitionSkinIconPath.isNullOrBlank()) {
                        AsyncImage(
                            model = File(partitionSkinIconPath),
                            contentDescription = "浏览全部分区",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(resolveTopTabSkinPartitionIconSize())
                        )
                    } else {
                        Icon(
                            resolveTopTabPartitionIcon(uiPreset),
                            contentDescription = "浏览全部分区",
                            tint = skinPlainContentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

internal enum class TopTabLiquidColorMode {
    /** Normal selected/unselected lerp. */
    NORMAL,
    /** Visible layer while glass is sliding — neutral so theme color lives under glass. */
    GLASS_VISIBLE,
    /** Hidden export layer monochrome glyphs before theme ColorFilter.tint. */
    GLASS_EXPORT
}

@Composable
private fun LightweightTopTabItem(
    renderer: HomeTopTabRenderer,
    category: String,
    categoryKey: String,
    index: Int,
    selectionFraction: Float,
    selectedIndex: Int,
    showIcon: Boolean,
    showText: Boolean,
    itemWidth: Dp,
    skinPlainStyle: Boolean = false,
    skinPlainContentColor: Color? = null,
    drawContainer: Boolean = true,
    skinIconPaths: TopTabSkinIconPaths? = null,
    hasSkinStickerIcon: Boolean = false,
    useClickIndication: Boolean = true,
    colorMode: TopTabLiquidColorMode = TopTabLiquidColorMode.NORMAL,
    exportMonochromeColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val uiPreset = LocalUiPreset.current
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()
    val selected = selectionFraction > 0.5f || index == selectedIndex
    val skinIconPath = skinIconPaths?.pathFor(selected)
    val icon = resolveTopTabCategoryIcon(categoryKey, uiPreset)
    val selectedColor = when (renderer) {
        HomeTopTabRenderer.IOS -> if (skinPlainStyle) {
            skinPlainContentColor ?: colorScheme.onSurface
        } else {
            resolveIosTopTabSelectedContentColor(colorScheme)
        }
        HomeTopTabRenderer.MD3 -> if (skinPlainStyle) {
            skinPlainContentColor ?: colorScheme.onSurface
        } else {
            colorScheme.primary
        }
        HomeTopTabRenderer.MIUIX -> if (skinPlainStyle) {
            skinPlainContentColor ?: colorScheme.onSurface
        } else {
            colorScheme.onSecondaryContainer
        }
    }
    val unselectedColor = if (skinPlainStyle) {
        resolveHomeSkinTopTabUnselectedContentColor(skinPlainContentColor ?: colorScheme.onSurface)
    } else {
        colorScheme.onSurfaceVariant
    }
    val contentColor = when (colorMode) {
        TopTabLiquidColorMode.GLASS_EXPORT -> exportMonochromeColor
        TopTabLiquidColorMode.GLASS_VISIBLE -> unselectedColor
        TopTabLiquidColorMode.NORMAL -> androidx.compose.ui.graphics.lerp(
            unselectedColor,
            selectedColor,
            selectionFraction
        )
    }
    val containerColor = when {
        !drawContainer || colorMode == TopTabLiquidColorMode.GLASS_EXPORT -> Color.Transparent
        skinPlainStyle -> Color.Transparent
        renderer == HomeTopTabRenderer.IOS && colorMode == TopTabLiquidColorMode.NORMAL ->
            resolveIosTopTabCapsuleContainerColor(
                isDarkTheme = isDarkTheme,
                selectionFraction = selectionFraction
            )
        renderer == HomeTopTabRenderer.MD3 -> Color.Transparent
        colorMode == TopTabLiquidColorMode.GLASS_VISIBLE -> Color.Transparent
        else -> colorScheme.secondaryContainer.copy(alpha = 0.70f * selectionFraction)
    }
    val itemShape = when {
        skinPlainStyle -> androidx.compose.ui.graphics.RectangleShape
        renderer == HomeTopTabRenderer.IOS -> resolveSharedBottomBarCapsuleShape()
        renderer == HomeTopTabRenderer.MD3 -> androidx.compose.ui.graphics.RectangleShape
        else -> AppShapes.container(ContainerLevel.Dialog)
    }

    Box(
        modifier = modifier
            .width(itemWidth)
            .fillMaxHeight()
            .padding(
                horizontal = 3.dp,
                vertical = if (hasSkinStickerIcon) {
                    resolveTopTabSkinStickerItemVerticalPadding(showText = showText)
                } else {
                    4.dp
                }
            )
            .clip(itemShape)
            .background(containerColor, itemShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (useClickIndication) LocalIndication.current else null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showIcon) {
                if (!skinIconPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(skinIconPath),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(resolveTopTabSkinStickerIconSize(showText = showText))
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(
                            resolveTopTabIconSizeDp(if (showText) 0 else 1).dp
                        )
                    )
                }
            }
            if (showIcon && showText) {
                Spacer(modifier = Modifier.height(resolveTopTabIconTextSpacingDp(0).dp))
            }
            if (showText) {
                val labelMode = when {
                    showIcon && showText -> 0
                    showIcon -> 1
                    else -> 2
                }
                Text(
                    text = category,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = resolveTopTabLabelTextSizeSp(labelMode).sp,
                    lineHeight = resolveTopTabLabelLineHeightSp(labelMode).sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor
                )
            }
            if (hasSkinStickerIcon && showText) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(resolveTopTabSkinStickerIndicatorWidth())
                        .height(2.dp)
                        .clip(AppShapes.container(ContainerLevel.Pill))
                        .background(selectedColor)
                        .alpha(selectionFraction)
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTabRow(
    categories: List<String> = resolveHomeTopCategories().map { it.label },
    categoryKeys: List<String> = resolveHomeTopCategories().map { it.name },
    selectedIndex: Int = 0,
    onCategorySelected: (Int) -> Unit = {},
    onPartitionClick: () -> Unit = {},
    pagerState: androidx.compose.foundation.pager.PagerState? = null, // [New] PagerState for sync
    labelMode: Int = 2,
    isLiquidGlassEnabled: Boolean = false,
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC,
    liquidGlassTuning: LiquidGlassTuning? = null,
    hazeState: HazeState? = null,
    backdrop: LayerBackdrop? = null,
    isFloatingStyle: Boolean = false,
    edgeToEdge: Boolean = false,
    hasOuterChromeSurface: Boolean = false,
    /** Shared with [HomeTopTabChrome.wrapDockWidth] so glass shell and tabs stay the same length. */
    wrapDockWidth: Boolean? = null,
    interactionBudget: HomeInteractionMotionBudget = HomeInteractionMotionBudget.FULL,
    motionTier: MotionTier = MotionTier.Normal,
    isTransitionRunning: Boolean = false,
    forceLowBlurBudget: Boolean = false,
    isViewportSyncEnabled: Boolean = true,
    skinPlainStyle: Boolean = false,
    skinPlainContentColor: Color? = null,
    topTabSkinIconPaths: Map<String, TopTabSkinIconPaths> = emptyMap(),
    partitionSkinIconPath: String? = null,
    forceMaterialUnderline: Boolean = false
) {
    val presetStyle = resolveHomeTopPresetStyle(
        uiPreset = LocalUiPreset.current,
        androidNativeVariant = LocalAndroidNativeVariant.current,
        labelMode = labelMode
    )
    val showPartitionAction = false
    val hasSkinStickerIcons = topTabSkinIconPaths.isNotEmpty() || !partitionSkinIconPath.isNullOrBlank()
    LightweightHomeTopTabs(
        renderer = presetStyle.renderer,
        categories = categories,
        categoryKeys = categoryKeys,
        selectedIndex = selectedIndex,
        onCategorySelected = onCategorySelected,
        onPartitionClick = onPartitionClick,
        pagerState = pagerState,
        labelMode = labelMode,
        isFloatingStyle = isFloatingStyle,
        edgeToEdge = edgeToEdge,
        skinPlainStyle = skinPlainStyle,
        skinPlainContentColor = skinPlainContentColor,
        isLiquidGlassEnabled = isLiquidGlassEnabled,
        liquidGlassStyle = liquidGlassStyle,
        liquidGlassTuning = liquidGlassTuning,
        backdrop = backdrop,
        topTabSkinIconPaths = topTabSkinIconPaths,
        partitionSkinIconPath = partitionSkinIconPath,
        hasOuterChromeSurface = hasOuterChromeSurface,
        wrapDockWidth = wrapDockWidth,
        isTransitionRunning = isTransitionRunning,
        showPartitionAction = showPartitionAction,
        forceMaterialUnderline = forceMaterialUnderline
    )
}

@Composable
private fun rememberTopTabPagerDragHeld(
    pagerState: androidx.compose.foundation.pager.PagerState?
): Boolean {
    if (pagerState == null) return false
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    return isDragged
}

internal fun resolveTopTabIndicatorHitLeftPx(
    indicatorPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    contentPaddingPx: Float,
    indicatorWidthPx: Float
): Float {
    if (itemWidthPx <= 0f || indicatorWidthPx <= 0f) return contentPaddingPx
    val centeredIndicatorInsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    return contentPaddingPx +
        indicatorPosition.coerceAtLeast(0f) * itemWidthPx -
        rowScrollOffsetPx +
        centeredIndicatorInsetPx
}

internal fun shouldStartTopTabIndicatorLongPressDrag(
    pointerX: Float,
    indicatorPosition: Float,
    itemWidthPx: Float,
    rowScrollOffsetPx: Float,
    contentPaddingPx: Float,
    indicatorWidthPx: Float
): Boolean {
    if (itemWidthPx <= 0f || indicatorWidthPx <= 0f) return false
    val indicatorLeftPx = resolveTopTabIndicatorHitLeftPx(
        indicatorPosition = indicatorPosition,
        itemWidthPx = itemWidthPx,
        rowScrollOffsetPx = rowScrollOffsetPx,
        contentPaddingPx = contentPaddingPx,
        indicatorWidthPx = indicatorWidthPx
    )
    return pointerX in indicatorLeftPx..(indicatorLeftPx + indicatorWidthPx)
}

private fun Modifier.topTabSelectedItemDrag(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float,
    itemCount: Int,
    onDragEngaged: () -> Unit
): Modifier = pointerInput(
    dragState,
    itemWidthPx,
    itemCount
) {
    val velocityTracker = VelocityTracker()
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                onDragEngaged()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                dragState.onDrag(over, itemWidthPx)
            } ?: continue

            var isCancelled = false
            try {
                horizontalDrag(dragStart.id) { change ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    val dragAmount = change.position.x - change.previousPosition.x
                    val velocityX = velocityTracker.calculateVelocity().x
                    dragState.onDrag(dragAmount, itemWidthPx, velocityX)
                }
            } catch (e: Exception) {
                isCancelled = true
            }

            val velocityX = if (isCancelled) 0f else velocityTracker.calculateVelocity().x
            dragState.onDragEnd(
                velocityX = velocityX,
                itemWidthPx = itemWidthPx,
                notifyIndexChanged = true
            )
        }
    }
}

internal fun resolveTopTabIndicatorVelocity(
    horizontalVelocityPxPerSecond: Float
): Float {
    // 顶部指示器仅响应横向分页滑动，避免页面纵向滚动触发胶囊形变。
    return horizontalVelocityPxPerSecond.coerceIn(-4200f, 4200f)
}

internal fun resolveTopTabPagerVelocityItemsPerSecond(
    currentPosition: Float,
    previousPosition: Float,
    elapsedNanos: Long
): Float {
    if (elapsedNanos <= 0L) return 0f
    val elapsedSeconds = elapsedNanos / 1_000_000_000f
    if (elapsedSeconds <= 0f) return 0f
    return ((currentPosition - previousPosition) / elapsedSeconds).coerceIn(-12f, 12f)
}

internal fun resolveTopTabIndicatorLayerVelocityItemsPerSecond(
    motionVelocityItemsPerSecond: Float
): Float = motionVelocityItemsPerSecond

internal fun shouldTopTabIndicatorBeInteracting(
    pagerIsDragging: Boolean = false,
    pagerIsScrolling: Boolean,
    combinedVelocityPxPerSecond: Float,
    liquidGlassEnabled: Boolean
): Boolean {
    if (pagerIsDragging) return true
    if (pagerIsScrolling) return true
    val combinedThreshold = if (liquidGlassEnabled) 20f else 60f
    return abs(combinedVelocityPxPerSecond) > combinedThreshold
}

internal fun resolveTopTabIndicatorInteractionReleaseDelayMillis(
    liquidGlassEnabled: Boolean
): Long {
    return if (liquidGlassEnabled) 140L else 0L
}

internal fun shouldTopTabIndicatorUseRefraction(
    position: Float,
    interacting: Boolean,
    velocityPxPerSecond: Float,
    positionEpsilon: Float = 0.015f,
    velocityEpsilon: Float = 45f
): Boolean {
    val fractional = abs(position - position.roundToInt().toFloat()) > positionEpsilon
    if (fractional) return true
    return abs(velocityPxPerSecond) > velocityEpsilon
}

internal fun shouldDeformTopTabIndicator(
    position: Float,
    isInMotion: Boolean,
    positionEpsilon: Float = 0.015f
): Boolean {
    if (!isInMotion) return false
    return abs(position - position.roundToInt().toFloat()) > positionEpsilon
}

internal fun resolveTopTabIndicatorVisualPolicy(
    position: Float,
    interacting: Boolean,
    velocityPxPerSecond: Float,
    useNeutralIndicatorTint: Boolean
): BottomBarIndicatorVisualPolicy {
    val shouldRefract = shouldTopTabIndicatorUseRefraction(
        position = position,
        interacting = interacting,
        velocityPxPerSecond = velocityPxPerSecond
    )
    return BottomBarIndicatorVisualPolicy(
        isInMotion = shouldRefract,
        shouldRefract = shouldRefract,
        useNeutralTint = shouldRefract && useNeutralIndicatorTint
    )
}

internal fun resolveTopTabStaticIndicatorVisualPolicy(
    useNeutralIndicatorTint: Boolean
): BottomBarIndicatorVisualPolicy {
    return BottomBarIndicatorVisualPolicy(
        isInMotion = false,
        shouldRefract = false,
        useNeutralTint = useNeutralIndicatorTint
    )
}

internal fun resolveTopTabIndicatorLayerTransform(
    motionProgress: Float,
    velocityItemsPerSecond: Float,
    motionSpec: com.android.purebilibili.core.ui.motion.BottomBarMotionSpec = resolveBottomBarMotionSpec()
): BottomBarIndicatorLayerTransform {
    val bottomBarTransform = resolveBottomBarIndicatorLayerTransform(
        motionProgress = motionProgress,
        velocityItemsPerSecond = velocityItemsPerSecond,
        isDragging = true,
        dragScaleProgress = motionProgress,
        motionSpec = motionSpec
    )
    return bottomBarTransform
}

internal fun resolveTopTabIndicatorScaleProgress(
    pagerSliding: Boolean,
    dragScaleProgress: Float,
    pressProgress: Float
): Float {
    if (pagerSliding) return 0f
    return maxOf(dragScaleProgress, pressProgress).coerceIn(0f, 1f)
}

internal fun resolveTopTabNeutralIndicatorColor(
    isDarkTheme: Boolean,
    alpha: Float
): Color {
    val baseColor = if (isDarkTheme) {
        Color(0xFFE1E8E5)
    } else {
        Color(0xFFEAF2EF)
    }
    return baseColor.copy(alpha = alpha)
}

internal fun resolveTopTabNeutralIndicatorTintAlpha(
    isDarkTheme: Boolean,
    configuredAlpha: Float
): Float {
    val floor = if (isDarkTheme) 0.38f else 0.42f
    return configuredAlpha.coerceAtLeast(floor)
}

internal data class TopTabIndicatorBackdropPolicy(
    val useIndicatorBackdrop: Boolean,
    val useCombinedBackdrop: Boolean
)

internal fun resolveTopTabIndicatorBackdropPolicy(
    effectiveLiquidGlassEnabled: Boolean,
    hasBackdrop: Boolean,
    indicatorVisualPolicy: BottomBarIndicatorVisualPolicy
): TopTabIndicatorBackdropPolicy {
    if (!effectiveLiquidGlassEnabled) {
        return TopTabIndicatorBackdropPolicy(
            useIndicatorBackdrop = indicatorVisualPolicy.shouldRefract && hasBackdrop,
            useCombinedBackdrop = false
        )
    }

    val useContentBackdrop = indicatorVisualPolicy.shouldRefract && effectiveLiquidGlassEnabled
    val useBackdrop = indicatorVisualPolicy.shouldRefract && hasBackdrop
    val useCombinedBackdrop = useContentBackdrop && useBackdrop
    return TopTabIndicatorBackdropPolicy(
        useIndicatorBackdrop = true,
        useCombinedBackdrop = useCombinedBackdrop
    )
}

internal data class TopTabRefractionMotionProfile(
    val lensAmountScale: Float,
    val lensHeightScale: Float,
    val chromaticBoostScale: Float,
    val forceChromaticAberration: Boolean,
    val visibleSelectionEmphasis: Float,
    val exportSelectionEmphasis: Float,
    val indicatorPanelOffsetFraction: Float,
    val visiblePanelOffsetFraction: Float,
    val exportPanelOffsetFraction: Float
)

internal fun resolveTopTabRefractionMotionProfile(
    position: Float,
    shouldRefract: Boolean,
    velocityPxPerSecond: Float,
    liquidGlassEnabled: Boolean
): TopTabRefractionMotionProfile {
    if (!shouldRefract || !liquidGlassEnabled) {
        return TopTabRefractionMotionProfile(
            lensAmountScale = 1f,
            lensHeightScale = 1f,
            chromaticBoostScale = 1f,
            forceChromaticAberration = false,
            visibleSelectionEmphasis = 1f,
            exportSelectionEmphasis = 1f,
            indicatorPanelOffsetFraction = 0f,
            visiblePanelOffsetFraction = 0f,
            exportPanelOffsetFraction = 0f
        )
    }
    val bottomMotionSpec = resolveBottomBarMotionSpec(BottomBarMotionProfile.IOS_FLOATING)
    val bottomProfile = resolveBottomBarRefractionMotionProfile(
        position = position,
        velocity = velocityPxPerSecond,
        isDragging = true,
        motionSpec = bottomMotionSpec
    )
    return TopTabRefractionMotionProfile(
        lensAmountScale = 1f,
        lensHeightScale = 1f,
        chromaticBoostScale = 1f,
        forceChromaticAberration = bottomProfile.progress > 0.02f,
        visibleSelectionEmphasis = bottomProfile.visibleSelectionEmphasis,
        exportSelectionEmphasis = bottomProfile.exportSelectionEmphasis,
        indicatorPanelOffsetFraction = bottomProfile.indicatorPanelOffsetFraction,
        visiblePanelOffsetFraction = bottomProfile.visiblePanelOffsetFraction,
        exportPanelOffsetFraction = bottomProfile.exportPanelOffsetFraction
    )
}

internal fun resolveTopTabRefractionMotionProfile(
    shouldRefract: Boolean,
    velocityPxPerSecond: Float,
    liquidGlassEnabled: Boolean
): TopTabRefractionMotionProfile {
    return resolveTopTabRefractionMotionProfile(
        position = 0f,
        shouldRefract = shouldRefract,
        velocityPxPerSecond = velocityPxPerSecond,
        liquidGlassEnabled = liquidGlassEnabled
    )
}

internal fun resolveTopTabItemMotionVisual(
    itemIndex: Int,
    indicatorPosition: Float,
    currentSelectedIndex: Int,
    isInMotion: Boolean,
    selectionEmphasis: Float
): BottomBarItemMotionVisual {
    return resolveBottomBarItemMotionVisual(
        itemIndex = itemIndex,
        indicatorPosition = indicatorPosition,
        currentSelectedIndex = currentSelectedIndex,
        motionProgress = if (isInMotion) 1f else 0f,
        selectionEmphasis = selectionEmphasis
    )
}

internal fun resolveTopTabHorizontalDeltaPx(
    positionDeltaPages: Float,
    tabWidthPx: Float,
    deadZonePages: Float = 0.0012f
): Float {
    if (tabWidthPx <= 0f) return 0f
    if (abs(positionDeltaPages) < deadZonePages) return 0f
    return positionDeltaPages * tabWidthPx
}

internal fun resolveTopTabIndicatorViewportShiftPx(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    tabWidthPx: Float
): Float {
    if (tabWidthPx <= 0f) return 0f
    if (firstVisibleItemIndex < 0) return 0f
    val clampedScrollOffsetPx = firstVisibleItemScrollOffsetPx.coerceAtLeast(0)
    return firstVisibleItemIndex * tabWidthPx + clampedScrollOffsetPx.toFloat()
}

internal fun resolveTopTabIndicatorViewportClampShiftPx(
    rowScrollOffsetPx: Float,
    indicatorPanelOffsetPx: Float
): Float {
    // 手动横向滚动顶栏只改变标签列表视口，不应把选中指示器夹到当前视口里。
    return 0f
}

@Composable
fun CategoryTabItem(
    category: String,
    categoryKey: String = category,
    index: Int,
    selectedIndex: Int,
    currentPosition: Float,
    primaryColor: Color,
    unselectedColor: Color,
    labelMode: Int,
    isInMotion: Boolean = false,
    selectionEmphasis: Float = 1f,
    isInteractive: Boolean = true,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit = {}
) {
     val uiPreset = LocalUiPreset.current
     val motionVisual = remember(
         index,
         currentPosition,
         selectedIndex,
         isInMotion,
         selectionEmphasis
     ) {
         resolveTopTabItemMotionVisual(
             itemIndex = index,
             indicatorPosition = currentPosition,
             currentSelectedIndex = selectedIndex,
             isInMotion = isInMotion,
             selectionEmphasis = selectionEmphasis
         )
     }
     val selectionFraction = motionVisual.themeWeight

     // 单层文本渲染，避免双层交叉透明带来的发虚/重影。
     val contentColor = androidx.compose.ui.graphics.lerp(
         unselectedColor,
         primaryColor,
         selectionFraction
     )
     val normalizedLabelMode = normalizeTopTabLabelMode(labelMode)
     val showIcon = shouldShowTopTabIcon(normalizedLabelMode)
     val showText = shouldShowTopTabText(normalizedLabelMode)
     val icon = resolveTopTabCategoryIcon(categoryKey, uiPreset)
     val iconSize = resolveTopTabIconSizeDp(normalizedLabelMode).dp
     val textSize = resolveTopTabLabelTextSizeSp(normalizedLabelMode).sp
     val textLineHeight = resolveTopTabLabelLineHeightSp(normalizedLabelMode).sp
     val contentMinHeight = resolveTopTabContentMinHeightDp(normalizedLabelMode).dp
     val contentVerticalPadding = resolveTopTabContentVerticalPaddingDp(normalizedLabelMode).dp
     val iconTextSpacing = resolveTopTabIconTextSpacingDp(normalizedLabelMode).dp
     
     val targetScale = resolveTopTabContentScale(
         selectionFraction = selectionFraction,
         showIcon = showIcon,
         showText = showText,
         uiPreset = uiPreset
     )
     
     // Font weight change still triggers relayout, but it's discrete (only happens at 0.6 threshold)
     // This is acceptable as it doesn't happen every frame.
     val fontWeight = if (selectionFraction > 0.6f) FontWeight.SemiBold else FontWeight.Medium

     val haptic = com.android.purebilibili.core.util.rememberHapticFeedback()

     Box(
         modifier = Modifier
             .clip(AppShapes.container(ContainerLevel.Pill))
             .then(
                 if (isInteractive) {
                     Modifier.combinedClickable(
                         interactionSource = remember { MutableInteractionSource() },
                         indication = null,
                         onClick = { onClick() },
                         onDoubleClick = onDoubleTap
                     )
                 } else {
                     Modifier
                 }
             )
             .padding(horizontal = 8.dp, vertical = contentVerticalPadding)
             .heightIn(min = contentMinHeight),
         contentAlignment = Alignment.Center
     ) {
         if (showIcon && showText) {
             Column(
                 horizontalAlignment = Alignment.CenterHorizontally,
                 verticalArrangement = Arrangement.Center,
                 modifier = Modifier.graphicsLayer {
                     scaleX = targetScale
                     scaleY = targetScale
                     transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                 }
             ) {
                Icon(
                     imageVector = icon,
                     contentDescription = null,
                     tint = contentColor,
                     modifier = Modifier
                         .size(iconSize)
                         .offset(y = (-0.5).dp)
                 )
                 Spacer(modifier = Modifier.height(iconTextSpacing))
                 Text(
                     text = category,
                     color = contentColor,
                     fontSize = textSize,
                     fontWeight = fontWeight,
                     lineHeight = textLineHeight,
                     maxLines = 1,
                     overflow = TextOverflow.Ellipsis
                 )
             }
         } else if (showIcon) {
             Icon(
                 imageVector = icon,
                 contentDescription = null,
                 tint = contentColor,
                 modifier = Modifier
                     .size(iconSize)
                     .graphicsLayer {
                         scaleX = targetScale
                         scaleY = targetScale
                         transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                     }
             )
         } else {
             Text(
                 text = category,
                 color = contentColor,
                 fontSize = textSize,
                 fontWeight = fontWeight,
                 lineHeight = textLineHeight,
                 modifier = Modifier.graphicsLayer {
                     scaleX = targetScale
                     scaleY = targetScale
                     transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                 },
                 maxLines = 1,
                 overflow = TextOverflow.Ellipsis
             )
         }
     }
}
