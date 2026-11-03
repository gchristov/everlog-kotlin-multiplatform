package com.everlog.application.features

import com.android.billingclient.api.Purchase
import com.everlog.android.auth.data.UserSession
import com.everlog.android.pro.ProPurchases
import com.everlog.android.pro.billing.BillingManager
import com.everlog.android.pro.data.model.ELProSkuDetails
import com.everlog.managers.billing.BillingBridge
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppProPurchases(private val session: UserSession) : ProPurchases {
    override fun isUserPro(): Boolean = session.user()?.isPro() == true

    override fun processPurchaseCompleted(purchases: List<Purchase>, skuDetails: ELProSkuDetails?) =
        BillingBridge.processPurchaseCompleted(purchases, skuDetails)

    override fun restorePurchases(listener: BillingManager.OnPurchasesRestoredListener?) = BillingBridge.restoreUserPurchases(listener)
}
