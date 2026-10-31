package com.everlog.android.template.perform

import com.everlog.android.template.perform.Header
import com.everlog.android.template.details.PresenterRoutineDetails
import com.everlog.android.template.perform.PerformRoutineHeaderAdapter

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