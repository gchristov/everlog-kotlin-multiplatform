package com.everlog.android.plan.create

import com.everlog.android.common.mvp.view.BaseViewMvpView
import rx.Observable

interface MvpViewWarningNotification : BaseViewMvpView {

    fun onClickClose(): Observable<Void>

    fun onClickAction(): Observable<Void>

    fun getType(): WarningNotificationView.WarningType

    fun hideNotification()
}