package com.android.purebilibili.feature.video.screen

import androidx.compose.animation.core.Easing
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionPlaybackIntent
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionEnterEasing
import com.android.purebilibili.core.ui.transition.resolveVideoCardSharedTransitionReturnEasing

private const val COVER_TAKEOVER_PRE_BACK_DELAY_MILLIS = 0L
internal const val VIDEO_CONTENT_COMMENT_TAB_INDEX = 1

internal fun resolveForceCoverOnlyForReturn(
    forceCoverOnlyOnReturn: Boolean,
    transitionEnabled: Boolean = true,
    isCardReturnExitInProgress: Boolean = false
): Boolean {
    if (!transitionEnabled || isCardReturnExitInProgress) return false
    return forceCoverOnlyOnReturn
}

/**
 * 返回「离开态」（次要内容淡出、标记离开等）。
 * 不等于封面接管：ImmediatePlayback 的 live morph 路径下播放器应保持可见。
 *
 * 预测拖动未提交时 isSessionReturningToCard 应为 false（尚未 markReturning）。
 */
internal fun shouldUseReturningVideoDetailVisualState(
    forceCoverOnlyForReturn: Boolean,
    isCardReturnExitInProgress: Boolean = false,
    isSessionReturningToCard: Boolean = false,
): Boolean {
    return forceCoverOnlyForReturn ||
        isCardReturnExitInProgress ||
        isSessionReturningToCard
}

/**
 * 详情 → 来源卡片 sharedBounds：实时画面跟手缩小（一镜到底）。
 * CoverFirst / back-preview 保活父页时关闭。
 * 详情正文未就绪（快速返回常见 Loading/骨架）时也关闭，否则 skeleton 会被缩进卡片位。
 */
internal fun shouldUseLiveReturnMorph(
    transitionEnabled: Boolean,
    sharedBoundsActive: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent,
    detailContentReady: Boolean = true,
): Boolean {
    return transitionEnabled &&
        sharedBoundsActive &&
        !keepLoadedContentForBackPreview &&
        playbackIntent == VideoSharedTransitionPlaybackIntent.ImmediatePlayback &&
        detailContentReady
}

/**
 * 详情页下方推荐/简介等是否已可安全参与 live morph。
 * Loading / 错误态仍可能画骨架或空壳，快速返回时不能当「实时组件」。
 */
internal fun shouldTreatVideoDetailContentReadyForLiveReturnMorph(
    hasSuccessfulDetailContent: Boolean,
): Boolean = hasSuccessfulDetailContent

/**
 * 是否把视觉主导权交给常驻封面（forceCover / 藏 surface）。
 * live morph 时必须为 false，否则会出现「先切封面再缩小」。
 */
internal fun shouldHandVisualOwnershipToResidentCover(
    useReturningVisualState: Boolean,
    hasResidentCover: Boolean,
    liveReturnMorph: Boolean,
): Boolean {
    return useReturningVisualState && hasResidentCover && !liveReturnMorph
}

internal fun resolveVideoDetailReturnCoverAlpha(
    transitionProgress: Float,
    isCommittedCardReturn: Boolean,
    hasResidentCover: Boolean,
    liveReturnMorph: Boolean = false,
): Float {
    if (liveReturnMorph || !hasResidentCover) return 0f
    val progress = transitionProgress.coerceIn(0f, 1f)
    return if (isCommittedCardReturn) 1f else 1f - progress
}

internal fun resolveVideoDetailReturnPlayerAlpha(
    transitionProgress: Float,
    isCommittedCardReturn: Boolean,
    hasResidentCover: Boolean,
    liveReturnMorph: Boolean = false,
): Float {
    if (liveReturnMorph) return 1f
    if (isCommittedCardReturn) return if (hasResidentCover) 0f else 1f
    return transitionProgress.coerceIn(0f, 1f)
}

internal fun resolveVideoDetailReturnContentAlpha(
    transitionProgress: Float,
    isCommittedCardReturn: Boolean,
    holdFullyOpaqueAfterBackPreview: Boolean = false,
    liveReturnMorph: Boolean = false,
): Float {
    // live morph：正文保持不透明，整页参与 shell 收缩；收束靠 bounds 形变而非先淡成封面。
    if (liveReturnMorph) return 1f
    if (isCommittedCardReturn) return 0f
    if (holdFullyOpaqueAfterBackPreview) return 1f
    return transitionProgress.coerceIn(0f, 1f)
}

internal fun shouldTreatVideoDetailCardExitAsReturning(
    isExitTransitionInProgress: Boolean,
    sharedBoundsActive: Boolean,
    keepLoadedContentForBackPreview: Boolean = false,
): Boolean {
    return isExitTransitionInProgress &&
        sharedBoundsActive &&
        !keepLoadedContentForBackPreview
}

