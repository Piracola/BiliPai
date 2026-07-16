package com.android.purebilibili.feature.settings

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.LocalPredictiveBackGestureEnabled

/**
 * Settings 内局部返回（overlay / drill-down / 搜索态）使用 [NavigationBackHandler]。
 * 跟手预览受全局「预测性返回手势」开关控制；关闭后仍可边缘返回，松手后直接完成返回。
 */
@Composable
internal fun SettingsLocalBackHandler(
    enabled: Boolean = true,
    onBackCompleted: () -> Unit,
) {
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    val predictiveBackGestureEnabled = LocalPredictiveBackGestureEnabled.current
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        reportPredictiveProgress = predictiveBackGestureEnabled,
        onBackCompleted = onBackCompleted,
    )
}
