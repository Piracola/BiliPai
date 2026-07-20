package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.OrientationEventListener
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import kotlin.math.abs

internal data class VideoDetailSystemBarsSnapshot(
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean,
    val systemBarsBehavior: Int
)

internal fun resolveVideoDetailSystemBarsSnapshot(
    statusBarColor: Int?,
    navigationBarColor: Int?,
    lightStatusBars: Boolean?,
    lightNavigationBars: Boolean?,
    systemBarsBehavior: Int?,
    fallbackColor: Int,
    fallbackLightBars: Boolean,
    fallbackSystemBarsBehavior: Int
): VideoDetailSystemBarsSnapshot {
    return VideoDetailSystemBarsSnapshot(
        statusBarColor = statusBarColor ?: fallbackColor,
        navigationBarColor = navigationBarColor ?: fallbackColor,
        lightStatusBars = lightStatusBars ?: fallbackLightBars,
        lightNavigationBars = lightNavigationBars ?: fallbackLightBars,
        systemBarsBehavior = systemBarsBehavior ?: fallbackSystemBarsBehavior
    )
}

internal fun shouldShowSystemBarsOnVideoDetailExit(): Boolean {
    return true
}

internal data class VideoDetailSystemBarsVisibilityPolicy(
    val hideStatusBars: Boolean,
    val hideNavigationBars: Boolean
)

internal enum class VideoDetailHiddenSystemBars {
    NONE,
    STATUS_BARS,
    SYSTEM_BARS
}

internal data class VideoDetailSystemBarsApplySpec(
    val hiddenBars: VideoDetailHiddenSystemBars,
    val systemBarsBehavior: Int,
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val lightStatusBars: Boolean,
    val lightNavigationBars: Boolean
)

internal fun resolveVideoDetailSystemBarsVisibilityPolicy(
    isFullscreenMode: Boolean,
    hideVideoPageStatusBar: Boolean,
    isInPipMode: Boolean,
    isScreenActive: Boolean
): VideoDetailSystemBarsVisibilityPolicy {
    if (!isScreenActive || isInPipMode) {
        return VideoDetailSystemBarsVisibilityPolicy(
            hideStatusBars = false,
            hideNavigationBars = false
        )
    }
    if (isFullscreenMode) {
        return VideoDetailSystemBarsVisibilityPolicy(
            hideStatusBars = true,
            hideNavigationBars = true
        )
    }
    return VideoDetailSystemBarsVisibilityPolicy(
        hideStatusBars = hideVideoPageStatusBar,
        hideNavigationBars = false
    )
}

internal fun resolveVideoDetailSystemBarsApplySpec(
    visibilityPolicy: VideoDetailSystemBarsVisibilityPolicy,
    useTabletLayout: Boolean,
    isLightBackground: Boolean,
    backgroundColor: Int,
    transparentColor: Int,
    blackColor: Int,
    transientBarsBehavior: Int
): VideoDetailSystemBarsApplySpec {
    if (visibilityPolicy.hideNavigationBars) {
        return VideoDetailSystemBarsApplySpec(
            hiddenBars = VideoDetailHiddenSystemBars.SYSTEM_BARS,
            systemBarsBehavior = transientBarsBehavior,
            statusBarColor = blackColor,
            navigationBarColor = blackColor,
            lightStatusBars = false,
            lightNavigationBars = false
        )
    }

    val hiddenBars = if (visibilityPolicy.hideStatusBars) {
        VideoDetailHiddenSystemBars.STATUS_BARS
    } else {
        VideoDetailHiddenSystemBars.NONE
    }
    return if (useTabletLayout) {
        VideoDetailSystemBarsApplySpec(
            hiddenBars = hiddenBars,
            systemBarsBehavior = transientBarsBehavior,
            statusBarColor = backgroundColor,
            navigationBarColor = backgroundColor,
            lightStatusBars = isLightBackground,
            lightNavigationBars = isLightBackground
        )
    } else {
        VideoDetailSystemBarsApplySpec(
            hiddenBars = hiddenBars,
            systemBarsBehavior = transientBarsBehavior,
            statusBarColor = transparentColor,
            navigationBarColor = transparentColor,
            lightStatusBars = false,
            lightNavigationBars = false
        )
    }
}

