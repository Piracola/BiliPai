package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.feature.video.player.PlayMode
import com.android.purebilibili.feature.video.state.rememberVideoPlayerState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel

internal fun resolveAudioPlayModeLabel(mode: PlayMode): String = when (mode) {
    PlayMode.SEQUENTIAL -> "顺序播放"
    PlayMode.SHUFFLE -> "随机播放"
    PlayMode.REPEAT_ONE -> "单曲循环"
    PlayMode.REPEAT_ALL -> "列表循环"
}

internal fun shouldUseAudioModeLiquidPlayModeControl(
    uiPreset: UiPreset,
    androidNativeLiquidGlassEnabled: Boolean
): Boolean = uiPreset != UiPreset.MD3 || androidNativeLiquidGlassEnabled

internal enum class AudioModePlayPauseAction {
    PAUSE,
    RESUME,
    RESTART_FROM_BEGINNING,
    PREPARE_AND_RESUME
}

internal fun resolveAudioModePlayPauseAction(
    isPlaying: Boolean,
    playbackState: Int,
    playWhenReady: Boolean
): AudioModePlayPauseAction {
    if (isPlaying) return AudioModePlayPauseAction.PAUSE
    return when (playbackState) {
        Player.STATE_ENDED -> AudioModePlayPauseAction.RESTART_FROM_BEGINNING
        Player.STATE_IDLE -> AudioModePlayPauseAction.PREPARE_AND_RESUME
        Player.STATE_READY, Player.STATE_BUFFERING -> AudioModePlayPauseAction.RESUME
        else -> if (playWhenReady) AudioModePlayPauseAction.PAUSE else AudioModePlayPauseAction.RESUME
    }
}

internal fun shouldCreateAudioModeStandalonePlayer(
    hasPlayer: Boolean,
    initialBvid: String
): Boolean = !hasPlayer && initialBvid.isNotBlank()

internal fun resolveAudioModePageSwitchAutoPlay(): Boolean = true

internal fun resolveAudioModeCollectionSwitchAutoPlay(): Boolean = true

internal fun shouldShowAudioModePipButton(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.O

internal fun parseAudioModeSleepTimerInput(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.all { it.isDigit() }) {
        return trimmed.toIntOrNull()?.takeIf { it > 0 }
    }
    val match = Regex("""^(\d{1,3})\s*[:：]\s*(\d{1,2})$""").matchEntire(trimmed) ?: return null
    val hours = match.groupValues[1].toIntOrNull() ?: return null
    val minutes = match.groupValues[2].toIntOrNull() ?: return null
    if (minutes !in 0..59) return null
    return (hours * 60 + minutes).takeIf { it > 0 }
}

internal fun formatAudioModeSleepTimerButtonLabel(minutes: Int?): String {
    minutes ?: return "定时关闭"
    return when {
        minutes < 60 -> "${minutes}分钟"
        minutes % 60 == 0 -> "${minutes / 60}小时"
        else -> "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    }
}

internal fun formatAudioModeSleepTimerInput(minutes: Int?): String {
    minutes ?: return ""
    return if (minutes >= 60 && minutes % 60 != 0) {
        "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
    } else {
        minutes.toString()
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

internal fun Player.handleAudioModePlayPause() {
    when (
        resolveAudioModePlayPauseAction(
            isPlaying = isPlaying,
            playbackState = playbackState,
            playWhenReady = playWhenReady
        )
    ) {
        AudioModePlayPauseAction.PAUSE -> pause()
        AudioModePlayPauseAction.RESUME -> play()
        AudioModePlayPauseAction.RESTART_FROM_BEGINNING -> {
            seekTo(0L)
            play()
        }
        AudioModePlayPauseAction.PREPARE_AND_RESUME -> {
            prepare()
            play()
        }
    }
}

private fun enterAudioModePip(activity: Activity?) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInPictureInPictureMode) return
    val paramsBuilder = PictureInPictureParams.Builder().setAspectRatio(Rational(1, 1))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        paramsBuilder.setSeamlessResizeEnabled(true)
    }
    activity.enterPictureInPictureMode(paramsBuilder.build())
}

