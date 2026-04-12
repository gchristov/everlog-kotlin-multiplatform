package com.everlog.ui.fragments.home.exercise.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.everlog.R
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.databinding.FragmentExerciseInfoBinding
import com.everlog.databinding.FragmentExerciseStatisticsBinding
import com.everlog.databinding.FragmentHistoryExerciseBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.fragments.home.exercise.BaseExerciseTabFragment
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab
import com.facebook.shimmer.ShimmerFrameLayout

class ExerciseHistoryFragment(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseHistory {

    private var mPresenter: PresenterExerciseHistory? = null
    private var _binding: FragmentHistoryExerciseBinding? = null
    private val binding get() = _binding!!

    override fun onFragmentCreated() {
        setupListView()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_history_exercise
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHistoryExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_EXERCISE_HISTORY
    }

    override fun getTabPresenter(): BasePresenterExerciseTab<*>? {
        return mPresenter
    }

    override fun getTitleResId(): Int {
        return R.string.history_title
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        BaseActivity.toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
        if (show) {
            binding.emptyView.visibility = View.GONE
        }
    }

    override fun toggleEmptyState(visible: Boolean) {
        binding.emptyView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        // No-op
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseHistory()
    }

    private fun setupListView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }
}