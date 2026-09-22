package com.everlog.ui.views.notification.home

import com.everlog.config.HomeNotification
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.views.base.BaseViewPresenter
import com.everlog.utils.Utils
import timber.log.Timber

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
                    val notification = mvpView.getNotification()
                    AnalyticsManager.manager.notificationHomeDismissed(notification?.title)
                    AppLaunchManager.manager.homeNotificationDismissed(notification)
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
        val notification = mvpView?.getNotification() ?: return
        AnalyticsManager.manager.notificationHomeTapped(notification.title)
        if (notification.appUpdateRequired()) {
            // Redirect user to Play Store to update app if action is not supported.
            navigator.openPlayStoreAppDetails()
        } else if (notification.hasSupportedUrl()) {
            navigator.openUrl(notification.actionUrl!!.trim())
            // The user acted on it (e.g. opened the survey), so don't keep showing it.
            AppLaunchManager.manager.homeNotificationDismissed(notification)
            handleHideNotification(true)
        } else {
            when (val action = notification.getAction()) {
                HomeNotification.ActionType.MUSCLE_GOALS -> navigator.openMuscleGoal()
                HomeNotification.ActionType.PLANS -> mvpView?.showPlans()
                HomeNotification.ActionType.SETTINGS -> mvpView?.showSettings()
                HomeNotification.ActionType.EXERCISES -> navigator.openExercises()
                HomeNotification.ActionType.NONE, HomeNotification.ActionType.MAINTENANCE, null -> {
                    Timber.tag(TAG).w("Home notification tapped without a supported action: %s", action)
                    // Nothing to do, so treat the tap as an acknowledgement and dismiss it.
                    AppLaunchManager.manager.homeNotificationDismissed(notification)
                    handleHideNotification(true)
                }
            }
        }
    }

    companion object {
        private const val TAG = "PresenterHomeNotification"
    }
}
