package com.android.purebilibili.feature.cast

import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder
import org.fourthline.cling.binding.xml.UDA10ServiceDescriptorBinderImpl

/**
 * Android's SAX driver does not support a security feature configured by
 * Cling's SAX service descriptor binder. Use Cling's DOM binder instead.
 */
class BiliPaiUpnpServiceImpl : AndroidUpnpServiceImpl() {
    override fun createConfiguration(): UpnpServiceConfiguration = BiliPaiUpnpServiceConfiguration()
}

class BiliPaiUpnpServiceConfiguration : AndroidUpnpServiceConfiguration() {
    override fun createServiceDescriptorBinderUDA10(): ServiceDescriptorBinder =
        UDA10ServiceDescriptorBinderImpl()
}
