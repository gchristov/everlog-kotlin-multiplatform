package com.everlog.ui.activities.home.musclegoal

import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewMuscleGoal : BaseActivityMvpView {

    fun onClickFooter(): Observable<Void>

    fun showData()
}