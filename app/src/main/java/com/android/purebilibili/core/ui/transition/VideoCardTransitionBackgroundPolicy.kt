package com.android.purebilibili.core.ui.transition

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.roundToInt

// 景深标定（对齐 iOS App 开合观感）：
// - 背景下沉约 7%（0.93），比旧 4.5% 更有“被压住”的层次
// - 峰值 blur 28px：靠 scale+scrim 补深度，降低整页实时模糊 GPU 成本
// - 压暗全程保留（含 HELD），避免打开完成后景深断裂
private const val VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX = 28f
private const val VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX = 2f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK = 0.28f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT = 0.14f
private const val VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_DARK = 0.16f
private const val VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_LIGHT = 0.08f
private const val VIDEO_CARD_TRANSITION_LIGHT_REDUCED_OPENING_SCRIM_ALPHA = 0.08f
private const val VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION = 0.07f
private val VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT = Color(0xFF8E8E93)

// 开场与返回时长由共享元素速度设置提供；取消仍固定为短恢复动画。
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS = 460
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS = 160

internal enum class VideoCardTransitionBackgroundPhase {
    IDLE,
    OPENING,
    HELD,
    RETURNING
}

internal data class VideoCardTransitionBackgroundFrame(
    val blurRadiusPx: Float,
    val scrimAlpha: Float,
    val contentScale: Float,
    val useLightScrimTint: Boolean = false,
)

internal data class VideoCardTransitionBackgroundState(
    val progressProvider: () -> Float = { 0f },
    val sourceRouteProvider: () -> String? = { null },
    val phaseProvider: () -> VideoCardTransitionBackgroundPhase = {
        VideoCardTransitionBackgroundPhase.IDLE
    },
    val isReturnGestureInProgressProvider: () -> Boolean = { false },
    val isGestureRestoreInProgressProvider: () -> Boolean = { false },
    val motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    val isLightBackgroundProvider: () -> Boolean = { false },
)

internal val LocalVideoCardTransitionBackgroundState = compositionLocalOf {
    VideoCardTransitionBackgroundState()
}

internal fun resolveVideoCardTransitionOpeningScrimAlpha(
    progress: Float,
    isLightBackground: Boolean,
    motionTier: MotionTier,
): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val maxAlpha = when {
        isLightBackground && motionTier == MotionTier.Reduced ->
            VIDEO_CARD_TRANSITION_LIGHT_REDUCED_OPENING_SCRIM_ALPHA
        isLightBackground ->
            VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT
        else ->
            VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK
    }
    return maxAlpha * clamped
}

internal fun resolveVideoCardTransitionReturningScrimAlpha(
    blurStrength: Float,
    isLightBackground: Boolean,
): Float {
    val maxAlpha = if (isLightBackground) {
        VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_LIGHT
    } else {
        VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA_DARK
    }
    return maxAlpha * blurStrength
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveVideoCardTransitionContentScale(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier,
    isGestureRestoreInProgress: Boolean,
): Float {
    if (phase == VideoCardTransitionBackgroundPhase.IDLE || motionTier == MotionTier.Reduced) {
        return 1f
    }
    // 下沉进度略前倾：卡片一开始放大时背景就明显“退进景深”，更像 iOS 主屏被压住。
    val depthProgress = resolveVideoCardTransitionDepthProgress(progress)
    return 1f - VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION * depthProgress
}

internal fun resolveVideoCardTransitionBackgroundFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier = MotionTier.Normal,
    isLightBackground: Boolean = false,
    isGestureRestoreInProgress: Boolean = false,
    sdkInt: Int = Build.VERSION.SDK_INT,
): VideoCardTransitionBackgroundFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val blurStrength = resolveVideoCardTransitionBlurStrength(clamped)
    // 低端/省电/无障碍减弱动画(Reduced)时跳过整帧 GPU 实时模糊与景深缩放，仅保留 scrim。
    val rawBlurRadiusPx = if (
        phase != VideoCardTransitionBackgroundPhase.IDLE &&
        motionTier != MotionTier.Reduced &&
        sdkInt >= Build.VERSION_CODES.S
    ) {
        VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX * blurStrength
    } else {
        0f
    }

    return VideoCardTransitionBackgroundFrame(
        blurRadiusPx = quantizeVideoCardTransitionBlurRadius(rawBlurRadiusPx),
        scrimAlpha = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING,
            VideoCardTransitionBackgroundPhase.HELD ->
                resolveVideoCardTransitionOpeningScrimAlpha(
                    progress = clamped,
                    isLightBackground = isLightBackground,
                    motionTier = motionTier,
                )
            VideoCardTransitionBackgroundPhase.RETURNING ->
                resolveVideoCardTransitionReturningScrimAlpha(
                    blurStrength = blurStrength,
                    isLightBackground = isLightBackground,
                )
            VideoCardTransitionBackgroundPhase.IDLE -> 0f
        },
        contentScale = resolveVideoCardTransitionContentScale(
            progress = clamped,
            phase = phase,
            motionTier = motionTier,
            isGestureRestoreInProgress = isGestureRestoreInProgress,
        ),
        useLightScrimTint = isLightBackground,
    )
}

