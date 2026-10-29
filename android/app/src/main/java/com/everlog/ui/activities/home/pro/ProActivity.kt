package com.everlog.ui.activities.home.pro

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.data.model.pro.ELProSkuDetails
import com.everlog.databinding.ActivityProBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.ActivityUtils
import com.everlog.utils.text.TextViewUtils
import com.everlog.utils.text.TouchableSpan
import rx.Observable
import rx.subjects.PublishSubject

class ProActivity : BaseActivity(), MvpViewPro {

    private var mPresenter: PresenterPro? = null
    private lateinit var binding: ActivityProBinding

    private var mShadowVisible = false

    private val mOnClickRestore = PublishSubject.create<Void>()
    private val mOnClickTerms = PublishSubject.create<Void>()
    private val mOnClickPrivacy = PublishSubject.create<Void>()

    override fun onActivityCreated() {
        setupTopBar()
        setupListView()
        setupTerms()
        setupScrollView()
        setupBackgroundImage()
        setupFreeTrial()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_pro
    }

    override fun getBindingView(): View? {
        binding = ActivityProBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_PRO
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickRestore(): Observable<Void> {
        return mOnClickRestore
    }

    override fun onClickTerms(): Observable<Void> {
        return mOnClickTerms
    }

    override fun onClickPrivacy(): Observable<Void> {
        return mOnClickPrivacy
    }

    override fun onClickSubMonth(): Observable<ELProSkuDetails> {
        return binding.subMonthBtn.onClickListener
    }

    override fun onClickSubYear(): Observable<ELProSkuDetails> {
        return binding.subYearBtn.onClickListener
    }

    override fun showPurchaseDetails(monthSku: ELProSkuDetails, yearSku: ELProSkuDetails) {
        binding.subMonthBtn.showSku(monthSku)
        binding.subYearBtn.showSku(yearSku)
    }

    override fun showPurchaseDetailsError() {
        binding.subMonthBtn.showError()
        binding.subYearBtn.showError()
    }

    override fun togglePlansLoading(show: Boolean) {
        binding.subMonthBtn.toggleLoading(show)
        binding.subYearBtn.toggleLoading(show)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterPro()
    }

    private fun setupTopBar() {
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupListView() {
        // Features
        val featuresManager = LinearLayoutManager(this)
        binding.proFeaturesList.layoutManager = featuresManager
        binding.proFeaturesList.adapter = mPresenter?.getFeaturesListAdapter()
        // Questions
        val questionsManager = LinearLayoutManager(this)
        binding.proQuestionsList.layoutManager = questionsManager
        binding.proQuestionsList.adapter = mPresenter?.getQuestionsListAdapter()
    }

    private fun setupTerms() {
        // Restore Membership
        val restoreSpan = object : TouchableSpan(binding.restoreLbl.currentTextColor) {
            override fun onClick(widget: View) {
                mOnClickRestore.onNext(null)
            }
        }
        TextViewUtils.addClickableSpans(binding.restoreLbl, arrayOf(restoreSpan), getString(R.string.pro_restore), arrayOf(getString(R.string.pro_restore)))
        // Terms
        val termsSpan = object : TouchableSpan(binding.termsLbl.currentTextColor) {
            override fun onClick(widget: View) {
                mOnClickTerms.onNext(null)
            }
        }
        TextViewUtils.addClickableSpans(binding.termsLbl, arrayOf(termsSpan), getString(R.string.login_terms), arrayOf(getString(R.string.login_terms)))
        // Privacy
        val privacySpan = object : TouchableSpan(binding.privacyLbl.currentTextColor) {
            override fun onClick(widget: View) {
                mOnClickPrivacy.onNext(null)
            }
        }
        TextViewUtils.addClickableSpans(binding.privacyLbl, arrayOf(privacySpan), getString(R.string.login_terms_privacy), arrayOf(getString(R.string.login_terms_privacy)))
    }

    private fun setupScrollView() {
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            mShadowVisible = if (scrollY > binding.headerView.height) {
                if (!mShadowVisible) {
                    binding.shadowView.animate().alpha(1f)
                }
                true
            } else {
                if (mShadowVisible) {
                    binding.shadowView.animate().alpha(0f)
                }
                false
            }
        })
    }

    private fun setupBackgroundImage() {
        ActivityUtils.setupBackgroundImage(this, R.drawable.background_pro, R.id.backgroundView)
    }

    private fun setupFreeTrial() {
        val text = binding.freeTrialLbl.text.toString()
        val h = getString(R.string.pro_free_trial_highlight)
        val i = text.lowercase().indexOf(h.lowercase())
        TextViewUtils.addProFeatureSpan(binding.freeTrialLbl, text, i, i + h.length)
    }
}