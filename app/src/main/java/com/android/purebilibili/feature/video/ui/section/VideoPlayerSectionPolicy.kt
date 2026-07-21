package com.android.purebilibili.feature.video.ui.section

import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.AndroidNativeVariant
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.feature.video.ui.components.GesturePercentMotionDefaults
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.android.purebilibili.feature.video.playback.session.PlaybackSeekSessionState
import com.android.purebilibili.feature.video.playback.session.shouldUsePlaybackSeekSessionPosition
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val INITIAL_PLAYER_CONTROLS_VISIBLE = false
internal const val INITIAL_PLAYER_CHROME_AUTO_HIDE_HANDLED = true

enum class VideoGestureMode { None, Brightness, Volume, Seek, SwipeToFullscreen }

private const val PLAYER_DRAG_GESTURE_BOTTOM_EXCLUSION_BUFFER_DP = 12
private const val PLAYBACK_STALL_LOG_THRESHOLD_MS = 700L
private const val VIDEO_PLAYER_COVER_FADE_ENTER_DURATION_MILLIS = 200
private const val VIDEO_PLAYER_COVER_FADE_EXIT_DURATION_MILLIS = 300
private const val VIDEO_PLAYER_COVER_REVEAL_HOLD_DELAY_MILLIS = 96
private const val VIDEO_PLAYER_SURFACE_REVEAL_DURATION_MILLIS = 220
private const val VIDEO_PLAYER_SURFACE_REVEAL_INITIAL_SCALE = 0.985f
private const val LONG_PRESS_SPEED_TAP_SUPPRESSION_WINDOW_MS = 450L
private const val LONG_PRESS_SPEED_UNLOCK_HOLD_MS = 1_000L

internal const val LONG_PRESS_SPEED_LOCK_ZONE_HEIGHT_DP = 96
internal const val FOREGROUND_SURFACE_RECOVERY_DELAY_MS = 80L
internal const val FOREGROUND_SURFACE_RECOVERY_TIMEOUT_MS = 1200L

internal data class LongPressSpeedLockSensitivityPolicy(
    val lockZoneHeightDp: Int,
    val minDragDistanceDp: Int
)

internal data class LongPressSpeedLockZoneVisualPolicy(
    val zoneFillAlpha: Float,
    val borderAlpha: Float,
    val edgeGradientAlpha: Float,
    val centerMarkerAlpha: Float,
    val edgeGradientHeightDp: Int,
    val centerMarkerHeightDp: Int,
    val centerMarkerWidthFraction: Float,
    val bottomVisualOffsetDp: Int
)

internal data class LongPressSpeedStartDecision(
    val originalPlaybackParameters: PlaybackParameters,
    val targetPlaybackParameters: PlaybackParameters,
    val clearExistingLock: Boolean
)

internal fun resolveLongPressSpeedLockZoneVisualPolicy(): LongPressSpeedLockZoneVisualPolicy {
    return LongPressSpeedLockZoneVisualPolicy(
        zoneFillAlpha = 0f,
        borderAlpha = 0f,
        edgeGradientAlpha = 0.24f,
        centerMarkerAlpha = 0.68f,
        edgeGradientHeightDp = 16,
        centerMarkerHeightDp = 4,
        centerMarkerWidthFraction = 0.22f,
        bottomVisualOffsetDp = 10
    )
}

internal fun resolveLongPressSpeedLockSensitivityPolicy(
    isFullscreen: Boolean
): LongPressSpeedLockSensitivityPolicy {
    return if (isFullscreen) {
        LongPressSpeedLockSensitivityPolicy(
            lockZoneHeightDp = LONG_PRESS_SPEED_LOCK_ZONE_HEIGHT_DP,
            minDragDistanceDp = 24
        )
    } else {
        LongPressSpeedLockSensitivityPolicy(
            lockZoneHeightDp = 36,
            minDragDistanceDp = 72
        )
    }
}

internal fun resolveGestureSeekableDurationMs(
    playbackDurationMs: Long,
    fallbackDurationMs: Long
): Long {
    return if (playbackDurationMs > 0L) {
        playbackDurationMs
    } else {
        fallbackDurationMs.coerceAtLeast(0L)
    }
}

internal fun shouldKeepVideoPlaybackAwake(
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int
): Boolean {
    if (!playWhenReady) return false
    if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) return false
    return isPlaying || playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY
}

internal fun resolveVideoPlayerBottomGestureExclusionHeightDp(
    controlBarBottomPaddingDp: Int,
    progressSpacingDp: Int,
    progressContainerHeightDp: Int,
    controlRowHeightDp: Int,
    extraBufferDp: Int = PLAYER_DRAG_GESTURE_BOTTOM_EXCLUSION_BUFFER_DP
): Int {
    return (
        controlBarBottomPaddingDp +
            progressSpacingDp +
            progressContainerHeightDp +
            controlRowHeightDp +
            extraBufferDp
        ).coerceAtLeast(0)
}

internal fun shouldIgnoreVideoPlayerDragStart(
    offsetY: Float,
    containerHeightPx: Float,
    topGestureExclusionPx: Float,
    bottomGestureExclusionPx: Float
): Boolean {
    if (containerHeightPx <= 0f) return false
    val clampedTopExclusionPx = topGestureExclusionPx.coerceIn(0f, containerHeightPx)
    val clampedBottomExclusionPx = bottomGestureExclusionPx.coerceIn(0f, containerHeightPx)
    return offsetY < clampedTopExclusionPx ||
        offsetY >= (containerHeightPx - clampedBottomExclusionPx)
}

internal data class VideoPlayerGestureVerticalExclusions(
    val topPx: Float,
    val bottomPx: Float
)

internal fun resolveVideoPlayerGestureVerticalExclusions(
    containerHeightPx: Float,
    isFullscreen: Boolean,
    controlsVisible: Boolean,
    requestedBottomControlsExclusionPx: Float,
    inlineTopExclusionPx: Float,
    inlineBottomExclusionPx: Float,
    fullscreenEdgeExclusionPx: Float
): VideoPlayerGestureVerticalExclusions {
    if (containerHeightPx <= 0f) {
        return VideoPlayerGestureVerticalExclusions(0f, 0f)
    }
    val requestedTop = if (isFullscreen) fullscreenEdgeExclusionPx else inlineTopExclusionPx
    val requestedBottom = when {
        isFullscreen && controlsVisible ->
            maxOf(fullscreenEdgeExclusionPx, requestedBottomControlsExclusionPx)
        isFullscreen -> fullscreenEdgeExclusionPx
        else -> inlineBottomExclusionPx
    }
    val maximumCombinedExclusion = containerHeightPx * 0.5f
    val combined = (requestedTop + requestedBottom).coerceAtLeast(0f)
    if (combined <= maximumCombinedExclusion || combined <= 0f) {
        return VideoPlayerGestureVerticalExclusions(requestedTop, requestedBottom)
    }
    val scale = maximumCombinedExclusion / combined
    return VideoPlayerGestureVerticalExclusions(
        topPx = requestedTop * scale,
        bottomPx = requestedBottom * scale
    )
}

