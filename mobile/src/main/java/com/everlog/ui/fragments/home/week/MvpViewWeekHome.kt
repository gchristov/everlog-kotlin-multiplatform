package com.everlog.ui.fragments.home.week

import com.everlog.config.HomeNotification
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import rx.Observable

interface MvpViewWeekHome : BaseFragmentMvpView {

    fun showHomeNotification(notification: HomeNotification?, workoutsCompleted: Int)

    fun showSettings();

    fun showStatistics();

    fun showCreateActivity();

    fun render(state: WeekViewState)

    // Week actions

    fun onClickWeekStats(): Observable<Void>

    fun onClickWeekGoal(): Observable<Void>

    fun onClickWeekEmptyState(): Observable<Void>

    // Plan actions

    fun onClickPlan(): Observable<Void>

    fun onClickPlanStart(): Observable<Void>

    fun onClickPlanSkip(): Observable<Void>
}