internal fun shouldForceBackPreviewPlayerCover(
    keepLoadedContentForBackPreview: Boolean,
    bindLivePlayerForBackPreview: Boolean
): Boolean {
    return keepLoadedContentForBackPreview && !bindLivePlayerForBackPreview
}

/**
 * 相关推荐「详情压详情」返回：父页刚从 back-preview 恢复时，
 * 若立刻按进场过渡把内容 alpha 从 0 淡入，会整页闪一下（滚动位置仍在）。
 */
internal fun shouldSuppressVideoDetailEnterFadeAfterBackPreview(
    wasKeptAsBackPreview: Boolean,
    keepLoadedContentForBackPreview: Boolean,
): Boolean {
    return wasKeptAsBackPreview && !keepLoadedContentForBackPreview
}

internal fun shouldUseVideoDetailRootTransitionProgress(
    detailShellSharedBoundsEnabled: Boolean,
    hasAnimatedVisibilityScope: Boolean,
    keepLoadedContentForBackPreview: Boolean,
): Boolean {
    return detailShellSharedBoundsEnabled &&
        hasAnimatedVisibilityScope &&
        !keepLoadedContentForBackPreview
}

internal fun shouldShowVideoDetailContent(
    isTransitionFinished: Boolean,
    isLeaving: Boolean,
    rootTransitionOwnsContentAlpha: Boolean,
    keepContentVisibleAfterBackPreview: Boolean = false,
): Boolean {
    if (keepContentVisibleAfterBackPreview && !isLeaving) return true
    return isTransitionFinished && (!isLeaving || rootTransitionOwnsContentAlpha)
}

internal fun resolveCoverTakeoverDelayBeforeBackNavigationMillis(): Long {
    // 封面常驻并直接读取根过渡进度，不再需要先抢一帧切换封面再导航。
    return COVER_TAKEOVER_PRE_BACK_DELAY_MILLIS
}

internal data class VideoDetailRouteSheetMotion(
    val enabled: Boolean,
    val durationMillis: Int,
    val mainDurationMillis: Int,
    val settleDurationMillis: Int,
    val initialScale: Float,
    val initialTranslationYDp: Float,
    val initialCornerDp: Float,
    val initialBackgroundScrimAlpha: Float,
    val settleScaleDelta: Float,
    val settleTranslationDp: Float,
    val enterEasing: Easing,
    val returnEasing: Easing
)

internal enum class VideoDetailRouteSheetSettleDirection {
    None,
    Enter,
    Return
}

internal data class VideoDetailRouteSheetFrame(
    val scale: Float,
    val translationYDp: Float,
    val cornerDp: Float,
    val backgroundScrimAlpha: Float,
    val settleProgress: Float
)

internal data class VideoDetailSecondaryContentTiming(
    val enterDelayMillis: Int,
    val enterDurationMillis: Int,
    val returnDelayMillis: Int,
    val returnDurationMillis: Int
)

internal fun resolveVideoDetailSecondaryContentTiming(
    fullDurationMillis: Int,
    contentDelayMillis: Int,
    contentDurationMillis: Int,
): VideoDetailSecondaryContentTiming {
    val safeDuration = fullDurationMillis.coerceAtLeast(0)
    val safeEnterDelay = contentDelayMillis.coerceIn(0, safeDuration)
    val safeEnterDuration = contentDurationMillis
        .coerceAtLeast(0)
        .coerceAtMost(safeDuration - safeEnterDelay)
    val safeReturnDuration = contentDurationMillis.coerceIn(0, safeDuration)
    return VideoDetailSecondaryContentTiming(
        enterDelayMillis = safeEnterDelay,
        enterDurationMillis = safeEnterDuration,
        returnDelayMillis = 0,
        returnDurationMillis = safeReturnDuration
    )
}

internal data class VideoDetailMotionSpec(
    val entryPhaseDurationMillis: Int,
    val contentSwapFadeDurationMillis: Int,
    val contentRevealFadeDurationMillis: Int
)

private const val VIDEO_DETAIL_ENTRY_PHASE_MIN_DURATION_MILLIS = 120
private const val VIDEO_DETAIL_CONTENT_PHASE_MIN_DURATION_MILLIS = 180
private const val HOME_VIDEO_ROUTE_SHEET_MAIN_DURATION_MILLIS = 320
private const val HOME_VIDEO_ROUTE_SHEET_SETTLE_DURATION_MILLIS = 96
private const val HOME_VIDEO_ROUTE_SHEET_DURATION_MILLIS =
    HOME_VIDEO_ROUTE_SHEET_MAIN_DURATION_MILLIS + HOME_VIDEO_ROUTE_SHEET_SETTLE_DURATION_MILLIS
