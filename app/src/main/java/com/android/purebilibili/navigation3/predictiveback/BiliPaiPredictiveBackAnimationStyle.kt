package com.android.purebilibili.navigation3.predictiveback

/**
 * 预测性返回动画样式，取值与 InstallerX-Revived 对齐。
 */
internal enum class BiliPaiPredictiveBackAnimationStyle(
    val storageValue: String,
    val displayName: String,
) {
    NONE("none", "无"),
    AOSP("aosp", "AOSP"),
    MIUIX("miuix", "Miuix"),
    SCALE("scale", "缩放"),
    CLASSIC("ksu_classic", "经典");

    val usesPredictivePreview: Boolean
        get() = this != NONE

    val showsExitDirectionOption: Boolean
        get() = this == AOSP || this == SCALE

    companion object {
        val Default: BiliPaiPredictiveBackAnimationStyle = MIUIX

        fun fromStorageValue(value: String?): BiliPaiPredictiveBackAnimationStyle {
            return when (value) {
                "default" -> Default
                "classic" -> CLASSIC
                else -> entries.find { it.storageValue == value } ?: SCALE
            }
        }
    }
}
