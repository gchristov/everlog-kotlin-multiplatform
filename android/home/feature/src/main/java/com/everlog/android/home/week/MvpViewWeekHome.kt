package com.everlog.android.home.week

import com.everlog.android.home.data.model.HomeNotification
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import rx.Observable

interface MvpViewWeekHome : BaseFragmentMvpView {

    fun showHomeNotification(notification: HomeNotification?)

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