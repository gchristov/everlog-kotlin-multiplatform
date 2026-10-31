package com.everlog.android.exercise.feature.details.statistics

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.everlog.android.exercise.feature.R
import com.everlog.android.exercise.feature.databinding.FragmentExerciseStatisticsBinding
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.kotlin.format.StatsFormatUtils
import com.everlog.android.common.kotlin.makeSingleLine
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.common.mvp.chart.BaseStatsController
import com.everlog.android.common.mvp.activity.BaseActivity
import com.everlog.android.common.mvp.chart.ChartRenderer
import com.everlog.android.exercise.feature.ExerciseScreenNames
import com.everlog.android.exercise.feature.details.BaseExerciseTabFragment
import com.everlog.android.exercise.feature.details.BasePresenterExerciseTab
import com.everlog.android.exercise.feature.details.statistics.ExerciseStatsController
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.github.mikephil.charting.components.YAxis
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.mvp.R as MvpR

class ExerciseStatisticsFragment(exercise: ELExercise,
                                 stats: ExerciseStatsController.StatsResult?,
                                 private var range: RangeType) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseStatistics {

    private var mPresenter: PresenterExerciseStatistics? = null
    private var _binding: FragmentExerciseStatisticsBinding? = null
    private val binding get() = _binding!!

    private var mChartRenderer: ChartRenderer? = null

    companion object {

        val onRangeChanged: PublishSubject<RangeType> = PublishSubject.create()
    }

    override fun onFragmentCreated() {
        setupCharts()
        setupRangeSpinner()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_exercise_statistics
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentExerciseStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getAnalyticsScreenName(): String {
        return ExerciseScreenNames.STATISTICS
    }

    override fun getTabPresenter(): BasePresenterExerciseTab<*>? {
        return mPresenter
    }

    override fun getTitleResId(): Int {
        return DesignR.string.statistics_title
    }

    override fun onClickFooter(): Observable<Void> {
        return RxView.clicks(binding.footerView)
    }

    override fun showFeedback(visible: Boolean) {
        binding.footerView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
        BaseActivity.toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
    }

    // The range picked in the spinner, set before the update with its stats
    fun updateRange(range: RangeType) {
        this.range = range
    }

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        // Updates keep the scroll position, so new stats don't jump back to the top
        renderRange(range)
        renderSummaryViews(stats)
        renderCharts(stats)
    }

    private fun renderSummaryViews(stats: ExerciseStatsController.StatsResult?) {
        if (stats == null) return
        binding.timesPerformedSummary.setSummary(StatsFormatUtils.formatNumberStatsLabel(stats.timesPerformed), null, requireContext().getString(R.string.exercise_statistics_times_performed).lowercase().makeSingleLine())
        binding.totalWeightSummary.setSummary(StatsFormatUtils.formatWeightStatsLabel(stats.totalWeight), weightUnitAbbreviation(SettingsManager.manager.weightUnit()), requireContext().getString(DesignR.string.home_week_weight_lifted).lowercase().makeSingleLine())
        binding.totalRepsSummary.setSummary(StatsFormatUtils.formatNumberStatsLabel(stats.totalReps), null, requireContext().getString(R.string.exercise_statistics_total_reps).lowercase().makeSingleLine())
        binding.maxWeightSummary.setSummary(StatsFormatUtils.formatWeightStatsLabel(stats.heaviestSet?.getWeight(SettingsManager.manager.weightUnit()) ?: 0f), weightUnitAbbreviation(SettingsManager.manager.weightUnit()), requireContext().getString(DesignR.string.workout_details_max_weight).lowercase().makeSingleLine())
    }

    private fun renderCharts(stats: ExerciseStatsController.StatsResult?) {
        if (stats == null) return
        // Rep/weight
        val repsDataDescriptor = ChartRenderer.ChartDataDescriptor(stats.repCounts)
        repsDataDescriptor.title = "Reps"
        val weightDataDescriptor = ChartRenderer.ChartDataDescriptor(stats.weightCounts)
        weightDataDescriptor.axisDependency = YAxis.AxisDependency.RIGHT
        weightDataDescriptor.colorResId = DesignR.color.pie_chart_4
        weightDataDescriptor.title = "Weight"
        binding.repWeightAxisRightTitleLbl.text = String.format("%s (%s)", getString(DesignR.string.weight), weightUnitAbbreviation(SettingsManager.manager.weightUnit()))
        mChartRenderer?.renderLineChart(binding.repWeightChart,
                binding.repWeightAxisLeftTitle,
                binding.repWeightAxisRightTitle,
                BaseStatsController.chartGranularity(),
                BaseStatsController.chartAxisFormatter(range),
                arrayOf(repsDataDescriptor, weightDataDescriptor))
        // 1RM
        binding.ormAxisLeftTitleLbl.text = binding.repWeightAxisRightTitleLbl.text
        mChartRenderer?.renderLineChart(binding.ormChart,
                binding.ormAxisLeftTitle,
                null,
                BaseStatsController.chartGranularity(),
                BaseStatsController.chartAxisFormatter(range),
                arrayOf(ChartRenderer.ChartDataDescriptor(stats.ormCounts)))
    }

    private fun renderRange(rangeType: RangeType) {
        binding.rangeSpinner.onItemSelectedListener = null
        binding.rangeSpinner.setSelection(RangeType.values().indexOf(rangeType))
        binding.rangeSpinner.post {
            // The fragment's view may have been destroyed by the time this runs
            if (_binding == null) return@post
            binding.rangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    onRangeChanged.onNext(RangeType.values()[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseStatistics()
    }

    private fun setupCharts() {
        mChartRenderer = ChartRenderer(requireContext())
    }

    private fun setupRangeSpinner() {
        val adapter = ArrayAdapter.createFromResource(requireContext(), DesignR.array.statistics_durations_titles, MvpR.layout.view_spinner_row_statistics)
        adapter.setDropDownViewResource(MvpR.layout.view_spinner_row_dropdown)
        binding.rangeSpinner.background?.setColorFilter(ContextCompat.getColor(requireContext(), DesignR.color.main_accent), PorterDuff.Mode.SRC_ATOP)
        binding.rangeSpinner.adapter = adapter
    }
}