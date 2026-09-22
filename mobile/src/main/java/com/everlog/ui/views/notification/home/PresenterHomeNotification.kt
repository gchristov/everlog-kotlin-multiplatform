package com.everlog.ui.views.notification.home

import com.everlog.BuildConfig
import com.everlog.config.HomeNotification
import com.everlog.managers.analytics.Analytic
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.views.base.BaseViewPresenter
import com.everlog.utils.Utils
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeParseException

open class PresenterHomeNotification(
        private val appLaunchManager: AppLaunchManager = AppLaunchManager.manager,
        private val analytics: Analytic = AnalyticsManager.manager
) : BaseViewPresenter<MvpViewHomeNotification>() {

    private var mNotification: HomeNotification? = null
    private var mLastShownId: String? = null

    override fun onReady() {
        observeCloseClick()
        observeActionClick()
        renderNotification()
    }

    fun onNotificationChanged(notification: HomeNotification?) {
        mNotification = notification
        renderNotification()
    }

    // Observers

    private fun observeCloseClick() {
        subscriptions.add(mvpView.onClickClose()
                .compose(applyUISchedulers())
                .subscribe({ onCloseClicked() }) { handleError(it) })
    }

    private fun observeActionClick() {
        subscriptions.add(mvpView.onClickAction()
                .compose(applyUISchedulers())
                .subscribe({ onActionClicked() }) { handleError(it) })
    }

    // Handlers

    private fun handleHideNotification(wait: Boolean) {
        Utils.runWithDelay({
            mvpView?.hideNotification()
        }, if (wait) 500 else 0)
    }

    private fun renderNotification() {
        val notification = mNotification
        if (notification != null && shouldShow(notification)) {
            mvpView?.showNotification(notification, appUpdateRequired(notification))
            // Rendering can happen many times for the same banner, so only report each one once.
            val identity = notificationIdentity(notification)
            if (mLastShownId != identity) {
                mLastShownId = identity
                analytics.notificationHomeShown(notification.title)
            }
        } else {
            mvpView?.hideNotification()
        }
    }

    private fun shouldShow(notification: HomeNotification): Boolean {
        if (!canShow(notification) || !isWithinSchedule(notification, Instant.now())) {
            return false
        }
        return notificationIdentity(notification) != appLaunchManager.lastDismissedHomeNotificationId()
    }

    /**
     * What counts as "the same" banner: the explicit `id` set in Remote Config when present, so a
     * developer can force a re-show just by changing it, or a hash of the whole content otherwise,
     * so a banner published without an id still doesn't collide with an unrelated one.
     */
    private fun notificationIdentity(notification: HomeNotification): String {
        val id = notification.id?.trim()
        return if (!id.isNullOrEmpty()) id else "hash:${notification.hashCode()}"
    }

    private fun canShow(notification: HomeNotification): Boolean {
        return !notification.title.isNullOrEmpty() && !notification.description.isNullOrEmpty()
    }

    private fun appUpdateRequired(notification: HomeNotification, versionCode: Int = BuildConfig.VERSION_CODE): Boolean {
        val belowMinVersion = versionCode < notification.minRequiredVersion
        // actionUrl takes priority over actionId, so a usable url is always a supported action.
        if (hasSupportedUrl(notification)) {
            return belowMinVersion
        }
        // Maintenance notices never ask for an update, whatever the version.
        if (notification.getAction() == HomeNotification.ActionType.MAINTENANCE) {
            return false
        }
        // An action id this app version doesn't know about was added in a newer release.
        return belowMinVersion || (!notification.actionId.isNullOrBlank() && notification.getAction() == null)
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

    internal fun onCloseClicked() {
        val notification = mNotification ?: return
        analytics.notificationHomeDismissed(notification.title)
        appLaunchManager.homeNotificationDismissed(notificationIdentity(notification))
        handleHideNotification(false)
    }

    internal fun onActionClicked() {
        val notification = mNotification ?: return
        analytics.notificationHomeTapped(notification.title)
        if (appUpdateRequired(notification)) {
            // Redirect user to Play Store to update app if action is not supported.
            navigator.openPlayStoreAppDetails()
        } else if (hasSupportedUrl(notification)) {
            navigator.openUrl(notification.actionUrl!!.trim())
            // The user acted on it (e.g. opened the survey), so don't keep showing it.
            appLaunchManager.homeNotificationDismissed(notificationIdentity(notification))
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
