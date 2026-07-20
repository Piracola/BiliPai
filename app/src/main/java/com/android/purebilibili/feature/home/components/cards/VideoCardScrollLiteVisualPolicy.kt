package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.normalizeSharedElementSourceRoute

internal data class VideoCardScrollLiteVisualPolicy(
    val coverShadowElevationDp: Float,
    val showCoverGradientMask: Boolean,
    val showHistoryProgressBar: Boolean,
    val showCompactStatsOnCover: Boolean,
    val showSecondaryStatsRow: Boolean
)

internal fun resolveVideoCardScrollLiteVisualPolicy(
    scrollLiteModeEnabled: Boolean,
    compactStatsOnCover: Boolean
): VideoCardScrollLiteVisualPolicy {
    if (scrollLiteModeEnabled) {
        return VideoCardScrollLiteVisualPolicy(
            coverShadowElevationDp = 0f,
            showCoverGradientMask = false,
            showHistoryProgressBar = false,
            showCompactStatsOnCover = compactStatsOnCover,
            showSecondaryStatsRow = !compactStatsOnCover
        )
    }

    return VideoCardScrollLiteVisualPolicy(
        coverShadowElevationDp = 0f,
        // 统计信息移到封面外时也不需要暗渐变；保持静止和滚动状态一致，避免整批封面明暗闪烁。
        showCoverGradientMask = false,
        showHistoryProgressBar = true,
        showCompactStatsOnCover = compactStatsOnCover,
        showSecondaryStatsRow = !compactStatsOnCover
    )
}

/**
 * 列表封面是否允许 Coil crossfade。
 *
 * 关键：快速/普通返回结束后 [isReturningFromDetail] 会被 clear，若此时把 crossfade
 * 从 false 拨回 true，ImageRequest 重建会触发一次假加载淡入 → **落位后再闪一下**。
 * 因此只要仍是 shared 返回目标卡（lastClicked），就持续关闭 crossfade，直到用户点了别的卡。
 */
internal fun shouldEnableVideoCardCoverCrossfade(
    isScrollInProgress: Boolean,
    isReturningFromDetail: Boolean,
    useCoverSharedBounds: Boolean,
    isSharedReturnTarget: Boolean
): Boolean {
    if (isScrollInProgress) return false
    // 返回目标：全程禁用 Coil 淡入（含 clearReturning 之后）。
    if (useCoverSharedBounds && isSharedReturnTarget) return false
    // 返回会话中非 shell 路径的兜底
    if (isReturningFromDetail && isSharedReturnTarget) return false
    return true
}

/**
 * shared 返回目标卡是否应钉住封面 URL/缓存键。
 *
 * 进场/快速返回期间若省流开关、插件或数据刷新改了 pic/质量，ImageRequest 重建会在
 * overlay 卸层后换源解码 → 落位闪。目标卡在 lastClicked 生命周期内保持点击时的源。
 */
internal fun shouldPinVideoCardCoverForSharedReturn(
    isSharedReturnTarget: Boolean,
): Boolean = isSharedReturnTarget

/**
 * 首页卡片 → 详情页 CARD_SHELL morph 期间，源卡片封面是否让位给 overlay。
 *
 * 始终不藏：列表封面一直在 shared overlay 下方待命。
 * 快速返回会打断 OPENING，若此时仍 alpha=0，overlay 撤掉的一帧就会露出
 * surfaceVariant 占位色 → 落位闪烁。进场时 overlay 本身盖住列表封面，
 * 保留可见封面的代价可接受。
 */
@Suppress("UNUSED_PARAMETER")
internal fun shouldHideHomeCardCoverDuringShellMorph(
    useCardContainerSharedBounds: Boolean,
    isSharedMorphSourceCard: Boolean,
    isReturningFromDetail: Boolean,
    transitionBackgroundPhase: VideoCardTransitionBackgroundPhase,
    isVideoCardReturnGestureInProgress: Boolean,
): Boolean {
    return false
}

/**
 * 返回落位进度达到该比例后开始淡入标题等 chrome。
 * 略高于源卡 Enter 延后比（0.55），让封面先露一拍、字不盖实时画面。
 */
internal const val HOME_CARD_CHROME_EARLY_REVEAL_SETTLE_START = 0.62f

/**
 * 源卡 chrome 在返回落位进度上的淡入曲线。
 * [settleProgress] 0=刚开始缩回，1=完全落位；[revealStart] 之前保持 0。
 */
internal fun resolveHomeCardChromeEarlyRevealAlpha(
    settleProgress: Float,
    revealStart: Float = HOME_CARD_CHROME_EARLY_REVEAL_SETTLE_START,
): Float {
    val clampedSettle = settleProgress.coerceIn(0f, 1f)
    val start = revealStart.coerceIn(0f, 1f)
    if (clampedSettle <= start) return 0f
    if (start >= 1f) return if (clampedSettle >= 1f) 1f else 0f
    return ((clampedSettle - start) / (1f - start)).coerceIn(0f, 1f)
}

