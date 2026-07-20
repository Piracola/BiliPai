package com.android.purebilibili.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.core.ui.TopReadabilityChrome
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.currentUnifiedBlurIntensity
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.feature.settings.SettingsPageScrollHost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    scrollHost: SettingsPageScrollHost = SettingsPageScrollHost.LazyColumn,
    topBarBlurEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    lazyListContent: (LazyListScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    val hazeState = rememberRecoverableHazeState()
    val blurIntensity = currentUnifiedBlurIntensity()
    val topBarSurfaceAlpha = if (topBarBlurEnabled) {
        BlurStyles.getBackgroundAlpha(blurIntensity)
    } else {
        0.86f
    }
    val pageContainerColor = AppSurfaceTokens.groupedListContainer()

    // InstallerX：设置页用不透明 grouped 底色，避免全局壁纸半透明与多页叠色闪烁。
    CompositionLocalProvider(LocalGlobalWallpaperBackdropVisible provides false) {
        AdaptiveScaffold(
            modifier = modifier,
            topBar = {
                Box {
                    TopReadabilityChrome(
                        height = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                        surfaceColor = pageContainerColor,
                        surfaceAlpha = topBarSurfaceAlpha,
                        hazeState = hazeState,
                        hazeEnabled = topBarBlurEnabled,
                    )
                    AdaptiveTopAppBar(
                        title = title,
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = rememberAppBackIcon(),
                                    contentDescription = backContentDescription,
                                )
                            }
                        },
                        actions = actions,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            },
            containerColor = pageContainerColor,
            contentWindowInsets = WindowInsets(0.dp),
        ) { padding ->
            val scrollModifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .hazeSourceCompat(state = hazeState)

            when (scrollHost) {
                SettingsPageScrollHost.LazyColumn -> {
                    LazyColumn(
                        state = listState,
                        modifier = scrollModifier,
                        contentPadding = PaddingValues(bottom = bottomContentPadding),
                    ) {
                        if (header != null) {
                            item {
                                header()
                            }
                        }
                        if (lazyListContent != null) {
                            lazyListContent()
                        } else {
                            item {
                                content()
                            }
                        }
                    }
                }

                SettingsPageScrollHost.External -> {
                    Column(modifier = scrollModifier) {
                        header?.invoke()
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = true)
                                .fillMaxSize(),
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}
