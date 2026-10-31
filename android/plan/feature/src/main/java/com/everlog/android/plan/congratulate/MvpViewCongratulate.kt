package com.everlog.android.plan.congratulate

import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewCongratulate : BaseActivityMvpView {

    fun onClickAction(): Observable<Void>
}