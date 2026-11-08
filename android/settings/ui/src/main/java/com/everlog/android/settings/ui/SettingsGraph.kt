package com.everlog.android.settings.ui

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.settings.data.UserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the settings screens get from the app's graph, through appGraph<SettingsGraph>()
@ContributesTo(AppScope::class)
interface SettingsGraph {
    val userSettings: UserSettings

    val userSession: UserSession

    val settingsAnalytics: SettingsAnalytics

    val navigator: Navigator

    val settingsConfig: SettingsConfig

    val settingsDialogs: SettingsDialogs

    val settingsAccount: SettingsAccount
}
