package com.android.purebilibili.core.ui.transition

/**
 * 详情 → 来源卡片返回的**单一时间轴契约**（纯 Kotlin，无 Compose）。
 *
 * 目标：
 * - 导航 seek / sharedBounds morph / 景深 blur / 封面 ownership / 源卡 chrome 共用同一套数字
 * - feature 接线层只读这里的结果，不再各自发明时长或 ownership 分支
 *
 * 三条封面路径：
 * - [VideoCardReturnCoverOwnership.LIVE_SURFACE]：实时画面跟壳缩（一镜到底）
 * - [VideoCardReturnCoverOwnership.RESIDENT_COVER]：常驻封面接管
 * - [VideoCardReturnCoverOwnership.FALLBACK_NO_SHARED]：无 shared 配对，依赖路由层动画
 */

/** 源卡 chrome（标题/UP）在返回 settle 进度上的淡入起点。 */
internal const val VIDEO_CARD_RETURN_CHROME_REVEAL_START = 0.62f

/**
 * 详情侧返回时封面视觉主导权。
 *
 * morph 中途禁止在 LIVE ↔ RESIDENT 之间切换，否则会出现「先切封面再缩小」或黑闪。
 */
internal enum class VideoCardReturnCoverOwnership {
    /** ImmediatePlayback + 正文就绪：player 可见，cover alpha=0 */
    LIVE_SURFACE,

    /** Loading / CoverFirst / 无可靠 surface：常驻封面主导 */
    RESIDENT_COVER,

    /** 无 shell/shared 配对：不得假设 morph 存在 */
    FALLBACK_NO_SHARED,
}

/**
 * 列表源卡在返回落位期间的封面契约。
 * 保证：始终有一层像素在卡片位待命，且 URL 不在卸层瞬间重建。
 */
internal data class VideoCardReturnListCoverContract(
    val pinCoverSource: Boolean,
    val enableCoilCrossfade: Boolean,
    /** 始终 false：列表封面在 overlay 下待命，避免卸层露 surfaceVariant */
    val hideCoverDuringShellMorph: Boolean,
)

/**
 * 一次返回会话的时间轴参数（与设置里的共享过渡时长对齐）。
 */
internal data class VideoCardReturnTimeline(
    val morphDurationMillis: Int,
    val settleBufferMillis: Long,
    val chromeRevealStart: Float,
    val sourceEnterFadeDelayRatio: Float,
) {
    val suppressionWindowMillis: Long
        get() = morphDurationMillis.coerceAtLeast(0).toLong() + settleBufferMillis
}

/**
 * 返回会话相位（逻辑相位，可与景深 [VideoCardTransitionBackgroundPhase] 对照）。
 */
internal enum class VideoCardReturnSessionPhase {
    Idle,
    Opening,
    Held,
    PredictiveSeek,
    ReturningMorph,
    CancelRestore,
}

internal fun resolveVideoCardReturnTimeline(
    morphDurationMillis: Int,
): VideoCardReturnTimeline {
    return VideoCardReturnTimeline(
        morphDurationMillis = morphDurationMillis.coerceAtLeast(0),
        settleBufferMillis = resolveVideoCardReturnSpringSettleBufferMs(),
        chromeRevealStart = VIDEO_CARD_RETURN_CHROME_REVEAL_START,
        sourceEnterFadeDelayRatio = VIDEO_CARD_SHELL_SOURCE_ENTER_FADE_DELAY_RATIO,
    )
}

/**
 * Navigation3 [SeekableTransitionState] 预测返回完成后半段时长。
 *
 * 与 NavDisplay 内公式一致：
 * `remaining = ((1 - fraction) * totalDuration).toInt()`
 *
 * 因此返回 bounds **必须**是固定时长 tween（Linear），不能是 spring，
 * 否则 totalDuration 不可靠 → 松手一闪 / 无落位动画。
 *
 * @param seekFraction 当前 seek 进度，0=起点（详情全屏），1=已落位
 * @param fullDurationMs 与进场/返回 morph 主时长一致
 */
internal fun resolveVideoCardSharedMorphRemainingDurationMs(
    seekFraction: Float,
    fullDurationMs: Int,
): Int {
    val fraction = seekFraction.coerceIn(0f, 1f)
    val full = fullDurationMs.coerceAtLeast(0)
    return ((1f - fraction) * full).toInt().coerceAtLeast(0)
}

/**
 * 景深 blur 在提交返回时的剩余动画时长。
 * [blurProgressAtCommit] 为当前虚化强度（1=满糊，0=已清），按比例缩短，与 morph 同速感。
 */
internal fun resolveVideoCardReturnDepthBlurRemainingDurationMs(
    blurProgressAtCommit: Float,
    fullDurationMs: Int,
    minDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
): Int {
    return resolveVideoCardTransitionBackgroundReturnDurationMs(
        startProgress = blurProgressAtCommit,
        fullDurationMs = fullDurationMs,
        minDurationMs = minDurationMs,
    )
}

/**
 * 是否允许 live morph（实时 surface 跟壳缩）。
 *
 * 详情正文未就绪时关闭，避免 Loading 骨架被缩进卡片位。
 */
