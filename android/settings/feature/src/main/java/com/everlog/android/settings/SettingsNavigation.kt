package com.everlog.android.settings

import android.content.Context

// The other screens Settings opens, through the app's Navigator
interface SettingsNavigation {
    // Set suggestions, or the old muscle goal screen while Remote Config's set_suggestions_v2 is off
    fun openSetSuggestions(context: Context)

    fun openPlayStoreAppDetails(context: Context)

    fun hasSettingsFeedback(): Boolean

    fun openSettingsFeedback(context: Context)

    fun shareApp(context: Context)

    fun openProBuyOrManage(context: Context)

    fun openDesignSystem(context: Context)

    fun openLogin(context: Context)
}
