package com.everlog.ui.fragments.home.exercise

import android.os.Bundle
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment

abstract class BaseExerciseTabFragment(exercise: ELExercise, private val stats: ExerciseStatsController.StatsResult?) : BaseTabFragment(), BaseMvpViewExerciseTab {

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
}