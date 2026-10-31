package com.everlog.android.settings.integration

import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewIntegration : BaseActivityMvpView {

    fun onClickDisconnect(): Observable<Void>

    fun getItemToEdit(): ELIntegration

    fun showData(integration: ELIntegration)
}