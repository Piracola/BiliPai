package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.core.ui.IOSModalBottomSheet
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Checkmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackOrderSelectionSheet(
    currentBehavior: PlaybackCompletionBehavior,
    onSelect: (PlaybackCompletionBehavior) -> Unit,
    onDismiss: () -> Unit,
    isFullscreen: Boolean = false,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutSpec = remember(isLandscape, isFullscreen) {
        resolvePlaybackOrderSheetLayoutSpec(
            isLandscape = isLandscape,
            isFullscreen = isFullscreen,
        )
    }

    when (layoutSpec.presentation) {
        PlaybackOrderSheetPresentation.COMPACT_OVERLAY -> {
            PlaybackOrderCompactOverlay(
                layoutSpec = layoutSpec,
                currentBehavior = currentBehavior,
                onSelect = onSelect,
                onDismiss = onDismiss,
            )
        }
        PlaybackOrderSheetPresentation.BOTTOM_SHEET -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            IOSModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                // 让面板底色延伸到手势条后方，避免底部白条割裂。
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                PlaybackOrderSheetContent(
                    layoutSpec = layoutSpec,
                    currentBehavior = currentBehavior,
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PlaybackOrderCompactOverlay(
    layoutSpec: PlaybackOrderSheetLayoutSpec,
    currentBehavior: PlaybackCompletionBehavior,
    onSelect: (PlaybackCompletionBehavior) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .widthIn(max = layoutSpec.maxWidthDp.dp)
                .fillMaxWidth(0.42f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            PlaybackOrderSheetContent(
                layoutSpec = layoutSpec,
                currentBehavior = currentBehavior,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PlaybackOrderSheetContent(
    layoutSpec: PlaybackOrderSheetLayoutSpec,
    currentBehavior: PlaybackCompletionBehavior,
    onSelect: (PlaybackCompletionBehavior) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentModifier = if (layoutSpec.extendUnderNavigationBars) {
        modifier.navigationBarsPadding()
    } else {
        modifier
    }
    Column(
        modifier = contentModifier.padding(
            horizontal = layoutSpec.horizontalPaddingDp.dp,
            vertical = layoutSpec.verticalPaddingDp.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "选择播放顺序",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = layoutSpec.titleVerticalPaddingDp.dp),
        )

        val options = listOf(
            PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
            PlaybackCompletionBehavior.PLAY_IN_ORDER,
            PlaybackCompletionBehavior.REPEAT_ONE,
            PlaybackCompletionBehavior.LOOP_PLAYLIST,
            PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
        )

        options.forEachIndexed { index, behavior ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(behavior) }
                    .padding(vertical = layoutSpec.rowVerticalPaddingDp.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = behavior.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (currentBehavior == behavior) {
                    Icon(
                        imageVector = CupertinoIcons.Outlined.Checkmark,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.size(22.dp))
                }
            }
            if (index < options.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
            }
        }
    }
}
