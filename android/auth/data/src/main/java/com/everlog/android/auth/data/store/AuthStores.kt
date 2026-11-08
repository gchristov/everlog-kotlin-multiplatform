package com.everlog.android.auth.data.store

import com.everlog.android.auth.data.UserSession

// The signed-in user's device store, one until destroy()
object AuthStores {

    // Set by the app at startup, until the app's graph builds the stores
    lateinit var session: UserSession

    private var device: ELUserDeviceStore? = null

    @JvmStatic
    @Synchronized
    fun deviceStore(): ELUserDeviceStore = device ?: ELUserDeviceStore(session).also { device = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        device?.destroy()
        device = null
    }
}
