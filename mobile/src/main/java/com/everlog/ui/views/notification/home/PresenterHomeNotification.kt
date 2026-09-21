package com.everlog.ui.views.notification.home

import com.everlog.BuildConfig
import com.everlog.config.HomeNotification
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.views.base.BaseViewPresenter
import com.everlog.utils.Utils
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeParseException

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

    fun shouldShow(notification: HomeNotification?): Boolean {
        if (notification == null || !notification.canShow() || !isWithinSchedule(notification, Instant.now())) {
            return false
        }
        return AppLaunchManager.manager.shouldShowHomeNotification(notification)
    }

    fun appUpdateRequired(notification: HomeNotification, versionCode: Int = BuildConfig.VERSION_CODE): Boolean {
        if (versionCode < notification.minRequiredVersion) {
            return true
        }
        // actionUrl takes priority over actionId, so a usable url is always a supported action.
        if (hasSupportedUrl(notification)) {
            return false
        }
        // An action id this app version doesn't know about was added in a newer release.
        return !notification.actionId.isNullOrBlank() && notification.getAction() == null
    }

    private fun hasSupportedUrl(notification: HomeNotification): Boolean {
        val url = notification.actionUrl?.trim() ?: return false
        return url.startsWith("https://", true) || url.startsWith("http://", true)
    }

    private fun isWithinSchedule(notification: HomeNotification, now: Instant): Boolean {
        return try {
            val start = notification.startAt?.trim()?.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }
            val end = notification.endAt?.trim()?.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }
            (start == null || !now.isBefore(start)) && (end == null || now.isBefore(end))
        } catch (e: DateTimeParseException) {
            // Unparseable schedule, so don't show rather than show forever.
            false
        }
    }

    private fun handleShowAction() {
        val notification = mvpView?.getNotification() ?: return
        if (appUpdateRequired(notification)) {
            // Redirect user to Play Store to update app if action is not supported.
            navigator.openPlayStoreAppDetails()
        } else if (hasSupportedUrl(notification)) {
            navigator.openUrl(notification.actionUrl!!.trim())
            // The user acted on it (e.g. opened the survey), so don't keep showing it.
            AppLaunchManager.manager.homeNotificationDismissed(notification)
            handleHideNotification(true)
        } else {
            val action = notification.getAction()
            if (action == null) {
                Timber.tag(TAG).w("Home notification tapped without an action or url, ignoring")
            } else {
                openScreen(action)
            }
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
