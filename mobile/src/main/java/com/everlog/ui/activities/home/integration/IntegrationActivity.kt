package com.everlog.ui.activities.home.integration

import android.content.Context
import android.content.Intent
import android.view.View
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.model.ELIntegration
import com.everlog.databinding.ActivityIntegrationBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class IntegrationActivity : BaseActivity(), MvpViewIntegration {

    private var mPresenter: PresenterIntegration? = null
    private lateinit var binding: ActivityIntegrationBinding

    companion object {

        class Properties {
            var integration: ELIntegration? = null
                private set
            fun integration(integration: ELIntegration) = apply { this.integration = integration }
        }

        @JvmStatic
        fun launchIntent(context: Context, properties: Properties): Intent {
            val intent = Intent(context, IntegrationActivity::class.java)
            intent.putExtra(ELConstants.EXTRA_INTEGRATION, properties.integration)
            return intent
        }
    }

    override fun onActivityCreated() {
        setupTopBar()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_integration
    }

    override fun getBindingView(): View? {
        binding = ActivityIntegrationBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_INTEGRATION
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickSync(): Observable<Void> {
        return RxView.clicks(binding.syncBtn)
    }

    override fun onClickUnsync(): Observable<Void> {
        return RxView.clicks(binding.unsyncBtn)
    }

    override fun onClickDisconnect(): Observable<Void> {
        return RxView.clicks(binding.disconnectBtn)
    }

    override fun getItemToEdit(): ELIntegration {
        return intent.getSerializableExtra(ELConstants.EXTRA_INTEGRATION) as ELIntegration
    }

    override fun showData(integration: ELIntegration) {
        integration.convertedType()?.getIcon()?.let { binding.iconImg.setImageResource(it) }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterIntegration()
    }

    private fun setupTopBar() {
        binding.appBar.toolbar.setNavigationIcon(R.drawable.ic_clear_white)
        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.title = getItemToEdit().convertedType()?.getTitle()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}