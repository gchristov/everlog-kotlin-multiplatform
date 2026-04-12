package com.everlog.ui.activities.home.web

import com.everlog.ui.activities.base.BaseActivityPresenter

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