@Composable
fun AudioModeScreen(
    viewModel: VideoPlaybackViewModel,
    onBack: () -> Unit,
    onVideoModeClick: (String, Long) -> Unit,
    isInPipMode: Boolean = false,
    initialBvid: String = "",
    initialCid: Long = 0L,
    initialResumePositionMs: Long = 0L,
    titleOverride: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsStateWithLifecycle(
        initialValue = HomeSettings(),
        context = kotlin.coroutines.EmptyCoroutineContext
    )
    var cachedSuccessState by remember { mutableStateOf<VideoPlaybackUiState.Success?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is VideoPlaybackUiState.Success) cachedSuccessState = uiState as VideoPlaybackUiState.Success
    }
    val displayState = (uiState as? VideoPlaybackUiState.Success) ?: cachedSuccessState
    val shouldCreateStandalonePlayer = remember(initialBvid) {
        shouldCreateAudioModeStandalonePlayer(
            hasPlayer = viewModel.currentPlayer != null,
            initialBvid = initialBvid
        )
    }
    val standalonePlayerState = if (shouldCreateStandalonePlayer) {
        rememberVideoPlayerState(
            context = context,
            viewModel = viewModel,
            bvid = initialBvid,
            cid = initialCid,
            fallbackResumePositionMs = initialResumePositionMs
        )
    } else {
        null
    }
    val player = standalonePlayerState?.player ?: viewModel.currentPlayer

    LaunchedEffect(initialBvid, initialCid, initialResumePositionMs, shouldCreateStandalonePlayer) {
        if (!shouldCreateStandalonePlayer && displayState == null && initialBvid.isNotBlank()) {
            viewModel.loadVideo(
                bvid = initialBvid,
                cid = initialCid,
                autoPlay = true,
                fallbackResumePositionMs = initialResumePositionMs
            )
        }
    }
    DisposableEffect(viewModel) {
        viewModel.setAudioMode(true)
        onDispose { viewModel.setAudioMode(false) }
    }

    if (displayState == null) {
        AudioModeInitialState(
            state = uiState,
            title = titleOverride?.takeIf { it.isNotBlank() } ?: "正在加载音频",
            onBack = onBack,
            onRetry = { viewModel.retry() }
        )
        return
    }

    val enterPip = remember(context) { { enterAudioModePip(context.findHostActivity()) } }
    AudioModeMusicPlayer(
        viewModel = viewModel,
        successState = displayState,
        player = player,
        onBack = onBack,
        onVideoModeClick = onVideoModeClick,
        isInPipMode = isInPipMode,
        showPipButton = shouldShowAudioModePipButton(Build.VERSION.SDK_INT),
        onEnterPip = enterPip,
        sleepTimerMinutes = sleepTimerMinutes,
        titleOverride = titleOverride,
        liquidGlassEffectsEnabled = homeSettings.androidNativeLiquidGlassEnabled
    )
}

@Composable
private fun AudioModeInitialState(
    state: VideoPlaybackUiState,
    title: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 12.dp)
                .height(48.dp)
        ) {
            Text("返回")
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (state) {
                is VideoPlaybackUiState.Error -> {
                    Text("音频加载失败", style = MaterialTheme.typography.headlineSmall)
                    Text(state.msg, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.canRetry) {
                        TextButton(onClick = onRetry, modifier = Modifier.height(48.dp)) { Text("重试") }
                    }
                }
                else -> {
                    CircularProgressIndicator()
                    Text(title)
                    if (title != "正在加载音频") {
                        Text("正在加载音频", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AudioModeSleepTimerDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onSelectPreset: (Int?) -> Unit,
    onConfirmCustom: (Int) -> Unit
) {
    var customInput by remember(currentMinutes) {
        mutableStateOf(formatAudioModeSleepTimerInput(currentMinutes))
    }
    val parsedCustomMinutes = remember(customInput) { parseAudioModeSleepTimerInput(customInput) }
    val showCustomError = customInput.isNotBlank() && parsedCustomMinutes == null
    val presetOptions = listOf<Int?>(null, 15, 30, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("定时关闭") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "选择常用时长，或输入分钟数 / 小时:分钟，例如 90、1:30。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.forEach { minutes ->
                        val isSelected = currentMinutes == minutes
                        Surface(
                            onClick = { onSelectPreset(minutes) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatAudioModeSleepTimerButtonLabel(minutes),
                                    fontSize = 13.sp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.take(8) },
                    singleLine = true,
                    label = { Text("自定义时间") },
                    placeholder = { Text("例如 45 或 1:30") },
                    isError = showCustomError,
                    supportingText = {
                        if (showCustomError) {
                            Text("请输入正整数分钟，或 小时:分钟，分钟需在 0-59。")
                        } else if (parsedCustomMinutes != null) {
                            Text("将于 ${formatAudioModeSleepTimerButtonLabel(parsedCustomMinutes)}后暂停播放")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedCustomMinutes?.let(onConfirmCustom) },
                enabled = parsedCustomMinutes != null
            ) { Text("应用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