/**
 * 预测式返回手势进行中时，把系统回退进度(0→1)映射为背景虚化进度(1→0)。
 *
 * - 手势起点(0)保持满虚化，与 [VideoCardTransitionBackgroundPhase.HELD] 无缝衔接；
 * - 拖到底(1)则背景基本清晰，从而让全屏 GPU 模糊随手势实时消退，
 *   与共享元素 morph 落位同步，避免"提交返回后才补一段 460ms 模糊 → 封面高斯模糊+闪烁"。
 */
internal fun resolveVideoCardTransitionBackgroundGestureProgress(
    backProgress: Float
): Float {
    val clamped = backProgress.coerceIn(0f, 1f)
    return 1f - clamped
}

/**
 * [VideoCardTransitionBackgroundPhase.OPENING] 阶段预测式返回：以当前开场虚化进度为起点，
 * 随手势线性消退至清晰。与 HELD 满值起点的 [resolveVideoCardTransitionBackgroundGestureProgress] 区分。
 */
internal fun resolveVideoCardTransitionBackgroundOpeningGestureProgress(
    openingBlurProgress: Float,
    backProgress: Float,
): Float {
    val clampedOpening = openingBlurProgress.coerceIn(0f, 1f)
    val clampedBack = backProgress.coerceIn(0f, 1f)
    return clampedOpening * (1f - clampedBack)
}

internal fun isVideoCardTransitionBackgroundGesturePhase(
    phase: VideoCardTransitionBackgroundPhase,
): Boolean {
    return phase == VideoCardTransitionBackgroundPhase.HELD ||
        phase == VideoCardTransitionBackgroundPhase.OPENING
}

internal fun resolveVideoCardTransitionBackgroundGestureBlurProgress(
    phase: VideoCardTransitionBackgroundPhase,
    currentBlurProgress: Float,
    backProgress: Float,
): Float {
    return when (phase) {
        VideoCardTransitionBackgroundPhase.HELD ->
            resolveVideoCardTransitionBackgroundGestureProgress(backProgress)
        VideoCardTransitionBackgroundPhase.OPENING ->
            resolveVideoCardTransitionBackgroundOpeningGestureProgress(
                openingBlurProgress = currentBlurProgress,
                backProgress = backProgress,
            )
        else -> currentBlurProgress
    }
}

/**
 * 返回动画提交时，若手势已消解部分虚化(startProgress < 1)，剩余 [RETURNING] 动画按比例缩短，
 * 保持与共享元素落位一致的视觉速度，避免手势拖到底后仍补一段完整时长的收尾。
 */
internal fun resolveVideoCardTransitionBackgroundReturnDurationMs(
    startProgress: Float,
    fullDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS,
    minDurationMs: Int = VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS
): Int {
    val clamped = startProgress.coerceIn(0f, 1f)
    return (fullDurationMs * clamped).roundToInt().coerceIn(minDurationMs, fullDurationMs)
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

/**
 * 视频卡片过渡期间 Nav 层全屏 backdrop：填补 sharedBounds morph / 预测式返回
 * 在屏幕边缘露出的窗口底色，视觉上延续首页虚化后的色调。
 */
internal data class VideoCardTransitionNavBackdropFrame(
    val scrimAlpha: Float,
    val useLightScrimTint: Boolean,
)

internal fun shouldShowVideoCardTransitionNavBackdrop(
    cardTransitionEnabled: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
    isVideoDetailOnStack: Boolean,
    isReturningToVideoDetail: Boolean = false,
): Boolean {
    if (!cardTransitionEnabled || !isVideoDetailOnStack || isReturningToVideoDetail) return false
    return phase == VideoCardTransitionBackgroundPhase.HELD ||
        phase == VideoCardTransitionBackgroundPhase.OPENING
}

internal fun resolveVideoCardTransitionNavBackdropFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    isLightBackground: Boolean,
): VideoCardTransitionNavBackdropFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val blurStrength = resolveVideoCardTransitionBlurStrength(clamped)
    val scrimAlpha = when (phase) {
        VideoCardTransitionBackgroundPhase.OPENING ->
            resolveVideoCardTransitionOpeningScrimAlpha(
                progress = clamped,
                isLightBackground = isLightBackground,
                motionTier = MotionTier.Normal,
            )
        VideoCardTransitionBackgroundPhase.HELD -> {
            if (isLightBackground) {
                VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_LIGHT * blurStrength
            } else {
                VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA_DARK * blurStrength
            }
        }
        else -> 0f
    }
    return VideoCardTransitionNavBackdropFrame(
        scrimAlpha = scrimAlpha,
        useLightScrimTint = isLightBackground,
    )
}

