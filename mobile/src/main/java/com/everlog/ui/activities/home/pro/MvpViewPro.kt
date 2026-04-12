package com.everlog.ui.activities.home.pro

import com.everlog.data.model.pro.ELProSkuDetails
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewPro : BaseActivityMvpView {

    fun onClickRestore(): Observable<Void>

    fun onClickTerms(): Observable<Void>

    fun onClickPrivacy(): Observable<Void>

    fun onClickSubMonth(): Observable<ELProSkuDetails>

    fun onClickSubYear(): Observable<ELProSkuDetails>

    fun showPurchaseDetails(monthSku: ELProSkuDetails, yearSku: ELProSkuDetails)

    fun showPurchaseDetailsError()

    fun togglePlansLoading(show: Boolean)
}