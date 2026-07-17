package com.android.purebilibili.feature.video.screen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberVideoDetailEntryTransitionFinished(
    deferLoad: Boolean,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    fallbackDurationMillis: Int,
): Boolean {
    if (!deferLoad) return true

    var finished by remember(deferLoad) { mutableStateOf(false) }
    val fallbackTimeoutMillis = remember(fallbackDurationMillis) {
        resolveVideoDetailEntryTransitionFallbackTimeoutMillis(fallbackDurationMillis)
    }

    LaunchedEffect(deferLoad, sharedTransitionScope, animatedVisibilityScope, fallbackTimeoutMillis) {
        if (!deferLoad) {
            finished = true
            return@LaunchedEffect
        }

        // 相关推荐返回会再次触发 shared/nav transition；若把 finished 打回 false，
        // 父详情简介/相关列表会被 AnimatedVisibility 整块卸掉，表现为播放器下方黑屏重载。
        if (finished) {
            return@LaunchedEffect
        }

        var hasObservedActiveTransition = false

        val timeoutJob = launch {
            kotlinx.coroutines.delay(fallbackTimeoutMillis.toLong())
            finished = true
        }

        try {
            snapshotFlow {
                val sharedActive = sharedTransitionScope?.isTransitionActive ?: false
                val navRunning = animatedVisibilityScope?.transition?.isRunning ?: false
                sharedActive to navRunning
            }
                .distinctUntilChanged()
                .collect { (sharedActive, navRunning) ->
                    if (sharedActive || navRunning) {
                        hasObservedActiveTransition = true
                    }
                    if (
                        shouldMarkVideoDetailEntryTransitionFinished(
                            hasObservedActiveTransition = hasObservedActiveTransition,
                            isSharedTransitionActive = sharedActive,
                            isNavEnterTransitionRunning = navRunning,
                        )
                    ) {
                        finished = true
                        timeoutJob.cancel()
                    }
                }
        } finally {
            timeoutJob.cancel()
        }
    }

    return finished
}
