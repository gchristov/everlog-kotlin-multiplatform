package com.everlog.android.settings.data.store

import com.everlog.android.auth.data.UserSession

// The signed-in user's integrations store, one until destroy()
object SettingsStores {

    // Set by the app at startup, until the app's graph builds the stores
    lateinit var session: UserSession

    private var integration: ELUserIntegrationStore? = null

    @JvmStatic
    @Synchronized
    fun integrationStore(): ELUserIntegrationStore = integration ?: ELUserIntegrationStore(session).also { integration = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        integration?.destroy()
        integration = null
    }
}
