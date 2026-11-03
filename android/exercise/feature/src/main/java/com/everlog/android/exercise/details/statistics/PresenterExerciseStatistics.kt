package com.everlog.android.exercise.details.statistics

import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.launcher
import com.everlog.android.exercise.details.BasePresenterExerciseTab

class PresenterExerciseStatistics(
    private val navigator: Navigator,
) : BasePresenterExerciseTab<MvpViewExerciseStatistics>() {

    private val feedback by lazy {
        mvpView.getParentActivity()?.let { navigator.launcher(it, FeedbackForm(FeedbackSource.EXERCISE_STATISTICS)) }
    }

    override fun onReady() {
        super.onReady()
        observeFooterClick()
        mvpView.showFeedback(feedback != null)
    }

    override fun onProChanged() {
        super.onProChanged()
        loadData()
    }

    // Observers

    private fun observeFooterClick() {
        subscriptions.add(mvpView.onClickFooter()
                .compose(applyUISchedulers())
                .subscribe { feedback?.invoke() })
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