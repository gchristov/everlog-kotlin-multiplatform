package com.everlog.application.features

import android.content.Context
import com.everlog.android.plan.congratulate.CongratulateActivity
import com.everlog.android.pro.ProNavigation
import com.everlog.constants.ELConstants
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppProNavigation : ProNavigation {
    override fun openTerms(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_TERMS, title)

    override fun openPrivacyPolicy(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_PRIVACY, title)

    override fun openCongratulate(context: Context) = ELNavigator(context).openCongratulate(CongratulateActivity.Type.PRO)
}
