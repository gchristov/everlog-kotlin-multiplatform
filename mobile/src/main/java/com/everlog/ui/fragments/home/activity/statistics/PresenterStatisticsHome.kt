package com.everlog.ui.fragments.home.activity.statistics

import com.everlog.config.AppConfig
import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.history.ELUserWorkoutsStore
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.navigator.Navigator
import com.everlog.utils.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.ArrayList

class PresenterStatisticsHome : BaseFragmentPresenter<MvpViewStatisticsHome>() {

    private var mRange = AppConfig.configuration.defaultStatsRange
    private val mHistory: MutableList<ELWorkout> = ArrayList()

    override fun onReady() {
        observeRangeChanged()
        observeFooterClick()
        loadHistory()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoryLoaded(event: ELUserWorkoutsStore.ELColStoreWorkoutsLoadedEvent) {
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

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        handleHistoryReady(mRange, mHistory)
    }

    // Observers

    private fun observeRangeChanged() {
        subscriptions.add(mvpView.onRangeChanged()
                .compose(applyUISchedulers())
                .subscribe { range ->
                    if (mRange != range) {
                        mRange = range
                        handleHistoryReady(range, mHistory)
                        AnalyticsManager.manager.statisticsRangeModified(range)
                    }
                })
    }

    private fun observeFooterClick() {
        subscriptions.add(mvpView.onClickFooter()
                .compose(applyUISchedulers())
                .subscribe {
                    navigator.sendEmail(Navigator.ContactType.STATISTICS, null)
                })
    }

    // Loading

    private fun loadHistory() {
        mvpView?.toggleLoadingOverlay(true)
        // Load initial items.
        ELDatastore.workoutsStore().getItems()
    }

    private fun handleHistoryReady(range: StatisticsHomeFragment.RangeType, history: List<ELWorkout>) {
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