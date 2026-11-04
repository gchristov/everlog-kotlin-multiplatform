package com.everlog.android.home.banner

import com.everlog.android.home.data.model.HomeNotification
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
