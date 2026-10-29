package com.everlog.ui.fragments.home.workouts

import com.everlog.ui.fragments.base.BaseFragmentMvpView
import rx.Observable

interface MvpViewWorkoutsHome : BaseFragmentMvpView {

    fun onClickExercises(): Observable<Void>

    // Plans

    fun onClickAddPlanTop(): Observable<Void>

    fun togglePlanCreateTop(visible: Boolean)

    fun toggleLoadingOverlayPlans(show: Boolean)

    // Routines

    fun onClickAddRoutineTop(): Observable<Void>

    fun toggleRoutineCreateTop(visible: Boolean)

    fun toggleLoadingOverlayRoutines(show: Boolean)
}