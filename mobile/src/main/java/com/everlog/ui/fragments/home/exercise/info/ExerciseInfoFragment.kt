package com.everlog.ui.fragments.home.exercise.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.R
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.databinding.FragmentExerciseInfoBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.fragments.home.exercise.BaseExerciseTabFragment
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab
import com.jakewharton.rxbinding.view.RxView
import rx.Observable

class ExerciseInfoFragment(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseInfo {

    private var mPresenter: PresenterExerciseInfo? = null
    private var _binding: FragmentExerciseInfoBinding? = null
    private val binding get() = _binding!!

    override fun onFragmentCreated() {
        // No-op
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_exercise_info
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentExerciseInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_EXERCISE_INFO
    }

    override fun getTabPresenter(): BasePresenterExerciseTab<*>? {
        return mPresenter
    }

    override fun getTitleResId(): Int {
        return R.string.exercise_info_title
    }

    override fun onClickYoutube(): Observable<Void> {
        return RxView.clicks(binding.youtubeBtn)
    }

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        binding.exerciseImg.setExercise(exercise)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseInfo()
    }
}