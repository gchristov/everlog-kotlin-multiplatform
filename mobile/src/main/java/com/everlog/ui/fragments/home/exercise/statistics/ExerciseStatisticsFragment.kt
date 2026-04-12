package com.everlog.ui.fragments.home.exercise.statistics

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.data.controllers.statistics.BaseStatsController
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.databinding.FragmentExerciseStatisticsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.ui.fragments.home.activity.statistics.charts.ChartRenderer
import com.everlog.ui.fragments.home.exercise.BaseExerciseTabFragment
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab
import com.everlog.utils.format.StatsFormatUtils
import com.everlog.utils.makeSingleLine
import com.github.mikephil.charting.components.YAxis
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject

class ExerciseStatisticsFragment(exercise: ELExercise,
                                 stats: ExerciseStatsController.StatsResult?,
                                 private val range: StatisticsHomeFragment.RangeType) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseStatistics {

    private var mPresenter: PresenterExerciseStatistics? = null
    private var _binding: FragmentExerciseStatisticsBinding? = null
    private val binding get() = _binding!!

    private var mChartRenderer: ChartRenderer? = null

    companion object {

        val onRangeChanged: PublishSubject<StatisticsHomeFragment.RangeType> = PublishSubject.create()
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
        return AnalyticsConstants.SCREEN_EXERCISE_STATISTICS
    }

    override fun getTabPresenter(): BasePresenterExerciseTab<*>? {
        return mPresenter
    }

    override fun getTitleResId(): Int {
        return R.string.statistics_title
    }

    override fun onClickFooter(): Observable<Void> {
        return RxView.clicks(binding.footerView)
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
        BaseActivity.toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
    }

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        renderRange(range)
        renderSummaryViews(stats)
        renderCharts(stats)
        binding.scrollView.scrollTo(0, 0)
    }

    private fun renderSummaryViews(stats: ExerciseStatsController.StatsResult?) {
        if (stats == null) return
        binding.timesPerformedSummary.setSummary(StatsFormatUtils.formatNumberStatsLabel(stats.timesPerformed), null, requireContext().getString(R.string.exercise_statistics_times_performed).lowercase().makeSingleLine())
        binding.totalWeightSummary.setSummary(StatsFormatUtils.formatWeightStatsLabel(stats.totalWeight), SettingsManager.weightUnitAbbreviation(), requireContext().getString(R.string.home_week_weight_lifted).lowercase().makeSingleLine())
        binding.totalRepsSummary.setSummary(StatsFormatUtils.formatNumberStatsLabel(stats.totalReps), null, requireContext().getString(R.string.exercise_statistics_total_reps).lowercase().makeSingleLine())
        binding.maxWeightSummary.setSummary(StatsFormatUtils.formatWeightStatsLabel(stats.heaviestSet?.getWeight() ?: 0f), SettingsManager.weightUnitAbbreviation(), requireContext().getString(R.string.workout_details_max_weight).lowercase().makeSingleLine())
    }

    private fun renderCharts(stats: ExerciseStatsController.StatsResult?) {
        if (stats == null) return
        // Rep/weight
        val repsDataDescriptor = ChartRenderer.ChartDataDescriptor(stats.repCounts)
        repsDataDescriptor.title = "Reps"
        val weightDataDescriptor = ChartRenderer.ChartDataDescriptor(stats.weightCounts)
        weightDataDescriptor.axisDependency = YAxis.AxisDependency.RIGHT
        weightDataDescriptor.colorResId = R.color.pie_chart_4
        weightDataDescriptor.title = "Weight"
        binding.repWeightAxisRightTitleLbl.text = String.format("%s (%s)", getString(R.string.weight), SettingsManager.weightUnitAbbreviation())
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

    private fun renderRange(rangeType: StatisticsHomeFragment.RangeType) {
        binding.rangeSpinner.onItemSelectedListener = null
        binding.rangeSpinner.setSelection(StatisticsHomeFragment.RangeType.values().indexOf(rangeType))
        binding.rangeSpinner.post {
            binding.rangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    onRangeChanged.onNext(StatisticsHomeFragment.RangeType.values()[position])
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
        val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.statistics_durations_titles, R.layout.view_spinner_row_statistics)
        adapter.setDropDownViewResource(R.layout.view_spinner_row_dropdown)
        binding.rangeSpinner.background?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.main_accent), PorterDuff.Mode.SRC_ATOP)
        binding.rangeSpinner.adapter = adapter
    }
}