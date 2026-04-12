package com.everlog.ui.fragments.home.week

import com.everlog.config.HomeNotification
import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.plan.ELPlanState
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import rx.Observable

interface MvpViewWeekHome : BaseFragmentMvpView {

    fun showHomeNotification(notification: HomeNotification?)

    fun showSettings();

    fun showStatistics();

    fun showCreateActivity();

    // Week actions

    fun onClickWeekStats(): Observable<Void>

    fun onClickWeekGoal(): Observable<Void>

    fun onClickWeekEmptyState(): Observable<Void>

    fun showWeekData(stats: UserStatsController.StatsResult?)

    // Plan actions

    fun onClickPlan(): Observable<Void>

    fun onClickPlanStart(): Observable<Void>

    fun onClickPlanSkip(): Observable<Void>

    fun showWeekData(plan: ELPlan?, state: ELPlanState?)
}