internal fun resolveEffectivePlaybackSpeed(
    requestedSpeed: Float,
    currentAudioQuality: Int
): Float {
    return requestedSpeed.coerceAtLeast(0.1f)
}

internal fun resolveSpeedSafePlaybackParameters(
    requestedSpeed: Float,
    currentAudioQuality: Int
): PlaybackParameters {
    return PlaybackParameters(
        resolveEffectivePlaybackSpeed(
            requestedSpeed = requestedSpeed,
            currentAudioQuality = currentAudioQuality
        ),
        1.0f
    )
}

internal fun resolveEffectiveLongPressSpeed(
    requestedSpeed: Float,
    currentAudioQuality: Int
): Float {
    return resolveEffectivePlaybackSpeed(
        requestedSpeed = requestedSpeed,
        currentAudioQuality = currentAudioQuality
    )
}

internal fun resolveLongPressPlaybackParameters(
    requestedSpeed: Float,
    currentAudioQuality: Int
): PlaybackParameters {
    return resolveSpeedSafePlaybackParameters(
        requestedSpeed = requestedSpeed,
        currentAudioQuality = currentAudioQuality
    )
}

internal fun resolveLongPressSpeedStartDecision(
    currentPlaybackParameters: PlaybackParameters,
    previousOriginalPlaybackParameters: PlaybackParameters,
    longPressSpeedLocked: Boolean,
    requestedSpeed: Float,
    currentAudioQuality: Int
): LongPressSpeedStartDecision {
    return LongPressSpeedStartDecision(
        originalPlaybackParameters = if (longPressSpeedLocked) {
            previousOriginalPlaybackParameters
        } else {
            currentPlaybackParameters
        },
        targetPlaybackParameters = resolveLongPressPlaybackParameters(
            requestedSpeed = requestedSpeed,
            currentAudioQuality = currentAudioQuality
        ),
        clearExistingLock = false
    )
}

internal fun shouldShowHiResLongPressCompatHint(
    requestedSpeed: Float,
    effectiveSpeed: Float,
    hasShownHint: Boolean
): Boolean {
    if (hasShownHint) return false
    return requestedSpeed - effectiveSpeed > 0.001f
}

internal fun shouldEnableLongPressSpeedGesture(
    isScreenLocked: Boolean,
    scale: Float,
    isMultiTouchActive: Boolean
): Boolean {
    return !isScreenLocked && !isMultiTouchActive && scale <= 1.01f
}

internal fun shouldEnableViewportTransformGesture(
    isScreenLocked: Boolean
): Boolean {
    // Disable pinch-to-zoom/pan during playback to avoid accidental viewport
    // distortion while keeping aspect ratio changes inside the explicit menu.
    return false
}

fun resolveSystemStreamVolumeFromGesture(
    startVolumeStep: Int,
    maxVolumeStep: Int,
    totalDragDistanceY: Float,
    screenHeightPx: Float,
    gestureSensitivity: Float
): Int {
    if (maxVolumeStep <= 0 || screenHeightPx <= 0f) return 0
    val deltaStep = (-totalDragDistanceY / screenHeightPx * maxVolumeStep * gestureSensitivity)
        .roundToInt()
    return (startVolumeStep + deltaStep).coerceIn(0, maxVolumeStep)
}

internal fun shouldTriggerPinchExitFullscreen(
    isFullscreen: Boolean,
    isScreenLocked: Boolean,
    twoFingerSpeedAxisLocked: Boolean,
    currentViewportScale: Float,
    cumulativeZoom: Float,
    minExitZoom: Float = 0.82f
): Boolean {
    if (!isFullscreen || isScreenLocked) return false
    if (twoFingerSpeedAxisLocked) return false
    if (currentViewportScale > 1.01f) return false
    return cumulativeZoom < minExitZoom.coerceIn(0.1f, 1.0f)
}

internal fun shouldLockLongPressSpeedInTargetZone(
    longPressSpeedLockEnabled: Boolean = true,
    isLongPressing: Boolean,
    alreadyLocked: Boolean,
    currentPointerY: Float,
    containerHeightPx: Float,
    lockZoneHeightPx: Float,
    accumulatedDragYPx: Float = 0f,
    minDragDistancePx: Float = 0f
): Boolean {
    if (!longPressSpeedLockEnabled) return false
    if (!isLongPressing || alreadyLocked) return false
    if (containerHeightPx <= 0f || lockZoneHeightPx <= 0f) return false
    if (abs(accumulatedDragYPx) < minDragDistancePx.coerceAtLeast(0f)) return false
    val clampedZoneHeightPx = lockZoneHeightPx.coerceAtMost(containerHeightPx * 0.25f)
    return currentPointerY <= clampedZoneHeightPx ||
        currentPointerY >= containerHeightPx - clampedZoneHeightPx
}

internal fun shouldConsumeExclusiveLongPressSpeedDrag(
    isLongPressing: Boolean,
    longPressSpeedLocked: Boolean
): Boolean {
    return isLongPressing && !longPressSpeedLocked
}

internal fun shouldBypassPlaybackSeekSessionProgressOverride(
    isLongPressing: Boolean,
    longPressSpeedLocked: Boolean
): Boolean {
    return isLongPressing || longPressSpeedLocked
}

internal fun resolveProgressDisplayOverridePositionMs(
    seekSession: PlaybackSeekSessionState,
    pendingPlaybackTransitionPositionMs: Long?,
    isLongPressing: Boolean,
    longPressSpeedLocked: Boolean
): Long? {
    if (shouldBypassPlaybackSeekSessionProgressOverride(isLongPressing, longPressSpeedLocked)) {
        return pendingPlaybackTransitionPositionMs
    }
    return if (shouldUsePlaybackSeekSessionPosition(seekSession)) {
        seekSession.sliderPositionMs
    } else {
        pendingPlaybackTransitionPositionMs
    }
}

internal fun resolveGestureSeekStartPositionMs(
    seekSession: PlaybackSeekSessionState,
    playbackPositionMs: Long
): Long {
    return if (seekSession.isSliderMoving) {
        seekSession.sliderPositionMs
    } else {
        playbackPositionMs.coerceAtLeast(0L)
    }
}

