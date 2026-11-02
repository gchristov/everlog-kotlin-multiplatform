package com.everlog.android.template.perform

import com.everlog.android.template.data.session.OngoingPlanRepository
import com.everlog.android.template.TemplateNavigation
import com.everlog.android.template.TemplateAnalytics
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.template.perform.Header
import com.everlog.android.template.details.PresenterRoutineDetails
import com.everlog.android.template.perform.PerformRoutineHeaderAdapter

class PresenterPerformRoutine(
    settings: UserSettings,
    analytics: TemplateAnalytics,
    navigation: TemplateNavigation,
    ongoingPlan: OngoingPlanRepository,
) : PresenterRoutineDetails<MvpViewPerformRoutine>(settings, analytics, navigation, ongoingPlan) {

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