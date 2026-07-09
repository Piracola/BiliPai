package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.resolveGlobalLiquidGlassReuseEnabled
import com.android.purebilibili.core.theme.calculateContrastRatio
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.rememberAppBookmarkIcon
import com.android.purebilibili.core.ui.rememberAppCoinIcon
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.feature.home.components.kernelSuFloatingDockSurface
import com.android.purebilibili.feature.home.components.resolveAndroidNativeBottomBarTuning
import com.android.purebilibili.feature.home.components.resolveAndroidNativeFloatingBottomBarContainerColor
import com.android.purebilibili.feature.home.components.resolveBottomBarDarkTheme
import com.android.purebilibili.feature.home.components.resolveSharedBottomBarCapsuleShape
import com.kyant.backdrop.Backdrop

internal const val BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST = 4.5f

internal fun resolveBottomInputBarPlaceholderTextColor(
    inputContainerColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
): Color {
    return listOf(
        onSurfaceColor,
        onSurfaceVariantColor,
        if (inputContainerColor.luminance() < 0.5f) Color.White else Color.Black
    ).firstOrNull { candidate ->
        calculateContrastRatio(candidate, inputContainerColor) >= BOTTOM_INPUT_BAR_PLACEHOLDER_MIN_CONTRAST
    } ?: onSurfaceColor
}

/**
 * Floating liquid-glass chrome for the detail comment/action bar is gated only by the
 * global "安卓原生液态玻璃" reuse master switch (option 1).
 */
internal fun shouldUseFloatingLiquidBottomInputBar(
    androidNativeLiquidGlassEnabled: Boolean
): Boolean = resolveGlobalLiquidGlassReuseEnabled(androidNativeLiquidGlassEnabled)

internal fun resolveBottomInputBarContentBottomPadding(
    showBar: Boolean,
    floatingLiquidGlass: Boolean,
    showActionButtonsFallback: Boolean
): Dp {
    if (!showBar) {
        return if (showActionButtonsFallback) 84.dp else 12.dp
    }
    return if (floatingLiquidGlass) 112.dp else 96.dp
}

@Composable
fun BottomInputBar(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
    backdrop: Backdrop? = null,
) {
    val context = LocalContext.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val floatingLiquidGlass = shouldUseFloatingLiquidBottomInputBar(
        androidNativeLiquidGlassEnabled = homeSettings.androidNativeLiquidGlassEnabled
    )

    if (floatingLiquidGlass) {
        FloatingLiquidBottomInputBar(
            modifier = modifier,
            backdrop = backdrop,
            homeSettings = homeSettings,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isCoined = isCoined,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick,
            onCoinClick = onCoinClick,
            onShareClick = onShareClick,
            onCommentClick = onCommentClick
        )
    } else {
        DockedSolidBottomInputBar(
            modifier = modifier,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isCoined = isCoined,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick,
            onCoinClick = onCoinClick,
            onShareClick = onShareClick,
            onCommentClick = onCommentClick
        )
    }
}

@Composable
private fun DockedSolidBottomInputBar(
    modifier: Modifier,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
) {
    val inputContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val inputTextColor = resolveBottomInputBarPlaceholderTextColor(
        inputContainerColor = inputContainerColor,
        onSurfaceColor = MaterialTheme.colorScheme.onSurface,
        onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        BottomInputBarContentRow(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            inputContainerColor = inputContainerColor,
            inputTextColor = inputTextColor,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isCoined = isCoined,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick,
            onCoinClick = onCoinClick,
            onShareClick = onShareClick,
            onCommentClick = onCommentClick
        )
    }
}

@Composable
private fun FloatingLiquidBottomInputBar(
    modifier: Modifier,
    backdrop: Backdrop?,
    homeSettings: HomeSettings,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
) {
    val blurIntensity = currentUnifiedBlurIntensity()
    val isDarkTheme = resolveBottomBarDarkTheme(AppSurfaceTokens.chromeBackground())
    val tuning = remember(isDarkTheme) {
        resolveAndroidNativeBottomBarTuning(
            blurEnabled = true,
            darkTheme = isDarkTheme
        )
    }
    val containerColor = resolveAndroidNativeFloatingBottomBarContainerColor(
        surfaceColor = MaterialTheme.colorScheme.surfaceContainer,
        tuning = tuning,
        glassEnabled = true,
        blurEnabled = true,
        blurIntensity = blurIntensity,
        liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
    )
    // Match home search capsule: same glass material, slightly clearer so the field reads as a control.
    val commentFieldContainerColor = containerColor.copy(
        alpha = (containerColor.alpha * 0.72f).coerceIn(0.18f, 0.55f)
    )
    val shellShape = resolveSharedBottomBarCapsuleShape()
    val inputTextColor = resolveBottomInputBarPlaceholderTextColor(
        inputContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        onSurfaceColor = MaterialTheme.colorScheme.onSurface,
        onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val bottomInset = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tuning.outerHorizontalPaddingDp.dp)
            .padding(bottom = bottomInset),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kernelSuFloatingDockSurface(
                    shape = shellShape,
                    backdrop = backdrop,
                    containerColor = containerColor,
                    blurEnabled = true,
                    glassEnabled = true,
                    blurRadius = tuning.shellBlurRadiusDp.dp,
                    hazeState = null,
                    motionTier = MotionTier.Normal,
                    isTransitionRunning = false,
                    forceLowBlurBudget = false,
                    liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset
                )
        ) {
            FloatingLiquidBottomInputBarContentRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                backdrop = backdrop,
                commentFieldContainerColor = commentFieldContainerColor,
                commentFieldShape = shellShape,
                blurRadius = tuning.shellBlurRadiusDp.dp,
                liquidGlassPreset = homeSettings.bottomBarLiquidGlassPreset,
                inputTextColor = inputTextColor,
                isLiked = isLiked,
                isFavorited = isFavorited,
                isCoined = isCoined,
                onLikeClick = onLikeClick,
                onFavoriteClick = onFavoriteClick,
                onCoinClick = onCoinClick,
                onShareClick = onShareClick,
                onCommentClick = onCommentClick
            )
        }
    }
}

