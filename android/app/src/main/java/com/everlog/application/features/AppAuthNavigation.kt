package com.everlog.application.features

import android.app.Activity
import android.content.Context
import com.everlog.android.auth.AuthNavigation
import com.everlog.constants.ELConstants
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppAuthNavigation : AuthNavigation {
    override fun openHome(activity: Activity) = ELNavigator(activity).openHome()

    override fun openOnboarding(activity: Activity) = ELNavigator(activity).openOnboarding()

    override fun openTerms(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_TERMS, title)

    override fun openPrivacyPolicy(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_PRIVACY, title)
}