internal fun shouldUnlockLockedLongPressSpeedFromRightDownDrag(
    longPressSpeedLocked: Boolean,
    isLongPressing: Boolean,
    startX: Float,
    startY: Float,
    currentY: Float,
    containerWidthPx: Float,
    holdDurationMs: Long,
    minDownDragPx: Float,
    minHoldDurationMs: Long = LONG_PRESS_SPEED_UNLOCK_HOLD_MS
): Boolean {
    if (!longPressSpeedLocked || !isLongPressing) return false
    if (containerWidthPx <= 0f || startX < containerWidthPx * 0.5f) return false
    if (holdDurationMs < minHoldDurationMs.coerceAtLeast(0L)) return false
    return currentY - startY >= minDownDragPx.coerceAtLeast(0f)
}

internal fun shouldReapplyLockedLongPressSpeed(
    longPressSpeedLocked: Boolean,
    isLongPressing: Boolean,
    observedPlaybackSpeed: Float,
    lockedLongPressSpeed: Float
): Boolean {
    return longPressSpeedLocked &&
        !isLongPressing &&
        abs(observedPlaybackSpeed - lockedLongPressSpeed) > 0.001f
}

internal fun shouldClearLockedLongPressSpeedForExplicitSpeedChange(
    longPressSpeedLocked: Boolean,
    isLongPressing: Boolean
): Boolean {
    return longPressSpeedLocked && !isLongPressing
}

internal fun shouldRestorePlaybackParametersAfterLongPressRelease(
    wasLongPressing: Boolean,
    longPressSpeedLocked: Boolean,
    gestureEnded: Boolean
): Boolean {
    return gestureEnded && wasLongPressing && !longPressSpeedLocked
}

internal fun shouldToggleControlsForVideoTap(
    longPressSpeedEndedAtMs: Long,
    nowMs: Long,
    suppressionWindowMs: Long = LONG_PRESS_SPEED_TAP_SUPPRESSION_WINDOW_MS
): Boolean {
    if (longPressSpeedEndedAtMs <= 0L || suppressionWindowMs <= 0L) return true
    val elapsedSinceLongPressEndMs = nowMs - longPressSpeedEndedAtMs
    if (elapsedSinceLongPressEndMs < 0L) return true
    return elapsedSinceLongPressEndMs > suppressionWindowMs
}

internal fun resolveVerticalGestureMode(
    isFullscreen: Boolean,
    isSwipeUp: Boolean,
    startX: Float,
    leftZoneEnd: Float,
    rightZoneStart: Float,
    portraitSwipeToFullscreenEnabled: Boolean,
    centerSwipeToFullscreenEnabled: Boolean,
    slideVolumeBrightnessEnabled: Boolean = true
): VideoGestureMode {
    if (!slideVolumeBrightnessEnabled && startX < leftZoneEnd) {
        return VideoGestureMode.None
    }
    if (!slideVolumeBrightnessEnabled && startX > rightZoneStart) {
        return VideoGestureMode.None
    }
    return when {
        startX < leftZoneEnd -> VideoGestureMode.Brightness
        startX > rightZoneStart -> VideoGestureMode.Volume
        else -> if (
            centerSwipeToFullscreenEnabled ||
            (!isFullscreen && portraitSwipeToFullscreenEnabled && isSwipeUp)
        ) {
            VideoGestureMode.SwipeToFullscreen
        } else {
            VideoGestureMode.None
        }
    }
}

internal fun shouldShowDanmakuLayers(
    isInPipMode: Boolean,
    danmakuEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    pipNoDanmakuEnabled: Boolean,
    hostLifecycleStarted: Boolean
): Boolean {
    if (!hostLifecycleStarted) return false
    if (!danmakuEnabled || isPortraitFullscreen) return false
    if (isInPipMode && pipNoDanmakuEnabled) return false
    return true
}

/**
 * Portrait-only surface mode for detail player danmaku.
 * Landscape fullscreen always stays on the video viewport so horizontal playback is unchanged.
 */
internal fun shouldUseScreenTopDanmakuSurface(
    portraitDisplayAreaMode: com.android.purebilibili.core.store.PortraitDanmakuDisplayAreaMode,
    isLandscapeFullscreen: Boolean
): Boolean {
    if (isLandscapeFullscreen) return false
    return portraitDisplayAreaMode ==
        com.android.purebilibili.core.store.PortraitDanmakuDisplayAreaMode.SCREEN_TOP
}

internal fun resolveDanmakuLayerTopOffsetPx(
    isFullscreen: Boolean,
    statusBarHeightPx: Int,
    useScreenTopSurface: Boolean = false
): Int {
    // Screen-top mode can sit under status bar when chrome is hidden; keep a small inset.
    if (useScreenTopSurface && isFullscreen) {
        return statusBarHeightPx.coerceAtLeast(0)
    }
    return 0
}

internal fun resolveHorizontalSeekDeltaMs(
    isFullscreen: Boolean,
    fullscreenSwipeSeekEnabled: Boolean,
    totalDragDistanceX: Float,
    containerWidthPx: Float,
    fullscreenSwipeSeekSeconds: Int?,
    inlineSwipeSeekSeconds: Int,
    gestureSensitivity: Float
): Long? {
    if (isFullscreen && fullscreenSwipeSeekEnabled) {
        val seekSeconds = fullscreenSwipeSeekSeconds ?: return null
        return resolveConfiguredSeekDeltaMs(
            totalDragDistanceX = totalDragDistanceX,
            containerWidthPx = containerWidthPx,
            seekSeconds = seekSeconds.coerceAtLeast(1),
            gestureSensitivity = gestureSensitivity
        )
    }
    if (!isFullscreen) {
        return resolveConfiguredSeekDeltaMs(
            totalDragDistanceX = totalDragDistanceX,
            containerWidthPx = containerWidthPx,
            seekSeconds = inlineSwipeSeekSeconds.coerceIn(1, 120),
            gestureSensitivity = gestureSensitivity
        )
    }
    return (totalDragDistanceX * 200f * gestureSensitivity).toLong()
}

private fun resolveConfiguredSeekDeltaMs(
    totalDragDistanceX: Float,
    containerWidthPx: Float,
    seekSeconds: Int,
    gestureSensitivity: Float
): Long {
    val effectiveDragRangePx = (containerWidthPx * 0.5f).coerceAtLeast(1f)
    val maxDeltaMs = seekSeconds * 1000L
    val rawDeltaMs = (
        totalDragDistanceX / effectiveDragRangePx * maxDeltaMs * gestureSensitivity
    ).toLong()
    return rawDeltaMs.coerceIn(-maxDeltaMs, maxDeltaMs)
}

internal fun resolveRelativeSeekTargetPosition(
    currentPositionMs: Long,
    deltaMs: Long,
    durationMs: Long
): Long {
    val basePositionMs = currentPositionMs.coerceAtLeast(0L)
    val targetPositionMs = (basePositionMs + deltaMs).coerceAtLeast(0L)
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    return if (safeDurationMs > 0L) {
        targetPositionMs.coerceAtMost(safeDurationMs)
    } else {
        targetPositionMs
    }
}

