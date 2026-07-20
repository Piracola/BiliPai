package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale

/**
 * shell sharedBounds 角色。
 *
 * 两侧均 Enter/Exit.None：源卡标题/封面与壳一起参与 morph，避免
 * 「先淡掉标题再飞入」或「回弹末段才出标题」。叠字由详情壳盖住源卡信息区消化。
 */
internal enum class VideoCardShellSharedBoundsRole {
    /** 列表源卡片 */
    SourceCard,

    /** 详情壳：整页放大/缩回 */
    DetailShell,
}

internal fun resolveVideoCardShellSharedBoundsEnter(
    role: VideoCardShellSharedBoundsRole,
    transitionDurationMillis: Int,
): EnterTransition {
    @Suppress("UNUSED_PARAMETER")
    val ignoredRole = role
    @Suppress("UNUSED_PARAMETER")
    val ignoredDuration = transitionDurationMillis
    return EnterTransition.None
}

internal fun resolveVideoCardShellSharedBoundsExit(
    role: VideoCardShellSharedBoundsRole,
): ExitTransition {
    @Suppress("UNUSED_PARAMETER")
    val ignoredRole = role
    return ExitTransition.None
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.videoCardShellSharedBoundsOrEmpty(
    enabled: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    bvid: String,
    sourceRoute: String?,
    motionSpec: VideoSharedTransitionMotionSpec,
    clipShape: Shape,
    role: VideoCardShellSharedBoundsRole = VideoCardShellSharedBoundsRole.SourceCard,
): Modifier {
    if (!enabled || sharedTransitionScope == null || animatedVisibilityScope == null || bvid.isBlank()) {
        return this
    }
    val enter = remember(role, motionSpec.durationMillis) {
        resolveVideoCardShellSharedBoundsEnter(
            role = role,
            transitionDurationMillis = motionSpec.durationMillis,
        )
    }
    val exit = remember(role) {
        resolveVideoCardShellSharedBoundsExit(role = role)
    }
    return then(
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = videoCardShellSharedElementKey(
                        bvid = bvid,
                        sourceRoute = sourceRoute
                    )
                ),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = enter,
                exit = exit,
                boundsTransform = { initialBounds, targetBounds ->
                    if (motionSpec.enabled) {
                        videoSharedElementBoundsTransformSpec(
                            motion = motionSpec,
                            initialBounds = initialBounds,
                            targetBounds = targetBounds
                        )
                    } else {
                        com.android.purebilibili.core.ui.motion.AppMotionTokens.spatialSpec()
                    }
                },
                // 默认 Center 会让卡片在飞行中往屏幕中心缩放，与详情页顶部播放器落点错位。
                resizeMode = scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
                clipInOverlayDuringTransition = OverlayClip(clipShape)
            )
        }
    )
}
