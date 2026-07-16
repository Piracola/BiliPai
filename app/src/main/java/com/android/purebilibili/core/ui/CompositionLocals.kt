package com.android.purebilibili.core.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * 用于控制全局底栏可见性的 CompositionLocal
 * 子页面可以通过此 Local 调用函数来显式显示/隐藏底栏
 * 参数: visible (Boolean)
 */
val LocalSetBottomBarVisible = compositionLocalOf<(Boolean) -> Unit> { 
    error("No SetBottomBarVisible provided") 
}

/**
 * 用于获取当前全局底栏可见性的 CompositionLocal (可选)
 */
val LocalBottomBarVisible = compositionLocalOf<Boolean> { true }

/**
 * 全局“预测性返回手势”设置：关闭后仍可边缘返回，但不上报跟手进度、不显示预测预览。
 * 由 [com.android.purebilibili.navigation.AppNavigation] 从用户偏好提供。
 */
val LocalPredictiveBackGestureEnabled = compositionLocalOf { true }
