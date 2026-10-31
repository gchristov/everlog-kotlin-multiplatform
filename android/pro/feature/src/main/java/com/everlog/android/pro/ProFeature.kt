package com.everlog.android.pro

import android.content.Context
import com.android.billingclient.api.Purchase
import com.everlog.android.pro.billing.BillingManager
import com.everlog.android.pro.data.model.ELProSkuDetails

/**
 * What the Pro feature needs from the app: services built on the app's managers, and the screens of other features,
 * which open through the app's Navigator. The app registers its implementation in [ProFeature] at startup. Shaped
 * like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface ProFeatureDependencies {
    fun proAnalytics(): ProAnalytics

    // Whether the signed-in user has Pro, once purchases are restored
    fun isUserPro(): Boolean

    // Saves the subscription [purchases] give to the signed-in user
    fun processPurchaseCompleted(purchases: List<Purchase>, skuDetails: ELProSkuDetails?)

    fun restorePurchases(listener: BillingManager.OnPurchasesRestoredListener?)

    fun openTerms(context: Context, title: String)

    fun openPrivacyPolicy(context: Context, title: String)

    // The thank-you screen after buying Pro
    fun openCongratulate(context: Context)
}

interface ProAnalytics {
    fun buyMonth()
    fun buyYear()
    fun purchased()
    fun cancelled()
    fun alreadyOwned()
    fun restore()
}

object ProFeature {
    lateinit var dependencies: ProFeatureDependencies
}

// The names analytics already has for the Pro screens
object ProScreenNames {
    const val PRO = "screen_pro"
}
