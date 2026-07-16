package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
internal fun LocalNavigationBackHandler(
    enabled: Boolean,
    onBackCompleted: () -> Unit,
) {
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    val predictiveBackGestureEnabled = LocalPredictiveBackGestureEnabled.current
    NavigationBackHandler(
        state = state,
        isBackEnabled = enabled,
        reportPredictiveProgress = predictiveBackGestureEnabled,
        onBackCompleted = { commitTransition: () -> Unit ->
            onBackCompleted()
            commitTransition()
        },
    )
}
