package com.android.purebilibili.feature.cast

import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl
import org.junit.Assert.assertTrue
import org.junit.Test

class BiliPaiUpnpServiceConfigurationTest {

    @Test
    fun `uses DOM service descriptor binder on Android`() {
        val configuration = BiliPaiUpnpServiceConfiguration()

        assertTrue(
            configuration.serviceDescriptorBinderUDA10 is UDA10ServiceDescriptorBinderImpl
        )
    }
}
