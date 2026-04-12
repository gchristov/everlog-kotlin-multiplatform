package com.everlog.ui.fragments.home.exercise

import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.ui.fragments.base.BaseFragmentPresenter

abstract class BasePresenterExerciseTab<T : BaseMvpViewExerciseTab> : BaseFragmentPresenter<T>() {

    internal var exercise: ELExercise? = null
    internal var stats: ExerciseStatsController.StatsResult? = null

    override fun onReady() {
        setupEditedItem()
        loadData()
    }

    // Loading

    internal open fun loadData() {
        mvpView?.loadExerciseDetails(exercise!!, stats)
    }

    // Setup

    private fun setupEditedItem() {
        exercise = mvpView?.getExercise()
        stats = mvpView?.getStats()
    }
}