package com.everlog.android.exercise.feature.details.statistics

import com.everlog.android.exercise.feature.details.BaseMvpViewExerciseTab
import rx.Observable

interface MvpViewExerciseStatistics : BaseMvpViewExerciseTab {

    fun onClickFooter(): Observable<Void>

    fun showFeedback(visible: Boolean)
}