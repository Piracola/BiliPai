package com.android.purebilibili.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
