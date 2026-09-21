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
        val notification = mvpView?.getNotification() ?: return
        when (val action = notification.resolveTapAction()) {
            is HomeNotification.TapAction.OpenPlayStore -> navigator.openPlayStoreAppDetails()
            is HomeNotification.TapAction.OpenUrl -> {
                navigator.openUrl(action.url)
                // The user acted on it (e.g. opened the survey), so don't keep showing it.
                AppLaunchManager.manager.homeNotificationDismissed(notification)
                handleHideNotification(true)
            }
            is HomeNotification.TapAction.OpenScreen -> openScreen(action.type)
            is HomeNotification.TapAction.None ->
                Timber.tag(TAG).w("Home notification tapped without a supported action (actionId=%s, actionUrl=%s)",
                        notification.actionId, notification.actionUrl)
        }
    }

    private fun openScreen(type: HomeNotification.ActionType) {
        when (type) {
            HomeNotification.ActionType.MUSCLE_GOALS -> navigator.openMuscleGoal()
            HomeNotification.ActionType.PLANS -> mvpView?.showPlans()
            HomeNotification.ActionType.SETTINGS -> mvpView?.showSettings()
            HomeNotification.ActionType.EXERCISES -> navigator.openExercises()
            HomeNotification.ActionType.NONE,
            HomeNotification.ActionType.MAINTENANCE ->
                Timber.tag(TAG).w("Unsupported home notification action: %s", type)
        }
    }

    companion object {
        private const val TAG = "PresenterHomeNotification"
    }
}
