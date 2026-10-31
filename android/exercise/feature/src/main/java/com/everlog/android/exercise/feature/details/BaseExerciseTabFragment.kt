package com.everlog.android.exercise.feature.details

import android.os.Bundle
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.exercise.feature.ExerciseExtras
import com.everlog.android.exercise.feature.details.statistics.ExerciseStatsController

abstract class BaseExerciseTabFragment(exercise: ELExercise, private var stats: ExerciseStatsController.StatsResult?) : BaseTabFragment(), BaseMvpViewExerciseTab {

    abstract fun getTabPresenter(): BasePresenterExerciseTab<*>?

    init {
        val bundle = Bundle()
        bundle.putSerializable(ExerciseExtras.EXERCISE, exercise)
        // TODO: Stats is not Serializable so cannot be passed here. Think of alternatives
        arguments = bundle
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return getTabPresenter() as? BaseFragmentPresenter<T>
    }

    override fun getExercise(): ELExercise {
        return arguments?.getSerializable(ExerciseExtras.EXERCISE) as ELExercise
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
        arguments?.putSerializable(ExerciseExtras.EXERCISE, exercise)
        this.stats = stats
        if (view != null) {
            getTabPresenter()?.update(exercise, stats)
        }
    }
}