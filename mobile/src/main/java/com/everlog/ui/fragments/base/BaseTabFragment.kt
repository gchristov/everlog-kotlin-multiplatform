package com.everlog.ui.fragments.base

import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.fragments.base.BaseFragment

abstract class BaseTabFragment : BaseFragment() {

    abstract fun getAnalyticsScreenName(): String

    abstract fun getTitleResId(): Int

    override fun onResume() {
        super.onResume()
        AnalyticsManager.manager.screenName(getParentActivity(), getAnalyticsScreenName())
    }
}