internal fun shouldCommitGestureSeek(
    currentPositionMs: Long,
    targetPositionMs: Long,
    minDeltaMs: Long = 800L
): Boolean {
    return abs(targetPositionMs - currentPositionMs) >= minDeltaMs
}

/**
 * Stepped haptics while scrubbing by horizontal seek:
 * fire once every [stepMs] of target position change (default 1s ticks).
 */
internal fun shouldTriggerSeekStepHaptic(
    previousTargetMs: Long,
    currentTargetMs: Long,
    stepMs: Long = 1_000L
): Boolean {
    if (stepMs <= 0L) return false
    if (previousTargetMs == currentTargetMs) return false
    return previousTargetMs / stepMs != currentTargetMs / stepMs
}

internal fun resolveOrientationSwitchHintText(isFullscreen: Boolean): String {
    return if (isFullscreen) "已切换到横屏" else "已切换到竖屏"
}

internal fun shouldTriggerFullscreenBySwipe(
    isFullscreen: Boolean,
    reverseGesture: Boolean,
    totalDragDistanceY: Float,
    thresholdPx: Float
): Boolean {
    if (thresholdPx <= 0f) return false
    val isSwipeUp = totalDragDistanceY < -thresholdPx
    val isSwipeDown = totalDragDistanceY > thresholdPx
    return if (!isFullscreen) {
        if (reverseGesture) isSwipeDown else isSwipeUp
    } else {
        if (reverseGesture) isSwipeUp else isSwipeDown
    }
}

internal fun shouldAllowPlaybackStateAutoFullscreen(
    smallestScreenWidthDp: Int
): Boolean {
    return smallestScreenWidthDp > 0
}

internal fun shouldToggleAutoFullscreenForCurrentPlaybackSnapshot(
    autoEnterFullscreenEnabled: Boolean,
    autoExitFullscreenEnabled: Boolean,
    allowPlaybackStateAutoFullscreen: Boolean,
    playbackState: Int,
    playWhenReady: Boolean,
    hasAutoEnteredFullscreen: Boolean,
    isFullscreen: Boolean,
    willContinueToNextItem: Boolean = false,
    autoExitFullscreenMode: com.android.purebilibili.core.store.AutoExitFullscreenMode =
        if (autoExitFullscreenEnabled) {
            com.android.purebilibili.core.store.AutoExitFullscreenMode.ALL_PARTS
        } else {
            com.android.purebilibili.core.store.AutoExitFullscreenMode.OFF
        },
): Boolean {
    return shouldToggleAutoFullscreenForPlaybackEvent(
        autoEnterFullscreenEnabled = autoEnterFullscreenEnabled,
        autoExitFullscreenEnabled = autoExitFullscreenEnabled,
        allowPlaybackStateAutoFullscreen = allowPlaybackStateAutoFullscreen,
        playbackState = playbackState,
        playWhenReady = playWhenReady,
        hasAutoEnteredFullscreen = hasAutoEnteredFullscreen,
        isFullscreen = isFullscreen,
        previousPlayWhenReady = false,
        willContinueToNextItem = willContinueToNextItem,
        autoExitFullscreenMode = autoExitFullscreenMode,
    )
}

internal fun shouldToggleAutoFullscreenForPlaybackEvent(
    autoEnterFullscreenEnabled: Boolean,
    autoExitFullscreenEnabled: Boolean,
    allowPlaybackStateAutoFullscreen: Boolean,
    playbackState: Int,
    playWhenReady: Boolean,
    hasAutoEnteredFullscreen: Boolean,
    isFullscreen: Boolean,
    previousPlayWhenReady: Boolean = playWhenReady,
    willContinueToNextItem: Boolean = false,
    autoExitFullscreenMode: com.android.purebilibili.core.store.AutoExitFullscreenMode =
        if (autoExitFullscreenEnabled) {
            com.android.purebilibili.core.store.AutoExitFullscreenMode.ALL_PARTS
        } else {
            com.android.purebilibili.core.store.AutoExitFullscreenMode.OFF
        },
): Boolean {
    if (!allowPlaybackStateAutoFullscreen) return false

    val shouldEnterFullscreen =
        autoEnterFullscreenEnabled &&
            playbackState == Player.STATE_READY &&
            playWhenReady &&
            !hasAutoEnteredFullscreen &&
            !isFullscreen &&
            (!previousPlayWhenReady || playbackState == Player.STATE_READY)
    if (shouldEnterFullscreen) return true

    return shouldAutoExitFullscreenOnPlaybackEnded(
        mode = autoExitFullscreenMode,
        isFullscreen = isFullscreen,
        playbackState = playbackState,
        willContinueToNextItem = willContinueToNextItem,
    )
}

/**
 * 播放结束是否应退出全屏。
 * - OFF：不退出
 * - CURRENT_PART：当前分P/视频结束即退
 * - ALL_PARTS：仍有下一段（分P/合集/队列）可连播时保持全屏
 */
internal fun shouldAutoExitFullscreenOnPlaybackEnded(
    mode: com.android.purebilibili.core.store.AutoExitFullscreenMode,
    isFullscreen: Boolean,
    playbackState: Int,
    willContinueToNextItem: Boolean,
): Boolean {
    if (!isFullscreen || playbackState != Player.STATE_ENDED) return false
    return when (mode) {
        com.android.purebilibili.core.store.AutoExitFullscreenMode.OFF -> false
        com.android.purebilibili.core.store.AutoExitFullscreenMode.CURRENT_PART -> true
        com.android.purebilibili.core.store.AutoExitFullscreenMode.ALL_PARTS -> !willContinueToNextItem
    }
}

/**
 * 当前条目结束后是否还会自动切到下一段（分P / 合集 / 播放列表）。
 */
internal fun resolveWillContinuePlaybackAfterCurrentItem(
    pageCount: Int,
    currentPageIndex: Int,
    hasUgcSeasonNext: Boolean,
    hasPlaylistNext: Boolean,
    completionAdvancesToNext: Boolean,
): Boolean {
    if (!completionAdvancesToNext) return false
    val hasNextPage = pageCount > 1 &&
        currentPageIndex >= 0 &&
        currentPageIndex < pageCount - 1
    return hasNextPage || hasUgcSeasonNext || hasPlaylistNext
}

internal fun resolveGestureIndicatorLabel(mode: VideoGestureMode): String {
    return when (mode) {
        VideoGestureMode.Brightness -> "亮度"
        VideoGestureMode.Volume -> "音量"
        else -> ""
    }
}

/**
 * @deprecated Prefer [com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle].
 * Kept as a thin bridge for older call sites during the overlay redesign.
 */
