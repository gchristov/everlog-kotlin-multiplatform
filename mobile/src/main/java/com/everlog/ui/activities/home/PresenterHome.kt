package com.everlog.ui.activities.home

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.ErrorManager
import com.everlog.managers.PlanManager
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.appupdate.AppUpdateController
import com.everlog.managers.billing.BillingBridge
import com.everlog.managers.billing.BillingManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.FCMUtils
import com.everlog.utils.Utils
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.imagepick.dialog.ELPickerDialog
import timber.log.Timber

class PresenterHome : BaseActivityPresenter<MvpViewHome>() {

    companion object {
        private const val TAG = "PresenterHome"
    }

    private val mPlanStartedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mvpView?.showWeek()
        }
    }

    // @State
    @JvmField
    var mSelectedTab: Int? = null

    // Assume empty (button hidden) until the async week stats load proves otherwise, to avoid a show-then-hide flash
    private var mWeekIsEmpty: Boolean = true

    private var mAppUpdateController: AppUpdateController? = null
    // The update currently offered in Play's prompt, to match against the flow result
    private var mPromptedAppUpdate: AppUpdateInfo? = null

    override fun init() {
        super.init()
        setupBroadcastReceivers()
    }

    override fun onReady() {
        observeAddClickFab()
        observeAddClickWeekEmptyState()
        observeAppUpdateFlowResult()
        observeAppUpdateRestartClick()
        setupAppUpdates()
        updateStartWorkoutButtonVisibility()
        // APP STARTUP: Delay to not block
        Utils.runWithDelay({
            identifyUser()
            FCMUtils.refreshFCMToken()
        }, 10)
    }

    override fun detachView() {
        LocalBroadcastManager.getInstance(mvpView.context).unregisterReceiver(mPlanStartedReceiver)
        mAppUpdateController?.unregisterInstallListener()
        mAppUpdateController = null
        ELDatastore.destroy()
        super.detachView()
    }

    override fun onActivityPaused() {
        super.onActivityPaused()
        if (isAttachedToView) {
            handleActivityPaused()
        }
    }

    override fun onActivityResumed() {
        super.onActivityResumed()
        if (isAttachedToView) {
            handleActivityResumed()
        }
    }

    internal fun getSelectedTab(): Int {
        if (mSelectedTab == null) {
            mSelectedTab = 0
        }
        return mSelectedTab!!
    }

    internal fun setSelectedTab(index: Int) {
        mSelectedTab = index
        updateStartWorkoutButtonVisibility()
    }

    internal fun setWeekIsEmpty(isEmpty: Boolean) {
        mWeekIsEmpty = isEmpty
        updateStartWorkoutButtonVisibility()
    }

    // Observers

    private fun observeAddClickFab() {
        subscriptions.add(mvpView.onClickAddFab()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.homeAddFabTapped()
                    handleAdd()
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observeAddClickWeekEmptyState() {
        subscriptions.add(mvpView.onClickAddWeekEmptyState()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.homeAddWeekEmptyStateTapped()
                    handleAdd()
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observeAppUpdateFlowResult() {
        subscriptions.add(mvpView.onAppUpdateFlowResult()
                .compose(applyUISchedulers())
                .subscribe({ resultCode: Int ->
                    handleAppUpdateFlowResult(resultCode)
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observeAppUpdateRestartClick() {
        subscriptions.add(mvpView.onClickAppUpdateRestart()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.appUpdateRestartTapped()
                    mAppUpdateController?.completeUpdate()
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observeDiscardOngoingWorkoutConfirm(workout: ELWorkout) {
        subscriptions.add(mvpView.showPrompt(R.string.home_week_ongoing_workout_prompt_title, R.string.home_week_ongoing_workout_prompt_subtitle, R.string.resume, R.string.discard)
                .compose(applyUISchedulers())
                .subscribe({ action: Int ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        navigator.resumeWorkout(workout)
                    } else if (action == DialogInterface.BUTTON_NEGATIVE) {
                        WorkoutManager.manager.clearOngoingWorkout()
                    }
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    // Handlers

    private fun handleActivityResumed() {
        navigator.cancelAppUseNotification()
        refreshAppConfig()
        refreshProPurchases()
        updateStartWorkoutButtonVisibility()
        if (WorkoutManager.manager.hasOngoingWorkout()) {
            observeDiscardOngoingWorkoutConfirm(WorkoutManager.manager.ongoingWorkout()!!)
        } else {
            // Don't prompt over the ongoing workout dialog, or offer a restart that would end the workout
            checkForAppUpdate()
        }
    }

    private fun handleActivityPaused() {
        navigator.scheduleAppUseNotification()
    }

    private fun updateStartWorkoutButtonVisibility() {
        val onWeekTab = getSelectedTab() == 0
        val hide = onWeekTab && (PlanManager.manager.hasOngoingPlan() || mWeekIsEmpty)
        mvpView?.toggleStartWorkoutButton(!hide)
    }

    private fun handleAdd() {
        ELPickerDialog.withActivity(mvpView.getActivity())
                .title(R.string.home_add_title)
                .menuLayout(R.menu.menu_sheet_home_add)
                .actionListener { which: Int ->
                    when (which) {
                        R.id.action_start_routine -> {
                            navigator.openRoutinePicker()
                        }
                        R.id.action_start_empty -> {
                            navigator.startWorkout(ELRoutine.buildEmptyWorkout(), false, false)
                        }
                    }
                }
                .show()
    }

    // App update

    private fun checkForAppUpdate() {
        val controller = mAppUpdateController ?: return
        subscriptions.add(controller.fetchUpdateInfo()
                .compose(applyUISchedulers())
                .subscribe({ info: AppUpdateInfo ->
                    if (controller.isUpdateDownloaded(info)) {
                        // Downloaded while the app was in the background or on another screen
                        mvpView?.showAppUpdateReady()
                    } else if (mPromptedAppUpdate == null && controller.shouldPromptUpdate(info)) {
                        promptAppUpdate(controller, info)
                    }
                }, { throwable: Throwable ->
                    // Expected when not installed from Play (e.g. debug builds), so don't report it as an error
                    Timber.tag(TAG).w("App update check failed: %s", throwable.message)
                }))
    }

    private fun promptAppUpdate(controller: AppUpdateController, info: AppUpdateInfo) {
        if (controller.startFlexibleUpdate(info, mvpView.appUpdateLauncher())) {
            mPromptedAppUpdate = info
            AnalyticsManager.manager.appUpdatePromptShown(info.availableVersionCode())
        } else {
            Timber.tag(TAG).w("App update flow not started: versionCode=%s", info.availableVersionCode())
        }
    }

    private fun handleAppUpdateFlowResult(resultCode: Int) {
        val info = mPromptedAppUpdate ?: return
        mPromptedAppUpdate = null
        when (resultCode) {
            Activity.RESULT_OK -> {
                AnalyticsManager.manager.appUpdateAccepted(info.availableVersionCode())
            }
            Activity.RESULT_CANCELED -> {
                mAppUpdateController?.updateDeclined(info)
                AnalyticsManager.manager.appUpdateDeclined(info.availableVersionCode())
            }
            else -> {
                // ActivityResult.RESULT_IN_APP_UPDATE_FAILED; the next resume will offer it again
                AnalyticsManager.manager.appUpdateFailed(resultCode)
            }
        }
    }

    // Remote config

    private fun refreshAppConfig() {
        RemoteConfigManager.manager.refreshAppConfig()
    }

    // Billing

    private fun refreshProPurchases() {
        BillingBridge.restoreUserPurchases(object : BillingManager.OnPurchasesRestoredListener() {
            override fun onPurchasesRestored() {
                // No-op
            }
        })
    }

    // Analytics

    private fun identifyUser() {
        AnalyticsManager.manager.userIdentify(getUserAccount()!!.id)
        ErrorManager.manager.userIdentify(getUserAccount()!!.id)
    }

    // Setup

    private fun setupAppUpdates() {
        mAppUpdateController = AppUpdateController(mvpView.context).apply {
            registerInstallListener(onDownloaded = {
                AnalyticsManager.manager.appUpdateDownloaded()
                if (!WorkoutManager.manager.hasOngoingWorkout()) {
                    mvpView?.showAppUpdateReady()
                }
            }, onFailed = { errorCode ->
                AnalyticsManager.manager.appUpdateFailed(errorCode)
            })
        }
    }

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(ELConstants.BROADCAST_CURRENT_PLAN_STARTED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mPlanStartedReceiver, filter)
    }
}