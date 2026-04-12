package com.everlog.ui.activities.home.routine.perform

import com.everlog.data.model.util.Header
import com.everlog.ui.activities.home.routine.details.PresenterRoutineDetails
import com.everlog.ui.adapters.routine.PerformRoutineHeaderAdapter

class PresenterPerformRoutine : PresenterRoutineDetails<MvpViewPerformRoutine>() {

    override fun loadViewData() {
        super.loadViewData()
        mDataListManager.add(0, Header())
    }

    override fun performingFromPlan(): Boolean {
        return mvpView?.performingFromPlan() ?: false
    }

    // Setup

    override fun setupListView() {
        super.setupListView()
        mAdapter.registerBinder(PerformRoutineHeaderAdapter.Binder())
    }
}