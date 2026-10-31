package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.settings.SetSuggestionText
import com.everlog.android.settings.SettingsAnalytics
import com.everlog.android.settings.SettingsFeatureDependencies
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.android.workout.setsuggestions.SetSuggestion
import com.everlog.config.FeedbackSource
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.auth.AuthManager
import com.everlog.managers.integrations.GoogleFitIntegrationManager
import com.everlog.ui.dialog.AppDialogs
import com.everlog.ui.navigator.ELNavigator
import com.everlog.utils.device.DeviceInfo
import rx.Observable

class AppSettingsFeatureDependencies : SettingsFeatureDependencies {

    override fun settingsAnalytics(): SettingsAnalytics = object : SettingsAnalytics {
        private val analytics = AnalyticsManager.manager

        override fun weightIncreaseModified(value: Float) = analytics.settingsWeightModified(value)

        override fun weeklyGoalModified(value: Int) = analytics.settingsWeeklyGoalModified(value)

        override fun keepScreenOnModified(value: Boolean) = analytics.settingsKeepScreenOnModified(value)

        override fun weightUnitModified(unit: String) = analytics.settingsWeightUnitModified(unit)

        override fun firstWeekDayModified(day: String) = analytics.settingsFirstWeekDayModified(day)

        override fun newsletterGranted() = analytics.consentNewsletterGranted()

        override fun newsletterDenied() = analytics.consentNewsletterDenied()
    }

    override fun openSetSuggestions(context: Context) = ELNavigator(context).openMuscleGoal()

    override fun setSuggestionsV2(): Boolean = RemoteConfigManager.manager.setSuggestionsV2()

    override fun setSuggestionText(context: Context, goal: MuscleGoal): SetSuggestionText? =
        SetSuggestion.of(goal)?.let { SetSuggestionText(context.getString(it.title), it.subtitle(context)) }

    override fun openPlayStoreAppDetails(context: Context) = ELNavigator(context).openPlayStoreAppDetails()

    override fun hasSettingsFeedback(): Boolean = RemoteConfigManager.manager.feedbackFormUrl(FeedbackSource.SETTINGS) != null

    override fun openSettingsFeedback(context: Context) = ELNavigator(context).openFeedbackForm(FeedbackSource.SETTINGS)

    override fun shareApp(context: Context) = ELNavigator(context).shareApp()

    override fun openProBuyOrManage(context: Context) = ELNavigator(context).openProBuyOrManage()

    override fun openDesignSystem(context: Context) = ELNavigator(context).openDesignSystem()

    override fun openLogin(context: Context) = ELNavigator(context).openLogin()

    override fun logout(onLoggedOut: () -> Unit) = AuthManager.logout(object : AuthManager.OnAuthActionListener() {
        override fun onLogout() = onLoggedOut()
    })

    override fun appInfo(): String = DeviceInfo.appInfo

    override fun showWeightIncreasePrompt(context: Context, value: Float): Observable<Float> =
        AppDialogs.showWeightIncreaseDialog(context, value)

    override fun showWeeklyGoalPrompt(context: Context, value: Int): Observable<Int> =
        AppDialogs.showPickerNumberDialog(context, value, AppDialogs.NumberPickerDialogType.WEEKLY_GOAL)

    override fun connectGoogleFit(view: BaseFragmentMvpView, onGranted: (ELIntegration) -> Unit) =
        GoogleFitIntegrationManager.connect(view, object : GoogleFitIntegrationManager.OnGoogleFitAccessListener {
            override fun onGranted(integration: ELIntegration) = onGranted(integration)
        })

    override fun googleFitActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        GoogleFitIntegrationManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun disconnectGoogleFit(integration: ELIntegration) = GoogleFitIntegrationManager.disconnect(integration)
}
