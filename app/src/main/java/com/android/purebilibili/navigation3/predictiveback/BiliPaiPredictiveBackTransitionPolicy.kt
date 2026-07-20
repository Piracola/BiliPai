package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import com.android.purebilibili.navigation3.BiliPaiNavRouteTransition
import com.android.purebilibili.navigation3.resolveBiliPaiNavContentTransform

/**
 * 预测性返回样式对「路由层 AnimatedContent」的占有方式。
 *
 * - [DECORATOR]：AOSP / Scale 用手势 decorator + commit Animatable；路由层必须 None，
 *   否则 entry 注入的 SETTINGS_IOS 等会在松手时覆盖预览，造成闪烁/层级错乱。
 * - [ROUTE]：Miuix / Classic 仍由 AnimatedContent ContentTransform 驱动。
 * - [NONE]：关闭跟手预览，退回原有路由过渡（含设置 iOS push/pop）。
 */
internal enum class BiliPaiPredictiveBackTransitionOwnership {
    DECORATOR,
    ROUTE,
    NONE,
}

internal fun resolveBiliPaiPredictiveBackTransitionOwnership(
    style: BiliPaiPredictiveBackAnimationStyle,
    predictiveBackEnabled: Boolean,
): BiliPaiPredictiveBackTransitionOwnership {
    if (!predictiveBackEnabled || !style.usesPredictivePreview) {
        return BiliPaiPredictiveBackTransitionOwnership.NONE
    }
    return when (style) {
        BiliPaiPredictiveBackAnimationStyle.AOSP,
        BiliPaiPredictiveBackAnimationStyle.SCALE ->
            BiliPaiPredictiveBackTransitionOwnership.DECORATOR
        BiliPaiPredictiveBackAnimationStyle.MIUIX,
        BiliPaiPredictiveBackAnimationStyle.CLASSIC ->
            BiliPaiPredictiveBackTransitionOwnership.ROUTE
        BiliPaiPredictiveBackAnimationStyle.NONE ->
            BiliPaiPredictiveBackTransitionOwnership.NONE
    }
}

private fun isRouteOwnedSpecialPopTransition(
    routeTransition: BiliPaiNavRouteTransition,
): Boolean {
    return routeTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT ||
        routeTransition == BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_LEFT ||
        routeTransition == BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_RETURN_TO_RIGHT ||
        // 合集详情等 sibling pop：必须保留路由层横滑，不能被 AOSP/Scale decorator 强制 None。
        routeTransition == BiliPaiNavRouteTransition.LIGHT_SIBLING_POP
}

/**
 * Entry 层 pop ContentTransform 必须与当前预测返回样式一致，避免：
 * 手势预览用 handler.predictivePop，松手却走 entry 的 SETTINGS_IOS → 闪一下。
 */
internal fun resolveBiliPaiEntryPopContentTransformForPredictiveStyle(
    routeTransition: BiliPaiNavRouteTransition,
    style: BiliPaiPredictiveBackAnimationStyle,
    predictiveBackEnabled: Boolean = true,
): ContentTransform {
    if (isRouteOwnedSpecialPopTransition(routeTransition)) {
        return resolveBiliPaiNavContentTransform(routeTransition)
    }
    return when (
        resolveBiliPaiPredictiveBackTransitionOwnership(
            style = style,
            predictiveBackEnabled = predictiveBackEnabled,
        )
    ) {
        BiliPaiPredictiveBackTransitionOwnership.DECORATOR ->
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        BiliPaiPredictiveBackTransitionOwnership.ROUTE -> when (style) {
            BiliPaiPredictiveBackAnimationStyle.CLASSIC -> classicPredictiveAlignedPopTransform()
            else -> miuixPredictiveAlignedPopTransform()
        }
        BiliPaiPredictiveBackTransitionOwnership.NONE ->
            resolveBiliPaiNavContentTransform(routeTransition)
    }
}

internal fun resolveBiliPaiEntryForwardContentTransformForPredictiveStyle(
    routeTransition: BiliPaiNavRouteTransition,
    style: BiliPaiPredictiveBackAnimationStyle,
    predictiveBackEnabled: Boolean = true,
): ContentTransform {
    if (
        routeTransition == BiliPaiNavRouteTransition.NO_OP_SHARED_ELEMENT ||
        routeTransition == BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_LEFT ||
        routeTransition == BiliPaiNavRouteTransition.CARD_DISABLED_VIDEO_FORWARD_FROM_RIGHT
    ) {
        return resolveBiliPaiNavContentTransform(routeTransition)
    }
    return when (
        resolveBiliPaiPredictiveBackTransitionOwnership(
            style = style,
            predictiveBackEnabled = predictiveBackEnabled,
        )
    ) {
        BiliPaiPredictiveBackTransitionOwnership.ROUTE -> when (style) {
            BiliPaiPredictiveBackAnimationStyle.CLASSIC,
            BiliPaiPredictiveBackAnimationStyle.MIUIX -> miuixPredictiveAlignedForwardTransform()
            else -> resolveBiliPaiNavContentTransform(routeTransition)
        }
        BiliPaiPredictiveBackTransitionOwnership.DECORATOR,
        BiliPaiPredictiveBackTransitionOwnership.NONE ->
            resolveBiliPaiNavContentTransform(routeTransition)
    }
}

private fun classicPredictiveAlignedPopTransform(): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }),
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    sizeTransform = null,
)

private fun miuixPredictiveAlignedPopTransform(): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(initialOffsetX = { -it / 4 }),
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    sizeTransform = null,
)

private fun miuixPredictiveAlignedForwardTransform(): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(initialOffsetX = { it / 4 }),
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    sizeTransform = null,
)