internal fun resolveVideoCardTransitionNavBackdropColor(
    baseBackgroundColor: Color,
    frame: VideoCardTransitionNavBackdropFrame,
): Color {
    if (frame.scrimAlpha <= 0.001f) return baseBackgroundColor
    val tint = if (frame.useLightScrimTint) {
        VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
    } else {
        Color.Black
    }
    return lerp(
        start = baseBackgroundColor,
        stop = tint,
        fraction = frame.scrimAlpha.coerceIn(0f, 1f),
    )
}

/**
 * 是否用「冻结 display list + 动态 blur/scale」路径。
 * Reduced / API<31 走轻量 scrim-only，避免无收益的 layer 开销。
 */
internal fun shouldUseVideoCardTransitionSnapshotBlur(
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean {
    if (phase == VideoCardTransitionBackgroundPhase.IDLE) return false
    if (motionTier == MotionTier.Reduced) return false
    return sdkInt >= Build.VERSION_CODES.S
}

/**
 * 每帧内多次读取同一 frame 时，用 (progress, phase, …) 缓存避免重复纯函数计算。
 */
private class VideoCardTransitionBackgroundFrameCache {
    private var lastProgress = Float.NaN
    private var lastPhase: VideoCardTransitionBackgroundPhase? = null
    private var lastMotionTier: MotionTier? = null
    private var lastIsLightBackground: Boolean? = null
    private var lastGestureRestoreInProgress: Boolean? = null
    private var cached = VideoCardTransitionBackgroundFrame(
        blurRadiusPx = 0f,
        scrimAlpha = 0f,
        contentScale = 1f,
    )

    fun resolve(
        progress: Float,
        phase: VideoCardTransitionBackgroundPhase,
        motionTier: MotionTier,
        isLightBackground: Boolean,
        isGestureRestoreInProgress: Boolean,
    ): VideoCardTransitionBackgroundFrame {
        if (
            progress != lastProgress ||
            phase != lastPhase ||
            motionTier != lastMotionTier ||
            isLightBackground != lastIsLightBackground ||
            isGestureRestoreInProgress != lastGestureRestoreInProgress
        ) {
            lastProgress = progress
            lastPhase = phase
            lastMotionTier = motionTier
            lastIsLightBackground = isLightBackground
            lastGestureRestoreInProgress = isGestureRestoreInProgress
            cached = resolveVideoCardTransitionBackgroundFrame(
                progress = progress,
                phase = phase,
                motionTier = motionTier,
                isLightBackground = isLightBackground,
                isGestureRestoreInProgress = isGestureRestoreInProgress,
            )
        }
        return cached
    }
}

/**
 * 冻结层状态：开场首帧 record 后停止重录 feed，只对静态 display list
 * 更新 scale / BlurEffect / scrim，实现「看起来实时的动态模糊」与稳帧共存。
 */
private class VideoCardTransitionSnapshotLayerState {
    val frameCache = VideoCardTransitionBackgroundFrameCache()
    var freezeRecording: Boolean = false
    var hasRecordedContent: Boolean = false
    var lastBlurRadiusPx: Float = Float.NaN

    fun reset() {
        freezeRecording = false
        hasRecordedContent = false
        lastBlurRadiusPx = Float.NaN
    }
}

/**
 * 卡片开合景深：
 * - OPENING 首帧 record 来源页 display list，随后冻结
 * - 过渡期间只对冻结层做 scale + 量化 BlurEffect + scrim（跟手/进度仍动态）
 * - HELD/RETURNING 复用同一冻结层；IDLE 释放并恢复普通绘制
 * - Reduced / API 31 以下：不模糊，仅 scrim（与 [resolveVideoCardTransitionBackgroundFrame] 一致）
 */
@Composable
internal fun Modifier.videoCardTransitionBackgroundEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    isGestureRestoreInProgressProvider: () -> Boolean = { false },
    motionTierProvider: () -> MotionTier = { MotionTier.Normal },
    isLightBackgroundProvider: () -> Boolean = { false },
): Modifier {
    val contentLayer = rememberGraphicsLayer()
    val snapshotState = remember { VideoCardTransitionSnapshotLayerState() }
    val phase = phaseProvider()
    val motionTier = motionTierProvider()
    val useSnapshotBlur = shouldUseVideoCardTransitionSnapshotBlur(
        phase = phase,
        motionTier = motionTier,
    )

    LaunchedEffect(phase, useSnapshotBlur) {
        if (!useSnapshotBlur) {
            snapshotState.reset()
            return@LaunchedEffect
        }
        when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING -> {
                // 允许至少一帧完整 record，再冻结，避免空 layer。
                snapshotState.freezeRecording = false
                withFrameNanos { }
                if (!snapshotState.hasRecordedContent) {
                    withFrameNanos { }
                }
                snapshotState.freezeRecording = true
            }
            VideoCardTransitionBackgroundPhase.HELD,
            VideoCardTransitionBackgroundPhase.RETURNING -> {
                if (!snapshotState.hasRecordedContent) {
                    snapshotState.freezeRecording = false
                    withFrameNanos { }
                    if (!snapshotState.hasRecordedContent) {
                        withFrameNanos { }
                    }
                }
                snapshotState.freezeRecording = true
            }
            VideoCardTransitionBackgroundPhase.IDLE -> snapshotState.reset()
        }
    }

    return this.drawWithContent {
        val activePhase = phaseProvider()
        val activeMotionTier = motionTierProvider()
        val frame = snapshotState.frameCache.resolve(
            progress = progressProvider(),
            phase = activePhase,
            motionTier = activeMotionTier,
            isLightBackground = isLightBackgroundProvider(),
            isGestureRestoreInProgress = isGestureRestoreInProgressProvider(),
        )
        val snapshotBlurActive = shouldUseVideoCardTransitionSnapshotBlur(
            phase = activePhase,
            motionTier = activeMotionTier,
        )

        if (!snapshotBlurActive) {
            // IDLE / Reduced / 低版本：正常绘制内容；需要时只叠 scrim。
            drawContent()
            if (frame.scrimAlpha > 0.001f) {
                val scrimColor = if (frame.useLightScrimTint) {
                    VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
                } else {
                    Color.Black
                }
                drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
            }
            return@drawWithContent
        }

        // 未冻结或尚未成功 record：重新录制来源页；冻结后跳过 feed 重绘。
        if (!snapshotState.freezeRecording || !snapshotState.hasRecordedContent) {
            contentLayer.record {
                this@drawWithContent.drawContent()
            }
            if (size.width > 0f && size.height > 0f) {
                snapshotState.hasRecordedContent = true
            }
        }

        contentLayer.pivotOffset = Offset(size.width / 2f, size.height / 2f)
        contentLayer.scaleX = frame.contentScale
        contentLayer.scaleY = frame.contentScale
        if (frame.blurRadiusPx != snapshotState.lastBlurRadiusPx) {
            snapshotState.lastBlurRadiusPx = frame.blurRadiusPx
            contentLayer.renderEffect = if (frame.blurRadiusPx > 0.01f) {
                BlurEffect(
                    radiusX = frame.blurRadiusPx,
                    radiusY = frame.blurRadiusPx,
                    edgeTreatment = TileMode.Clamp,
                )
            } else {
                null
            }
        }
        drawLayer(contentLayer)

        if (frame.scrimAlpha > 0.001f) {
            val scrimColor = if (frame.useLightScrimTint) {
                VIDEO_CARD_TRANSITION_LIGHT_SCRIM_TINT
            } else {
                Color.Black
            }
            drawRect(scrimColor.copy(alpha = frame.scrimAlpha))
        }
    }
}

/**
 * 景深进度：ease-in（前倾），让 scale/模糊在开合前半段就建立层次。
 */
internal fun resolveVideoCardTransitionDepthProgress(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return 1f - (1f - clamped) * (1f - clamped)
}

private fun resolveVideoCardTransitionBlurStrength(progress: Float): Float {
    // 与景深进度同源：模糊与背景下沉同步建立，避免“先糊后沉”的分层错位。
    return resolveVideoCardTransitionDepthProgress(progress)
}

private fun quantizeVideoCardTransitionBlurRadius(radiusPx: Float): Float {
    if (radiusPx <= 0f) return 0f
    return ((radiusPx / VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX).roundToInt() *
        VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX)
        .coerceIn(0f, VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX)
}

private fun normalizeVideoCardTransitionRoute(route: String?): String? {
    val normalized = route?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        "home"
    } else {
        normalized.substringBefore("?")
    }
}
