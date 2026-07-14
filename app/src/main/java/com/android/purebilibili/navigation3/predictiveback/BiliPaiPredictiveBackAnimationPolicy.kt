package com.android.purebilibili.navigation3.predictiveback

import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition
import com.android.purebilibili.navigation3.BiliPaiNavKey

internal fun resolveBiliPaiPredictiveBackAnimationHandler(
    routeTransition: BiliPaiNavRouteTransition,
    targetBackKey: BiliPaiNavKey? = null,
    cardTransitionEnabled: Boolean = true,
    predictiveBackEnabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER")
    style: BiliPaiPredictiveBackAnimationStyle = BiliPaiPredictiveBackAnimationStyle.SCALE,
    @Suppress("UNUSED_PARAMETER")
    exitDirection: BiliPaiPredictiveBackExitDirection = BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT,
): BiliPaiPredictiveBackAnimationHandler {
    if (!predictiveBackEnabled) {
        return BiliPaiDisabledPredictiveBackAnimation()
    }
    if (routeTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT) {
        return BiliPaiSharedElementPredictiveBackAnimation()
    }
    if (cardTransitionEnabled && targetBackKey is BiliPaiNavKey.VideoDetail) {
        return BiliPaiVideoDetailTargetPredictiveBackAnimation()
    }
    return BiliPaiDefaultPredictiveBackAnimation()
}
