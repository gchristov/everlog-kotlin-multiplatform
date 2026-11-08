package com.everlog.application.features

import com.everlog.android.auth.data.AuthActionListener
import com.everlog.android.settings.ui.SettingsAccount
import com.everlog.managers.auth.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppSettingsAccount : SettingsAccount {
    override fun logout(onLoggedOut: () -> Unit) = AuthManager.logout(object : AuthActionListener() {
        override fun onLogout() = onLoggedOut()
    })
}
