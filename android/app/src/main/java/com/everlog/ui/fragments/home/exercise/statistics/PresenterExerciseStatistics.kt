package com.everlog.ui.fragments.home.exercise.statistics

import com.everlog.config.FeedbackSource
import com.everlog.managers.RemoteConfigManager
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab

class PresenterExerciseStatistics : BasePresenterExerciseTab<MvpViewExerciseStatistics>() {

    override fun onReady() {
        super.onReady()
        observeFooterClick()
        mvpView.showFeedback(RemoteConfigManager.manager.feedbackFormUrl(FeedbackSource.EXERCISE_STATISTICS) != null)
    }

    override fun onProChanged() {
        super.onProChanged()
        loadData()
    }

    // Observers

    private fun observeFooterClick() {
        subscriptions.add(mvpView.onClickFooter()
                .compose(applyUISchedulers())
                .subscribe {
                    navigator.openFeedbackForm(FeedbackSource.EXERCISE_STATISTICS)
                })
    }

    // Loading

    override fun loadData() {
        super.loadData()
        if (stats == null) {
            mvpView?.toggleLoadingOverlay(true)
        } else {
            mvpView?.toggleLoadingOverlay(false)
        }
    }
}