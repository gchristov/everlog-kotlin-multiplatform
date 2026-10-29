package com.everlog.ui.views.notification.warning

import com.everlog.ui.views.base.BaseViewMvpView
import rx.Observable

interface MvpViewWarningNotification : BaseViewMvpView {

    fun onClickClose(): Observable<Void>

    fun onClickAction(): Observable<Void>

    fun getType(): WarningNotificationView.WarningType

    fun hideNotification()
}