package com.everlog.ui.activities.home.exercise.details

import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import rx.Observable

interface MvpViewExerciseDetails : BaseActivityMvpView {

    fun onClickEdit(): Observable<Void>

    fun onClickDelete(): Observable<Void>

    fun getExercise(): ELExercise

    fun getType(): ExerciseDetailsActivity.Companion.Type

    fun loadExerciseDetails(exercise: ELExercise,
                            stats: ExerciseStatsController.StatsResult?,
                            range: StatisticsHomeFragment.RangeType)
}