internal fun shouldUseVideoCardLiveReturnMorph(
    transitionEnabled: Boolean,
    sharedBoundsActive: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent,
    detailContentReady: Boolean,
): Boolean {
    return transitionEnabled &&
        sharedBoundsActive &&
        !keepLoadedContentForBackPreview &&
        playbackIntent == VideoSharedTransitionPlaybackIntent.ImmediatePlayback &&
        detailContentReady
}

/**
 * 解析详情返回时的封面路径类型（与「当前是否正在离开」无关）。
 *
 * - LIVE_SURFACE：满足 live morph 门闩 → 离开时 player 主导
 * - RESIDENT_COVER：有 shared，但不走 live（Loading/CoverFirst 等）→ 离开时封面主导
 * - FALLBACK_NO_SHARED：无配对 → 不得假设 shell morph
 *
 * 「现在是否把视觉交给封面」还要乘 [useReturningVisualState]，见
 * [shouldHandVisualOwnershipToResidentCoverForOwnership]。
 */
@Suppress("UNUSED_PARAMETER") // hasResidentCover：handoff 门闩在 shouldHand*，路径类型不依赖是否已有 URL
internal fun resolveVideoCardReturnCoverOwnership(
    transitionEnabled: Boolean,
    sharedBoundsActive: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    playbackIntent: VideoSharedTransitionPlaybackIntent,
    detailContentReady: Boolean,
    hasResidentCover: Boolean,
): VideoCardReturnCoverOwnership {
    if (!transitionEnabled || !sharedBoundsActive) {
        return VideoCardReturnCoverOwnership.FALLBACK_NO_SHARED
    }
    val live = shouldUseVideoCardLiveReturnMorph(
        transitionEnabled = transitionEnabled,
        sharedBoundsActive = sharedBoundsActive,
        keepLoadedContentForBackPreview = keepLoadedContentForBackPreview,
        playbackIntent = playbackIntent,
        detailContentReady = detailContentReady,
    )
    if (live) {
        return VideoCardReturnCoverOwnership.LIVE_SURFACE
    }
    // 非 live：路径 B。无封面时 hand 仍为 false（hasResidentCover 门闩），player 保持可见防黑底。
    return VideoCardReturnCoverOwnership.RESIDENT_COVER
}

/**
 * 离开态是否把视觉主导权交给常驻封面。
 * live 路径永远 false；其余路径在 [useReturningVisualState] 且有封面时 true。
 */
internal fun shouldHandVisualOwnershipToResidentCoverForOwnership(
    ownership: VideoCardReturnCoverOwnership,
    useReturningVisualState: Boolean,
    hasResidentCover: Boolean,
): Boolean {
    if (!useReturningVisualState || !hasResidentCover) return false
    return when (ownership) {
        VideoCardReturnCoverOwnership.LIVE_SURFACE -> false
        VideoCardReturnCoverOwnership.RESIDENT_COVER,
        VideoCardReturnCoverOwnership.FALLBACK_NO_SHARED -> true
    }
}

internal fun isVideoCardLiveReturnMorphOwnership(
    ownership: VideoCardReturnCoverOwnership,
): Boolean = ownership == VideoCardReturnCoverOwnership.LIVE_SURFACE

/**
 * 列表源卡封面契约：pin 源、关 crossfade、不藏封面。
 */
internal fun resolveVideoCardReturnListCoverContract(
    isSharedReturnTarget: Boolean,
    isScrollInProgress: Boolean,
    isReturningFromDetail: Boolean,
    useCoverSharedBounds: Boolean,
): VideoCardReturnListCoverContract {
    val pin = isSharedReturnTarget
    val crossfade = when {
        isScrollInProgress -> false
        useCoverSharedBounds && isSharedReturnTarget -> false
        isReturningFromDetail && isSharedReturnTarget -> false
        else -> true
    }
    return VideoCardReturnListCoverContract(
        pinCoverSource = pin,
        enableCoilCrossfade = crossfade,
        hideCoverDuringShellMorph = false,
    )
}

/**
 * 把景深 phase + 手势标志映射为逻辑返回相位（便于单测与日志）。
 */
internal fun resolveVideoCardReturnSessionPhase(
    backgroundPhase: VideoCardTransitionBackgroundPhase,
    isReturnGestureInProgress: Boolean,
    isGestureRestoreInProgress: Boolean,
): VideoCardReturnSessionPhase {
    if (isGestureRestoreInProgress) {
        return VideoCardReturnSessionPhase.CancelRestore
    }
    if (isReturnGestureInProgress) {
        return VideoCardReturnSessionPhase.PredictiveSeek
    }
    return when (backgroundPhase) {
        VideoCardTransitionBackgroundPhase.IDLE -> VideoCardReturnSessionPhase.Idle
        VideoCardTransitionBackgroundPhase.OPENING -> VideoCardReturnSessionPhase.Opening
        VideoCardTransitionBackgroundPhase.HELD -> VideoCardReturnSessionPhase.Held
        VideoCardTransitionBackgroundPhase.RETURNING -> VideoCardReturnSessionPhase.ReturningMorph
    }
}

/**
 * 主路径返回 bounds 必须可 seek：固定时长 Linear tween。
 * 结构/策略测试用此开关锁定契约，防止再滑回 spring。
 */
internal fun shouldUseSeekableLinearReturnBoundsTransform(): Boolean = true
