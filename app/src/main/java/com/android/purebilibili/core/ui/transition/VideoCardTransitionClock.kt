package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.purebilibili.core.ui.motion.AppMotionEasing
import kotlin.math.roundToInt

/**
 * 卡片 ↔ 详情 **唯一时间轴契约**。
 *
 * ## 语义（morphFraction）
 * - **0** = 落在列表源卡（景深清晰）
 * - **1** = 落在详情全屏（景深满糊）
 *
 * 景深 blur / scale / scrim、chrome settle、壁纸 depth **只读** [depthProgress]，
 * 不再各自 Animatable 并行。
 *
 * ## 驱动优先级（高 → 低）
 * 1. **预测手势** [gestureBackProgress]
 * 2. **Shared morph**（详情 AVS 与 sharedBounds 同一 Transition 的 progress）
 * 3. **Host fallback Animatable**（无 shared / 首帧详情未挂上时）
 *
 * Shared bounds 仍由 Compose SharedTransition 播 bounds（无法外注 progress），
 * 但 AVS `animateFloat` 与 boundsTransform **强制同一 duration + easing**，
 * 再把 AVS progress 回灌本时钟 → 墙钟与曲线同源。
 */
@Stable
internal class VideoCardTransitionClock {
    var phase: VideoCardTransitionBackgroundPhase by mutableStateOf(
        VideoCardTransitionBackgroundPhase.IDLE,
    )
        private set

    var sourceRoute: String? by mutableStateOf(null)

    /** 预测返回：系统 back progress 0→1；null = 无手势。 */
    var gestureBackProgress: Float? by mutableStateOf(null)

    /** 手势开始时的 depth（通常 1，OPENING 中途可能 <1）。 */
    var gestureStartDepth: Float by mutableFloatStateOf(1f)

    private val fallback = Animatable(0f)

    /**
     * Shared morph 回灌：与详情 shell 同一 Transition 的 fraction（0 卡 / 1 详情）。
     * null = 本帧无有效回灌。
     */
    private var sharedMorphFraction: Float? = null
    private var sharedMorphActive: Boolean = false

    val fallbackValue: Float
        get() = fallback.value

    /**
     * 当前景深 / morph 进度（只读入口）。
     * 所有视觉层必须走这里，禁止再读独立 Animatable。
     */
    fun depthProgress(): Float {
        val gesture = gestureBackProgress
        if (gesture != null) {
            return resolveVideoCardTransitionBackgroundGestureBlurProgress(
                phase = when (phase) {
                    VideoCardTransitionBackgroundPhase.OPENING ->
                        VideoCardTransitionBackgroundPhase.OPENING
                    else -> VideoCardTransitionBackgroundPhase.HELD
                },
                currentBlurProgress = gestureStartDepth,
                backProgress = gesture,
            )
        }
        val shared = sharedMorphFraction
        if (sharedMorphActive && shared != null) {
            return shared.coerceIn(0f, 1f)
        }
        return fallback.value.coerceIn(0f, 1f)
    }

    /**
     * 详情侧每帧回灌 shared morph progress。
     * [morphFraction]：Visible=1、PostExit/PreEnter=0。
     * [active]：shared 或 enter/exit transition 进行中。
     * 相位仍由 Host 的 beginOpening/beginReturning/mark* 拥有；此处只灌 fraction。
     */
    fun reportSharedMorphProgress(morphFraction: Float, active: Boolean) {
        sharedMorphActive = active
        sharedMorphFraction = if (active) {
            morphFraction.coerceIn(0f, 1f)
        } else {
            null
        }
        // 进场 shared 跑完：升到 HELD（Host fallback 可能已先 markHeld，幂等）
        if (!active &&
            phase == VideoCardTransitionBackgroundPhase.OPENING &&
            morphFraction >= 0.999f
        ) {
            phase = VideoCardTransitionBackgroundPhase.HELD
        }
        // 返回 shared 跑完：Idle（列表落位）
        if (!active &&
            phase == VideoCardTransitionBackgroundPhase.RETURNING &&
            morphFraction <= 0.001f
        ) {
            phase = VideoCardTransitionBackgroundPhase.IDLE
            sourceRoute = null
        }
    }

    fun clearSharedMorphProgress() {
        sharedMorphActive = false
        sharedMorphFraction = null
    }

    fun beginOpening(sourceRoute: String?) {
        this.sourceRoute = sourceRoute
        phase = VideoCardTransitionBackgroundPhase.OPENING
        gestureBackProgress = null
        clearSharedMorphProgress()
    }

    fun beginReturning(sourceRoute: String?) {
        this.sourceRoute = sourceRoute
        phase = VideoCardTransitionBackgroundPhase.RETURNING
        gestureBackProgress = null
        // 保留 shared 回灌通道；fallback 从当前 depth 起
    }

    fun markHeld() {
        if (phase == VideoCardTransitionBackgroundPhase.OPENING ||
            phase == VideoCardTransitionBackgroundPhase.RETURNING
        ) {
            phase = VideoCardTransitionBackgroundPhase.HELD
        }
    }

