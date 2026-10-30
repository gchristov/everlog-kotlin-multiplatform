package com.everlog.android.exercise.feature.details

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.feature.details.statistics.ExerciseStatsController
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter

abstract class BasePresenterExerciseTab<T : BaseMvpViewExerciseTab> : BaseFragmentPresenter<T>() {

    internal var exercise: ELExercise? = null
    internal var stats: ExerciseStatsController.StatsResult? = null

    override fun onReady() {
        setupEditedItem()
        loadData()
    }

    // The tab's exercise or stats changed, see BaseExerciseTabFragment.update
    fun update(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        this.exercise = exercise
        this.stats = stats
        if (isAttachedToView) {
            loadData()
        }
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