package com.everlog.android.history.statistics

import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import rx.Observable

interface MvpViewStatisticsHome : BaseFragmentMvpView {

    fun onClickFooter(): Observable<Void>

    fun showFeedback(visible: Boolean)

    fun onRangeChanged(): Observable<RangeType>

    fun setStatsData(stats: UserStatsController.StatsResult)
}