    fun markIdle() {
        phase = VideoCardTransitionBackgroundPhase.IDLE
        sourceRoute = null
        gestureBackProgress = null
        clearSharedMorphProgress()
    }

    fun beginGesture(backProgress: Float) {
        if (gestureBackProgress == null) {
            gestureStartDepth = depthProgress()
        }
        gestureBackProgress = backProgress.coerceIn(0f, 1f)
    }

    fun endGesture() {
        gestureBackProgress = null
    }

    suspend fun snapFallback(value: Float) {
        fallback.snapTo(value.coerceIn(0f, 1f))
    }

    /**
     * 仅当 shared 未接管时跑 fallback。
     * shared 一旦 active，读路径走 shared，fallback 在后台跟到终点以免切换跳变。
     */
    suspend fun animateFallbackTo(
        target: Float,
        durationMillis: Int,
        easing: Easing,
    ) {
        val safeDuration = durationMillis.coerceAtLeast(0)
        if (safeDuration <= 0) {
            fallback.snapTo(target.coerceIn(0f, 1f))
            return
        }
        fallback.animateTo(
            targetValue = target.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = safeDuration, easing = easing),
        )
    }

    suspend fun snapClearAndIdle() {
        clearSharedMorphProgress()
        gestureBackProgress = null
        fallback.snapTo(0f)
        phase = VideoCardTransitionBackgroundPhase.IDLE
    }
}

/**
 * 详情 → 时钟的回灌口（CompositionLocal）。
 */
internal fun interface VideoCardMorphProgressReporter {
    fun report(morphFraction: Float, active: Boolean)
}

internal val LocalVideoCardMorphProgressReporter =
    compositionLocalOf<VideoCardMorphProgressReporter?> { null }

internal val LocalVideoCardTransitionClock =
    compositionLocalOf<VideoCardTransitionClock?> { null }

/**
 * 开合 **唯一** duration/easing 表：boundsTransform、AVS progress、fallback 必须同源。
 */
internal data class VideoCardTransitionTimelineSpec(
    val durationMillis: Int,
    val enterEasing: Easing,
    val returnEasing: Easing,
) {
    companion object {
        fun fromDurationMillis(durationMillis: Int): VideoCardTransitionTimelineSpec {
            return VideoCardTransitionTimelineSpec(
                durationMillis = durationMillis.coerceAtLeast(0),
                enterEasing = AppMotionEasing.Continuity,
                returnEasing = LinearEasing,
            )
        }
    }
}

internal fun resolveVideoCardTimelineSpec(durationMillis: Int): VideoCardTransitionTimelineSpec =
    VideoCardTransitionTimelineSpec.fromDurationMillis(durationMillis)

/**
 * 是否应采用 shared morph 回灌作为 depth 主源。
 */
internal fun shouldPreferSharedMorphProgress(
    sharedMorphActive: Boolean,
    hasSharedFraction: Boolean,
    gestureActive: Boolean,
): Boolean {
    if (gestureActive) return false
    return sharedMorphActive && hasSharedFraction
}

/**
 * 解析最终 depth（纯函数，供测试）。
 */
internal fun resolveVideoCardClockDepthProgress(
    gestureBackProgress: Float?,
    gestureStartDepth: Float,
    phase: VideoCardTransitionBackgroundPhase,
    sharedMorphActive: Boolean,
    sharedMorphFraction: Float?,
    fallbackProgress: Float,
): Float {
    if (gestureBackProgress != null) {
        return resolveVideoCardTransitionBackgroundGestureBlurProgress(
            phase = if (phase == VideoCardTransitionBackgroundPhase.OPENING) {
                VideoCardTransitionBackgroundPhase.OPENING
            } else {
                VideoCardTransitionBackgroundPhase.HELD
            },
            currentBlurProgress = gestureStartDepth,
            backProgress = gestureBackProgress,
        )
    }
    if (shouldPreferSharedMorphProgress(
            sharedMorphActive = sharedMorphActive,
            hasSharedFraction = sharedMorphFraction != null,
            gestureActive = false,
        )
    ) {
        return sharedMorphFraction!!.coerceIn(0f, 1f)
    }
    return fallbackProgress.coerceIn(0f, 1f)
}

/**
 * morphFraction(0 卡→1 详情) 与 settle(0 刚缩→1 落位) 转换。
 */
internal fun morphFractionToReturnSettle(morphFraction: Float): Float =
    (1f - morphFraction.coerceIn(0f, 1f)).coerceIn(0f, 1f)

internal fun resolveMorphAlignedFallbackDurationMs(
    timelineDurationMs: Int,
    startDepth: Float,
    targetDepth: Float,
): Int {
    val span = kotlin.math.abs(startDepth.coerceIn(0f, 1f) - targetDepth.coerceIn(0f, 1f))
    return (timelineDurationMs.coerceAtLeast(0) * span).roundToInt().coerceAtLeast(0)
}

@Composable
internal fun rememberVideoCardTransitionClock(): VideoCardTransitionClock {
    return androidx.compose.runtime.remember { VideoCardTransitionClock() }
}
