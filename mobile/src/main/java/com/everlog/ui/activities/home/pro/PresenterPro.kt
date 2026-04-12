package com.everlog.ui.activities.home.pro

import android.content.DialogInterface
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.pro.ELProFeature
import com.everlog.data.model.pro.ELProQuestion
import com.everlog.data.model.pro.ELProSkuDetails
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.billing.BillingBridge
import com.everlog.managers.billing.BillingManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.home.congratulate.CongratulateActivity
import com.everlog.ui.adapters.ProUpgradeAdapter
import com.everlog.ui.navigator.Navigator
import timber.log.Timber

class PresenterPro : BaseActivityPresenter<MvpViewPro>() {

    private val TAG = "PresenterPro"

    private var mSelectedSku: ELProSkuDetails? = null

    // Features

    private lateinit var mFeaturesAdapter: RecyclerAdapter
    private var mFeaturesDataListManager: DataListManager<ELProFeature>? = null

    // Questions

    private lateinit var mQuestionsAdapter: RecyclerAdapter
    private var mQuestionsDataListManager: DataListManager<ELProQuestion>? = null

    override fun init() {
        super.init()
        setupListViews()
    }

    override fun onReady() {
        observeSubMonthClick()
        observeSubYearClick()
        observeRestore()
        observeTerms()
        observePrivacy()
        observePurchasesCompletedOrAlreadyOwned()
        observePurchaseError()
        observePurchaseCancelled()
        loadData()
    }

    fun getFeaturesListAdapter(): RecyclerAdapter? {
        return mFeaturesAdapter
    }

    fun getQuestionsListAdapter(): RecyclerAdapter? {
        return mQuestionsAdapter
    }

    // Observers

    private fun observePurchasesCompletedOrAlreadyOwned() {
        subscriptions.add(BillingManager.manager.onPurchasesCompleted()
                .compose(applyUISchedulers())
                .subscribe({purchases ->
                    handlePurchasesCompleted(purchases)
                }, { throwable -> handleError(throwable) }))
        subscriptions.add(BillingManager.manager.onPurchaseAlreadyOwned()
                .compose(applyUISchedulers())
                .subscribe({
                    handlePurchaseAlreadyOwned()
                }, { throwable -> handleError(throwable) }))
    }

