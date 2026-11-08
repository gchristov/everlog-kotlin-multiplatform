package com.everlog.android.settings.ui

interface SettingsAccount {
    fun logout(onLoggedOut: () -> Unit)
}
