package com.android.purebilibili.core.ui.transition.native

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.PathInterpolator
import androidx.compose.runtime.staticCompositionLocalOf
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.android.purebilibili.core.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class NativeVideoCardTransitionOpenRequest(
    val videoKey: String,
    val coverUrl: String?,
    val sourceRect: NativeVideoTransitionRect,
    val sourceCornerRadiusPx: Float,
    val fallbackTargetCornerRadiusPx: Float
)

internal data class NativeVideoCardTransitionCloseRequest(
    val videoKey: String,
    val coverUrl: String?,
    val sourceRect: NativeVideoTransitionRect,
    val sourceCornerRadiusPx: Float
)

private data class NativeVideoCardTransitionTarget(
    val rect: NativeVideoTransitionRect,
    val cornerRadiusPx: Float
)

private data class ActiveCloseTransition(
    val videoKey: String,
    val spec: NativeVideoCardTransitionSpec
)

internal val LocalNativeVideoCardTransitionController =
    staticCompositionLocalOf<NativeVideoCardTransitionController?> { null }

internal class NativeVideoCardTransitionController(
    private val context: Context,
    private val contentView: View,
    private val overlayView: NativeVideoCardTransitionOverlayView,
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val targetBounds = linkedMapOf<String, NativeVideoCardTransitionTarget>()
    private val interpolator = PathInterpolator(0.18f, 0.76f, 0.22f, 1f)
    private var animator: ValueAnimator? = null
    private var coverLoadJob: Job? = null
    private var isRunning = false
    private var runningPhase: NativeVideoCardTransitionPhase? = null
    private var activeCloseTransition: ActiveCloseTransition? = null
    private var previewCloseProgress = 0f

    fun reportTargetBounds(
        videoKey: String,
        rect: NativeVideoTransitionRect,
        cornerRadiusPx: Float
    ) {
        if (videoKey.isBlank() || !rect.isUsable()) return
        targetBounds[videoKey] = NativeVideoCardTransitionTarget(
            rect = rect,
            cornerRadiusPx = cornerRadiusPx.coerceAtLeast(0f)
        )
    }

    fun clearTargetBounds(videoKey: String) {
        targetBounds.remove(videoKey)
    }

    fun startOpen(
        request: NativeVideoCardTransitionOpenRequest,
        navigateAction: () -> Unit
    ) {
        if (!request.sourceRect.isUsable()) {
            navigateAction()
            return
        }
        if (isRunning) return

        isRunning = true
        runningPhase = NativeVideoCardTransitionPhase.Opening
        cancelAnimatorOnly()
        loadCoverBitmap(request.coverUrl)
        val target = NativeVideoCardTransitionTarget(
            rect = resolveFallbackTargetRect(request.sourceRect),
            cornerRadiusPx = request.fallbackTargetCornerRadiusPx
        )
        val spec = NativeVideoCardTransitionSpec(
            sourceRect = request.sourceRect,
            targetRect = target.rect,
            sourceCornerRadiusPx = request.sourceCornerRadiusPx,
            targetCornerRadiusPx = target.cornerRadiusPx
        )
        renderFrame(
            resolveNativeVideoCardTransitionFrame(
                spec = spec,
                progress = 0f,
                phase = NativeVideoCardTransitionPhase.Opening
            )
        )
        animate(
            spec = spec,
            phase = NativeVideoCardTransitionPhase.Opening,
            onEnd = {
                navigateAction()
                mainHandler.postDelayed(
                    {
                        if (runningPhase == NativeVideoCardTransitionPhase.Opening) {
                            clearTransitionState()
                        }
                    },
                    OPEN_NAVIGATION_HANDOFF_DELAY_MS
                )
            }
        )
    }

    fun startClose(
        request: NativeVideoCardTransitionCloseRequest,
        popAction: () -> Unit
    ) {
        val activeClose = activeCloseTransition
        if (activeClose?.videoKey == request.videoKey) {
            finishPreviewClose(popAction)
            return
        }
        val spec = resolveCloseSpec(request)
        if (spec == null) {
            popAction()
            return
        }
        when (runningPhase) {
            NativeVideoCardTransitionPhase.Closing -> return
            NativeVideoCardTransitionPhase.Opening -> {
                cancel()
                popAction()
                return
            }
            null -> Unit
        }

        isRunning = true
        runningPhase = NativeVideoCardTransitionPhase.Closing
        cancelAnimatorOnly()
        loadCoverBitmap(request.coverUrl)
        activeCloseTransition = ActiveCloseTransition(request.videoKey, spec)
        previewCloseProgress = 0f
        renderFrame(
            resolveNativeVideoCardTransitionFrame(
                spec = spec,
                progress = 0f,
                phase = NativeVideoCardTransitionPhase.Closing
            )
        )
        popAction()
        overlayView.post {
            animate(
                spec = spec,
                phase = NativeVideoCardTransitionPhase.Closing,
                startProgress = 0f,
                onEnd = ::clearTransitionState
            )
        }
    }

    fun previewClose(
        request: NativeVideoCardTransitionCloseRequest,
        progress: Float
    ): Boolean {
        val spec = resolveCloseSpec(request) ?: return false
        if (runningPhase == NativeVideoCardTransitionPhase.Opening) return false
        val clampedProgress = progress.coerceIn(0f, 1f)
        val activeClose = activeCloseTransition
        if (activeClose?.videoKey != request.videoKey) {
            cancelAnimatorOnly()
            loadCoverBitmap(request.coverUrl)
            activeCloseTransition = ActiveCloseTransition(request.videoKey, spec)
        }
        isRunning = true
        runningPhase = NativeVideoCardTransitionPhase.Closing
        previewCloseProgress = clampedProgress
        renderFrame(
            resolveNativeVideoCardTransitionFrame(
                spec = spec,
                progress = clampedProgress,
                phase = NativeVideoCardTransitionPhase.Closing
            )
        )
        return true
    }

    fun finishPreviewClose(popAction: () -> Unit): Boolean {
        val activeClose = activeCloseTransition ?: return false
        popAction()
        overlayView.post {
            animate(
                spec = activeClose.spec,
                phase = NativeVideoCardTransitionPhase.Closing,
                startProgress = previewCloseProgress.coerceIn(0f, 1f),
                onEnd = ::clearTransitionState
            )
        }
        return true
    }

    fun cancelPreviewClose(): Boolean {
        val activeClose = activeCloseTransition ?: return false
        val startProgress = previewCloseProgress.coerceIn(0f, 1f)
        animate(
            spec = activeClose.spec,
            phase = NativeVideoCardTransitionPhase.Closing,
            startProgress = startProgress,
            endProgress = 0f,
            onEnd = ::clearTransitionState
        )
        return true
    }

    fun cancel() {
        cancelAnimatorOnly()
        clearTransitionState()
    }

    private fun animate(
        spec: NativeVideoCardTransitionSpec,
        phase: NativeVideoCardTransitionPhase,
        startProgress: Float = 0f,
        endProgress: Float = 1f,
        onEnd: () -> Unit
    ) {
        cancelAnimatorOnly()
        val clampedStart = startProgress.coerceIn(0f, 1f)
        val clampedEnd = endProgress.coerceIn(0f, 1f)
        animator = ValueAnimator.ofFloat(clampedStart, clampedEnd).apply {
            duration = resolveRemainingDurationMillis(clampedStart, clampedEnd)
            interpolator = this@NativeVideoCardTransitionController.interpolator
            addUpdateListener { valueAnimator ->
                val progress = valueAnimator.animatedValue as Float
                renderFrame(
                    resolveNativeVideoCardTransitionFrame(
                        spec = spec,
                        progress = progress,
                        phase = phase
                    )
                )
            }
            doOnFinish {
                animator = null
                onEnd()
            }
            start()
        }
    }

    private fun ValueAnimator.doOnFinish(action: () -> Unit) {
        addListener(
            object : AnimatorListenerAdapter() {
                private var canceled = false
                private var finished = false

                override fun onAnimationCancel(animation: Animator) {
                    canceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (finished || canceled) return
                    finished = true
                    action()
                }
            }
        )
    }

    private fun renderFrame(frame: NativeVideoCardTransitionFrame) {
        overlayView.showFrame(frame)
        contentView.scaleX = frame.contentScale
        contentView.scaleY = frame.contentScale
        applyRenderEffect(frame.blurRadiusPx)
    }

    @SuppressLint("NewApi")
    private fun applyRenderEffect(blurRadiusPx: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        contentView.setRenderEffect(
            if (blurRadiusPx > 0.5f) {
                RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
            } else {
                null
            }
        )
    }

    private fun clearTransitionState() {
        isRunning = false
        runningPhase = null
        activeCloseTransition = null
        previewCloseProgress = 0f
        coverLoadJob?.cancel()
        coverLoadJob = null
        overlayView.clearFrame()
        contentView.scaleX = 1f
        contentView.scaleY = 1f
        clearRenderEffect()
    }

    @SuppressLint("NewApi")
    private fun clearRenderEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contentView.setRenderEffect(null)
        }
    }

    private fun cancelAnimatorOnly() {
        animator?.cancel()
        animator = null
    }

    private fun resolveFallbackTargetRect(sourceRect: NativeVideoTransitionRect): NativeVideoTransitionRect {
        val width = overlayView.width.takeIf { it > 0 }?.toFloat() ?: contentView.width.toFloat()
        if (width <= 1f) return sourceRect
        val height = width * 9f / 16f
        return NativeVideoTransitionRect(
            left = 0f,
            top = 0f,
            right = width,
            bottom = height
        )
    }

    private fun resolveCloseSpec(request: NativeVideoCardTransitionCloseRequest): NativeVideoCardTransitionSpec? {
        val playerTarget = targetBounds[request.videoKey]
        if (!request.sourceRect.isUsable() || playerTarget == null) return null
        return NativeVideoCardTransitionSpec(
            sourceRect = playerTarget.rect,
            targetRect = request.sourceRect,
            sourceCornerRadiusPx = playerTarget.cornerRadiusPx,
            targetCornerRadiusPx = request.sourceCornerRadiusPx
        )
    }

    private fun resolveRemainingDurationMillis(startProgress: Float, endProgress: Float): Long {
        val distance = kotlin.math.abs(endProgress - startProgress).coerceIn(0f, 1f)
        return (NATIVE_VIDEO_CARD_TRANSITION_DURATION_MILLIS * distance)
            .toLong()
            .coerceAtLeast(MIN_TRANSITION_DURATION_MILLIS)
    }

    private fun loadCoverBitmap(coverUrl: String?) {
        coverLoadJob?.cancel()
        overlayView.setCoverBitmap(null)
        if (coverUrl.isNullOrBlank()) return

        coverLoadJob = scope.launch(Dispatchers.IO) {
            val bitmap = runCatching {
                val request = ImageRequest.Builder(context)
                    .data(FormatUtils.fixImageUrl(coverUrl))
                    .allowHardware(false)
                    .scale(Scale.FILL)
                    .build()
                val result = context.imageLoader.execute(request)
                (result as? SuccessResult)?.drawable?.toBitmapSafely()
            }.getOrNull()

            withContext(Dispatchers.Main.immediate) {
                if (isRunning) {
                    overlayView.setCoverBitmap(bitmap)
                }
            }
        }
    }

    private fun Drawable.toBitmapSafely(): Bitmap? {
        if (this is BitmapDrawable) return bitmap
        val safeWidth = intrinsicWidth.takeIf { it > 0 } ?: return null
        val safeHeight = intrinsicHeight.takeIf { it > 0 } ?: return null
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    companion object {
        private const val OPEN_NAVIGATION_HANDOFF_DELAY_MS = 240L
        private const val MIN_TRANSITION_DURATION_MILLIS = 80L
    }
}
