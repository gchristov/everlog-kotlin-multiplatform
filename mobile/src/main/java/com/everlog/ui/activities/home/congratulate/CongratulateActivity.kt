package com.everlog.ui.activities.home.congratulate

import android.content.Context
import android.content.Intent
import android.view.View
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.databinding.ActivityCongratulateBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.ViewUtils
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class CongratulateActivity : BaseActivity(), MvpViewCongratulate {

    enum class Type {
        PRO,
        PLAN_FINISH
    }

    private var mPresenter: PresenterCongratulate? = null
    private lateinit var binding: ActivityCongratulateBinding

    private var mType: Type? = null

    companion object {

        class Properties {
            var type: Type? = null
                private set
            fun type(type: Type) = apply { this.type = type }
        }

        @JvmStatic
        fun launchIntent(context: Context, properties: Properties): Intent {
            val intent = Intent(context, CongratulateActivity::class.java)
            intent.putExtra(ELConstants.EXTRA_TYPE, properties.type)
            return intent
        }
    }

    override fun onActivityCreated() {
        mType = intent?.extras?.getSerializable(ELConstants.EXTRA_TYPE) as? Type
        setData()
        revealViews()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_congratulate
    }

    override fun getBindingView(): View? {
        binding = ActivityCongratulateBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_CONGRATULATE
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickAction(): Observable<Void> {
        return RxView.clicks(binding.closeBtn)
    }

    private fun setData() {
        when (mType) {
            Type.PRO -> {
                binding.titleLbl.setText(R.string.pro_confirm_title)
                binding.subtitleLbl.setText(R.string.pro_confirm_subtitle)
            }
            Type.PLAN_FINISH -> {
                binding.titleLbl.setText(R.string.plan_details_complete_title)
                binding.subtitleLbl.setText(R.string.plan_details_complete_subtitle)
            }

            null -> TODO()
        }
    }

    private fun revealViews() {
        val yMove = ViewUtils.dpToPx(30).toFloat()
        binding.coverImage.translationY = yMove
        binding.contentPanel.translationY = yMove
        binding.coverImage.animate()
                .setStartDelay(300)
                .setDuration(300)
                .alpha(1f)
                .translationYBy(-yMove)
        binding.contentPanel.animate()
                .setStartDelay(400)
                .setDuration(300)
                .alpha(1f)
                .translationYBy(-yMove)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterCongratulate()
    }
}