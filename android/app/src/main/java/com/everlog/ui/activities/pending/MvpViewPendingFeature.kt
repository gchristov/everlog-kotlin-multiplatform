package com.everlog.ui.activities.pending

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewPendingFeature : BaseActivityMvpView {

    fun onClickEmptyAction(): Observable<Void>
}