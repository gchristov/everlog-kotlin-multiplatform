package com.everlog.android.settings.data.store

// The signed-in user's integrations store, one until destroy()
object SettingsStores {

    private var integration: ELUserIntegrationStore? = null

    @JvmStatic
    @Synchronized
    fun integrationStore(): ELUserIntegrationStore = integration ?: ELUserIntegrationStore().also { integration = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        integration?.destroy()
        integration = null
    }
}
