package com.everlog.ui.fragments.home.activity.statistics

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.everlog.R
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.kotlin.format.StatsFormatUtils
import com.everlog.android.common.kotlin.makeSingleLine
import com.everlog.android.settings.data.SettingsManager
import com.everlog.config.AppConfig
import com.everlog.data.controllers.statistics.BaseStatsController
import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.databinding.FragmentHomeStatisticsBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.android.common.mvp.activity.BaseActivity
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.common.mvp.chart.ChartRenderer
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.jakewharton.rxbinding.view.RxView
import rx.Observable
import rx.subjects.PublishSubject
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.R as KotlinR

class StatisticsHomeFragment : BaseTabFragment(), MvpViewStatisticsHome {

    private var mPresenter: PresenterStatisticsHome? = null
    private var _binding: FragmentHomeStatisticsBinding? = null
    private val binding get() = _binding!!

    private var mChartRenderer: ChartRenderer? = null

    private val mOnRangeChanged = PublishSubject.create<RangeType>()

    override fun onFragmentCreated() {
        setupSpinner()
        setupCharts()
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_home_statistics
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHomeStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME_STATISTICS
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun getTitleResId(): Int {
        return R.string.statistics_title
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
        BaseActivity.toggleShimmerLayout(binding.root.findViewById(R.id.shimmerView), show, true)
    }

    override fun onClickFooter(): Observable<Void> {
        return RxView.clicks(binding.footerView)
    }

    override fun showFeedback(visible: Boolean) {
        binding.footerView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onRangeChanged(): Observable<RangeType> {
        return mOnRangeChanged
    }

    override fun setStatsData(stats: UserStatsController.StatsResult) {
        renderSummaryViews(stats)
        renderCharts(stats)
        binding.scrollView.scrollTo(0, 0)
    }

    private fun renderSummaryViews(stats: UserStatsController.StatsResult) {
        binding.workoutsCompletedSummary.setSummary(StatsFormatUtils.formatNumberStatsLabel(stats.workoutsCompleted), null, requireContext().getString(R.string.home_week_workouts_completed).lowercase().makeSingleLine())
        binding.totalWeightSummary.setSummary(StatsFormatUtils.formatWeightStatsLabel(stats.totalWeightLifted), weightUnitAbbreviation(SettingsManager.manager.weightUnit()), requireContext().getString(R.string.home_week_weight_lifted).lowercase().makeSingleLine())
        binding.totalTimeSummary.setSummary(StatsFormatUtils.formatTimeStatsLabel(stats.totalWorkoutTimeMillis), requireContext().getString(KotlinR.string.hour), requireContext().getString(R.string.home_week_total_time).lowercase().makeSingleLine())
        binding.averageTimeSummary.setSummary(StatsFormatUtils.formatTimeStatsLabel(stats.averageSessionTimeMillis), requireContext().getString(KotlinR.string.hour), requireContext().getString(R.string.home_week_average_time).lowercase().makeSingleLine())
        binding.longestSessionSummary.setSummary(StatsFormatUtils.formatTimeStatsLabel(stats.longestSessionTimeMillis), requireContext().getString(KotlinR.string.hour), requireContext().getString(R.string.statistics_longest_session).lowercase().makeSingleLine())
        binding.maxWeightSummary.setSummary(StatsFormatUtils.formatWeightStatsLabel(stats.maxWeightLifted), weightUnitAbbreviation(SettingsManager.manager.weightUnit()), requireContext().getString(R.string.workout_details_max_weight).lowercase().makeSingleLine())
    }

    private fun renderCharts(stats: UserStatsController.StatsResult) {
        // Muscles trained
        binding.musclesTrainedChartTitle.text = getString(R.string.statistics_muscles_trained).makeSingleLine()
        mChartRenderer?.renderPieChart(binding.musclesTrainedChart, stats.categoryCounts)
        // Volume lifted
        binding.weightLiftedChartTitle.text = getString(R.string.home_week_weight_lifted).makeSingleLine()
        binding.weightLiftedAxisLeftTitleLbl.text = String.format("%s (%s)", getString(R.string.weight), weightUnitAbbreviation(SettingsManager.manager.weightUnit()))
        mChartRenderer?.renderBarChart(binding.weightLiftedChart,
                binding.weightLiftedAxisLeftTitle,
                null,
                BaseStatsController.chartGranularity(),
                BaseStatsController.chartAxisFormatter(stats.range),
                arrayOf(ChartRenderer.ChartDataDescriptor(stats.weightLiftedCounts)))
        // Session duration
        when (stats.range) {
            RangeType.YEAR,
            RangeType.OVERALL -> binding.durationChartTitle.setText(R.string.statistics_session_duration_avg)
            else -> binding.durationChartTitle.setText(R.string.statistics_session_duration)
        }
        mChartRenderer?.renderLineChart(binding.durationChart,
                binding.durationAxisLeftTitle,
                null,
                BaseStatsController.chartGranularity(),
                BaseStatsController.chartAxisFormatter(stats.range),
                arrayOf(ChartRenderer.ChartDataDescriptor(stats.durationCounts)))
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterStatisticsHome()
    }

    private fun setupCharts() {
        mChartRenderer = ChartRenderer(requireContext())
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.statistics_durations_titles, R.layout.view_spinner_row_statistics)
        adapter.setDropDownViewResource(R.layout.view_spinner_row_dropdown)
        binding.rangeSpinner.background?.setColorFilter(ContextCompat.getColor(requireContext(), DesignR.color.main_accent), PorterDuff.Mode.SRC_ATOP)
        binding.rangeSpinner.adapter = adapter
        binding.rangeSpinner.setSelection(RangeType.values().indexOf(AppConfig.configuration.defaultStatsRange))
        binding.rangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mOnRangeChanged.onNext(RangeType.values()[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}