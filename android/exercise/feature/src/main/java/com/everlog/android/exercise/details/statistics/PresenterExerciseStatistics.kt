package com.everlog.android.exercise.details.statistics

import com.everlog.android.exercise.ExerciseNavigation
import com.everlog.android.exercise.ExerciseFeedbackForm
import com.everlog.android.exercise.details.BasePresenterExerciseTab

class PresenterExerciseStatistics(
    private val navigation: ExerciseNavigation,
) : BasePresenterExerciseTab<MvpViewExerciseStatistics>() {

    private val feedback by lazy {
        mvpView.getParentActivity()?.let { navigation.feedbackForm(it, ExerciseFeedbackForm.Statistics) }
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