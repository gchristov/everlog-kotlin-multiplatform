package com.everlog.ui.fragments.home.exercise

import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.ui.fragments.base.BaseFragmentMvpView

interface BaseMvpViewExerciseTab : BaseFragmentMvpView {

    fun getExercise(): ELExercise

    fun getStats(): ExerciseStatsController.StatsResult?

    fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?)
}