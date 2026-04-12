package com.everlog.ui.views.notification.home

import com.everlog.config.HomeNotification
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.views.base.BaseViewPresenter
import com.everlog.utils.Utils

class PresenterHomeNotification : BaseViewPresenter<MvpViewHomeNotification>() {

    override fun onReady() {
        observeCloseClick()
        observeActionClick()
    }

    // Observers

    private fun observeCloseClick() {
        subscriptions.add(mvpView.onClickClose()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.notificationHomeDismissed()
                    AppLaunchManager.manager.homeNotificationDismissed(mvpView.getNotification())
                    handleHideNotification(false)
                }) { handleError(it) })
    }

    private fun observeActionClick() {
        subscriptions.add(mvpView.onClickAction()
                .compose(applyUISchedulers())
                .subscribe({ handleShowAction() }) { handleError(it) })
    }

    // Handlers

    private fun handleHideNotification(wait: Boolean) {
        Utils.runWithDelay({
            mvpView?.hideNotification()
        }, if (wait) 500 else 0)
    }

    private fun handleShowAction() {
        val notification = mvpView?.getNotification()
        if (notification != null) {
            if (notification.appUpdateRequired()) {
                // Redirect user to Play Store to update app if action is not supported.
                navigator.openPlayStoreAppDetails()
            } else {
                when (notification.getAction()) {
                    HomeNotification.ActionType.MUSCLE_GOALS -> navigator.openMuscleGoal()
                    HomeNotification.ActionType.PLANS -> mvpView?.showPlans()
                    HomeNotification.ActionType.SETTINGS -> mvpView?.showSettings()
                    HomeNotification.ActionType.EXERCISES -> navigator.openExercises()
                    HomeNotification.ActionType.NONE -> TODO()
                    HomeNotification.ActionType.MAINTENANCE -> TODO()
                    null -> TODO()
                }
            }
        }
    }
}