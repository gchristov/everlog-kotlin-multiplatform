package com.everlog.ui.activities.home.web

import com.everlog.android.common.mvp.activity.BaseActivityPresenter

class PresenterWebView : BaseActivityPresenter<MvpViewWeb>() {

    override fun onReady() {
        loadWebData()
    }

    // Loading

    private fun loadWebData() {
        val url = mvpView.getWebUrl()
        mvpView.loadWebsite(url)
    }
}