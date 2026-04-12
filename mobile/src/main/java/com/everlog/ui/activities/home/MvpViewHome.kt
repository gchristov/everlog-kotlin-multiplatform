package com.everlog.ui.activities.home

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewHome : BaseActivityMvpView {

    fun onClickAdd(): Observable<Void>

    fun showWeek()
}