/**
 * 返回 shell morph 期间源卡 **chrome**（标题/UP/信息区）的 alpha。
 *
 * - morph 未进行：始终 1，避免相关推荐等落位后标题空白
 * - 进场飞向详情：保持 0，避免字叠在播放器上
 * - 返回落位：按 settle 进度在末段淡入，与封面同步出现，且中段不盖住实时画面
 * - 快速返回：直接 1
 */
internal fun resolveHomeCardChromeAlphaDuringShellReturnMorph(
    useCardContainerSharedBounds: Boolean,
    isSharedMorphSourceCard: Boolean,
    isReturningFromDetail: Boolean,
    transitionBackgroundPhase: VideoCardTransitionBackgroundPhase =
        VideoCardTransitionBackgroundPhase.IDLE,
    isVideoCardReturnGestureInProgress: Boolean = false,
    isSharedTransitionActive: Boolean = false,
    transitionBackgroundProgress: Float = 0f,
    isQuickReturnFromDetail: Boolean = false,
): Float {
    if (!useCardContainerSharedBounds || !isSharedMorphSourceCard) return 1f
    if (isQuickReturnFromDetail) return 1f

    val morphActive = isSharedTransitionActive || isVideoCardReturnGestureInProgress
    // morph 已结束：立刻恢复（哪怕景深 phase 仍短暂停留在 RETURNING）
    if (!morphActive) return 1f

    val isReturnMorph = isReturningFromDetail ||
        isVideoCardReturnGestureInProgress ||
        transitionBackgroundPhase == VideoCardTransitionBackgroundPhase.RETURNING
    if (!isReturnMorph) {
        // 进场：藏字，避免标题飞过实时播放器
        return 0f
    }

    // 返回：景深 progress 1→0；settle 0→1。末段再露字，与延后淡入的封面对齐。
    val settleProgress = 1f - transitionBackgroundProgress.coerceIn(0f, 1f)
    return resolveHomeCardChromeEarlyRevealAlpha(settleProgress = settleProgress)
}

/**
 * 兼容旧布尔语义：chrome 尚未完全露出版视为仍在抑制。
 */
internal fun shouldSuppressHomeCardVisualDuringShellReturnMorph(
    useCardContainerSharedBounds: Boolean,
    isSharedMorphSourceCard: Boolean,
    isReturningFromDetail: Boolean,
    transitionBackgroundPhase: VideoCardTransitionBackgroundPhase =
        VideoCardTransitionBackgroundPhase.IDLE,
    isVideoCardReturnGestureInProgress: Boolean = false,
    isSharedTransitionActive: Boolean = false,
    transitionBackgroundProgress: Float = 0f,
    isQuickReturnFromDetail: Boolean = false,
): Boolean {
    return resolveHomeCardChromeAlphaDuringShellReturnMorph(
        useCardContainerSharedBounds = useCardContainerSharedBounds,
        isSharedMorphSourceCard = isSharedMorphSourceCard,
        isReturningFromDetail = isReturningFromDetail,
        transitionBackgroundPhase = transitionBackgroundPhase,
        isVideoCardReturnGestureInProgress = isVideoCardReturnGestureInProgress,
        isSharedTransitionActive = isSharedTransitionActive,
        transitionBackgroundProgress = transitionBackgroundProgress,
        isQuickReturnFromDetail = isQuickReturnFromDetail,
    ) < 1f
}

internal fun normalizeVideoCardSourceRouteForSharedKey(sourceRoute: String?): String? {
    return normalizeSharedElementSourceRoute(sourceRoute)
}

internal fun resolveVideoCardSharedReturnTargetKey(
    bvid: String,
    sourceRoute: String?,
): String? {
    val normalizedBvid = bvid.trim()
    val normalizedRoute = normalizeVideoCardSourceRouteForSharedKey(sourceRoute) ?: return null
    if (normalizedBvid.isEmpty()) return null
    return "$normalizedRoute:$normalizedBvid"
}

internal fun isVideoCardSharedReturnTarget(
    bvid: String,
    sourceRoute: String?,
    lastClickedVideoSourceKey: String?,
): Boolean {
    val key = resolveVideoCardSharedReturnTargetKey(bvid, sourceRoute) ?: return false
    return key == lastClickedVideoSourceKey
}

internal data class StoryVideoCardScrollLiteVisualPolicy(
    val coverShadowElevationDp: Float,
    val showSecondaryStatsRow: Boolean
)

internal fun resolveStoryVideoCardScrollLiteVisualPolicy(
    scrollLiteModeEnabled: Boolean
): StoryVideoCardScrollLiteVisualPolicy {
    return if (scrollLiteModeEnabled) {
        StoryVideoCardScrollLiteVisualPolicy(
            coverShadowElevationDp = 0f,
            showSecondaryStatsRow = true
        )
    } else {
        StoryVideoCardScrollLiteVisualPolicy(
            coverShadowElevationDp = 0f,
            showSecondaryStatsRow = true
        )
    }
}
