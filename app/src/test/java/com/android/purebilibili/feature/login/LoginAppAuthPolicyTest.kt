package com.android.purebilibili.feature.login

import com.android.purebilibili.core.network.AppSignUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginAppAuthPolicyTest {

    @Test
    fun `sms login uses Android app credential parameters`() {
        val params = buildAndroidSmsLoginParams(
            phone = "13800138000",
            countryCode = 86,
            code = 123456,
            captchaKey = "captcha-key",
            timestampSeconds = 123L
        )

        assertEquals(AppSignUtils.ANDROID_APP_KEY, params["appkey"])
        assertEquals("android", params["mobi_app"])
        assertEquals("captcha-key", params["captcha_key"])
        assertEquals("123456", params["code"])
    }
}
