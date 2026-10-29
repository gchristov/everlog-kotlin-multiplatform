package com.everlog.ui.activities.home.routine

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewRoutinePicker : BaseActivityMvpView {

    fun onClickAddRoutine(): Observable<Void>

    fun onClickEmptyAction(): Observable<Void>

    fun toggleEmptyState(visible: Boolean)
}