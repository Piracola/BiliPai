package com.android.purebilibili.navigation3.predictiveback

import com.android.purebilibili.navigation3.BiliPaiNavCardSourceDirection
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition

internal fun resolveBiliPaiAutoPredictiveBackExitDirection(
    popRouteTransition: BiliPaiNavRouteTransition,
    cardSourceDirection: BiliPaiNavCardSourceDirection,
): BiliPaiPredictiveBackExitDirection {
    if (popRouteTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT) {
        return BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
    }
    return when (cardSourceDirection) {
        BiliPaiNavCardSourceDirection.SOURCE_LEFT -> BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        BiliPaiNavCardSourceDirection.SOURCE_RIGHT -> BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT
        BiliPaiNavCardSourceDirection.NONE -> BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
    }
}

internal fun resolveBiliPaiPredictiveBackExitDirection(
    storageValue: String?,
    autoDerived: BiliPaiPredictiveBackExitDirection,
): BiliPaiPredictiveBackExitDirection {
    return when (storageValue) {
        BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE.storageValue ->
            BiliPaiPredictiveBackExitDirection.FOLLOW_GESTURE
        BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT.storageValue ->
            BiliPaiPredictiveBackExitDirection.ALWAYS_RIGHT
        BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT.storageValue ->
            BiliPaiPredictiveBackExitDirection.ALWAYS_LEFT
        else -> autoDerived
    }
}
