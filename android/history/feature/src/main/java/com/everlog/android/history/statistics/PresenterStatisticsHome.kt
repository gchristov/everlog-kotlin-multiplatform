package com.everlog.android.history.statistics

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.workout.data.store.ELColStoreWorkoutsLoadedEvent
import com.everlog.android.workout.data.store.WorkoutStores
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.history.HistoryFeature
import com.everlog.android.common.kotlin.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.ArrayList

class PresenterStatisticsHome : BaseFragmentPresenter<MvpViewStatisticsHome>() {

    private var mRange = UserStatsController.DEFAULT_RANGE
    private val mHistory: MutableList<ELWorkout> = ArrayList()

    override fun onReady() {
        observeRangeChanged()
        observeFooterClick()
        showFeedback()
        loadHistory()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoryLoaded(event: ELColStoreWorkoutsLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                mvpView.toggleLoadingOverlay(false)
                handleError(event.error)
            } else {
                mHistory.clear()
                mHistory.addAll(event.items)
                handleHistoryReady(mRange, mHistory)
            }
        }
    }

    // The tab is built at launch, often before the first Remote Config refresh brings the form
    override fun onRemoteConfigChanged() {
        super.onRemoteConfigChanged()
        showFeedback()
    }

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        handleHistoryReady(mRange, mHistory)
    }

    private fun showFeedback() {
        mvpView?.showFeedback(HistoryFeature.dependencies.hasStatisticsFeedback())
    }

    // Observers

    private fun observeRangeChanged() {
        subscriptions.add(mvpView.onRangeChanged()
                .compose(applyUISchedulers())
                .subscribe { range ->
                    if (mRange != range) {
                        mRange = range
                        handleHistoryReady(range, mHistory)
                        HistoryFeature.dependencies.statisticsRangeModified(range)
                    }
                })
    }

    private fun observeFooterClick() {
        subscriptions.add(mvpView.onClickFooter()
                .compose(applyUISchedulers())
                .subscribe {
                    context?.let(HistoryFeature.dependencies::openStatisticsFeedback)
                })
    }

    // Loading

    private fun loadHistory() {
        mvpView?.toggleLoadingOverlay(true)
        // Load initial items.
        WorkoutStores.workoutsStore().getItems()
    }

    private fun handleHistoryReady(range: RangeType, history: List<ELWorkout>) {
        mvpView?.toggleLoadingOverlay(true)
        Utils.runWithDelay({
            if (isAttachedToView) {
                UserStatsController().loadStats(range,
                        history,
                        object : UserStatsController.OnCompleteListener {

                    override fun onComplete(result: UserStatsController.StatsResult) {
                        result.range = range
                        handleGlobalStatsReady(result)
                    }

                    override fun onError(throwable: Throwable) {
                        mvpView?.toggleLoadingOverlay(false)
                    }
                })
            }
        }, 600)
    }

    // Handlers

    private fun handleGlobalStatsReady(stats: UserStatsController.StatsResult) {
        mvpView?.toggleLoadingOverlay(false)
        mvpView?.setStatsData(stats)
    }
}