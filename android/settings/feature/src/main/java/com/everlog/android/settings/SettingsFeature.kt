package com.everlog.android.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.android.settings.data.model.ELIntegration
import rx.Observable

/**
 * What the settings feature needs from the app: services built on the app's managers, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [SettingsFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface SettingsFeatureDependencies {
    fun settingsAnalytics(): SettingsAnalytics

    // Set suggestions, or the old muscle goal screen while Remote Config's set_suggestions_v2 is off
    fun openSetSuggestions(context: Context)

    fun setSuggestionsV2(): Boolean

    // How Set suggestions names [goal], or null for a goal it doesn't offer
    fun setSuggestionText(context: Context, goal: MuscleGoal): SetSuggestionText?

    fun openPlayStoreAppDetails(context: Context)

    fun hasSettingsFeedback(): Boolean

    fun openSettingsFeedback(context: Context)

    fun shareApp(context: Context)

    fun openProBuyOrManage(context: Context)

    fun openDesignSystem(context: Context)

    fun openLogin(context: Context)

    fun logout(onLoggedOut: () -> Unit)

    // The app's version and build, for the bottom of Settings
    fun appInfo(): String

    fun showWeightIncreasePrompt(context: Context, value: Float): Observable<Float>

    fun showWeeklyGoalPrompt(context: Context, value: Int): Observable<Int>

    // Asks for Google Fit access, and calls [onGranted] with the new integration once given
    fun connectGoogleFit(view: BaseFragmentMvpView, onGranted: (ELIntegration) -> Unit)

    // Google sign-in's answer for connectGoogleFit, from onActivityResult
    fun googleFitActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun disconnectGoogleFit(integration: ELIntegration)
}

data class SetSuggestionText(val value: String, val subtitle: String)

interface SettingsAnalytics {
    fun weightIncreaseModified(value: Float)
    fun weeklyGoalModified(value: Int)
    fun keepScreenOnModified(value: Boolean)
    fun weightUnitModified(unit: String)
    fun firstWeekDayModified(day: String)
    fun newsletterGranted()
    fun newsletterDenied()
}

object SettingsFeature {
    lateinit var dependencies: SettingsFeatureDependencies
}

// What the settings screens are opened with. The app reads the same names.
object SettingsExtras {
    const val INTEGRATION = "EXTRA_INTEGRATION"
}

// The names analytics already has for the settings screens
object SettingsScreenNames {
    const val HOME = "screen_settings"
    const val INTEGRATION = "screen_integration"
}
