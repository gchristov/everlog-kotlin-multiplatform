package com.everlog.ui.fragments.home.exercise

import android.os.Bundle
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment

abstract class BaseExerciseTabFragment(exercise: ELExercise, private var stats: ExerciseStatsController.StatsResult?) : BaseTabFragment(), BaseMvpViewExerciseTab {

    abstract fun getTabPresenter(): BasePresenterExerciseTab<*>?

    init {
        val bundle = Bundle()
        bundle.putSerializable(ELConstants.EXTRA_EXERCISE, exercise)
        // TODO: Stats is not Serializable so cannot be passed here. Think of alternatives
        arguments = bundle
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return getTabPresenter() as? BaseFragmentPresenter<T>
    }

    override fun getExercise(): ELExercise {
        return arguments?.getSerializable(ELConstants.EXTRA_EXERCISE) as ELExercise
    }

    override fun getStats(): ExerciseStatsController.StatsResult? {
        return stats
    }

    /**
     * The exercise or its stats changed (the exercise store answering, stats loading, the range
     * changing). ExerciseDetailsActivity keeps its tabs and passes changes here, so each tab keeps its
     * state and scroll position instead of being replaced. A tab without a view yet reads them when
     * it's created.
     */
    fun update(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        arguments?.putSerializable(ELConstants.EXTRA_EXERCISE, exercise)
        this.stats = stats
        if (view != null) {
            getTabPresenter()?.update(exercise, stats)
        }
    }
}