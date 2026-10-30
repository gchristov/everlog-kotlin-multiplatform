package com.everlog.ui.activities.home.exercise.details

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewExerciseDetails : BaseActivityMvpView {

    fun onClickEdit(): Observable<Void>

    fun onClickDelete(): Observable<Void>

    fun getExercise(): ELExercise

    fun getType(): ExerciseDetailsActivity.Companion.Type

    fun loadExerciseDetails(exercise: ELExercise,
                            stats: ExerciseStatsController.StatsResult?,
                            range: RangeType)
}