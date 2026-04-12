package com.everlog.ui.fragments.home.activity.history

import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.history.ELUserWorkoutsStore
import com.everlog.data.model.util.HistoryCalendarContainer
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.adapters.history.BaseTimelineViewHolder
import com.everlog.ui.adapters.history.HistoryAdapter
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.utils.toLocalDate
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import rx.Observable
import java.util.Date
import java.util.concurrent.TimeUnit

class PresenterHistoryHome : BaseFragmentPresenter<MvpViewHistoryHome>() {

    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<ELWorkout>(mAdapter)
    private var mCalendarContainer: HistoryCalendarContainer? = null

    companion object {

        private val DATE_FORMAT_KEY = DateTimeFormatter.ofPattern("dd/LL/yyyy")
        private val MONTH_FORMAT_KEY = DateTimeFormatter.ofPattern("LL/yyyy")

        fun buildDateDayKey(date: LocalDate?): String {
            return DATE_FORMAT_KEY.format(date)
        }

        fun buildDateMonthKey(date: LocalDate?): String {
            return MONTH_FORMAT_KEY.format(date)
        }
    }

    override fun init() {
        super.init()
        setupListView()
    }

    override fun onReady() {
        observeViewTypeChanged()
        observeDateSelected()
        loadHistory()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoryLoaded(event: ELUserWorkoutsStore.ELColStoreWorkoutsLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                mvpView?.toggleLoadingOverlay(false)
                handleCheckEmptyState()
                handleError(event.error)
            } else {
                handleHistoryReady(event.items)
            }
        }
    }

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        mAdapter.notifyDataSetChanged()
        mvpView?.refreshCalendar(findMinHistoryDate())
    }

    fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    fun containsDate(date: LocalDate): Boolean {
        return mCalendarContainer?.containsDate(date) == true
    }

    // Observers

    private fun observeViewTypeChanged() {
        subscriptions.add(mvpView.onViewTypeChanged()
                .compose(applyUISchedulers())
                .subscribe { mvpView?.setViewType(it) })
    }

    private fun observeDateSelected() {
        subscriptions.add(mvpView.onDateSelected()
                .delay(200, TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe {
                    // Find position of day
                    subscriptions.add(findPositionForDate(it)
                            .compose(applySchedulers())
                            .subscribe({ index ->
                                if (index >= 0) {
                                    // Switch to list
                                    mvpView?.setViewType(HistoryHomeFragment.ViewType.LIST)
                                    mvpView?.scrollToDay(index)
                                } else {
                                    mvpView?.showToast(R.string.history_empty_day_prompt)
                                }
                            }) { throwable -> handleError(throwable) })
                })
    }

    // Loading

    private fun loadHistory() {
        if (mDataListManager.isEmpty) {
            mvpView?.toggleLoadingOverlay(true)
        }
        // Load initial items
        ELDatastore.workoutsStore().getItems()
    }

    // Handlers

    private fun handleCheckEmptyState() {
        mvpView?.toggleEmptyState(mDataListManager.isEmpty)
    }

    private fun handleHistoryReady(items: List<ELWorkout>) {
        // Show history in list
        mDataListManager.clear()
        mDataListManager.addAll(items)
        mAdapter.notifyDataSetChanged()
        handleCheckEmptyState()
        mvpView?.toggleLoadingOverlay(false)
        // SHow history in calendar
        subscriptions.add(buildWorkoutsMap(items)
                .compose(applySchedulers())
                .subscribe({ (first, second) ->
                    mCalendarContainer = HistoryCalendarContainer(first, second)
                    mvpView?.refreshCalendar(findMinHistoryDate())
                }) { throwable -> handleError(throwable) })
    }

    private fun buildWorkoutsMap(items: List<ELWorkout>): Observable<Pair<Map<String, ELWorkout>, Map<String, List<ELWorkout>>>> {
        return Observable.fromCallable {
            val daysMap = HashMap<String, ELWorkout>()
            val monthsMap = HashMap<String, MutableList<ELWorkout>>()
            val list = ArrayList<ELWorkout>(items)
            list.forEach {
                val date = Instant.ofEpochMilli(it.completedDate).atZone(ZoneId.systemDefault()).toLocalDate()
                // Obtain workout day key
                val dayKey = buildDateDayKey(date)
                daysMap[dayKey] = it
                // Obtain workout month key
                val monthKey = buildDateMonthKey(date)
                val monthWorkouts = monthsMap[monthKey] ?: ArrayList()
                monthWorkouts.add(it)
                monthsMap[monthKey] = monthWorkouts
            }
            Pair<Map<String, ELWorkout>, Map<String, MutableList<ELWorkout>>>(daysMap, monthsMap)
        }
    }

    private fun findPositionForDate(date: LocalDate): Observable<Int> {
        return Observable.fromCallable {
            for (i in 0 until mDataListManager.count) {
                if (mDataListManager[i].getCompletedDateAsDate().toLocalDate() == date) {
                    return@fromCallable i
                }
            }
            -1
        }
    }

    private fun findMinHistoryDate(): Date? {
        return if (mDataListManager.count > 0) {
            mDataListManager.get(mDataListManager.count - 1).getCompletedDateAsDate()
        } else {
            null
        }
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinders(HistoryAdapter.Binder(object : BaseTimelineViewHolder.OnTimelineListener<ELWorkout> {
            override fun collectionSize(): Int {
                return mDataListManager.count
            }

            override fun dateOfPreviousItem(position: Int): Long? {
                return if (position > 0) {
                    mDataListManager.get(position - 1).completedDate
                } else null
            }

            override fun dateOfNextItem(position: Int): Long? {
                return if (position < mDataListManager.count - 1) {
                    mDataListManager.get(position + 1).completedDate
                } else null
            }

            override fun onItemClicked(item: ELWorkout, position: Int) {
                navigator.openWorkoutDetails(item, false, false)
            }
        }))
    }
}