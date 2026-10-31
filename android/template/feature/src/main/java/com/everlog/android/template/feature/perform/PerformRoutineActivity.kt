package com.everlog.android.template.feature.perform

import android.view.Menu
import com.everlog.android.template.feature.TemplateExtras
import com.everlog.android.template.feature.TemplateScreenNames
import com.everlog.android.template.feature.details.PresenterRoutineDetails
import com.everlog.android.template.feature.details.RoutineDetailsActivity

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
        mPresenter = PresenterPerformRoutine()
    }
}