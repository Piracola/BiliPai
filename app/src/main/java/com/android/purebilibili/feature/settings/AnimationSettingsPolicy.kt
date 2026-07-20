package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.store.LiquidGlassMode
import com.android.purebilibili.core.store.normalizeLiquidGlassProgress
import com.android.purebilibili.core.store.normalizeLiquidGlassStrength
import com.android.purebilibili.core.store.resolveLegacyLiquidGlassProgress
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackExitDirection
import com.android.purebilibili.navigation3.predictiveback.isPredictiveBackStyleAllowed
import com.android.purebilibili.navigation3.predictiveback.resolveEffectivePredictiveBackAnimationStyle

internal const val PREDICTIVE_BACK_ANIMATION_TITLE = "预测性返回动画"
internal const val PREDICTIVE_BACK_EXIT_DIRECTION_TITLE = "退出方向"
internal const val PREDICTIVE_BACK_ANIMATION_SUBTITLE_PREFIX = "当前："
internal const val PREDICTIVE_BACK_ANIMATION_DISABLED_SUBTITLE =
    "需先开启「预测性返回手势」后，才能调整动画样式"
internal const val PREDICTIVE_BACK_AOSP_BLOCKED_BY_CARD_TRANSITION_SUBTITLE =
    "过渡动画开启时不可使用 AOSP，已改用缩放"

internal data class PredictiveBackAnimationUiState(
    val title: String,
    val enabled: Boolean,
    val selectedStyle: BiliPaiPredictiveBackAnimationStyle,
    val subtitle: String,
    val showExitDirection: Boolean,
    val selectedExitDirection: BiliPaiPredictiveBackExitDirection?,
    val exitDirectionSubtitle: String,
    val aospBlockedByCardTransition: Boolean = false,
)

internal data class LiquidGlassPreviewUiState(
    val modeLabel: String,
    val subtitle: String,
    val normalizedProgress: Float,
    val strengthLabel: String,
)

internal fun resolvePredictiveBackAnimationUiState(
    predictiveBackEnabled: Boolean,
    styleStorageValue: String?,
    exitDirectionStorageValue: String?,
    cardTransitionEnabled: Boolean = false,
): PredictiveBackAnimationUiState {
    val storedStyle = BiliPaiPredictiveBackAnimationStyle.fromStorageValue(styleStorageValue)
    val style = resolveEffectivePredictiveBackAnimationStyle(
        style = storedStyle,
        cardTransitionEnabled = cardTransitionEnabled,
    )
    val aospBlocked = cardTransitionEnabled &&
        storedStyle == BiliPaiPredictiveBackAnimationStyle.AOSP
    if (!predictiveBackEnabled) {
        return PredictiveBackAnimationUiState(
            title = PREDICTIVE_BACK_ANIMATION_TITLE,
            enabled = false,
            selectedStyle = style,
            subtitle = PREDICTIVE_BACK_ANIMATION_DISABLED_SUBTITLE,
            showExitDirection = false,
            selectedExitDirection = null,
            exitDirectionSubtitle = "",
            aospBlockedByCardTransition = aospBlocked,
        )
    }
    val exitDirection = resolvePredictiveBackExitDirectionForUi(exitDirectionStorageValue)
    val subtitle = if (aospBlocked) {
        PREDICTIVE_BACK_AOSP_BLOCKED_BY_CARD_TRANSITION_SUBTITLE
    } else {
        "$PREDICTIVE_BACK_ANIMATION_SUBTITLE_PREFIX${style.displayName}"
    }
    return PredictiveBackAnimationUiState(
        title = PREDICTIVE_BACK_ANIMATION_TITLE,
        enabled = true,
        selectedStyle = style,
        subtitle = subtitle,
        showExitDirection = style.showsExitDirectionOption,
        selectedExitDirection = exitDirection,
        exitDirectionSubtitle = "$PREDICTIVE_BACK_ANIMATION_SUBTITLE_PREFIX${exitDirection.displayName}",
        aospBlockedByCardTransition = aospBlocked,
    )
}

internal fun resolvePredictiveBackStyleOptions(
    cardTransitionEnabled: Boolean = false,
): List<BiliPaiPredictiveBackAnimationStyle> {
    return BiliPaiPredictiveBackAnimationStyle.entries.filter { style ->
        isPredictiveBackStyleAllowed(
            style = style,
            cardTransitionEnabled = cardTransitionEnabled,
        )
    }
}

internal fun resolvePredictiveBackExitDirectionOptions(): List<BiliPaiPredictiveBackExitDirection> {
    return BiliPaiPredictiveBackExitDirection.entries
}

internal fun resolvePredictiveBackExitDirectionForUi(
    storageValue: String?,
): BiliPaiPredictiveBackExitDirection {
    return when (storageValue) {
        BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE.storageValue ->
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
        BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT.storageValue ->
            BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT
        else -> BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
    }
}

internal fun resolveLiquidGlassPreviewUiState(
    progress: Float,
): LiquidGlassPreviewUiState {
    val normalizedProgress = normalizeLiquidGlassProgress(progress)
    val (modeLabel, subtitle) = when {
        normalizedProgress < 0.34f -> "通透" to "更清晰、更通透，折射更明显"
        normalizedProgress < 0.68f -> "柔化" to "开始柔化背景，但仍保留液态折射"
        else -> "磨砂" to "更柔和、更雾化，适合弱化背景干扰"
    }
    return LiquidGlassPreviewUiState(
        modeLabel = modeLabel,
        subtitle = subtitle,
        normalizedProgress = normalizedProgress,
        strengthLabel = "${(normalizedProgress * 100).toInt()}%",
    )
}

internal fun resolveLiquidGlassPreviewUiState(
    mode: LiquidGlassMode,
    strength: Float,
): LiquidGlassPreviewUiState {
    return resolveLiquidGlassPreviewUiState(
        progress = resolveLegacyLiquidGlassProgress(
            mode = mode,
            strength = normalizeLiquidGlassStrength(strength),
        ),
    )
}
