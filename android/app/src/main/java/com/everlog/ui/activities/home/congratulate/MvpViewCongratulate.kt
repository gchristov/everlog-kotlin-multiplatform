package com.everlog.ui.activities.home.congratulate

import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewCongratulate : BaseActivityMvpView {

    fun onClickAction(): Observable<Void>
}