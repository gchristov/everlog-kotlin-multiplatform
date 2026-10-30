package com.everlog.android.pro.data.model

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.everlog.android.common.data.model.ELFirestoreModel
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.common.kotlin.calendarDaysDifference
import com.everlog.android.common.kotlin.timestampWithAddedDays
import java.io.Serializable
import java.util.*

data class ELProSubscription (

        var uuid: String? = null,
        var createdDate: Long = 0,
        var active: Boolean = false,
        var freeTrialEndDate: Long = 0,

        // Billing

        var orderId: String? = null,
        var purchaseTime: Long = 0,
        var purchaseToken: String? = null,
        @PurchaseState
        var purchaseState: Int = PurchaseState.UNSPECIFIED_STATE,
        var signature: String? = null,
        var sku: String? = null,
        var acknowledged: Boolean = false,
        var autoRenewing: Boolean = false

) : Serializable, ELFirestoreModel {

    companion object {

        @JvmStatic
        fun getDefault(): ELProSubscription {
            val subscription = ELProSubscription()
            subscription.uuid = UUID.randomUUID().toString()
            subscription.createdDate = Date().time
            subscription.active = false
            subscription.freeTrialEndDate = 0
            return subscription
        }
    }

    override fun documentId(): String {
        return uuid!!
    }

    override fun asMap(): MutableMap<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        map[FirestoreFields.UUID] = uuid
        map[FirestoreFields.CREATED_DATE] = createdDate
        map["active"] = active
        map["freeTrialEndDate"] = freeTrialEndDate
        map["orderId"] = orderId
        map["purchaseTime"] = purchaseTime
        map["purchaseToken"] = purchaseToken
        map["purchaseState"] = purchaseState
        map["signature"] = signature
        map["sku"] = sku
        map["acknowledged"] = acknowledged
        map["autoRenewing"] = autoRenewing
        return map
    }

    fun makeActive(purchase: Purchase, skuDetails: ELProSkuDetails?) {
        if (freeTrialEndDate <= 0 && (skuDetails?.freeTrialDays() ?: 0) > 0) {
            // Only set free trial once per user
            freeTrialEndDate = Date().timestampWithAddedDays(skuDetails?.freeTrialDays() ?: 0)
        }
        active = true
        orderId = purchase.orderId
        purchaseTime = purchase.purchaseTime
        purchaseToken = purchase.purchaseToken
        purchaseState = purchase.purchaseState
        signature = purchase.signature
        sku = /*purchase.sku*/ ""
        acknowledged = purchase.isAcknowledged
        autoRenewing = purchase.isAutoRenewing
    }

    fun makeInactive() {
        active = false
        orderId = null
        purchaseTime = 0L
        purchaseToken = null
        purchaseState = PurchaseState.UNSPECIFIED_STATE
        signature = null
        sku = null
        acknowledged = false
        autoRenewing = false
    }

    fun isPro(): Boolean {
        return isProWithinFreeTrial() || isProPurchased()
    }

    fun isProWithinFreeTrial(): Boolean {
        return proFreeTrialDaysRemaining() >= 0
    }

    fun proFreeTrialDaysRemaining(): Int {
        return Date().calendarDaysDifference(Date(freeTrialEndDate))
    }

    private fun isProPurchased(): Boolean {
        return active
                && orderId != null
                && purchaseTime > 0
                && purchaseToken != null
                && signature != null
                && sku != null
    }
}