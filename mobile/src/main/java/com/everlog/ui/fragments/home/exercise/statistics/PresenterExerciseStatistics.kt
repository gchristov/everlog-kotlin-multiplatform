package com.everlog.ui.fragments.home.exercise.statistics

import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab
import com.everlog.ui.navigator.Navigator

class PresenterExerciseStatistics : BasePresenterExerciseTab<MvpViewExerciseStatistics>() {

    override fun onReady() {
        super.onReady()
        observeFooterClick()
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
                    navigator.sendEmail(Navigator.ContactType.STATISTICS, null)
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