package com.everlog.android.template.perform

import com.everlog.android.template.TemplateGraph
import com.everlog.android.common.kotlin.di.appGraph
import android.view.Menu
import com.everlog.android.template.TemplateExtras
import com.everlog.android.template.TemplateScreenNames
import com.everlog.android.template.details.PresenterRoutineDetails
import com.everlog.android.template.details.RoutineDetailsActivity

class PerformRoutineActivity : RoutineDetailsActivity(), MvpViewPerformRoutine {

    private var mPresenter: PresenterPerformRoutine? = null

    public override fun getAnalyticsScreenName(): String {
        return TemplateScreenNames.PERFORM
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun getRoutinePresenter(): PresenterRoutineDetails<*>? {
        return mPresenter
    }

    override fun performingFromPlan(): Boolean {
        return intent.getBooleanExtra(TemplateExtras.FROM_PLAN, false)
    }

    // Setup

    public override fun setupPresenter() {
        val graph = appGraph<TemplateGraph>()
        mPresenter = PresenterPerformRoutine(graph.userSettings, graph.templateAnalytics, graph.templateNavigation, graph.ongoingPlanRepository)
    }
}