enum class GestureLevelIconStyle {
    SharedMaterial,
    Md3,
    Ios,
    Miuix
}

internal fun resolveGestureLevelIconStyle(
    uiPreset: UiPreset,
    androidNativeVariant: AndroidNativeVariant = AndroidNativeVariant.MATERIAL3
): GestureLevelIconStyle {
    return when (
        com.android.purebilibili.feature.video.ui.gesture.resolveGestureLevelOverlayStyle(
            uiPreset = uiPreset,
            androidNativeVariant = androidNativeVariant
        )
    ) {
        com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle.Md3 ->
            GestureLevelIconStyle.Md3
        com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle.Ios ->
            GestureLevelIconStyle.Ios
        com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle.Miuix ->
            GestureLevelIconStyle.Miuix
    }
}

private fun GestureLevelIconStyle.toOverlayStyle():
    com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle {
    return when (this) {
        GestureLevelIconStyle.SharedMaterial,
        GestureLevelIconStyle.Md3 ->
            com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle.Md3
        GestureLevelIconStyle.Ios ->
            com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle.Ios
        GestureLevelIconStyle.Miuix ->
            com.android.purebilibili.feature.video.ui.gesture.GestureLevelOverlayStyle.Miuix
    }
}

internal fun resolveGestureDisplayIcon(
    mode: VideoGestureMode,
    percent: Float,
    fallbackIcon: ImageVector?,
    iconStyle: GestureLevelIconStyle = GestureLevelIconStyle.Md3
): ImageVector {
    val kind = com.android.purebilibili.feature.video.ui.gesture.resolveGestureLevelKind(mode)
        ?: return fallbackIcon
            ?: com.android.purebilibili.feature.video.ui.gesture.resolveGestureLevelIcon(
                style = iconStyle.toOverlayStyle(),
                kind = com.android.purebilibili.feature.video.ui.gesture.GestureLevelKind.Brightness,
                percent = 1f
            )
    return com.android.purebilibili.feature.video.ui.gesture.resolveGestureLevelIcon(
        style = iconStyle.toOverlayStyle(),
        kind = kind,
        percent = percent
    )
}

internal fun resolveVolumeGestureIcon(
    percent: Float,
    iconStyle: GestureLevelIconStyle
): ImageVector {
    return com.android.purebilibili.feature.video.ui.gesture.resolveGestureLevelIcon(
        style = iconStyle.toOverlayStyle(),
        kind = com.android.purebilibili.feature.video.ui.gesture.GestureLevelKind.Volume,
        percent = percent
    )
}

internal fun resolveBrightnessGestureIcon(
    percent: Float,
    iconStyle: GestureLevelIconStyle
): ImageVector {
    return com.android.purebilibili.feature.video.ui.gesture.resolveGestureLevelIcon(
        style = iconStyle.toOverlayStyle(),
        kind = com.android.purebilibili.feature.video.ui.gesture.GestureLevelKind.Brightness,
        percent = percent
    )
}

internal data class GestureLevelOverlayVisualPolicy(
    val accentColor: Color,
    val containerAlpha: Float,
    val borderAlpha: Float,
    val glowAlpha: Float
)

internal data class VideoGestureMotionSpec(
    val digitInitialBlurRadiusDp: Float,
    val digitInitialAlpha: Float,
    val digitBlurHoldDurationMillis: Int,
    val digitBlurResetDurationMillis: Int,
    val digitAlphaResetDurationMillis: Int,
    val digitEnterFadeDurationMillis: Int,
    val digitExitFadeDurationMillis: Int,
    val digitScaleDurationMillis: Int,
    val digitSlideSpringDampingRatio: Float,
    val digitSlideSpringStiffness: Float,
    val levelOverlayEnterFadeDurationMillis: Int,
    val levelOverlayEnterTransformDurationMillis: Int,
    val levelOverlayExitDurationMillis: Int,
    val levelProgressDurationMillis: Int,
    val levelIconScaleDurationMillis: Int,
    val levelValueScaleDurationMillis: Int,
    val levelIconEnterFadeDurationMillis: Int,
    val levelIconExitFadeDurationMillis: Int,
    val levelIconContentScaleDurationMillis: Int,
    val orientationHintEnterFadeDurationMillis: Int,
    val orientationHintEnterTransformDurationMillis: Int,
    val orientationHintExitDurationMillis: Int,
    val longPressHintDurationMillis: Int,
    val longPressArrowCycleDurationMillis: Int,
    val longPressArrowPhaseStepDurationMillis: Int
)

internal fun resolveVideoGestureMotionSpec(): VideoGestureMotionSpec {
    return VideoGestureMotionSpec(
        digitInitialBlurRadiusDp = GesturePercentMotionDefaults.InitialBlurRadiusDp,
        digitInitialAlpha = GesturePercentMotionDefaults.InitialAlpha,
        digitBlurHoldDurationMillis = GesturePercentMotionDefaults.BlurHoldDurationMillis,
        digitBlurResetDurationMillis = GesturePercentMotionDefaults.BlurResetDurationMillis,
        digitAlphaResetDurationMillis = GesturePercentMotionDefaults.AlphaResetDurationMillis,
        digitEnterFadeDurationMillis = GesturePercentMotionDefaults.EnterFadeDurationMillis,
        digitExitFadeDurationMillis = GesturePercentMotionDefaults.ExitFadeDurationMillis,
        digitScaleDurationMillis = 0,
        digitSlideSpringDampingRatio = GesturePercentMotionDefaults.SlideSpringDampingRatio,
        digitSlideSpringStiffness = GesturePercentMotionDefaults.SlideSpringStiffness,
        levelOverlayEnterFadeDurationMillis = 160,
        levelOverlayEnterTransformDurationMillis = 220,
        levelOverlayExitDurationMillis = 200,
        levelProgressDurationMillis = 130,
        levelIconScaleDurationMillis = 180,
        levelValueScaleDurationMillis = 140,
        levelIconEnterFadeDurationMillis = 120,
        levelIconExitFadeDurationMillis = 110,
        levelIconContentScaleDurationMillis = 180,
        orientationHintEnterFadeDurationMillis = 150,
        orientationHintEnterTransformDurationMillis = 230,
        orientationHintExitDurationMillis = 200,
        longPressHintDurationMillis = 200,
        longPressArrowCycleDurationMillis = 900,
        longPressArrowPhaseStepDurationMillis = 300
    )
}

internal fun resolveGestureRenderProgress(percent: Float): Float {
    return percent.coerceIn(0f, 1f)
}

