package com.everlog.managers.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType
import com.everlog.BuildConfig
import com.everlog.application.ELApplication
import com.everlog.ui.dialog.ToastBuilder
import rx.Observable
import rx.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Class to handle InApp Billing requests.
 *
 * Implementation details - https://developer.android.com/google/play/billing/billing_subscriptions
 * Error codes - https://developer.android.com/google/play/billing/billing_reference.html
 * Testing - https://developer.android.com/google/play/billing/billing_testing
 */
class BillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    private val CONNECTION_RETRY_TIMES = 3

    private val SKU_SUB_MONTH = "everlog_pro_sub_month"
    private val SKU_SUB_YEAR = "everlog_pro_sub_year"
    private val SKUS = hashMapOf(SkuType.SUBS to listOf(SKU_SUB_MONTH, SKU_SUB_YEAR))

    companion object {

        const val TAG = "BillingManager"

        val manager = BillingManager(ELApplication.getInstance())
    }

    private val mContextRef = WeakReference(context)
    private var mBillingClient: BillingClient? = null

    private val mPurchasesCompleted = PublishSubject.create<List<Purchase>>()
    private val mPurchaseError = PublishSubject.create<Int>()
    private val mPurchaseAlreadyOwned = PublishSubject.create<Void>()
    private val mPurchaseCancelled = PublishSubject.create<Void>()

    fun destroy() {
        Timber.tag(TAG).i("Destroying")
        mBillingClient?.endConnection()
        mBillingClient = null
    }

    fun onPurchasesCompleted(): Observable<List<Purchase>> {
        return mPurchasesCompleted
    }

    fun onPurchaseError(): Observable<Int> {
        return mPurchaseError
    }

    fun onPurchaseAlreadyOwned(): Observable<Void> {
        return mPurchaseAlreadyOwned
    }

    fun onPurchaseCancelled(): Observable<Void> {
        return mPurchaseCancelled
    }

    val skus: List<String>
        get() = SKUS[SkuType.SUBS]!!

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Timber.tag(TAG).i("Purchases updated")
            // Extract supported purchases
            val supported = extractSupportedCompletePurchases(purchases)
            if (supported.isNotEmpty()) {
                Timber.tag(TAG).i("Purchases supported by app: purchases=%s", supported)
                mPurchasesCompleted.onNext(supported)
            } else {
                Timber.tag(TAG).e("Purchases not supported by app")
                mPurchaseError.onNext(-1)
            }
            // Acknowledge completed purchases with Google asynchronously
            acknowledgePurchases(0, supported)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow
            Timber.tag(TAG).i("User canceled purchase")
            mPurchaseCancelled.onNext(null)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Timber.tag(TAG).i("User already owns item")
            mPurchaseAlreadyOwned.onNext(null)
        } else {
            Timber.tag(TAG).e("Unsupported billing error: code=%s", billingResult.responseCode)
            mPurchaseError.onNext(billingResult.responseCode)
        }
    }

    /**
     * Get purchases details for all the items bought within your app.
     */
    fun getPurchaseHistory(listener: OnPurchaseHistoryListener?) {
        getPurchaseHistory(SkuType.SUBS, listener)
    }

    private fun getPurchaseHistory(@SkuType skuType: String, listener: OnPurchaseHistoryListener?) {
        Timber.tag(TAG).i("Fetching purchase history: type=%s", skuType)
        ensureValidBillingRequest(0, object : OnInAppBillingAvailabilityListener() {
            override fun onBillingAvailable() {
                super.onBillingAvailable()
//                val purchasesResult = mBillingClient?.queryPurchases(skuType)
//                val purchases = purchasesResult?.purchasesList ?: ArrayList()
//                if (purchasesResult?.responseCode == BillingClient.BillingResponseCode.OK) {
//                    Timber.tag(TAG).i("Purchase history retrieved: history=%s", purchases)
//                    listener?.onPurchasesLoaded(purchases)
//                    // Check if any existing purchases still need to be acknowledged
//                    acknowledgePurchases(0, purchases)
//                } else {
//                    Timber.tag(TAG).e("Unsupported purchase history error: code=%s", purchasesResult?.responseCode)
//                    listener?.onBillingNotAvailable(purchasesResult?.responseCode ?: BillingClient.BillingResponseCode.ERROR)
//                }
            }

            override fun onBillingNotAvailable(code: Int) {
                listener?.onBillingNotAvailable(code)
            }
        })
    }

    fun querySkuDetails(@SkuType itemType: String,
                        skuList: List<String>,
                        listener: SkuDetailsResponseListener?) {
        Timber.tag(TAG).i("Querying sku details: type=%s skuList=%s", itemType, skuList)
        ensureValidBillingRequest(0, object : OnInAppBillingAvailabilityListener() {
            override fun onBillingAvailable() {
                super.onBillingAvailable()
                val skuDetailsParams = SkuDetailsParams
                    .newBuilder()
                    .setSkusList(skuList)
                    .setType(itemType)
                    .build()
//                    mBillingClient?.querySkuDetailsAsync(skuDetailsParams) { billingResult: BillingResult, skuDetailsList: List<SkuDetails>? ->
//                        Timber.tag(TAG).i("Query SKU result: code=%s, items=%s", billingResult.responseCode, skuDetailsList)
//                        listener?.onSkuDetailsResponse(billingResult, skuDetailsList ?: ArrayList())
//                    }
            }

            override fun onBillingNotAvailable(code: Int) {
                listener?.onBillingNotAvailable(code)
            }
        })
    }

    fun launchPurchaseFlow(context: Activity, skuDetails: SkuDetails) {
        Timber.tag(TAG).i("Launching purchase flow: sku=%s", skuDetails)
        ensureValidBillingRequest(0, object : OnInAppBillingAvailabilityListener() {
            override fun onBillingAvailable() {
                super.onBillingAvailable()
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build()
                mBillingClient?.launchBillingFlow(context, billingFlowParams)
            }
        })
    }

    // Handlers

    private fun extractSupportedCompletePurchases(purchases: List<Purchase>): List<Purchase> {
        val approved: MutableList<Purchase> = ArrayList()
        for (purchase in purchases) {
            if (purchaseIsSupported(purchase)) {
                approved.add(purchase)
            } else {
                Timber.tag(TAG).e("Purchase not supported by app")
            }
        }
        return approved
    }

    private fun purchaseIsSupported(purchase: Purchase): Boolean {
        return skus.contains(/*purchase.sku*/"")
    }

    private fun acknowledgePurchases(index: Int, purchases: List<Purchase>) {
        if (index < purchases.size) {
            // Get next purchase to acknowledge
            val purchase = purchases[index]
            if (!purchase.isAcknowledged) {
                Timber.tag(TAG).i("Acknowledging purchase: purchase=%s", purchase)
                ensureValidBillingRequest(0, object : OnInAppBillingAvailabilityListener() {
                    override fun onBillingAvailable() {
                        super.onBillingAvailable()
                        val params = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        mBillingClient?.acknowledgePurchase(params) { billingResult: BillingResult ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                Timber.tag(TAG).i("Purchase acknowledged: purchase=%s", purchase)
                            } else {
                                Timber.tag(TAG).i("Failed to acknowledge purchase: purchase=%s", purchase)
                            }
                            acknowledgePurchases(index + 1, purchases)
                        }
                    }
                })
            } else {
                acknowledgePurchases(index + 1, purchases)
            }
        }
    }

    // Connection

    private fun obtainNewBillingClient(): BillingClient {
        return BillingClient
                .newBuilder(mContextRef.get()!!)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener(this)
                .build()
    }

    private fun ensureValidBillingRequest(tries: Int, listener: OnInAppBillingAvailabilityListener?) {
        if (mBillingClient?.isReady == true) {
            listener?.onBillingAvailable()
        } else {
            if (tries < CONNECTION_RETRY_TIMES) {
                mBillingClient = obtainNewBillingClient()
                mBillingClient?.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        val available = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                        if (available) {
                            listener?.onBillingAvailable()
                        } else {
                            Timber.tag(TAG).w("Billing service setup error. Retrying: code=%s, times=%s", billingResult.responseCode, tries+1)
                            ensureValidBillingRequest(tries + 1, listener)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Timber.tag(TAG).w("Billing service disconnected. Retrying: times=%s", tries+1)
                        ensureValidBillingRequest(tries + 1, listener)
                    }
                })
            } else {
                listener?.onBillingNotAvailable(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                destroy()
            }
        }
    }

    open class OnInAppBillingAvailabilityListener {
        open fun onBillingAvailable() {
            Timber.tag(TAG).i("Billing available")
        }

        open fun onBillingNotAvailable(code: Int) {
            val e = "Billing not available on this device: code=$code"
            Timber.tag(TAG).e(e)
            if (code != BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
                ToastBuilder.showToast(ELApplication.getInstance(), e)
            }
        }
    }

    abstract class OnPurchaseHistoryListener : OnInAppBillingAvailabilityListener() {
        abstract fun onPurchasesLoaded(purchases: List<Purchase>)
    }

    abstract class OnPurchasesRestoredListener : OnInAppBillingAvailabilityListener() {
        abstract fun onPurchasesRestored()
    }

    abstract class SkuDetailsResponseListener : OnInAppBillingAvailabilityListener() {
        abstract fun onSkuDetailsResponse(result: BillingResult, skus: List<SkuDetails>)
    }
}