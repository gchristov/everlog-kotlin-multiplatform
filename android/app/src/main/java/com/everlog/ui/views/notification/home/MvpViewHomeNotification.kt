package com.everlog.ui.views.notification.home

import com.everlog.config.HomeNotification
import com.everlog.android.common.mvp.view.BaseViewMvpView
import rx.Observable

interface MvpViewHomeNotification : BaseViewMvpView {

    fun onClickClose(): Observable<Void>

    fun onClickAction(): Observable<Void>

    fun getNotification(): HomeNotification?

    fun hideNotification()

    fun showPlans()

    fun showSettings()
}
