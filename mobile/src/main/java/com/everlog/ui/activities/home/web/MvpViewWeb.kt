package com.everlog.ui.activities.home.web

import com.everlog.ui.activities.base.BaseActivityMvpView

interface MvpViewWeb : BaseActivityMvpView {

    fun getWebUrl(): String

    fun getWebTitle(): String

    fun loadWebsite(url: String)
}