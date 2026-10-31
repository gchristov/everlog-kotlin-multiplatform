package com.everlog.android.exercise.details

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.details.statistics.ExerciseStatsController
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView

interface BaseMvpViewExerciseTab : BaseFragmentMvpView {

    fun getExercise(): ELExercise

    fun getStats(): ExerciseStatsController.StatsResult?

    fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?)
}