package com.everlog.android.auth.data.store

import com.everlog.android.auth.data.UserSession

// The signed-in user's consent and device stores, one of each until destroy()
object AuthStores {

    // Set by the app at startup, until the app's graph builds the stores
    lateinit var session: UserSession

    private var consent: ELUserConsentStore? = null
    private var device: ELUserDeviceStore? = null

    @JvmStatic
    @Synchronized
    fun consentStore(): ELUserConsentStore = consent ?: ELUserConsentStore(session).also { consent = it }

    @JvmStatic
    @Synchronized
    fun deviceStore(): ELUserDeviceStore = device ?: ELUserDeviceStore(session).also { device = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        consent?.destroy()
        consent = null
        device?.destroy()
        device = null
    }
}
