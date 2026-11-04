package com.everlog.android.home.banner

import com.everlog.android.common.kotlin.Utils
import com.everlog.android.common.mvp.view.BaseViewPresenter
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.ExerciseLibrary
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.PlayStoreListing
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.WebLink
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.home.HomeConfig
import com.everlog.android.home.data.HomeNotifications
import com.everlog.android.home.data.model.HomeNotification
import timber.log.Timber

class PresenterHomeNotification(
    private val notifications: HomeNotifications,
    private val analytics: HomeAnalytics,
    private val config: HomeConfig,
    private val navigator: Navigator,
) : BaseViewPresenter<MvpViewHomeNotification>() {

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
                    analytics.notificationDismissed(notification?.title)
                    notification?.let(notifications::dismissed)
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
        analytics.notificationTapped(notification.title)
        if (notification.appUpdateRequired(config.appVersionCode)) {
            // Redirect user to Play Store to update app if action is not supported.
            open(PlayStoreListing)
        } else if (notification.hasSupportedUrl()) {
            open(WebLink(notification.actionUrl!!.trim()))
            // The user acted on it (e.g. opened the survey), so don't keep showing it.
            notifications.dismissed(notification)
            handleHideNotification(true)
        } else {
            when (val action = notification.getAction()) {
                HomeNotification.ActionType.MUSCLE_GOALS -> open(SetSuggestions)
                HomeNotification.ActionType.PLANS -> mvpView?.showPlans()
                HomeNotification.ActionType.SETTINGS -> mvpView?.showSettings()
                HomeNotification.ActionType.EXERCISES -> open(ExerciseLibrary)
                HomeNotification.ActionType.NONE, HomeNotification.ActionType.MAINTENANCE, null ->
                    Timber.tag(TAG).w("Home notification tapped without a supported action: %s", action)
            }
            // The user acted on it (or the tap had nothing to do), so don't keep showing it.
            notifications.dismissed(notification)
            handleHideNotification(true)
        }
    }

    private fun open(destination: Destination) {
        context?.let { navigator.open(it, destination) }
    }

    companion object {
        private const val TAG = "PresenterHomeNotification"
    }
}