@Composable
private fun FloatingLiquidBottomInputBarContentRow(
    modifier: Modifier,
    backdrop: Backdrop?,
    commentFieldContainerColor: Color,
    commentFieldShape: androidx.compose.ui.graphics.Shape,
    blurRadius: Dp,
    liquidGlassPreset: com.android.purebilibili.core.store.BottomBarLiquidGlassPreset,
    inputTextColor: Color,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
) {
    val favoriteIcon = rememberAppBookmarkIcon()
    val coinIcon = rememberAppCoinIcon()
    val likeIcon = rememberAppLikeIcon()
    val likeFilledIcon = rememberAppLikeFilledIcon()
    val shareIcon = rememberAppShareIcon()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Same liquid dock surface as home bottom-bar search capsule (sibling glass, not solid chip).
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .kernelSuFloatingDockSurface(
                    shape = commentFieldShape,
                    backdrop = backdrop,
                    containerColor = commentFieldContainerColor,
                    blurEnabled = true,
                    glassEnabled = true,
                    drawShellLens = false,
                    blurRadius = blurRadius,
                    hazeState = null,
                    motionTier = MotionTier.Normal,
                    isTransitionRunning = false,
                    forceLowBlurBudget = false,
                    liquidGlassPreset = liquidGlassPreset
                )
                .clickable { onCommentClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "评论 UP 主和大家...",
                color = inputTextColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        BottomInputBarActionButtons(
            favoriteIcon = favoriteIcon,
            coinIcon = coinIcon,
            likeIcon = likeIcon,
            likeFilledIcon = likeFilledIcon,
            shareIcon = shareIcon,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isCoined = isCoined,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick,
            onCoinClick = onCoinClick,
            onShareClick = onShareClick
        )
    }
}

@Composable
private fun BottomInputBarContentRow(
    modifier: Modifier,
    inputContainerColor: Color,
    inputTextColor: Color,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
) {
    val favoriteIcon = rememberAppBookmarkIcon()
    val coinIcon = rememberAppCoinIcon()
    val likeIcon = rememberAppLikeIcon()
    val likeFilledIcon = rememberAppLikeFilledIcon()
    val shareIcon = rememberAppShareIcon()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(inputContainerColor)
                .clickable { onCommentClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "评论 UP 主和大家...",
                color = inputTextColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        BottomInputBarActionButtons(
            favoriteIcon = favoriteIcon,
            coinIcon = coinIcon,
            likeIcon = likeIcon,
            likeFilledIcon = likeFilledIcon,
            shareIcon = shareIcon,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isCoined = isCoined,
            onLikeClick = onLikeClick,
            onFavoriteClick = onFavoriteClick,
            onCoinClick = onCoinClick,
            onShareClick = onShareClick
        )
    }
}

@Composable
private fun BottomInputBarActionButtons(
    favoriteIcon: ImageVector,
    coinIcon: ImageVector,
    likeIcon: ImageVector,
    likeFilledIcon: ImageVector,
    shareIcon: ImageVector,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconActionButton(
            icon = if (isLiked) likeFilledIcon else likeIcon,
            label = "点赞",
            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onLikeClick,
            showLabel = false
        )
        IconActionButton(
            icon = coinIcon,
            label = "投币",
            tint = if (isCoined) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onCoinClick,
            showLabel = false
        )
        IconActionButton(
            icon = favoriteIcon,
            label = "收藏",
            tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            onClick = onFavoriteClick,
            showLabel = false
        )
        IconActionButton(
            icon = shareIcon,
            label = "分享",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = onShareClick,
            showLabel = false
        )
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    showLabel: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = tint
            )
        }
    }
}
