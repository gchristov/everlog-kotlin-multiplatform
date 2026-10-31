package com.everlog.android.template.feature.perform

import com.everlog.android.template.feature.perform.Header
import com.everlog.android.template.feature.details.PresenterRoutineDetails
import com.everlog.android.template.feature.perform.PerformRoutineHeaderAdapter

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