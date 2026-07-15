package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import com.kyant.backdrop.Backdrop
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Person

internal data class CommentSortSegmentedControlSpec(
    val itemWidthDp: Int,
    val heightDp: Int,
    val indicatorHeightDp: Int
)

internal fun resolveCommentSortSegmentedControlSpec(itemCount: Int): CommentSortSegmentedControlSpec {
    return CommentSortSegmentedControlSpec(
        itemWidthDp = if (itemCount >= 4) 56 else 66,
        heightDp = 40,
        indicatorHeightDp = 27
    )
}

internal fun hasCommentSortIndicatorScaleClearance(
    containerHeightDp: Int,
    indicatorHeightDp: Int
): Boolean {
    val bottomBarScale = 78f / 56f
    return containerHeightDp >= indicatorHeightDp * bottomBarScale + 2f
}

/**
 *  评论排序筛选栏 (iOS Style)
 *  Header: "评论 (123)"
 *  Controls: Segmented Control [按热度 | 按时间]
 */
@Composable
fun CommentSortFilterBar(
    count: Int,
    sortMode: CommentSortMode,
    onSortModeChange: (CommentSortMode) -> Unit,
    upOnly: Boolean = false,
    onUpOnlyToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null
) {
    val sortModes = remember { CommentSortMode.entries.toList() }
    val appearance = rememberVideoCommentAppearance()

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        //  Left: Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "评论",
                fontSize = 20.sp, // iOS Large Title style scale
                fontWeight = FontWeight.Bold,
                color = appearance.primaryTextColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = FormatUtils.formatStat(count.toLong()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = appearance.secondaryTextColor
            )
        }

        // Right: Sort Control + Only UP Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Only UP Toggle
            iOSToggleButton(
                isChecked = upOnly,
                onToggle = onUpOnlyToggle,
                icon = CupertinoIcons.Filled.Person
            )

            // Segmented Control
            iOSSegmentedControl(
                items = sortModes.map { it.label },
                selectedIndex = sortModes.indexOf(sortMode).coerceAtLeast(0),
                onScaleChange = { index ->
                    sortModes.getOrNull(index)?.let(onSortModeChange)
                },
                backdrop = backdrop
            )
        }
    }
}

/**
 * Bottom-bar matched segmented control.
 */
@Composable
fun iOSSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onScaleChange: (Int) -> Unit,
    backdrop: Backdrop? = null
) {
    val context = LocalContext.current
    val homeSettings by SettingsManager
        .getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    val spec = remember(items.size) {
        resolveCommentSortSegmentedControlSpec(itemCount = items.size)
    }
    BottomBarLiquidSegmentedControl(
        items = items,
        selectedIndex = selectedIndex,
        onSelected = onScaleChange,
        itemWidth = spec.itemWidthDp.dp,
        height = spec.heightDp.dp,
        indicatorHeight = spec.indicatorHeightDp.dp,
        labelFontSize = 13.sp,
        backdrop = backdrop,
        forceLiquidChrome = homeSettings.androidNativeLiquidGlassEnabled,
        liquidGlassEffectsEnabled = backdrop != null,
        tapPressRefractionEnabled = false
    )
}

/**
 * iOS Style Toggle Button (Optional usage)
 */
@Composable
fun iOSToggleButton(
    isChecked: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val appearance = rememberVideoCommentAppearance()
    val backgroundColor = if (isChecked) {
        appearance.toggleCheckedBackgroundColor
    } else {
        appearance.toggleUncheckedBackgroundColor
    }
    val contentColor = if (isChecked) {
        appearance.toggleCheckedContentColor
    } else {
        appearance.toggleUncheckedContentColor
    }
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
