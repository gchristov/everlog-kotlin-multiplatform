package com.everlog.managers.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.everlog.data.model.pro.ELProSkuDetails
import com.everlog.data.model.pro.ELProSubscription
import com.everlog.managers.auth.AuthManager
import com.everlog.managers.auth.LocalUserManager
import timber.log.Timber

class BillingBridge {

    companion object {

        fun processPurchaseCompleted(purchases: List<Purchase>, skuDetails: ELProSkuDetails? = null) {
            Timber.tag(BillingManager.TAG).i("Processing completed purchase")
            var shouldSaveUser = false
            // Obtain current subscription
            val user = LocalUserManager.getUser()
            var subscription = user?.subscription
            if (subscription == null) {
                shouldSaveUser = true
                subscription = ELProSubscription.getDefault()
            }
            if (purchases.isEmpty()) {
                if (subscription.active) {
                    // Subscription has changed, so we should save the user
                    shouldSaveUser = true
                }
                subscription.makeInactive()
            } else {
                if (!subscription.active) {
                    // Subscription has changed, so we should save the user
                    shouldSaveUser = true
                }
                // Only handle the first purchase for now
                subscription.makeActive(purchases.first(), skuDetails)
            }
            Timber.tag(BillingManager.TAG).i("Subscription validity: valid=%s", subscription.isPro())
            if (shouldSaveUser) {
                // Only save if changes have happened to prevent accidental local overrides
                user?.subscription = subscription
                AuthManager.saveUser(user!!)
            }
        }

        fun restoreUserPurchases(listener: BillingManager.OnPurchasesRestoredListener?) {
            Timber.tag(BillingManager.TAG).i("Restoring purchases")
            BillingManager.manager.getPurchaseHistory(object : BillingManager.OnPurchaseHistoryListener() {
                override fun onPurchasesLoaded(purchases: List<Purchase>) {
                    Timber.tag(BillingManager.TAG).i("Purchases restored")
                    processPurchaseCompleted(purchases)
                    listener?.onPurchasesRestored()
                }

                override fun onBillingNotAvailable(code: Int) {
                    listener?.onBillingNotAvailable(code)
                }
            })
        }

        fun convertSkus(result: BillingResult, skuDetailsList: List<SkuDetails>?): List<ELProSkuDetails> {
            val results = ArrayList<ELProSkuDetails>()
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                skuDetailsList?.forEach {
                    results.add(ELProSkuDetails.withSkuDetails(it))
                }
            }
            return results
        }
    }
}