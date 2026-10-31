package com.everlog.android.settings.integration

import android.content.Context
import android.content.Intent
import android.view.View
import com.everlog.android.settings.R
import com.everlog.android.settings.SettingsExtras
import com.everlog.android.settings.SettingsScreenNames
import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.android.settings.databinding.ActivityIntegrationBinding
import com.everlog.android.common.mvp.activity.BaseActivity
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.settings.integration.getIcon
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import com.everlog.android.common.design.R as DesignR

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
            intent.putExtra(SettingsExtras.INTEGRATION, properties.integration)
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

    override fun getRequiredExtras(): List<String> {
        return listOf(SettingsExtras.INTEGRATION)
    }

    override fun getAnalyticsScreenName(): String {
        return SettingsScreenNames.INTEGRATION
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    override fun onClickDisconnect(): Observable<Void> {
        return RxView.clicks(binding.disconnectBtn)
    }

    override fun getItemToEdit(): ELIntegration {
        return intent.getSerializableExtra(SettingsExtras.INTEGRATION) as ELIntegration
    }

    override fun showData(integration: ELIntegration) {
        integration.convertedType()?.getIcon()?.let { binding.iconImg.setImageResource(it) }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterIntegration()
    }

    private fun setupTopBar() {
        binding.appBar.toolbar.setNavigationIcon(DesignR.drawable.ic_clear_white)
        setSupportActionBar(binding.appBar.toolbar)
        supportActionBar?.title = getItemToEdit().convertedType()?.getTitle()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}