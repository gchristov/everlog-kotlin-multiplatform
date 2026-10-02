package com.everlog.ui.activities.home

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
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.appupdate.AppUpdateController
import com.everlog.managers.billing.BillingBridge
import com.everlog.managers.billing.BillingManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.FCMUtils
import com.everlog.utils.Utils
import com.imagepick.dialog.ELPickerDialog

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

    // The prompt to resume or discard an ongoing workout, shown on resume, so it isn't shown twice
    private var mOngoingWorkoutPromptShowing = false

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
        mAppUpdateController?.release()
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
                    mAppUpdateController?.onUpdateFlowResult(resultCode)
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observeAppUpdateRestartClick() {
        subscriptions.add(mvpView.onClickAppUpdateRestart()
                .compose(applyUISchedulers())
                .subscribe({
                    mAppUpdateController?.completeUpdate()
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observeDiscardOngoingWorkoutConfirm(workout: ELWorkout) {
        if (mOngoingWorkoutPromptShowing) {
            // Still open from before the app went to the background
            return
        }
        mOngoingWorkoutPromptShowing = true
        val setsCompleted = workout.getCompletedSetsCount()
        val context = mvpView.context
        val message = if (setsCompleted > 0) {
            context.resources.getQuantityString(R.plurals.home_week_ongoing_workout_prompt_sets_logged, setsCompleted, setsCompleted)
        } else {
            context.getString(R.string.home_week_ongoing_workout_prompt_subtitle)
        }
        AnalyticsManager.manager.workoutDiscardPromptShown(AnalyticsConstants.DISCARD_PROMPT_SOURCE_HOME, setsCompleted)
        subscriptions.add(mvpView.showChoicePrompt(context.getString(R.string.home_week_ongoing_workout_prompt_title),
                message,
                context.getString(R.string.resume),
                context.getString(R.string.discard),
                null,
                DialogInterface.BUTTON_NEGATIVE)
                .take(1)
                .compose(applyUISchedulers())
                .subscribe({ action: Int ->
                    mOngoingWorkoutPromptShowing = false
                    if (action == DialogInterface.BUTTON_NEGATIVE) {
                        WorkoutManager.manager.clearOngoingWorkout()
                        AnalyticsManager.manager.workoutStopped()
                        // Removes the notification if the app's process was killed mid-workout and Android hasn't
                        // restarted the service yet, which removes it too
                        navigator.stopWorkoutService()
                    } else {
                        // Resumed, or closed with back, which keeps the workout for next time
                        AnalyticsManager.manager.workoutDiscardPromptCancelled(AnalyticsConstants.DISCARD_PROMPT_SOURCE_HOME)
                        if (action == DialogInterface.BUTTON_POSITIVE) {
                            navigator.resumeWorkout(workout)
                        }
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
            mAppUpdateController?.checkForUpdate()
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
        mAppUpdateController = AppUpdateController(mvpView.context, mvpView.appUpdateLauncher()) {
            // Don't offer a restart that would end the ongoing workout
            if (!WorkoutManager.manager.hasOngoingWorkout()) {
                mvpView?.showAppUpdateReady()
            }
        }
    }

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(ELConstants.BROADCAST_CURRENT_PLAN_STARTED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mPlanStartedReceiver, filter)
    }
}