internal fun resolveVideoDetailStableStatusBarHeightDp(
    visibleStatusBarHeightDp: Float,
    statusBarIgnoringVisibilityHeightDp: Float,
    hideStatusBars: Boolean
): Float {
    fun sanitize(value: Float): Float {
        return value.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    }

    val visibleInset = sanitize(visibleStatusBarHeightDp)
    val stableInset = sanitize(statusBarIgnoringVisibilityHeightDp)
    return if (hideStatusBars) {
        stableInset.coerceAtLeast(visibleInset)
    } else {
        visibleInset
    }
}

internal fun resolveVideoDetailPortraitPlayerTopInsetDp(
    stableStatusBarHeightDp: Float,
    hideStatusBars: Boolean
): Float {
    val stableInset = stableStatusBarHeightDp
        .takeIf { it.isFinite() }
        ?.coerceAtLeast(0f)
        ?: 0f
    return if (hideStatusBars) 0f else stableInset
}

internal fun shouldRestoreSystemBarsDuringVideoDetailExitTransition(
    isExitTransitionInProgress: Boolean,
    isActuallyLeaving: Boolean
): Boolean {
    if (!isExitTransitionInProgress) return false
    if (isActuallyLeaving) return false
    return true
}

/**
 * 预测返回手势开始退出时会提前 restore 状态栏；若手势取消、详情仍留在栈顶，
 * 需重新激活沉浸式，否则会一直停在非沉浸（常见为黑底状态栏）。
 */
internal fun shouldReactivateVideoDetailSystemBarsAfterCancelledExit(
    isExitTransitionInProgress: Boolean,
    isActuallyLeaving: Boolean,
    isScreenActive: Boolean,
): Boolean {
    if (isExitTransitionInProgress) return false
    if (isActuallyLeaving) return false
    return !isScreenActive
}

/** 从相关视频等上层页预测返回后，底层详情重新成为栈顶时需强制重套系统栏。 */
internal fun shouldReapplyVideoDetailSystemBarsAfterBecomingTop(
    wasKeepLoadedContentForBackPreview: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    isActuallyLeaving: Boolean,
): Boolean {
    if (isActuallyLeaving) return false
    return wasKeepLoadedContentForBackPreview && !keepLoadedContentForBackPreview
}

