package com.everlog.ui.fragments.home.exercise.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.everlog.R
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.databinding.FragmentExerciseInfoLegacyBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.fragments.home.exercise.BaseExerciseTabFragment
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab

// The Info tab as it was before ExerciseInfoFragment: only the image. Shown while exercise_details_v2 is off;
// remove it with the flag.
class LegacyExerciseInfoFragment(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseInfo {

    private var mPresenter: PresenterExerciseInfo? = null
    private var _binding: FragmentExerciseInfoLegacyBinding? = null
    private val binding get() = _binding!!

    override fun onFragmentCreated() {
        // A large image, so it's rounded like a card
        binding.exerciseImg.applyMask(R.drawable.mask_rounded_large)
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_exercise_info_legacy
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentExerciseInfoLegacyBinding.inflate(inflater, container, false)
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

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        binding.exerciseImg.setExercise(exercise)
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseInfo()
    }
}