    private fun observePurchaseCancelled() {
        subscriptions.add(BillingManager.manager.onPurchaseCancelled()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.proBuyCancelled()
                }, { throwable -> handleError(throwable) }))
    }

    private fun observePurchaseError() {
        subscriptions.add(BillingManager.manager.onPurchaseError()
                .compose(applyUISchedulers())
                .subscribe({
                    handlePurchaseError()
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeSubMonthClick() {
        subscriptions.add(mvpView.onClickSubMonth()
                .compose(applyUISchedulers())
                .subscribe({sku ->
                    AnalyticsManager.manager.proBuyMonth()
                    handleStartPurchaseFlow(sku)
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeSubYearClick() {
        subscriptions.add(mvpView.onClickSubYear()
                .compose(applyUISchedulers())
                .subscribe({sku ->
                    AnalyticsManager.manager.proBuyYear()
                    handleStartPurchaseFlow(sku)
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeRestore() {
        subscriptions.add(mvpView.onClickRestore()
                .compose(applyUISchedulers())
                .subscribe({
                    handleRestoreMembership()
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeTerms() {
        subscriptions.add(mvpView.onClickTerms()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openWebView(ELConstants.URL_TERMS, mvpView.context.getString(R.string.login_terms))
                }, { throwable -> handleError(throwable) }))
    }

    private fun observePrivacy() {
        subscriptions.add(mvpView.onClickPrivacy()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openWebView(ELConstants.URL_PRIVACY, mvpView.context.getString(R.string.login_terms_privacy))
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeRestoreNoMembershipFoundPrompt() {
        subscriptions.add(mvpView.showPrompt(R.string.pro_title, R.string.pro_error_restore, R.string.settings_contact_us, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        navigator.sendEmail(Navigator.ContactType.RESTORE_PRO, null)
                    }
                }, { throwable -> handleError(throwable) }))
    }

    // Loading

    private fun loadData() {
        mvpView.togglePlansLoading(true)
        loadFeatures()
        loadQuestions()
        loadProducts()
    }

    private fun loadFeatures() {
        val items = ArrayList<ELProFeature>()
        var feature = ELProFeature(R.string.pro_feature_1, null, true)
        items.add(feature)
        feature = ELProFeature(R.string.pro_feature_2, null, true)
        items.add(feature)
        feature = ELProFeature(R.string.pro_feature_3, null, true)
        items.add(feature)
        feature = ELProFeature(R.string.pro_feature_4, null, false)
        items.add(feature)
        feature = ELProFeature(R.string.pro_feature_5, null, false)
        items.add(feature)
        feature = ELProFeature(R.string.pro_feature_6, null, false)
        items.add(feature)
        mFeaturesDataListManager?.addAll(items)
    }

    private fun loadQuestions() {
        val items = ArrayList<ELProQuestion>()
        var question = ELProQuestion(R.string.pro_question_1, R.string.pro_question_1_answer)
        items.add(question)
        question = ELProQuestion(R.string.pro_question_2, R.string.pro_question_2_answer)
        items.add(question)
        question = ELProQuestion(R.string.pro_question_3, R.string.pro_question_3_answer)
        items.add(question)
        question = ELProQuestion(R.string.pro_question_4, R.string.pro_question_4_answer)
        items.add(question)
        mQuestionsDataListManager?.addAll(items)
    }

    private fun loadProducts() {
        BillingManager.manager.querySkuDetails(BillingClient.SkuType.SUBS,
                BillingManager.manager.skus,
                object : BillingManager.SkuDetailsResponseListener() {
            override fun onSkuDetailsResponse(result: BillingResult, skus: List<SkuDetails>) {
                handlePurchasesLoaded(result, skus)
            }

            override fun onBillingNotAvailable(code: Int) {
                super.onBillingNotAvailable(code)
                showPurchaseInfo(null, null)
            }
        })
    }

    // Handlers

    private fun handleRestoreMembership() {
        AnalyticsManager.manager.proBuyRestore()
        mvpView.toggleLoadingOverlay(true)
        BillingBridge.restoreUserPurchases(object : BillingManager.OnPurchasesRestoredListener() {
            override fun onPurchasesRestored() {
                if (isAttachedToView) {
                    mvpView?.toggleLoadingOverlay(false)
                    if (userAccount?.isPro() == true) {
                        mvpView?.showLongToast(R.string.pro_restore_success)
                        mvpView?.closeScreen()
                    } else {
                        observeRestoreNoMembershipFoundPrompt()
                    }
                }
            }

            override fun onBillingNotAvailable(code: Int) {
                super.onBillingNotAvailable(code)
                mvpView?.toggleLoadingOverlay(false)
            }
        })
    }

    private fun handlePurchasesLoaded(result: BillingResult, skuDetailsList: List<SkuDetails>?) {
        if (isAttachedToView) {
            val skus = BillingBridge.convertSkus(result, skuDetailsList)
            val monthSku = if (skus.isNotEmpty()) skus[0] else null
            val yearSku = if (skus.isNotEmpty()) skus[1] else null
            Timber.tag(TAG).i("Sku details: month=%s, year=%s", monthSku, yearSku)
            mvpView?.togglePlansLoading(false)
            showPurchaseInfo(monthSku, yearSku)
        }
    }

    private fun showPurchaseInfo(monthSku: ELProSkuDetails?, yearSku: ELProSkuDetails?) {
        if (monthSku == null || yearSku == null) {
            Timber.tag(TAG).e("Could not load purchase info. Missing either month or year subscription")
            mvpView?.showPurchaseDetailsError()
        } else {
            Timber.tag(TAG).i("Purchase info loaded")
            mvpView?.showPurchaseDetails(monthSku, yearSku)
        }
    }

    private fun handleStartPurchaseFlow(sku : ELProSkuDetails) {
        mSelectedSku = sku
        BillingManager.manager.launchPurchaseFlow(mvpView.getActivity()!!, sku.skuDetails)
    }

    private fun handlePurchaseError() {
        if (isAttachedToView) {
            mvpView.showLongToast(R.string.pro_error_purchase)
        }
    }

    private fun handlePurchaseAlreadyOwned() {
        if (isAttachedToView) {
            AnalyticsManager.manager.proBuyAlreadyOwned()
            sendBroadcast(Intent(ELConstants.BROADCAST_PRO_CHANGED))
            mvpView.closeScreen()
        }
    }

    private fun handlePurchasesCompleted(purchases: List<Purchase>) {
        if (isAttachedToView) {
            AnalyticsManager.manager.proBuyPurchased()
            BillingBridge.processPurchaseCompleted(purchases, mSelectedSku)
            if (mSelectedSku != null) {
                purchases.forEach {
//                    Qonversion.instance?.purchase(mSelectedSku!!.skuDetails, it)
                }
            }
            navigator.openCongratulate(CongratulateActivity.Type.PRO)
            mvpView?.closeScreen()
        }
    }

    // Setup

    private fun setupListViews() {
        // Features
        mFeaturesAdapter = RecyclerAdapter()
        mFeaturesDataListManager = DataListManager(mFeaturesAdapter)
        mFeaturesAdapter.addDataManager(mFeaturesDataListManager)
        mFeaturesAdapter.registerBinder(ProUpgradeAdapter.FeatureBinder())
        // Questions
        mQuestionsAdapter = RecyclerAdapter()
        mQuestionsDataListManager = DataListManager(mQuestionsAdapter)
        mQuestionsAdapter.addDataManager(mQuestionsDataListManager)
        mQuestionsAdapter.registerBinder(ProUpgradeAdapter.QuestionBinder())
    }
}