internal fun applyVideoDetailSystemBarsSpec(
    window: Window,
    insetsController: WindowInsetsControllerCompat,
    spec: VideoDetailSystemBarsApplySpec
) {
    when (spec.hiddenBars) {
        VideoDetailHiddenSystemBars.SYSTEM_BARS -> {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        VideoDetailHiddenSystemBars.STATUS_BARS -> {
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
        VideoDetailHiddenSystemBars.NONE -> {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    insetsController.systemBarsBehavior = spec.systemBarsBehavior
    insetsController.isAppearanceLightStatusBars = spec.lightStatusBars
    insetsController.isAppearanceLightNavigationBars = spec.lightNavigationBars
    window.statusBarColor = spec.statusBarColor
    window.navigationBarColor = spec.navigationBarColor
}


internal fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

internal fun toggleVideoDetailFullscreen(
    activity: Activity?,
    isOrientationDrivenFullscreen: Boolean,
    isLandscape: Boolean,
    isFullscreenMode: Boolean,
    isCompactDevice: Boolean,
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    isVerticalVideo: Boolean,
    portraitExperienceEnabled: Boolean,
    onEnterPortraitFullscreen: () -> Unit,
    onUserRequestedFullscreenChange: (Boolean) -> Unit,
    onManualPortraitHoldActiveChange: (Boolean) -> Unit
) {
    if (activity == null) return

    val isInMultiWindowMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode
    val isInPictureInPictureMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        activity.isInPictureInPictureMode
    if (shouldUseInWindowFullscreenForSystemMultiWindow(
            isInMultiWindowMode = isInMultiWindowMode,
            isInPictureInPictureMode = isInPictureInPictureMode,
            isOrientationDrivenFullscreen = isOrientationDrivenFullscreen,
            isFullscreenMode = isFullscreenMode
        )
    ) {
        onUserRequestedFullscreenChange(true)
        onManualPortraitHoldActiveChange(false)
        return
    }

    if (isOrientationDrivenFullscreen && isInMultiWindowMode && isFullscreenMode) {
        onUserRequestedFullscreenChange(false)
        onManualPortraitHoldActiveChange(false)
        return
    }

    if (!isOrientationDrivenFullscreen) {
        val nextRequestedFullscreen = !isFullscreenMode
        onUserRequestedFullscreenChange(nextRequestedFullscreen)
        if (!nextRequestedFullscreen &&
            isCompactDevice &&
            fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL
        ) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        return
    }

    if (isLandscape) {
        onUserRequestedFullscreenChange(false)
        onManualPortraitHoldActiveChange(true)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return
    }

    val targetOrientation = resolvePhoneFullscreenEnterOrientation(
        fullscreenMode = fullscreenMode,
        isVerticalVideo = isVerticalVideo
    ) ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

    if (shouldEnterPortraitFullscreenOnFullscreenToggle(
            targetOrientation = targetOrientation,
            portraitExperienceEnabled = portraitExperienceEnabled
        )
    ) {
        onUserRequestedFullscreenChange(false)
        onManualPortraitHoldActiveChange(false)
        onEnterPortraitFullscreen()
        return
    }

    onUserRequestedFullscreenChange(true)
    onManualPortraitHoldActiveChange(false)
    activity.requestedOrientation = targetOrientation
}

internal fun resolveNextPlayerHeightOffset(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    maxOffsetPx: Float = 0f,
    minUpdateDeltaPx: Float = 0.75f
): Float? {
    if (abs(deltaPx) < minUpdateDeltaPx) return null
    val nextOffset = (currentOffsetPx + deltaPx).coerceIn(minOffsetPx, maxOffsetPx)
    return if (abs(nextOffset - currentOffsetPx) < minUpdateDeltaPx) {
        null
    } else {
        nextOffset
    }
}

internal fun resolveIsPlayerCollapsed(
    swipeHidePlayerEnabled: Boolean,
    playerHeightOffsetPx: Float,
    videoHeightPx: Float,
    collapseTolerancePx: Float = 10f
): Boolean {
    if (!swipeHidePlayerEnabled) return false
    return playerHeightOffsetPx <= (-videoHeightPx + collapseTolerancePx)
}

internal fun resolveIsPlaybackPausedForCollapse(
    playWhenReady: Boolean,
    playbackState: Int
): Boolean {
    // 这里按用户暂停意图判断，而不是按 isPlaying，避免缓冲态误判为“暂停时可缩小”。
    return !playWhenReady && playbackState != Player.STATE_ENDED
}

internal fun shouldUseTabletVideoLayout(
    isExpandedScreen: Boolean,
    isTabletDevice: Boolean
): Boolean {
    return isExpandedScreen && isTabletDevice
}

internal fun shouldUseOrientationDrivenFullscreen(
    isCompactDevice: Boolean
): Boolean {
    return isCompactDevice
}

internal fun shouldRotateToPortraitOnSplitBack(
    useTabletLayout: Boolean,
    isCompactDevice: Boolean,
    orientation: Int
): Boolean {
    return useTabletLayout && isCompactDevice && orientation == Configuration.ORIENTATION_LANDSCAPE
}

internal fun shouldShowDetachedVideoCommentThreadHost(
    useTabletLayout: Boolean
): Boolean {
    return !useTabletLayout
}

internal fun resolveVideoDetailCommentThreadHostMainSheetVisible(
    useEmbeddedPresentation: Boolean,
    subReplyVisible: Boolean
): Boolean {
    return useEmbeddedPresentation && subReplyVisible
}

internal fun shouldForceInitializeDetachedCommentThreadHostForRoute(
    routeCommentRootRpid: Long,
    aid: Long,
    hasHandledRouteComment: Boolean
): Boolean {
    return routeCommentRootRpid > 0L && aid > 0L && !hasHandledRouteComment
}

internal fun shouldApplyPhoneAutoRotatePolicy(
    isCompactDevice: Boolean
): Boolean {
    return isCompactDevice
}

internal fun resolveVideoDetailFullscreenMode(
    isOrientationDrivenFullscreen: Boolean,
    isLandscape: Boolean,
    userRequestedFullscreen: Boolean,
    isInMultiWindowMode: Boolean
): Boolean {
    if (!isOrientationDrivenFullscreen) return userRequestedFullscreen
    return isLandscape || (isInMultiWindowMode && userRequestedFullscreen)
}

internal fun shouldApplyStartFullscreenOrientationRequest(
    startInFullscreen: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    isLandscape: Boolean,
    isInMultiWindowMode: Boolean
): Boolean {
    if (!startInFullscreen) return false
    if (!isOrientationDrivenFullscreen) return false
    if (isLandscape) return false
    // 系统小窗/分屏内强写 requestedOrientation 会让部分 ROM 在横竖窗口间反复重建。
    if (isInMultiWindowMode) return false
    return true
}

internal fun resolvePhoneFullscreenEnterOrientation(
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    isVerticalVideo: Boolean
): Int? {
    return when (fullscreenMode) {
        com.android.purebilibili.core.store.FullscreenMode.NONE -> null
        com.android.purebilibili.core.store.FullscreenMode.VERTICAL -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        com.android.purebilibili.core.store.FullscreenMode.HORIZONTAL -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        com.android.purebilibili.core.store.FullscreenMode.AUTO -> {
            if (isVerticalVideo) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
}

internal fun shouldKeepManualFullscreenRequest(
    manualFullscreenRequested: Boolean,
    hasEnteredFullscreenDuringRequest: Boolean,
    isFullscreenMode: Boolean
): Boolean {
    if (!manualFullscreenRequested) return false
    if (isFullscreenMode) return true
    return !hasEnteredFullscreenDuringRequest
}

@Composable
internal fun ManualFullscreenRequestLifecycleEffect(
    manualFullscreenRequested: Boolean,
    isFullscreenMode: Boolean,
    onReleaseManualFullscreenRequest: () -> Unit
) {
    var hasEnteredFullscreenDuringRequest by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(manualFullscreenRequested, isFullscreenMode) {
        if (manualFullscreenRequested && isFullscreenMode) {
            hasEnteredFullscreenDuringRequest = true
            return@LaunchedEffect
        }

        if (
            !shouldKeepManualFullscreenRequest(
                manualFullscreenRequested = manualFullscreenRequested,
                hasEnteredFullscreenDuringRequest = hasEnteredFullscreenDuringRequest,
                isFullscreenMode = isFullscreenMode
            )
        ) {
            if (manualFullscreenRequested) {
                onReleaseManualFullscreenRequest()
            }
            hasEnteredFullscreenDuringRequest = false
        }
    }
}

internal fun resolvePhoneVideoRequestedOrientation(
    autoRotateEnabled: Boolean,
    systemAutoRotateEnabled: Boolean = true,
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    isCompactDevice: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    isFullscreenMode: Boolean,
    manualFullscreenRequested: Boolean = false,
    manualPortraitHoldActive: Boolean = false,
    isVerticalVideo: Boolean = false,
    currentRequestedOrientation: Int? = null,
    isInMultiWindowMode: Boolean = false
): Int? {
    if (isInMultiWindowMode) {
        return null
    }
    if (!shouldApplyPhoneAutoRotatePolicy(isCompactDevice)) {
        return if (isFullscreenMode || manualFullscreenRequested) {
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = fullscreenMode,
                isVerticalVideo = isVerticalVideo
            )
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.NONE) {
        return null
    }
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL) {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    if (!isOrientationDrivenFullscreen) {
        return null
    }
    if (manualPortraitHoldActive) {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    if (resolveEffectivePhoneAutoRotateEnabled(
            autoRotateEnabled = autoRotateEnabled,
            systemAutoRotateEnabled = systemAutoRotateEnabled,
            manualPortraitHoldActive = manualPortraitHoldActive
        )
    ) {
        return when {
            manualFullscreenRequested -> {
                resolvePhoneFullscreenEnterOrientation(
                    fullscreenMode = fullscreenMode,
                    isVerticalVideo = isVerticalVideo
                ) ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            isFullscreenMode -> resolveCurrentExactLandscapeOrientation(currentRequestedOrientation)
                ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    if (autoRotateEnabled && !systemAutoRotateEnabled && !manualFullscreenRequested) {
        return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    return if (isFullscreenMode) {
        resolvePhoneFullscreenEnterOrientation(
            fullscreenMode = fullscreenMode,
            isVerticalVideo = isVerticalVideo
        ) ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

internal fun resolvePhoneAutoRotateRequestedOrientation(
    orientationDegrees: Int,
    isCurrentlyLandscape: Boolean,
    portraitSnapDegrees: Int = 25,
    landscapeEnterMinDegrees: Int = 60,
    landscapeEnterMaxDegrees: Int = 120,
    landscapeKeepMinDegrees: Int = 40,
    landscapeKeepMaxDegrees: Int = 140
): Int? {
    if (orientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) return null
    val normalized = ((orientationDegrees % 360) + 360) % 360

    val portraitStable = isPhoneOrientationPortraitStable(
        orientationDegrees = normalized,
        portraitSnapDegrees = portraitSnapDegrees
    )
    val exactLandscapeEntry = resolveExactLandscapeOrientation(
        orientationDegrees = normalized,
        minLeftSideTopDegrees = landscapeEnterMinDegrees,
        maxLeftSideTopDegrees = landscapeEnterMaxDegrees,
        minRightSideTopDegrees = 240,
        maxRightSideTopDegrees = 300
    )
    val exactLandscapeKeep = resolveExactLandscapeOrientation(
        orientationDegrees = normalized,
        minLeftSideTopDegrees = landscapeKeepMinDegrees,
        maxLeftSideTopDegrees = landscapeKeepMaxDegrees,
        minRightSideTopDegrees = 220,
        maxRightSideTopDegrees = 320
    )

    return when {
        isCurrentlyLandscape && exactLandscapeKeep != null -> exactLandscapeKeep
        portraitStable -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        !isCurrentlyLandscape && exactLandscapeEntry != null -> exactLandscapeEntry
        else -> null
    }
}

internal const val PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS = 500L

internal fun resolvePhoneAutoRotateTargetToApply(
    candidateOrientation: Int?,
    lastLandscapeAppliedAtMs: Long?,
    nowMs: Long,
    landscapeSettleMs: Long = PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS
): Int? {
    if (candidateOrientation == null) return null
    // 系统配置切到横屏有延迟；按最近一次横屏写入时间保护，避免刚进横屏又被残留竖屏角度拉回。
    if (
        candidateOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT &&
        lastLandscapeAppliedAtMs != null &&
        nowMs - lastLandscapeAppliedAtMs < landscapeSettleMs
    ) {
        return null
    }
    return candidateOrientation
}

private fun resolveCurrentExactLandscapeOrientation(currentRequestedOrientation: Int?): Int? {
    return when (currentRequestedOrientation) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE -> currentRequestedOrientation
        else -> null
    }
}

private fun resolveExactLandscapeOrientation(
    orientationDegrees: Int,
    minLeftSideTopDegrees: Int,
    maxLeftSideTopDegrees: Int,
    minRightSideTopDegrees: Int,
    maxRightSideTopDegrees: Int
): Int? {
    return when {
        withinWrappedRange(
            orientationDegrees,
            minLeftSideTopDegrees,
            maxLeftSideTopDegrees
        ) -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        withinWrappedRange(
            orientationDegrees,
            minRightSideTopDegrees,
            maxRightSideTopDegrees
        ) -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else -> null
    }
}

internal fun isLandscapeRequestedOrientation(requestedOrientation: Int): Boolean {
    return requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
}

/**
 * 详情页/全屏路径会写入的横屏锁。连切相关视频时若把这类值当成「进入前方向」快照，
 * 退出会再次写回横屏，导致首页也横屏。
 */
internal fun isVideoDrivenLandscapeOrientationLock(requestedOrientation: Int): Boolean {
    return requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
}

/**
 * 进入详情时记录「离开后应恢复」的方向。已是视频横屏锁时改记 UNSPECIFIED，
 * 避免旧详情 dispose 延迟恢复前、新详情把横屏锁当成入口快照。
 */
internal fun resolveVideoDetailEntryOrientationSnapshot(
    currentRequestedOrientation: Int?
): Int {
    val current = currentRequestedOrientation
        ?: return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    return if (isVideoDrivenLandscapeOrientationLock(current)) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        current
    }
}

internal fun shouldEnterPortraitFullscreenOnFullscreenToggle(
    targetOrientation: Int,
    portraitExperienceEnabled: Boolean
): Boolean {
    return portraitExperienceEnabled && targetOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

internal fun shouldUseInWindowFullscreenForSystemMultiWindow(
    isInMultiWindowMode: Boolean,
    isInPictureInPictureMode: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    isFullscreenMode: Boolean
): Boolean {
    if (!isOrientationDrivenFullscreen) return false
    if (isFullscreenMode) return false
    if (isInPictureInPictureMode) return false
    return isInMultiWindowMode
}

internal fun resolvePortraitRotateTargetOrientation(
    isOrientationDrivenFullscreen: Boolean,
    manualPortraitHoldActive: Boolean = false
): Int? {
    return if (isOrientationDrivenFullscreen && !manualPortraitHoldActive) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
        null
    }
}

internal fun resolveEffectivePhoneAutoRotateEnabled(
    autoRotateEnabled: Boolean,
    systemAutoRotateEnabled: Boolean,
    manualPortraitHoldActive: Boolean
): Boolean {
    return autoRotateEnabled && systemAutoRotateEnabled && !manualPortraitHoldActive
}

internal fun shouldObservePhoneAutoRotate(
    autoRotateEnabled: Boolean,
    systemAutoRotateEnabled: Boolean,
    isCompactDevice: Boolean,
    isOrientationDrivenFullscreen: Boolean,
    fullscreenMode: com.android.purebilibili.core.store.FullscreenMode,
    manualPortraitHoldActive: Boolean,
    isInMultiWindowMode: Boolean = false
): Boolean {
    if (!autoRotateEnabled) return false
    if (!systemAutoRotateEnabled) return false
    if (isInMultiWindowMode) return false
    if (!shouldApplyPhoneAutoRotatePolicy(isCompactDevice)) return false
    if (!isOrientationDrivenFullscreen) return false
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.NONE) return false
    if (fullscreenMode == com.android.purebilibili.core.store.FullscreenMode.VERTICAL) return false
    return true
}

internal fun shouldReleasePhoneManualPortraitHold(
    orientationDegrees: Int,
    portraitSnapDegrees: Int = 25
): Boolean {
    if (orientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) return false
    return isPhoneOrientationPortraitStable(
        orientationDegrees = orientationDegrees,
        portraitSnapDegrees = portraitSnapDegrees
    )
}

private fun isPhoneOrientationPortraitStable(
    orientationDegrees: Int,
    portraitSnapDegrees: Int
): Boolean {
    val normalized = ((orientationDegrees % 360) + 360) % 360
    return withinWrappedRange(normalized, 0, portraitSnapDegrees) ||
        withinWrappedRange(normalized, 180 - portraitSnapDegrees, 180 + portraitSnapDegrees) ||
        withinWrappedRange(normalized, 360 - portraitSnapDegrees, 359)
}

private fun withinWrappedRange(
    value: Int,
    min: Int,
    max: Int
): Boolean {
    return if (min <= max) {
        value in min..max
    } else {
        value >= min || value <= max
    }
}

@Composable
internal fun rememberSystemAutoRotateEnabled(context: Context): State<Boolean> {
    return produceState(initialValue = readSystemAutoRotateEnabled(context), context) {
        val contentResolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                value = readSystemAutoRotateEnabled(context)
            }
        }
        value = readSystemAutoRotateEnabled(context)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            observer
        )
        awaitDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }
}

private fun readSystemAutoRotateEnabled(context: Context): Boolean {
    return runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION
        ) == 1
    }.getOrDefault(true)
}


internal fun resolveVideoDetailExitRequestedOrientation(
    originalRequestedOrientation: Int?
): Int {
    val original = originalRequestedOrientation
        ?: return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    return if (isVideoDrivenLandscapeOrientationLock(original)) {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    } else {
        original
    }
}

internal fun shouldEnablePortraitExperience(): Boolean {
    return true
}

internal fun shouldShowVideoDetailBottomInteractionBar(
    useTabletLayout: Boolean,
    selectedTabIndex: Int,
    isFullscreenMode: Boolean,
    isPortraitFullscreen: Boolean,
    isCommentInputVisible: Boolean,
    isCommentThreadVisible: Boolean,
    isFavoriteFolderDialogVisible: Boolean,
    isExternalPlaylistQueueBarVisible: Boolean
): Boolean {
    return !useTabletLayout &&
        selectedTabIndex == VIDEO_CONTENT_COMMENT_TAB_INDEX &&
        !isFullscreenMode &&
        !isPortraitFullscreen &&
        !isCommentInputVisible &&
        !isCommentThreadVisible &&
        !isFavoriteFolderDialogVisible &&
        !isExternalPlaylistQueueBarVisible
}

internal fun shouldShowVideoDetailActionButtons(): Boolean {
    return true
}

internal data class RefreshModeCandidate(
    val modeId: Int,
    val refreshRate: Float,
    val width: Int,
    val height: Int
)

internal fun resolvePreferredHighRefreshModeId(
    currentModeId: Int,
    supportedModes: List<RefreshModeCandidate>,
    minRefreshRate: Float = 90f
): Int? {
    if (supportedModes.isEmpty()) return null
    val currentMode = supportedModes.firstOrNull { it.modeId == currentModeId } ?: return null
    val candidates = supportedModes.filter {
        it.refreshRate >= minRefreshRate &&
            it.width == currentMode.width &&
            it.height == currentMode.height
    }
    if (candidates.isEmpty()) return null

    return candidates.maxWithOrNull(
        compareBy<RefreshModeCandidate> { it.refreshRate }
            .thenBy { if (it.modeId == currentModeId) 1 else 0 }
    )?.modeId
}

// VideoContentSection 已提取到 VideoContentSection.kt
// VideoTagsRow 和 VideoTagChip 也已提取到 VideoContentSection.kt
