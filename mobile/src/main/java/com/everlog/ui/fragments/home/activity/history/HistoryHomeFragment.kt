package com.everlog.ui.fragments.home.activity.history

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.everlog.R
import com.everlog.config.AppConfig
import com.everlog.databinding.FragmentHistoryHomeBinding
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.base.BaseActivity
import com.everlog.ui.adapters.CalendarAdapter
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.base.BaseTabFragment
import com.everlog.utils.toJava
import com.everlog.utils.toLocalDate
import org.threeten.bp.LocalDate
import rx.Observable
import rx.subjects.PublishSubject
import java.time.YearMonth
import java.util.*

class HistoryHomeFragment : BaseTabFragment(), MvpViewHistoryHome {

    enum class ViewType {
        CALENDAR,
        LIST,
    }

    private var mPresenter: PresenterHistoryHome? = null
    private var _binding: FragmentHistoryHomeBinding? = null
    private val binding get() = _binding!!

    private var mCalendarSetup = false

    private val mOnViewTypeChanged = PublishSubject.create<ViewType>()
    private val mDateSelected = PublishSubject.create<LocalDate>()

    override fun onFragmentCreated() {
        setupSpinner()
        setupListView()
        setupCalendar()
    }

    override fun onResume() {
        super.onResume()
        // Calendar is full setup the first time it becomes visible
        mCalendarSetup = true
    }

    override fun getLayoutResId(): Int {
        return R.layout.fragment_history_home
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View? {
        _binding = FragmentHistoryHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_HOME_HISTORY
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun getTitleResId(): Int {
        return R.string.history_title
    }

    override fun onViewTypeChanged(): Observable<ViewType> {
        return mOnViewTypeChanged
    }

    override fun onDateSelected(): Observable<LocalDate> {
        return mDateSelected
    }

    override fun refreshCalendar(minDate: Date?) {
        val now = YearMonth.now()
        // Calendar start
        var firstMonth = now.minusYears(1)
        if (minDate != null) {
            val localDate = minDate.toLocalDate()
            firstMonth = YearMonth.of(localDate.year, localDate.month.value)
        }
        // Calendar end
        val lastMonth = now.plusMonths(1)
        // Remember previous position before resetting
        val currMonth = if (mCalendarSetup) binding.calendarView.findFirstVisibleMonth() else null
        binding.calendarView.setup(firstMonth, lastMonth, SettingsManager.manager.firstDayOfWeek().toJava())
        binding.calendarView.post {
            if (currMonth != null) {
                // Apply latest position
                binding.calendarView.scrollToMonth(YearMonth.of(currMonth.year, currMonth.month))
            } else {
                // If no position, scroll to today
                binding.calendarView.scrollToMonth(YearMonth.now())
            }
        }
    }

    override fun toggleLoadingOverlay(show: Boolean) {
        BaseActivity.toggleShimmerLayout(binding.listView.findViewById(R.id.shimmerView), show, true)
    }

    override fun toggleEmptyState(visible: Boolean) {
        binding.emptyView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setViewType(type: ViewType) {
        binding.calendarView.visibility = if (type == ViewType.CALENDAR) View.VISIBLE else View.GONE
        binding.listView.visibility = if (type == ViewType.LIST) View.VISIBLE else View.GONE
        if (ViewType.values().indexOf(type) != binding.viewTypeSpinner.selectedItemPosition) {
            // Update the spinner it was changed programmatically
            binding.viewTypeSpinner.setSelection(ViewType.values().indexOf(type))
        }
    }

    override fun scrollToDay(index: Int) {
        binding.recyclerView.post {
            binding.recyclerView.smoothScrollToPosition(index)
        }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterHistoryHome()
    }

    private fun setupListView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = mPresenter?.getListAdapter()
    }

    private fun setupCalendar() {
        val adapter = CalendarAdapter()
        binding.calendarView.monthHeaderBinder = adapter.buildMonth(CalendarAdapter.MonthBuilder()
                .calendar(binding.calendarView))
        binding.calendarView.dayBinder = adapter.buildDay(CalendarAdapter.DayBuilder()
                .calendar(binding.calendarView).listener(object : CalendarAdapter.OnCalendarListener {
                    override fun onDateSelected(date: LocalDate) {
                        mDateSelected.onNext(date)
                    }

                    override fun hasEvents(date: LocalDate): Boolean {
                        return mPresenter?.containsDate(date) == true
                    }
                }))
        refreshCalendar(null)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.history_view_type_titles, R.layout.view_spinner_row_statistics)
        adapter.setDropDownViewResource(R.layout.view_spinner_row_dropdown)
        binding.viewTypeSpinner.background?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.main_accent), PorterDuff.Mode.SRC_ATOP)
        binding.viewTypeSpinner.adapter = adapter
        binding.viewTypeSpinner.setSelection(ViewType.values().indexOf(AppConfig.configuration.defaultHistoryViewType))
        binding.viewTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                mOnViewTypeChanged.onNext(ViewType.values()[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}