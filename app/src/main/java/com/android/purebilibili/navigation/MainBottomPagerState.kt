package com.android.purebilibili.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class MainBottomPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    var navigationStartPage by mutableIntStateOf(pagerState.currentPage)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        val previousJob = navJob
        navJob = null
        previousJob?.cancel()

        navigationStartPage = pagerState.currentPage
        selectedPage = targetIndex
        isNavigating = true

        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val scrollPixels =
            (targetIndex - pagerState.currentPage - pagerState.currentPageOffsetFraction) * pageSize
        val duration = resolveBottomPagerNavigationDurationMillis()

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                previousJob?.join()
                if (pageSize > 0) {
                    pagerState.animateScrollBy(
                        value = scrollPixels,
                        animationSpec = tween(easing = EaseInOut, durationMillis = duration)
                    )
                }
            } catch (_: IllegalStateException) {
                // Pager 在测量竞争期间可能拒绝强制滚动，避免底栏快速切换直接闪退。
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    selectedPage = targetIndex
                    navigationStartPage = targetIndex
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }

    /**
     * 立即跳到目标页，不播放横向滚动动画。
     * 用于「返回首页」按钮：在视频详情把 MainHost 完全遮挡时静默切到 HOME，
     * 待 [popBiliPaiNavKeyToRoot] 触发的横向过渡播放时背后已经是首页。
     */
    fun snapToPage(targetIndex: Int) {
        if (targetIndex == pagerState.currentPage && targetIndex == selectedPage) {
            return
        }
        val previousJob = navJob
        navJob = null
        previousJob?.cancel()
        navigationStartPage = targetIndex
        selectedPage = targetIndex
        isNavigating = false
        navJob = coroutineScope.launch {
            try {
                previousJob?.join()
                awaitScrollIdle()
                awaitNextFrame()

                pagerState.scrollToPage(targetIndex)
            } catch (_: IllegalStateException) {
                // 同 animateToPage：取消旧滚动后的测量竞争不应导致闪退。
            } finally {
                if (pagerState.currentPage == targetIndex) {
                    selectedPage = targetIndex
                    navigationStartPage = targetIndex
                }
            }
        }
    }

    private suspend fun awaitScrollIdle() {
        if (pagerState.isScrollInProgress) {
            snapshotFlow { pagerState.isScrollInProgress }.first { !it }
        }
    }

    private suspend fun awaitNextFrame() {
        withFrameNanos { }
    }
}

@Composable
internal fun rememberMainBottomPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): MainBottomPagerState {
    return remember(pagerState, coroutineScope) {
        MainBottomPagerState(
            pagerState = pagerState,
            coroutineScope = coroutineScope
        )
    }
}
