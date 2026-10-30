package com.everlog.android.common.mvp.fragment

import com.everlog.android.common.mvp.CommonMvp

abstract class BaseTabFragment : BaseFragment() {

    abstract fun getAnalyticsScreenName(): String

    abstract fun getTitleResId(): Int

    override fun onResume() {
        super.onResume()
        CommonMvp.dependencies.screenViewed(getAnalyticsScreenName())
    }
}