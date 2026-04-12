package com.everlog.ui.fragments.home.activity.statistics

import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.ui.fragments.base.BaseFragmentMvpView
import rx.Observable

interface MvpViewStatisticsHome : BaseFragmentMvpView {

    fun onClickFooter(): Observable<Void>

    fun onRangeChanged(): Observable<StatisticsHomeFragment.RangeType>

    fun setStatsData(stats: UserStatsController.StatsResult)
}