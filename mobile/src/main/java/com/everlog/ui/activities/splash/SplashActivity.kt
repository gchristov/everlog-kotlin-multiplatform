package com.everlog.ui.activities.splash

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.everlog.R
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.activities.base.BaseActivityPresenter

class SplashActivity : BaseActivity(), MvpViewSplash {

    private var mPresenter: PresenterSplash? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            mPresenter?.isReady == false
        }
    }

    public override fun onActivityCreated() {
        // No-op
    }

    public override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_SPLASH
    }

    public override fun getLayoutResId(): Int {
        return 0
    }

    override fun <T : BaseActivityMvpView> getPresenter(): BaseActivityPresenter<T>? {
        return mPresenter as? BaseActivityPresenter<T>
    }

    // Setup

    public override fun setupPresenter() {
        mPresenter = PresenterSplash()
    }
}