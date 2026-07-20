package com.android.purebilibili.navigation3.predictiveback

import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition

internal fun resolveBiliPaiPredictiveBackAnimationHandler(
    routeTransition: BiliPaiNavRouteTransition,
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
    if (routeTransition == BiliPaiNavRouteTransition.SETTINGS_IOS_PUSH_POP) {
        return BiliPaiSettingsIosPredictiveBackAnimation()
    }
    return BiliPaiDefaultPredictiveBackAnimation()
}
