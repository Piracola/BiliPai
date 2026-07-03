package com.android.purebilibili.core.ui.transition

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.roundToInt

private const val VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX = 36f
private const val VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX = 2f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA = 0.22f
private const val VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION = 0.045f

internal const val VIDEO_CARD_TRANSITION_BACKGROUND_FORWARD_DURATION_MS = 160
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS = 180
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS = 160

internal enum class VideoCardTransitionBackgroundPhase {
    IDLE,
    OPENING,
    RETURNING
}

internal data class VideoCardTransitionBackgroundFrame(
    val blurRadiusPx: Float,
    val scrimAlpha: Float,
    val contentScale: Float
)

internal data class VideoCardTransitionBackgroundState(
    val progressProvider: () -> Float = { 0f },
    val phaseProvider: () -> VideoCardTransitionBackgroundPhase = {
        VideoCardTransitionBackgroundPhase.IDLE
    }
)

internal val LocalVideoCardTransitionBackgroundState = compositionLocalOf {
    VideoCardTransitionBackgroundState()
}

internal fun resolveVideoCardTransitionBackgroundFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    sdkInt: Int = Build.VERSION.SDK_INT
): VideoCardTransitionBackgroundFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val blurStrength = resolveVideoCardTransitionBlurStrength(clamped)
    val rawBlurRadiusPx = if (sdkInt >= Build.VERSION_CODES.S) {
        VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX * blurStrength
    } else {
        0f
    }

    return VideoCardTransitionBackgroundFrame(
        blurRadiusPx = quantizeVideoCardTransitionBlurRadius(rawBlurRadiusPx),
        scrimAlpha = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING ->
                VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA * clamped
            VideoCardTransitionBackgroundPhase.IDLE,
            VideoCardTransitionBackgroundPhase.RETURNING -> 0f
        },
        contentScale = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING ->
                1f - VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION * clamped
            VideoCardTransitionBackgroundPhase.IDLE,
            VideoCardTransitionBackgroundPhase.RETURNING -> 1f
        }
    )
}

internal fun shouldApplyVideoCardTransitionBackgroundToRoute(
    entryRoute: String?,
    sourceRoute: String?,
    activeMainHostRoute: String?
): Boolean {
    val normalizedEntryRoute = normalizeVideoCardTransitionRoute(entryRoute) ?: return false
    val normalizedSourceRoute = normalizeVideoCardTransitionRoute(sourceRoute) ?: return false
    if (!isVideoCardReturnTargetRoute(normalizedSourceRoute)) return false
    if (normalizedEntryRoute == normalizedSourceRoute) return true
    return normalizedEntryRoute == "main_host" &&
        normalizeVideoCardTransitionRoute(activeMainHostRoute) == normalizedSourceRoute
}

internal fun Modifier.videoCardTransitionBackgroundEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase
): Modifier {
    return graphicsLayer {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = progressProvider(),
            phase = phaseProvider()
        )
        scaleX = frame.contentScale
        scaleY = frame.contentScale
        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && frame.blurRadiusPx > 0.01f) {
            RenderEffect
                .createBlurEffect(
                    frame.blurRadiusPx,
                    frame.blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
                .asComposeRenderEffect()
        } else {
            null
        }
    }.drawWithContent {
        drawContent()
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = progressProvider(),
            phase = phaseProvider()
        )
        if (frame.scrimAlpha > 0.001f) {
            drawRect(Color.Black.copy(alpha = frame.scrimAlpha))
        }
    }
}

private fun resolveVideoCardTransitionBlurStrength(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    // 模糊要比位移/缩放更早进入可感知区，否则 160ms 内肉眼很难看出背景虚化。
    return 1f - (1f - clamped) * (1f - clamped)
}

private fun quantizeVideoCardTransitionBlurRadius(radiusPx: Float): Float {
    if (radiusPx <= 0f) return 0f
    return ((radiusPx / VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX).roundToInt() *
        VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX)
        .coerceIn(0f, VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX)
}

private fun normalizeVideoCardTransitionRoute(route: String?): String? {
    return route
        ?.substringBefore("?")
        ?.takeIf { it.isNotBlank() }
}
