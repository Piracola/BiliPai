package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.AppSignUtils

internal fun buildAndroidSmsSendParams(
    phone: String,
    countryCode: Int,
    token: String,
    challenge: String,
    validate: String,
    seccode: String,
    timestampSeconds: Long
): Map<String, String> = buildMap {
    putAll(androidLoginBaseParams(timestampSeconds))
    put("cid", countryCode.toString())
    put("tel", phone)
    put("token", token)
    put("challenge", challenge)
    put("validate", validate)
    put("seccode", seccode)
}

internal fun buildAndroidSmsLoginParams(
    phone: String,
    countryCode: Int,
    code: Int,
    captchaKey: String,
    timestampSeconds: Long
): Map<String, String> = buildMap {
    putAll(androidLoginBaseParams(timestampSeconds))
    put("cid", countryCode.toString())
    put("tel", phone)
    put("code", code.toString())
    put("captcha_key", captchaKey)
}

private fun androidLoginBaseParams(timestampSeconds: Long): Map<String, String> = mapOf(
    "appkey" to AppSignUtils.ANDROID_APP_KEY,
    "build" to "8130300",
    "device" to "android",
    "mobi_app" to "android",
    "platform" to "android",
    "ts" to timestampSeconds.toString()
)
