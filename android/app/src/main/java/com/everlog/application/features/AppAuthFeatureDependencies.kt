package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.auth.AuthFeatureDependencies
import com.everlog.android.auth.data.AuthActionListener
import com.everlog.android.settings.data.SettingsManager
import com.everlog.constants.ELConstants
import com.everlog.managers.auth.AuthManager
import com.everlog.ui.navigator.ELNavigator

class AppAuthFeatureDependencies : AuthFeatureDependencies {

    override fun login(email: String, password: String, listener: AuthActionListener) = AuthManager.login(email, password, listener)

    override fun loginAnonymously(listener: AuthActionListener) = AuthManager.loginAnonymously(listener)

    override fun loginWithGoogle(activity: Activity, listener: AuthActionListener) = AuthManager.loginWithGoogle(ELNavigator(activity), listener)

    override fun authActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AuthManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun register(fullName: String, email: String, password: String, newsletter: Boolean, listener: AuthActionListener) =
        AuthManager.register(fullName, email, password, newsletter, listener)

    override fun requestPasswordReset(email: String, listener: AuthActionListener) = AuthManager.requestPasswordReset(email, listener)

    override fun onboardingPending(): Boolean = SettingsManager.manager.onboardingPending()

    override fun setOnboardingPending(pending: Boolean) = SettingsManager.manager.setOnboardingPending(pending)

    override fun openHome(activity: Activity) = ELNavigator(activity).openHome()

    override fun openOnboarding(activity: Activity) = ELNavigator(activity).openOnboarding()

    override fun openTerms(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_TERMS, title)

    override fun openPrivacyPolicy(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_PRIVACY, title)

    override fun cancelAppUseReminder(context: Context) = ELNavigator(context).cancelAppUseNotification()
}
