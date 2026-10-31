package com.everlog.android.template.feature.picker

import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewRoutinePicker : BaseActivityMvpView {

    fun onClickAddRoutine(): Observable<Void>

    fun onClickEmptyAction(): Observable<Void>

    fun toggleEmptyState(visible: Boolean)
}