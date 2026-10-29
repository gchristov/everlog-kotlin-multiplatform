package com.everlog.ui.activities.home.routine.perform

import android.view.Menu
import com.everlog.constants.ELConstants
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.activities.home.routine.details.PresenterRoutineDetails
import com.everlog.ui.activities.home.routine.details.RoutineDetailsActivity

class PerformRoutineActivity : RoutineDetailsActivity(), MvpViewPerformRoutine {

    private var mPresenter: PresenterPerformRoutine? = null

    public override fun getAnalyticsScreenName(): String {
        return AnalyticsConstants.SCREEN_ROUTINE_PERFORM
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    override fun getRoutinePresenter(): PresenterRoutineDetails<*>? {
        return mPresenter
    }

    override fun performingFromPlan(): Boolean {
        return intent.getBooleanExtra(ELConstants.EXTRA_WORKOUT_FROM_PLAN, false)
    }

    // Setup

    public override fun setupPresenter() {
        mPresenter = PresenterPerformRoutine()
    }
}