internal fun resolveGestureLevelOverlayVisualPolicy(
    mode: VideoGestureMode,
    percent: Float
): GestureLevelOverlayVisualPolicy {
    val progress = resolveGestureRenderProgress(percent)
    return when (mode) {
        VideoGestureMode.Brightness -> GestureLevelOverlayVisualPolicy(
            accentColor = Color(0xFFFFD54F),
            // Dark scrim must stay opaque enough for white text on bright frames.
            containerAlpha = 0.70f + progress * 0.06f,
            borderAlpha = 0.48f + progress * 0.22f,
            glowAlpha = 0.30f + progress * 0.40f
        )

        VideoGestureMode.Volume -> GestureLevelOverlayVisualPolicy(
            accentColor = Color(0xFF80DEEA),
            containerAlpha = 0.70f + progress * 0.06f,
            borderAlpha = 0.46f + progress * 0.22f,
            glowAlpha = 0.28f + progress * 0.38f
        )

        else -> GestureLevelOverlayVisualPolicy(
            accentColor = Color.White,
            containerAlpha = 0.72f,
            borderAlpha = 0.50f,
            glowAlpha = 0.32f
        )
    }
}

internal fun resolveGesturePercentDigits(percent: Int): List<Char?> {
    val normalized = percent.coerceIn(0, 100)
    val hundreds = (normalized / 100)
    val tens = (normalized / 10) % 10
    val ones = normalized % 10
    return listOf(
        hundreds.takeIf { it > 0 }?.let { ('0'.code + it).toChar() },
        if (hundreds > 0 || tens > 0) ('0'.code + tens).toChar() else null,
        ('0'.code + ones).toChar()
    )
}

internal fun resolveGesturePercentDigitChangeMask(
    previousPercent: Int,
    currentPercent: Int
): List<Boolean> {
    val previousDigits = resolveGesturePercentDigits(previousPercent)
    val currentDigits = resolveGesturePercentDigits(currentPercent)
    return currentDigits.indices.map { index ->
        previousDigits.getOrNull(index) != currentDigits.getOrNull(index)
    }
}

internal fun shouldUseTextureSurfaceForFlip(
    isFlippedHorizontal: Boolean,
    isFlippedVertical: Boolean,
    liveBackPreview: Boolean = false,
    navigationTransformEnabled: Boolean = false
): Boolean {
    return isFlippedHorizontal ||
        isFlippedVertical ||
        liveBackPreview ||
        navigationTransformEnabled
}

internal fun shouldEnableLivePlayerSharedElement(
    transitionEnabled: Boolean,
    allowLivePlayerSharedElement: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean,
    forceCoverDuringReturnAnimation: Boolean = false
): Boolean {
    if (forceCoverDuringReturnAnimation) return false
    return transitionEnabled &&
        allowLivePlayerSharedElement &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope
}

internal fun resolveSubtitleLanguageLabel(
    languageCode: String?,
    fallbackLabel: String
): String {
    val normalized = languageCode?.lowercase().orEmpty()
    return when {
        normalized.contains("zh") -> "中文"
        normalized.contains("en") -> "英文"
        languageCode.isNullOrBlank() -> fallbackLabel
        else -> languageCode
    }
}

internal fun shouldForceCoverDuringReturnAnimation(
    forceCoverOnly: Boolean
): Boolean {
    return forceCoverOnly
}

internal fun shouldShowCoverImage(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean,
    hasStartedSmoothReveal: Boolean
): Boolean {
    return shouldHoldEntryCoverUnderlay(
        isFirstFrameRendered = isFirstFrameRendered,
        forceCoverDuringReturnAnimation = forceCoverDuringReturnAnimation,
        shouldKeepCoverForManualStart = shouldKeepCoverForManualStart,
        hasStartedSmoothReveal = hasStartedSmoothReveal,
    )
}

/**
 * 即播进场 / CoverFirst / 返回：封面作为不透明垫底，直到首帧揭示或手动起播。
 * 垫底期间禁止淡入淡出与 Coil crossfade，避免 Hero morph 透出黑底。
 */
internal fun shouldHoldEntryCoverUnderlay(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean,
    hasStartedSmoothReveal: Boolean,
): Boolean {
    return forceCoverDuringReturnAnimation ||
        shouldKeepCoverForManualStart ||
        !isFirstFrameRendered ||
        !hasStartedSmoothReveal
}

internal data class VideoPlayerCoverBootstrapState(
    val isFirstFrameRendered: Boolean,
    val hasStartedSmoothReveal: Boolean
)

/**
 * 进场封面 bootstrap。
 *
 * - 可复用已渲染首帧时：跳过等待 FIRST_FRAME 事件，但 **不** 直接把 smooth reveal 置 true。
 *   否则「详情已出画 → 回首页 → 再进详情」会瞬间揭开画面，封面→画面过渡丢失。
 * - 快速返回再进时若首帧标志短暂清空，会重新走完整 reveal；两条路径应统一为：
 *   先垫封面，再 delay 后 hasStartedSmoothReveal=true。
 */
internal fun resolveVideoPlayerCoverBootstrapState(
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean,
    hasPersistedRenderedFirstFrame: Boolean
): VideoPlayerCoverBootstrapState {
    val shouldReuseRenderedFrame = !forceCoverDuringReturnAnimation &&
        !shouldKeepCoverForManualStart &&
        hasPersistedRenderedFirstFrame
    return VideoPlayerCoverBootstrapState(
        isFirstFrameRendered = shouldReuseRenderedFrame,
        // 始终从封面垫底起步，由 shouldStartSmoothCoverReveal + hold delay 触发过渡。
        hasStartedSmoothReveal = false,
    )
}

internal fun shouldStartSmoothCoverReveal(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean
): Boolean {
    return isFirstFrameRendered &&
        !forceCoverDuringReturnAnimation &&
        !shouldKeepCoverForManualStart
}

internal fun shouldKeepCoverForManualStart(
    playWhenReady: Boolean,
    currentPositionMs: Long,
    autoPlayEnabled: Boolean = true,
    hasManualStartPlaybackIntent: Boolean = false
): Boolean {
    if (!autoPlayEnabled && !hasManualStartPlaybackIntent) return true
    if (playWhenReady) return false
    return currentPositionMs <= 0L
}

internal fun shouldShowManualStartPlayButton(
    shouldKeepCoverForManualStart: Boolean
): Boolean {
    return shouldKeepCoverForManualStart
}

internal fun shouldEnableManualStartCoverOverlay(
    shouldKeepCoverForManualStart: Boolean
): Boolean {
    return shouldKeepCoverForManualStart
}

internal enum class VideoPlayerCoverContentScaleMode {
    Crop,
    Fit
}

internal data class VideoPlayerEntryPresentationSpec(
    val coverUsesSharedBounds: Boolean,
    val fillCoverViewport: Boolean,
    val showManualStartPlayButton: Boolean,
    val enableManualStartCoverOverlay: Boolean,
    val coverContentScaleMode: VideoPlayerCoverContentScaleMode
)

