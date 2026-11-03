package com.everlog.android.pro

import com.android.billingclient.api.Purchase
import com.everlog.android.pro.billing.BillingManager
import com.everlog.android.pro.data.model.ELProSkuDetails

// The user's Pro subscription, which the app keeps on the user
interface ProPurchases {
    fun isUserPro(): Boolean

    fun processPurchaseCompleted(purchases: List<Purchase>, skuDetails: ELProSkuDetails?)

    fun restorePurchases(listener: BillingManager.OnPurchasesRestoredListener?)
}
