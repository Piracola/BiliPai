package com.android.purebilibili.navigation

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled

/**
 * 主页底栏 Tab 二级返回：栈顶为 [com.android.purebilibili.navigation3.BiliPaiNavKey.MainHost]
 * 且当前不在首页 Tab 时，边缘返回手势回到首页 Tab（而非直接退出应用）。
 *
 * 使用 [NavigationBackHandler] 替代 [androidx.activity.compose.BackHandler]。
 * 跟手预览受全局「预测性返回手势」开关控制。
 *
 * 关键：回首页走立即落地（snap）路径而非横向滚动动画（animate）。
 * 预测式返回手势在被系统执行 commit 时，HorizontalPager 内部常因上一帧的惯性测量
 * 竞争产生小数偏移，而 animateToPage 基于该小数偏移计算 scrollPixels，会落到相邻页』；
 * snapToPage 路径 awaitScrollIdle 后才 scrollToPage，绕开偏移读数，与 VideoDetail
 * onHomeClick 已验证的回首页链路完全一致。
 */
@Composable
internal fun MainHostTabBackHandler(
    enabled: Boolean,
    onReturnToHomeTab: () -> Unit,
) {
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    val predictiveBackGestureEnabled = LocalPredictiveBackGestureEnabled.current
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        reportPredictiveProgress = predictiveBackGestureEnabled,
        onBackCompleted = onReturnToHomeTab,
    )
}