package com.android.purebilibili.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.videoCardTransitionBackgroundEffect

/**
 * App 根层全局壁纸。
 * 景深 progress 仍由外部写入（兼容旧接线），但壁纸本身不跟卡片缩/糊，避免全屏壁纸猛缩猛放。
 */
@Composable
internal fun DepthSyncedGlobalHomeWallpaperBackdrop(
    wallpaperUri: String,
    appearance: HomeWallpaperBackdropAppearance,
    baseColor: Color,
    depthProgress: MutableFloatState,
    depthPhase: MutableState<VideoCardTransitionBackgroundPhase>,
    depthGestureRestore: MutableState<Boolean>,
    isDataSaverActive: Boolean = false,
    isLightBackground: Boolean = false,
    realtimeBlurEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val phase by depthPhase
    val applyDepth = shouldApplyVideoCardDepthToGlobalHomeWallpaper(
        wallpaperVisible = appearance.visible,
        phase = phase,
    )
    val motionTier =
        if (rememberSystemReduceMotion()) MotionTier.Reduced else MotionTier.Normal
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (applyDepth) {
                    Modifier.videoCardTransitionBackgroundEffect(
                        progressProvider = { depthProgress.floatValue },
                        phaseProvider = { depthPhase.value },
                        isGestureRestoreInProgressProvider = { depthGestureRestore.value },
                        motionTierProvider = { motionTier },
                        isLightBackgroundProvider = { isLightBackground },
                        realtimeBlurEnabledProvider = { realtimeBlurEnabled },
                    )
                } else {
                    Modifier
                }
            )
    ) {
        HomeWallpaperBackdrop(
            wallpaperUri = wallpaperUri,
            appearance = appearance,
            baseColor = baseColor,
            isDataSaverActive = isDataSaverActive,
        )
    }
}

@Composable
internal fun HomeWallpaperBackdrop(
    wallpaperUri: String,
    appearance: HomeWallpaperBackdropAppearance,
    baseColor: Color,
    isDataSaverActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        if (!appearance.visible) return@Box

        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val decodeSize = remember(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            density.density,
            isDataSaverActive
        ) {
            resolveHomeWallpaperDecodeSizePx(
                screenWidthDp = configuration.screenWidthDp,
                screenHeightDp = configuration.screenHeightDp,
                density = density.density,
                isDataSaverActive = isDataSaverActive
            )
        }
        val imageRequest = remember(context, wallpaperUri, decodeSize) {
            val cacheKey = "home_wallpaper_${wallpaperUri.hashCode()}_${decodeSize.first}x${decodeSize.second}"
            ImageRequest.Builder(context)
                .data(wallpaperUri)
                .size(decodeSize.first, decodeSize.second)
                .scale(Scale.FILL)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .crossfade(180)
                .build()
        }
        val imageModifier = Modifier
            .fillMaxSize()
            .then(
                if (appearance.blurRadiusDp > 0f) {
                    Modifier.blur(appearance.blurRadiusDp.dp)
                } else {
                    Modifier
                }
            )

        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseColor.copy(alpha = appearance.baseBackgroundAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = appearance.scrimAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            baseColor.copy(alpha = appearance.bottomScrimAlpha)
                        )
                    )
                )
        )
    }
}