private const val HOME_VIDEO_ROUTE_SHEET_INITIAL_SCALE = 0.965f
private const val HOME_VIDEO_ROUTE_SHEET_INITIAL_TRANSLATION_Y_DP = 56f
private const val HOME_VIDEO_ROUTE_SHEET_INITIAL_CORNER_DP = 28f
private const val HOME_VIDEO_ROUTE_SHEET_INITIAL_SCRIM_ALPHA = 0.18f
private const val HOME_VIDEO_ROUTE_SHEET_SETTLE_SCALE_DELTA = 0.0015f
private const val HOME_VIDEO_ROUTE_SHEET_SETTLE_TRANSLATION_DP = 1.5f

internal fun resolveVideoDetailMotionSpec(
    transitionEnterDurationMillis: Int
): VideoDetailMotionSpec {
    return VideoDetailMotionSpec(
        entryPhaseDurationMillis = transitionEnterDurationMillis
            .coerceAtLeast(VIDEO_DETAIL_ENTRY_PHASE_MIN_DURATION_MILLIS),
        contentSwapFadeDurationMillis = transitionEnterDurationMillis
            .coerceAtLeast(VIDEO_DETAIL_CONTENT_PHASE_MIN_DURATION_MILLIS),
        contentRevealFadeDurationMillis = transitionEnterDurationMillis
            .coerceAtLeast(VIDEO_DETAIL_CONTENT_PHASE_MIN_DURATION_MILLIS)
    )
}

internal fun resolveVideoDetailRouteSheetMotion(
    sourceRoute: String?,
    transitionEnabled: Boolean
): VideoDetailRouteSheetMotion {
    val enabled = transitionEnabled &&
        com.android.purebilibili.navigation.isVideoCardReturnTargetRoute(sourceRoute)
    return VideoDetailRouteSheetMotion(
        enabled = enabled,
        durationMillis = HOME_VIDEO_ROUTE_SHEET_DURATION_MILLIS,
        mainDurationMillis = HOME_VIDEO_ROUTE_SHEET_MAIN_DURATION_MILLIS,
        settleDurationMillis = HOME_VIDEO_ROUTE_SHEET_SETTLE_DURATION_MILLIS,
        initialScale = HOME_VIDEO_ROUTE_SHEET_INITIAL_SCALE,
        initialTranslationYDp = HOME_VIDEO_ROUTE_SHEET_INITIAL_TRANSLATION_Y_DP,
        initialCornerDp = HOME_VIDEO_ROUTE_SHEET_INITIAL_CORNER_DP,
        initialBackgroundScrimAlpha = HOME_VIDEO_ROUTE_SHEET_INITIAL_SCRIM_ALPHA,
        settleScaleDelta = HOME_VIDEO_ROUTE_SHEET_SETTLE_SCALE_DELTA,
        settleTranslationDp = HOME_VIDEO_ROUTE_SHEET_SETTLE_TRANSLATION_DP,
        enterEasing = resolveVideoCardSharedTransitionEnterEasing(),
        returnEasing = resolveVideoCardSharedTransitionReturnEasing()
    )
}

internal fun resolveVideoDetailRouteSheetFrame(
    rawProgress: Float,
    settleProgress: Float = 0f,
    settleDirection: VideoDetailRouteSheetSettleDirection = VideoDetailRouteSheetSettleDirection.None,
    motion: VideoDetailRouteSheetMotion
): VideoDetailRouteSheetFrame {
    if (!motion.enabled) {
        return VideoDetailRouteSheetFrame(
            scale = 1f,
            translationYDp = 0f,
            cornerDp = 0f,
            backgroundScrimAlpha = 0f,
            settleProgress = 0f
        )
    }
    val progress = rawProgress.coerceIn(0f, 1f)
    val safeSettleProgress = settleProgress.coerceIn(0f, 1f)
    val settleScale = when (settleDirection) {
        VideoDetailRouteSheetSettleDirection.Enter -> motion.settleScaleDelta * safeSettleProgress
        VideoDetailRouteSheetSettleDirection.Return -> -motion.settleScaleDelta * safeSettleProgress
        VideoDetailRouteSheetSettleDirection.None -> 0f
    }
    val settleTranslation = when (settleDirection) {
        VideoDetailRouteSheetSettleDirection.Enter -> -motion.settleTranslationDp * safeSettleProgress
        VideoDetailRouteSheetSettleDirection.Return -> motion.settleTranslationDp * safeSettleProgress
        VideoDetailRouteSheetSettleDirection.None -> 0f
    }
    return VideoDetailRouteSheetFrame(
        scale = lerpVideoDetailFloat(motion.initialScale, 1f, progress) + settleScale,
        translationYDp = lerpVideoDetailFloat(motion.initialTranslationYDp, 0f, progress) + settleTranslation,
        cornerDp = lerpVideoDetailFloat(motion.initialCornerDp, 0f, progress),
        backgroundScrimAlpha = lerpVideoDetailFloat(motion.initialBackgroundScrimAlpha, 0f, progress),
        settleProgress = safeSettleProgress
    )
}

private fun lerpVideoDetailFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
