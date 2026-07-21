package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale

/**
 * shell sharedBounds 角色。
 *
 * - 进场（首页等大卡）：源卡 Exit.None、详情壳 Enter.None，整卡跟手放大。
 * - 进场（相关/分区横条卡）：源卡短淡出，避免横条卡内容叠在详情播放器上。
 * - 返回（首页等大卡）：详情壳 Exit.None 保住实时画面；源卡 Enter 延后淡入，
 *   避免封面一开始盖住直播画面。
 * - 返回（相关/分区横条卡）：源卡 Enter.None，标题与封面同步落位。
 */
internal enum class VideoCardShellSharedBoundsRole {
    /** 列表源卡片 */
    SourceCard,

    /** 详情壳：整页放大/缩回 */
    DetailShell,
}

/**
 * 返回时源卡延后淡入的起点（占 morph 总时长比例）。
 * 与 [VIDEO_CARD_RETURN_SOURCE_ENTER_FADE_DELAY_RATIO] 同源；当前为 0。
 */
internal const val VIDEO_CARD_SHELL_SOURCE_ENTER_FADE_DELAY_RATIO =
    VIDEO_CARD_RETURN_SOURCE_ENTER_FADE_DELAY_RATIO

/** 横条卡进场源卡淡出时长（占 morph 总时长比例）。 */
internal const val VIDEO_CARD_SHELL_SOURCE_EXIT_FADE_RATIO = 0.28f

/**
 * 源卡 shell 是否延后 Enter。
 * 一律 false：封面待命 + chrome 独立淡入，见 [canCoexistLiveSurfaceStableCoverAndChromeOnReturn]。
 */
internal fun shouldDelaySourceCardEnterForLiveReturnMorph(
    sourceRoute: String?,
    isQuickReturnFromDetail: Boolean = false,
): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = sourceRoute
    return shouldDelaySourceCardEnterOnReturn(isQuickReturnFromDetail)
}

/**
 * 竖卡进场 Exit.None；不再对横条做特判淡出。
 */
internal fun shouldFadeOutShellSourceCardOnOpen(sourceRoute: String?): Boolean {
    @Suppress("UNUSED_PARAMETER")
    val ignored = sourceRoute
    return false
}

internal fun resolveVideoCardShellSourceEnterFadeDelayMillis(
    transitionDurationMillis: Int,
): Int {
    val duration = transitionDurationMillis.coerceAtLeast(0)
    return (duration * VIDEO_CARD_SHELL_SOURCE_ENTER_FADE_DELAY_RATIO).toInt().coerceIn(0, duration)
}

internal fun resolveVideoCardShellSourceExitFadeDurationMillis(
    transitionDurationMillis: Int,
): Int {
    val duration = transitionDurationMillis.coerceAtLeast(0)
    return (duration * VIDEO_CARD_SHELL_SOURCE_EXIT_FADE_RATIO).toInt().coerceIn(72, duration.coerceAtLeast(72))
}

internal fun resolveVideoCardShellSharedBoundsEnter(
    role: VideoCardShellSharedBoundsRole,
    transitionDurationMillis: Int,
    delaySourceCardEnterForLiveReturn: Boolean = true,
): EnterTransition {
    if (
        role == VideoCardShellSharedBoundsRole.SourceCard &&
        delaySourceCardEnterForLiveReturn
    ) {
        val duration = transitionDurationMillis.coerceAtLeast(0)
        val delay = resolveVideoCardShellSourceEnterFadeDelayMillis(duration)
        return fadeIn(
            animationSpec = tween(
                durationMillis = (duration - delay).coerceAtLeast(0),
                delayMillis = delay,
            ),
        )
    }
    return EnterTransition.None
}

internal fun resolveVideoCardShellSharedBoundsExit(
    role: VideoCardShellSharedBoundsRole,
    fadeOutSourceCardOnOpen: Boolean = false,
    transitionDurationMillis: Int = 0,
): ExitTransition {
    if (
        role == VideoCardShellSharedBoundsRole.SourceCard &&
        fadeOutSourceCardOnOpen
    ) {
        return fadeOut(
            animationSpec = tween(
                durationMillis = resolveVideoCardShellSourceExitFadeDurationMillis(
                    transitionDurationMillis,
                ),
            ),
        )
    }
    return ExitTransition.None
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.videoCardShellSharedBoundsOrEmpty(
    enabled: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    bvid: String,
    sourceRoute: String?,
    motionSpec: VideoSharedTransitionMotionSpec,
    clipShape: Shape,
    role: VideoCardShellSharedBoundsRole = VideoCardShellSharedBoundsRole.SourceCard,
    /**
     * 详情页顶部播放器：FillWidth + TopCenter。
     * 竖屏直达 Story 全屏：FillBounds + Center，卡片从列表位整卡展开。
     */
    fillFullscreenShell: Boolean = false,
): Modifier {
    if (!enabled || sharedTransitionScope == null || animatedVisibilityScope == null || bvid.isBlank()) {
        return this
    }
    val bgState = LocalVideoCardTransitionBackgroundState.current
    // 快速返回：源卡 Enter.None，标题/UP 与封面同步落位，避免先占位后出字。
    val isQuickReturnFromDetail = bgState.isQuickReturnFromDetailProvider()
    val delaySourceCardEnter = shouldDelaySourceCardEnterForLiveReturnMorph(
        sourceRoute = sourceRoute,
        isQuickReturnFromDetail = isQuickReturnFromDetail,
    )
    val fadeOutSourceOnOpen = remember(sourceRoute) {
        shouldFadeOutShellSourceCardOnOpen(sourceRoute)
    }
    val enter = remember(role, motionSpec.durationMillis, delaySourceCardEnter) {
        resolveVideoCardShellSharedBoundsEnter(
            role = role,
            transitionDurationMillis = motionSpec.durationMillis,
            delaySourceCardEnterForLiveReturn = delaySourceCardEnter,
        )
    }
    val exit = remember(role, motionSpec.durationMillis, fadeOutSourceOnOpen) {
        resolveVideoCardShellSharedBoundsExit(
            role = role,
            fadeOutSourceCardOnOpen = fadeOutSourceOnOpen,
            transitionDurationMillis = motionSpec.durationMillis,
        )
    }
    val resizeMode = remember(fillFullscreenShell) {
        if (fillFullscreenShell) {
            scaleToBounds(ContentScale.Crop, Alignment.Center)
        } else {
            // 默认 Center 会让卡片在飞行中往屏幕中心缩放，与详情页顶部播放器落点错位。
            scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter)
        }
    }
    return then(
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = videoCardShellSharedElementKey(
                        bvid = bvid,
                        sourceRoute = sourceRoute
                    )
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = enter,
                exit = exit,
                boundsTransform = { initialBounds, targetBounds ->
                    if (motionSpec.enabled) {
                        // duration/easing 与 VideoCardTransitionTimelineSpec /
                        // 详情 AVS morph clock 强制同源（进 Continuity / 回 Linear）。
                        videoSharedElementBoundsTransformSpec(
                            motion = motionSpec,
                            initialBounds = initialBounds,
                            targetBounds = targetBounds,
                            durationMillis = motionSpec.durationMillis,
                        )
                    } else {
                        com.android.purebilibili.core.ui.motion.AppMotionTokens.spatialSpec()
                    }
                },
                resizeMode = resizeMode,
                clipInOverlayDuringTransition = OverlayClip(clipShape)
            )
        }
    )
}
