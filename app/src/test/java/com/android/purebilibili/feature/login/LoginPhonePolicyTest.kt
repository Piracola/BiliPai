package com.android.purebilibili.feature.login

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.roundToInt

class LoginPhonePolicyTest {

    @Test
    fun `phone regions include non-mainland choices`() {
        val regions = resolveSupportedPhoneRegions()
        assertTrue(regions.any { it.cid != 86 })
        assertTrue(regions.any { it.cid == 852 })
        assertTrue(regions.any { it.cid == 1 })
    }

    @Test
    fun `international phone numbers can pass captcha eligibility`() {
        val us = resolveSupportedPhoneRegions().first { it.cid == 1 }
        assertTrue(isPhoneEligibleForCaptcha(phoneDigits = "4155552671", region = us))
    }

    @Test
    fun `captcha dialog policy keeps usable height on mobile`() {
        val spec = resolveCaptchaDialogLayoutPolicy(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            density = 3f
        )

        assertEquals((1080 * 0.96f).roundToInt(), spec.widthPx)
        assertEquals((2400 * 0.88f).roundToInt(), spec.heightPx)
        assertEquals(0.42f, spec.dimAmount)
    }
}
