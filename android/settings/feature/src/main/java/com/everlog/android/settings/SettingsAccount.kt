package com.everlog.android.settings

interface SettingsAccount {
    fun logout(onLoggedOut: () -> Unit)
}
