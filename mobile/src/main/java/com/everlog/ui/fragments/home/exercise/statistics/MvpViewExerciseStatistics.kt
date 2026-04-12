package com.everlog.ui.fragments.home.exercise.statistics

import com.everlog.ui.fragments.home.exercise.BaseMvpViewExerciseTab
import rx.Observable

interface MvpViewExerciseStatistics : BaseMvpViewExerciseTab {

    fun onClickFooter(): Observable<Void>
}