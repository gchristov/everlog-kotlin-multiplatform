package com.everlog.ui.activities.home.congratulate

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewCongratulate : BaseActivityMvpView {

    fun onClickAction(): Observable<Void>
}