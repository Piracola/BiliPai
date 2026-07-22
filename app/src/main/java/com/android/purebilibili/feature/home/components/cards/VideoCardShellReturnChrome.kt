package com.android.purebilibili.feature.home.components.cards

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.LocalVideoCardTransitionBackgroundState
import com.android.purebilibili.core.ui.transition.resolveVideoCardSiblingDepthScale
import com.android.purebilibili.core.util.CardPositionManager

/**
 * 源卡信息区（标题/UP 等）在 shell 返回 morph 时的 chrome alpha。
 * 封面保持可见；返回末段按景深进度淡入字，避免叠实时画面又落后封面；
 * 快速返回不藏字；绘制阶段读 progress，避免整卡重组。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.videoCardShellReturnChromeAlpha(
    enabled: Boolean,
    bvid: String,
    sourceRoute: String?,
    isReturningFromDetail: Boolean = false,
    isQuickReturnFromDetail: Boolean = false,
): Modifier {
    if (!enabled || bvid.isBlank()) return this
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val bgState = LocalVideoCardTransitionBackgroundState.current
    val isSharedMorphSourceCard = remember(
        bvid,
        sourceRoute,
        CardPositionManager.lastClickedVideoSourceKey,
    ) {
        isVideoCardSharedReturnTarget(
            bvid = bvid,
            sourceRoute = sourceRoute,
            lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
        )
    }
    return graphicsLayer {
        alpha = resolveHomeCardChromeAlphaDuringShellReturnMorph(
            useCardContainerSharedBounds = enabled,
            isSharedMorphSourceCard = isSharedMorphSourceCard,
            isReturningFromDetail = isReturningFromDetail,
            transitionBackgroundPhase = bgState.phaseProvider(),
            isVideoCardReturnGestureInProgress = bgState.isReturnGestureInProgressProvider(),
            isSharedTransitionActive = sharedTransitionScope?.isTransitionActive == true,
            transitionBackgroundProgress = bgState.progressProvider(),
            isQuickReturnFromDetail = isQuickReturnFromDetail ||
                bgState.isQuickReturnFromDetailProvider(),
        )
    }
}

/**
 * 首页其他卡片跟随景深轻微收缩；当前飞卡（shared morph 源）保持 1，
 * 避免与 sharedBounds 双重缩放。进度在绘制阶段读取。
 */
@Composable
internal fun Modifier.videoCardSiblingDepthScale(
    bvid: String,
    sourceRoute: String?,
): Modifier {
    if (bvid.isBlank()) return this
    val bgState = LocalVideoCardTransitionBackgroundState.current
    val isSharedMorphSourceCard = remember(
        bvid,
        sourceRoute,
        CardPositionManager.lastClickedVideoSourceKey,
    ) {
        isVideoCardSharedReturnTarget(
            bvid = bvid,
            sourceRoute = sourceRoute,
            lastClickedVideoSourceKey = CardPositionManager.lastClickedVideoSourceKey,
        )
    }
    return graphicsLayer {
        val scale = resolveVideoCardSiblingDepthScale(
            depthProgress = bgState.progressProvider(),
            phase = bgState.phaseProvider(),
            isSharedMorphSourceCard = isSharedMorphSourceCard,
            motionTier = bgState.motionTierProvider(),
        )
        scaleX = scale
        scaleY = scale
    }
}
