package com.everlog.ui.activities.home.web

import com.everlog.android.common.mvp.activity.BaseActivityMvpView

interface MvpViewWeb : BaseActivityMvpView {

    fun getWebUrl(): String

    fun getWebTitle(): String

    fun loadWebsite(url: String)
}