package com.everlog.application.features

import android.content.Context
import com.everlog.android.settings.SettingsNavigation
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.managers.RemoteConfigManager
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppSettingsNavigation : SettingsNavigation {
    override fun openSetSuggestions(context: Context) = ELNavigator(context).openMuscleGoal()

    override fun openPlayStoreAppDetails(context: Context) = ELNavigator(context).openPlayStoreAppDetails()

    override fun hasSettingsFeedback(): Boolean = RemoteConfigManager.manager.feedbackFormUrl(FeedbackSource.SETTINGS) != null

    override fun openSettingsFeedback(context: Context) = ELNavigator(context).openFeedbackForm(FeedbackSource.SETTINGS)

    override fun shareApp(context: Context) = ELNavigator(context).shareApp()

    override fun openProBuyOrManage(context: Context) = ELNavigator(context).openProBuyOrManage()

    override fun openDesignSystem(context: Context) = ELNavigator(context).openDesignSystem()

    override fun openLogin(context: Context) = ELNavigator(context).openLogin()
}
