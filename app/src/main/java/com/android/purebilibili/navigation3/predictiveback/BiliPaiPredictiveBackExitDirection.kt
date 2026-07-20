package com.android.purebilibili.navigation3.predictiveback

internal enum class BiliPaiPredictiveBackExitDirection(
    val storageValue: String,
    val displayName: String,
) {
    FOLLOW_GESTURE("follow_gesture", "跟随手势"),
    ALWAYS_RIGHT("always_right", "始终向右"),
    ALWAYS_LEFT("always_left", "始终向左");
}
