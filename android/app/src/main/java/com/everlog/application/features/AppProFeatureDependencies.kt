package com.everlog.application.features

import android.content.Context
import com.android.billingclient.api.Purchase
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.plan.congratulate.CongratulateActivity
import com.everlog.android.pro.ProAnalytics
import com.everlog.android.pro.ProFeatureDependencies
import com.everlog.android.pro.billing.BillingManager
import com.everlog.android.pro.data.model.ELProSkuDetails
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.billing.BillingBridge
import com.everlog.ui.navigator.ELNavigator

class AppProFeatureDependencies : ProFeatureDependencies {

    override fun proAnalytics(): ProAnalytics = object : ProAnalytics {
        private val analytics = AnalyticsManager.manager

        override fun buyMonth() = analytics.proBuyMonth()

        override fun buyYear() = analytics.proBuyYear()

        override fun purchased() = analytics.proBuyPurchased()

        override fun cancelled() = analytics.proBuyCancelled()

        override fun alreadyOwned() = analytics.proBuyAlreadyOwned()

        override fun restore() = analytics.proBuyRestore()
    }

    override fun isUserPro(): Boolean = LocalUserManager.getUser()?.isPro() == true

    override fun processPurchaseCompleted(purchases: List<Purchase>, skuDetails: ELProSkuDetails?) =
        BillingBridge.processPurchaseCompleted(purchases, skuDetails)

    override fun restorePurchases(listener: BillingManager.OnPurchasesRestoredListener?) = BillingBridge.restoreUserPurchases(listener)

    override fun openTerms(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_TERMS, title)

    override fun openPrivacyPolicy(context: Context, title: String) = ELNavigator(context).openWebView(ELConstants.URL_PRIVACY, title)

    override fun openCongratulate(context: Context) = ELNavigator(context).openCongratulate(CongratulateActivity.Type.PRO)
}
