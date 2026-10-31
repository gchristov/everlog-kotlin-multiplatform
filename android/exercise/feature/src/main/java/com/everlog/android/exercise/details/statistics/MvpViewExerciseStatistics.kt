package com.everlog.android.exercise.details.statistics

import com.everlog.android.exercise.details.BaseMvpViewExerciseTab
import rx.Observable

interface MvpViewExerciseStatistics : BaseMvpViewExerciseTab {

    fun onClickFooter(): Observable<Void>

    fun showFeedback(visible: Boolean)
}