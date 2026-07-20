package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

internal const val SETTINGS_IOS_PUSH_DURATION_MS = 350
internal const val SETTINGS_IOS_PUSH_PARALLAX_FACTOR = 0.33f

internal fun resolveSettingsIosPushTransitionMillis(
    animationEnabled: Boolean,
    reduceMotion: Boolean,
): Int {
    if (!animationEnabled || reduceMotion) return 0
    return SETTINGS_IOS_PUSH_DURATION_MS
}

internal fun resolveSettingsIosPushForwardContentTransform(
    durationMillis: Int = SETTINGS_IOS_PUSH_DURATION_MS,
): ContentTransform {
    if (durationMillis <= 0) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val spec = tween<IntOffset>(durationMillis = durationMillis, easing = EaseInOut)
    val parallaxOffset: (Int) -> Int = { width ->
        -(width * SETTINGS_IOS_PUSH_PARALLAX_FACTOR).toInt()
    }
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = spec,
    ) togetherWith slideOutHorizontally(
        targetOffsetX = parallaxOffset,
        animationSpec = spec,
    )
}

internal fun resolveSettingsIosPushPopContentTransform(
    durationMillis: Int = SETTINGS_IOS_PUSH_DURATION_MS,
): ContentTransform {
    if (durationMillis <= 0) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    val spec = tween<IntOffset>(durationMillis = durationMillis, easing = EaseInOut)
    val parallaxOffset: (Int) -> Int = { width ->
        -(width * SETTINGS_IOS_PUSH_PARALLAX_FACTOR).toInt()
    }
    return slideInHorizontally(
        initialOffsetX = parallaxOffset,
        animationSpec = spec,
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = spec,
    )
}

/**
 * 设置预测式返回专用：目标页保持全屏，只横滑顶页。
 * 若对目标页再套 parallax 入场，手势 seek 时两页之间会露出 windowBackground 灰缝，并显得卡手。
 */
internal fun resolveSettingsIosPredictivePopContentTransform(
    durationMillis: Int = SETTINGS_IOS_PUSH_DURATION_MS,
): ContentTransform {
    if (durationMillis <= 0) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    return ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = durationMillis, easing = EaseInOut),
        ),
    )
}
