package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase

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
 * 历史 API：曾按落位进度淡入 chrome。现改为始终可见，避免回弹末段才出标题。
 */
internal const val HOME_CARD_CHROME_EARLY_REVEAL_SETTLE_START = 0f

/**
 * 历史 API：现恒为 1，保留供测试/调用方兼容。
 */
@Suppress("UNUSED_PARAMETER")
internal fun resolveHomeCardChromeEarlyRevealAlpha(
    settleProgress: Float,
    revealStart: Float = HOME_CARD_CHROME_EARLY_REVEAL_SETTLE_START,
): Float = 1f

/**
 * 返回 shell morph 期间首页源卡 **chrome**（标题/UP/信息区）的 alpha。
 *
 * 始终 1：标题与封面同步参与落位，不再等回弹末段淡入。
 * 飞行中由详情壳盖住源卡信息区，避免再靠压透明制造延迟。
 */
@Suppress("UNUSED_PARAMETER")
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
): Float = 1f

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
    val normalized = sourceRoute?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        normalized
    } else {
        normalized.substringBefore("?")
    }
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
