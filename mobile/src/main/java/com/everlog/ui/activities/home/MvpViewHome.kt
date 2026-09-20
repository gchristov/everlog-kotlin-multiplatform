package com.everlog.ui.activities.home

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewHome : BaseActivityMvpView {

    fun onClickAddFab(): Observable<Void>

    fun onClickAddWeekEmptyState(): Observable<Void>

    fun showWeek()

    fun toggleStartWorkoutButton(visible: Boolean)
}