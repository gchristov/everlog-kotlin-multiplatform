package com.everlog.android.auth.data.store

// The signed-in user's consent and device stores, one of each until destroy()
object AuthStores {

    private var consent: ELUserConsentStore? = null
    private var device: ELUserDeviceStore? = null

    @JvmStatic
    @Synchronized
    fun consentStore(): ELUserConsentStore = consent ?: ELUserConsentStore().also { consent = it }

    @JvmStatic
    @Synchronized
    fun deviceStore(): ELUserDeviceStore = device ?: ELUserDeviceStore().also { device = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        consent?.destroy()
        consent = null
        device?.destroy()
        device = null
    }
}