internal fun resolveVideoPlayerEntryPresentationSpec(
    shouldKeepCoverForManualStart: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    isVerticalVideo: Boolean,
    targetMode: com.android.purebilibili.core.ui.transition.VideoSharedTransitionTargetMode
): VideoPlayerEntryPresentationSpec {
    val targetFillsViewport =
        targetMode == com.android.purebilibili.core.ui.transition.VideoSharedTransitionTargetMode.LandscapeFullscreen ||
            targetMode == com.android.purebilibili.core.ui.transition.VideoSharedTransitionTargetMode.PortraitFullscreen
    val fillCoverViewport = !forceCoverDuringReturnAnimation &&
        (targetFillsViewport || (shouldKeepCoverForManualStart && isVerticalVideo))
    return VideoPlayerEntryPresentationSpec(
        coverUsesSharedBounds = forceCoverDuringReturnAnimation || shouldKeepCoverForManualStart,
        fillCoverViewport = fillCoverViewport,
        showManualStartPlayButton = shouldKeepCoverForManualStart,
        enableManualStartCoverOverlay = shouldKeepCoverForManualStart,
        coverContentScaleMode = if (fillCoverViewport && isVerticalVideo) {
            VideoPlayerCoverContentScaleMode.Fit
        } else {
            VideoPlayerCoverContentScaleMode.Crop
        }
    )
}

internal fun shouldFillPlayerViewportForManualStartCover(
    shouldKeepCoverForManualStart: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    isVerticalVideo: Boolean = false
): Boolean {
    if (forceCoverDuringReturnAnimation) return false
    return isVerticalVideo
}

internal enum class ManualStartPlayButtonAnchor {
    Center,
    CenterEnd,
    BottomEnd
}

internal data class ManualStartPlayButtonLayoutSpec(
    val anchor: ManualStartPlayButtonAnchor,
    val endPaddingDp: Int,
    val iconWidthDp: Int,
    val iconHeightDp: Int,
    val showCoverScrim: Boolean,
    val showTopDecorations: Boolean
)

internal fun resolveManualStartPlayButtonLayoutSpec(): ManualStartPlayButtonLayoutSpec {
    return ManualStartPlayButtonLayoutSpec(
        anchor = ManualStartPlayButtonAnchor.BottomEnd,
        endPaddingDp = 24,
        iconWidthDp = 72,
        iconHeightDp = 60,
        showCoverScrim = false,
        showTopDecorations = false
    )
}

internal fun shouldDisableCoverFadeAnimation(
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return forceCoverDuringReturnAnimation
}

internal data class VideoPlayerCoverMotionSpec(
    val shouldAnimateFade: Boolean,
    val enterFadeDurationMillis: Int,
    val exitFadeDurationMillis: Int
)

internal data class VideoPlayerRevealMotionSpec(
    val coverRevealHoldDelayMillis: Int,
    val surfaceRevealDurationMillis: Int,
    val surfaceRevealInitialScale: Float
)

internal data class VideoPlayerSurfaceRevealSpec(
    val alpha: Float,
    val scale: Float
)

internal fun resolveVideoPlayerCoverMotionSpec(
    forceCoverDuringReturnAnimation: Boolean,
    holdEntryCoverUnderlay: Boolean = false,
): VideoPlayerCoverMotionSpec {
    return VideoPlayerCoverMotionSpec(
        shouldAnimateFade = !forceCoverDuringReturnAnimation && !holdEntryCoverUnderlay,
        enterFadeDurationMillis = VIDEO_PLAYER_COVER_FADE_ENTER_DURATION_MILLIS,
        exitFadeDurationMillis = VIDEO_PLAYER_COVER_FADE_EXIT_DURATION_MILLIS
    )
}

internal fun resolveVideoPlayerRevealMotionSpec(): VideoPlayerRevealMotionSpec {
    return VideoPlayerRevealMotionSpec(
        coverRevealHoldDelayMillis = VIDEO_PLAYER_COVER_REVEAL_HOLD_DELAY_MILLIS,
        surfaceRevealDurationMillis = VIDEO_PLAYER_SURFACE_REVEAL_DURATION_MILLIS,
        surfaceRevealInitialScale = VIDEO_PLAYER_SURFACE_REVEAL_INITIAL_SCALE
    )
}

internal fun resolveVideoPlayerSurfaceRevealSpec(
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean,
    hasStartedSmoothReveal: Boolean,
    surfaceRevealInitialScale: Float = VIDEO_PLAYER_SURFACE_REVEAL_INITIAL_SCALE
): VideoPlayerSurfaceRevealSpec {
    if (forceCoverDuringReturnAnimation) {
        return VideoPlayerSurfaceRevealSpec(alpha = 0f, scale = 1f)
    }
    if (shouldKeepCoverForManualStart) {
        return VideoPlayerSurfaceRevealSpec(alpha = 0f, scale = 1f)
    }
    if (!hasStartedSmoothReveal) {
        return VideoPlayerSurfaceRevealSpec(
            alpha = 0f,
            scale = surfaceRevealInitialScale
        )
    }
    return VideoPlayerSurfaceRevealSpec(alpha = 1f, scale = 1f)
}

internal fun shouldHidePlayerSurfaceDuringForcedReturn(
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return forceCoverDuringReturnAnimation
}

internal fun shouldKeepInlinePlayerContentOnReset(
    isPortraitFullscreen: Boolean,
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return !isPortraitFullscreen && !forceCoverDuringReturnAnimation
}

internal fun shouldShowInlinePlayerView(
    isPortraitFullscreen: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    shouldKeepCoverForManualStart: Boolean = false
): Boolean {
    return !isPortraitFullscreen &&
        !forceCoverDuringReturnAnimation &&
        !shouldKeepCoverForManualStart
}

internal fun shouldEnableCoverImageCrossfade(
    forceCoverDuringReturnAnimation: Boolean,
    holdEntryCoverUnderlay: Boolean = false,
): Boolean {
    return !forceCoverDuringReturnAnimation && !holdEntryCoverUnderlay
}

internal fun resolvePreferredVideoCoverUrl(
    entryCoverUrl: String,
    detailCoverUrl: String,
    preferDetailCoverUrl: Boolean = false
): String {
    val normalizedDetailCoverUrl = detailCoverUrl.trim()
    if (preferDetailCoverUrl && normalizedDetailCoverUrl.isNotEmpty()) {
        return normalizedDetailCoverUrl
    }

    val normalizedEntryCoverUrl = entryCoverUrl.trim()
    if (normalizedEntryCoverUrl.isNotEmpty()) return normalizedEntryCoverUrl

    if (normalizedDetailCoverUrl.isNotEmpty()) return normalizedDetailCoverUrl

    return ""
}

