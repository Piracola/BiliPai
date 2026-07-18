package com.android.purebilibili.feature.login

import kotlin.math.roundToInt

data class PhoneRegion(
    val cid: Int,
    val dialingCode: String,
    val name: String,
    val minDigits: Int,
    val maxDigits: Int
)

data class CaptchaDialogLayoutPolicy(
    val widthPx: Int,
    val heightPx: Int,
    val dimAmount: Float
)

fun resolveSupportedPhoneRegions(): List<PhoneRegion> {
    return listOf(
        PhoneRegion(cid = 86, dialingCode = "+86", name = "中国大陆", minDigits = 11, maxDigits = 11),
        PhoneRegion(cid = 852, dialingCode = "+852", name = "中国香港", minDigits = 8, maxDigits = 8),
        PhoneRegion(cid = 853, dialingCode = "+853", name = "中国澳门", minDigits = 8, maxDigits = 8),
        PhoneRegion(cid = 886, dialingCode = "+886", name = "中国台湾", minDigits = 9, maxDigits = 9),
        PhoneRegion(cid = 81, dialingCode = "+81", name = "日本", minDigits = 10, maxDigits = 11),
        PhoneRegion(cid = 82, dialingCode = "+82", name = "韩国", minDigits = 9, maxDigits = 11),
        PhoneRegion(cid = 1, dialingCode = "+1", name = "美国/加拿大", minDigits = 10, maxDigits = 10),
        PhoneRegion(cid = 44, dialingCode = "+44", name = "英国", minDigits = 10, maxDigits = 10),
        PhoneRegion(cid = 65, dialingCode = "+65", name = "新加坡", minDigits = 8, maxDigits = 8),
        PhoneRegion(cid = 60, dialingCode = "+60", name = "马来西亚", minDigits = 9, maxDigits = 10),
        PhoneRegion(cid = 61, dialingCode = "+61", name = "澳大利亚", minDigits = 9, maxDigits = 9)
    )
}

fun isPhoneDigitsValidForRegion(phoneDigits: String, region: PhoneRegion): Boolean {
    if (phoneDigits.isBlank()) return false
    if (!phoneDigits.all { it.isDigit() }) return false
    return phoneDigits.length in region.minDigits..region.maxDigits
}

fun isPhoneEligibleForCaptcha(phoneDigits: String, region: PhoneRegion): Boolean {
    return isPhoneDigitsValidForRegion(phoneDigits = phoneDigits, region = region)
}

fun resolveCaptchaDialogLayoutPolicy(
    screenWidthPx: Int,
    screenHeightPx: Int,
    density: Float
): CaptchaDialogLayoutPolicy {
    val widthPx = (screenWidthPx * 0.96f).roundToInt().coerceAtLeast(1)
    val heightPx = (screenHeightPx * 0.88f).roundToInt().coerceAtLeast(1)

    return CaptchaDialogLayoutPolicy(
        widthPx = widthPx,
        heightPx = heightPx,
        dimAmount = 0.42f
    )
}
