package com.everlog.ui.activities.home.integration

import com.everlog.data.model.ELIntegration
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewIntegration : BaseActivityMvpView {

    fun onClickSync(): Observable<Void>

    fun onClickUnsync(): Observable<Void>

    fun onClickDisconnect(): Observable<Void>

    fun getItemToEdit(): ELIntegration

    fun showData(integration: ELIntegration)
}