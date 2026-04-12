package com.everlog.ui.fragments.home.exercise.history

import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.data.model.exercise.ELExerciseHistory
import com.everlog.ui.adapters.history.BaseTimelineViewHolder
import com.everlog.ui.adapters.history.ExerciseHistoryAdapter
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab

class PresenterExerciseHistory : BasePresenterExerciseTab<MvpViewExerciseHistory>() {

    private val mAdapter = RecyclerAdapter()
    private val mDataListManager = DataListManager<ELExerciseHistory>(mAdapter)

    override fun init() {
        super.init()
        setupListView()
    }

    internal fun getListAdapter(): RecyclerAdapter? {
        return mAdapter
    }

    // Loading

    override fun loadData() {
        super.loadData()
        if (stats == null) {
            mvpView?.toggleLoadingOverlay(true)
        } else {
            mvpView?.toggleLoadingOverlay(false)
            mDataListManager.clear()
            mDataListManager.addAll(stats!!.history)
            handleCheckEmptyState()
        }
    }

    // Handlers

    private fun handleCheckEmptyState() {
        mvpView?.toggleEmptyState(mDataListManager.isEmpty)
    }

    // Setup

    private fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinder(ExerciseHistoryAdapter.Binder(object : BaseTimelineViewHolder.OnTimelineListener<ELExerciseHistory> {
            override fun collectionSize(): Int {
                return mDataListManager.count
            }

            override fun dateOfPreviousItem(position: Int): Long? {
                return if (position > 0) {
                    mDataListManager.get(position - 1).workout?.completedDate
                } else null
            }

            override fun dateOfNextItem(position: Int): Long? {
                return if (position < mDataListManager.count - 1) {
                    mDataListManager.get(position + 1).workout?.completedDate
                } else null
            }

            override fun onItemClicked(item: ELExerciseHistory?, position: Int) {
                // No-op
            }
        }))
    }
}