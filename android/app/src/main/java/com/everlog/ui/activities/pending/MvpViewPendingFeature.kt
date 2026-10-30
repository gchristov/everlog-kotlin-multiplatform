package com.everlog.ui.activities.pending

import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewPendingFeature : BaseActivityMvpView {

    fun onClickEmptyAction(): Observable<Void>
}