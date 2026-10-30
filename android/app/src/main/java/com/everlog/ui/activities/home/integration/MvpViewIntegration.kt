package com.everlog.ui.activities.home.integration

import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewIntegration : BaseActivityMvpView {

    fun onClickDisconnect(): Observable<Void>

    fun getItemToEdit(): ELIntegration

    fun showData(integration: ELIntegration)
}