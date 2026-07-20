package com.android.purebilibili.navigation3.predictiveback

import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition

/**
 * 卡片过渡动画与 AOSP 预测返回互斥：共享元素路径会抢走 decorator，AOSP 缩放预览无法稳定共存。
 * 开启过渡动画时强制回退到缩放样式。
 */
internal fun resolveEffectivePredictiveBackAnimationStyle(
    style: BiliPaiPredictiveBackAnimationStyle,
    cardTransitionEnabled: Boolean,
): BiliPaiPredictiveBackAnimationStyle {
    if (cardTransitionEnabled && style == BiliPaiPredictiveBackAnimationStyle.AOSP) {
        return BiliPaiPredictiveBackAnimationStyle.SCALE
    }
    return style
}

internal fun isPredictiveBackStyleAllowed(
    style: BiliPaiPredictiveBackAnimationStyle,
    cardTransitionEnabled: Boolean,
): Boolean {
    return !(cardTransitionEnabled && style == BiliPaiPredictiveBackAnimationStyle.AOSP)
}

internal fun resolveBiliPaiPredictiveBackAnimationHandler(
    routeTransition: BiliPaiNavRouteTransition,
    predictiveBackEnabled: Boolean = true,
    style: BiliPaiPredictiveBackAnimationStyle = BiliPaiPredictiveBackAnimationStyle.SCALE,
    exitDirection: BiliPaiPredictiveBackExitDirection = BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
    cardTransitionEnabled: Boolean = false,
): BiliPaiPredictiveBackAnimationHandler {
    val effectiveStyle = resolveEffectivePredictiveBackAnimationStyle(
        style = style,
        cardTransitionEnabled = cardTransitionEnabled,
    )
    if (!predictiveBackEnabled || !effectiveStyle.usesPredictivePreview) {
        return BiliPaiDisabledPredictiveBackAnimation()
    }
    if (routeTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT) {
        return BiliPaiSharedElementPredictiveBackAnimation()
    }
    // 合集详情等 sibling 返回：Scale/AOSP decorator 会与横滑叠层；改走 route-owned 样式。
    if (routeTransition == BiliPaiNavRouteTransition.LIGHT_SIBLING_POP) {
        return when (effectiveStyle) {
            BiliPaiPredictiveBackAnimationStyle.NONE -> BiliPaiDisabledPredictiveBackAnimation()
            BiliPaiPredictiveBackAnimationStyle.CLASSIC -> BiliPaiClassicPredictiveBackAnimation()
            else -> BiliPaiMiuixPredictiveBackAnimation()
        }
    }
    return when (effectiveStyle) {
        BiliPaiPredictiveBackAnimationStyle.NONE -> BiliPaiDisabledPredictiveBackAnimation()
        BiliPaiPredictiveBackAnimationStyle.AOSP -> BiliPaiAospPredictiveBackAnimation(exitDirection)
        BiliPaiPredictiveBackAnimationStyle.MIUIX -> BiliPaiMiuixPredictiveBackAnimation()
        BiliPaiPredictiveBackAnimationStyle.SCALE -> BiliPaiScalePredictiveBackAnimation(exitDirection)
        BiliPaiPredictiveBackAnimationStyle.CLASSIC -> BiliPaiClassicPredictiveBackAnimation()
    }
}