internal fun shouldEnableForcedReturnCoverSharedBounds(
    forceCoverDuringReturnAnimation: Boolean,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean,
    sourceRoute: String?
): Boolean {
    return shouldEnableCoverOverlaySharedBounds(
        useCoverOverlaySharedBounds = forceCoverDuringReturnAnimation,
        transitionEnabled = transitionEnabled,
        hasSharedTransitionScope = hasSharedTransitionScope,
        hasAnimatedVisibilityScope = hasAnimatedVisibilityScope,
        sourceRoute = sourceRoute
    )
}

internal fun shouldEnableCoverOverlaySharedBounds(
    useCoverOverlaySharedBounds: Boolean,
    transitionEnabled: Boolean,
    hasSharedTransitionScope: Boolean,
    hasAnimatedVisibilityScope: Boolean,
    sourceRoute: String?
): Boolean {
    val sourceRouteBase = sourceRoute?.substringBefore("?")
    // 返回阶段或手动封面阶段由封面承接同一个 cover key，
    // 避免播放器画面和封面各自跑一段动画。
    val allowBySourceRoute = sourceRouteBase == null ||
        (
            com.android.purebilibili.navigation.isVideoCardReturnTargetRoute(sourceRouteBase) &&
                !com.android.purebilibili.core.ui.transition.shouldSkipVideoCardSharedBoundsMorph(
                    sourceRouteBase
                )
        )
    return useCoverOverlaySharedBounds &&
        transitionEnabled &&
        hasSharedTransitionScope &&
        hasAnimatedVisibilityScope &&
        allowBySourceRoute
}

internal fun resolveForcedReturnCoverSharedElementSourceRoute(sourceRoute: String?): String? {
    val sourceRouteBase = sourceRoute?.substringBefore("?")
    return if (com.android.purebilibili.navigation.isVideoCardReturnTargetRoute(sourceRouteBase)) {
        sourceRouteBase?.takeIf { it.isNotBlank() }
    } else {
        null
    }
}

internal fun shouldUseReturnLandingMotionForForcedReturnCover(
    forceCoverDuringReturnAnimation: Boolean
): Boolean {
    return forceCoverDuringReturnAnimation
}

internal fun shouldPromoteFirstFrameByPlaybackFallback(
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    playbackState: Int,
    playWhenReady: Boolean,
    currentPositionMs: Long,
    videoWidth: Int,
    videoHeight: Int
): Boolean {
    if (isFirstFrameRendered || forceCoverDuringReturnAnimation) return false
    val hasVideoTrack = videoWidth > 0 && videoHeight > 0
    return hasVideoTrack &&
        playWhenReady &&
        playbackState == Player.STATE_READY &&
        currentPositionMs > 300L
}

internal fun shouldAutoHidePlayerChromeOnPlaybackStart(
    showControls: Boolean,
    hasAutoHiddenForCurrentVideo: Boolean,
    isPlaying: Boolean,
    isFirstFrameRendered: Boolean,
    forceCoverDuringReturnAnimation: Boolean,
    isSeekScrubbing: Boolean
): Boolean {
    return showControls &&
        !hasAutoHiddenForCurrentVideo &&
        isPlaying &&
        isFirstFrameRendered &&
        !forceCoverDuringReturnAnimation &&
        !isSeekScrubbing
}

internal fun shouldRebindPlayerSurfaceOnForeground(
    hasPlayerView: Boolean,
    isInPipMode: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    needsSurfaceRecovery: Boolean = true
): Boolean {
    if (!hasPlayerView || isInPipMode) return false
    if (needsSurfaceRecovery) return true
    return videoWidth <= 0 || videoHeight <= 0
}

internal fun shouldStartForegroundSurfaceRecovery(
    hasPlayerView: Boolean,
    shouldBindInlinePlayerView: Boolean,
    isInPipMode: Boolean,
    needsSurfaceRecovery: Boolean = true,
    videoWidth: Int = 0,
    videoHeight: Int = 0
): Boolean {
    if (!hasPlayerView || !shouldBindInlinePlayerView || isInPipMode) return false
    if (needsSurfaceRecovery) return true
    return videoWidth <= 0 || videoHeight <= 0
}

internal fun shouldKickPlaybackAfterSurfaceRecovery(
    playWhenReady: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
    hasPlaybackResumeIntent: Boolean = true
): Boolean {
    return hasPlaybackResumeIntent &&
        playWhenReady &&
        !isPlaying &&
        (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)
}

internal fun shouldLogForegroundSurfaceRecoveryTimeout(
    hasRenderedFirstFrameSinceRecovery: Boolean,
    playWhenReady: Boolean,
    playbackState: Int
): Boolean {
    if (hasRenderedFirstFrameSinceRecovery || !playWhenReady) return false
    return playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING
}

internal fun shouldLogPlaybackStall(
    bufferingDurationMs: Long,
    playWhenReady: Boolean,
    currentPositionMs: Long
): Boolean {
    return bufferingDurationMs >= PLAYBACK_STALL_LOG_THRESHOLD_MS &&
        playWhenReady &&
        currentPositionMs > 0L
}

internal fun shouldBindInlinePlayerViewToPlayer(
    isPortraitFullscreen: Boolean,
    hostLifecycleStarted: Boolean,
    isInPipMode: Boolean,
    liveBackPreview: Boolean = false
): Boolean {
    return !isPortraitFullscreen &&
        (hostLifecycleStarted || isInPipMode || liveBackPreview)
}

internal fun shouldRecoverInlinePlayerAfterPredictiveBackCancel(
    recoveryGeneration: Int,
    hasPlayerView: Boolean,
    shouldBindInlinePlayerView: Boolean,
    isInPipMode: Boolean
): Boolean {
    return recoveryGeneration > 0 &&
        hasPlayerView &&
        shouldBindInlinePlayerView &&
        !isInPipMode
}

internal fun shouldLoadDanmakuForForegroundHost(
    hostLifecycleStarted: Boolean,
    shouldLoadImmediately: Boolean
): Boolean {
    return hostLifecycleStarted && shouldLoadImmediately
}

internal fun rebindPlayerSurfaceIfNeeded(
    playerView: PlayerView,
    player: Player
) {
    when (val videoSurface = playerView.videoSurfaceView) {
        is TextureView -> {
            player.clearVideoTextureView(videoSurface)
        }
        is SurfaceView -> {
            player.clearVideoSurfaceView(videoSurface)
        }
    }
    if (playerView.player === player) {
        playerView.player = null
    }
    playerView.player = player
    when (val videoSurface = playerView.videoSurfaceView) {
        is TextureView -> {
            player.setVideoTextureView(videoSurface)
        }
        is SurfaceView -> {
            player.setVideoSurfaceView(videoSurface)
        }
    }
}
