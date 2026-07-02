package com.android.purebilibili.core.ui.transition.native

import android.os.Build

internal data class NativeVideoTransitionRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun isUsable(): Boolean {
        return width > 1f && height > 1f
    }
}

internal enum class NativeVideoCardTransitionPhase {
    Opening,
    Closing
}

internal data class NativeVideoCardTransitionSpec(
    val sourceRect: NativeVideoTransitionRect,
    val targetRect: NativeVideoTransitionRect,
    val sourceCornerRadiusPx: Float,
    val targetCornerRadiusPx: Float,
    val maxBlurRadiusPx: Float = NATIVE_VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX,
    val maxScrimAlpha: Float = NATIVE_VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA,
    val minContentScale: Float = NATIVE_VIDEO_CARD_TRANSITION_MIN_CONTENT_SCALE
)

internal data class NativeVideoCardTransitionFrame(
    val cardRect: NativeVideoTransitionRect,
    val cornerRadiusPx: Float,
    val blurRadiusPx: Float,
    val scrimAlpha: Float,
    val contentScale: Float,
    val coverAlpha: Float
)

internal const val NATIVE_VIDEO_CARD_TRANSITION_DURATION_MILLIS = 420L
internal const val NATIVE_VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX = 48f
private const val NATIVE_VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA = 0.34f
private const val NATIVE_VIDEO_CARD_TRANSITION_MIN_CONTENT_SCALE = 0.92f

internal fun resolveNativeVideoCardTransitionFrame(
    spec: NativeVideoCardTransitionSpec,
    progress: Float,
    phase: NativeVideoCardTransitionPhase,
    sdkInt: Int = Build.VERSION.SDK_INT
): NativeVideoCardTransitionFrame {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val effectStrength = resolveNativeVideoCardTransitionEffectStrength(clampedProgress, phase)
    val blurRadiusPx = if (sdkInt >= Build.VERSION_CODES.S) {
        spec.maxBlurRadiusPx.coerceAtLeast(0f) * effectStrength
    } else {
        0f
    }
    val coverAlpha = when (phase) {
        NativeVideoCardTransitionPhase.Opening -> 1f - clampedProgress
        NativeVideoCardTransitionPhase.Closing -> clampedProgress
    }

    return NativeVideoCardTransitionFrame(
        cardRect = lerp(spec.sourceRect, spec.targetRect, clampedProgress),
        cornerRadiusPx = lerp(
            spec.sourceCornerRadiusPx.coerceAtLeast(0f),
            spec.targetCornerRadiusPx.coerceAtLeast(0f),
            clampedProgress
        ),
        blurRadiusPx = blurRadiusPx,
        scrimAlpha = spec.maxScrimAlpha.coerceIn(0f, 1f) * effectStrength,
        contentScale = lerp(1f, spec.minContentScale.coerceIn(0.9f, 1f), effectStrength),
        coverAlpha = coverAlpha
    )
}

private fun resolveNativeVideoCardTransitionEffectStrength(
    progress: Float,
    phase: NativeVideoCardTransitionPhase
): Float {
    val easedProgress = smoothStep(progress.coerceIn(0f, 1f))
    return when (phase) {
        NativeVideoCardTransitionPhase.Opening -> easeOutQuart(progress)
        NativeVideoCardTransitionPhase.Closing -> 1f - easedProgress
    }
}

private fun smoothStep(progress: Float): Float {
    return progress * progress * (3f - 2f * progress)
}

private fun easeOutQuart(progress: Float): Float {
    val inverse = 1f - progress.coerceIn(0f, 1f)
    return 1f - (inverse * inverse * inverse * inverse)
}

private fun lerp(start: NativeVideoTransitionRect, end: NativeVideoTransitionRect, fraction: Float): NativeVideoTransitionRect {
    return NativeVideoTransitionRect(
        left = lerp(start.left, end.left, fraction),
        top = lerp(start.top, end.top, fraction),
        right = lerp(start.right, end.right, fraction),
        bottom = lerp(start.bottom, end.bottom, fraction)
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + ((end - start) * fraction.coerceIn(0f, 1f))
}
