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
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.billing.BillingBridge
import com.everlog.managers.billing.BillingManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.utils.FCMUtils
import com.everlog.utils.Utils
import com.imagepick.picker.dialog.ELPickerDialog
import io.customerly.Customerly

class PresenterHome : BaseActivityPresenter<MvpViewHome>() {

    private val mPlanStartedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mvpView?.showWeek()
        }
    }

    // @State
    @JvmField
    var mSelectedTab: Int? = null

    override fun init() {
        super.init()
        setupBroadcastReceivers()
    }

    override fun onReady() {
        observeAddClick()
        // APP STARTUP: Delay to not block
        Utils.runWithDelay({
            identifyUser()
            FCMUtils.refreshFCMToken()
        }, 10)
    }

    override fun detachView() {
        LocalBroadcastManager.getInstance(mvpView.context).unregisterReceiver(mPlanStartedReceiver)
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
    }

    // Observers

    private fun observeAddClick() {
        subscriptions.add(mvpView.onClickAdd()
                .compose(applyUISchedulers())
                .subscribe({
                    handleAdd()
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
        if (WorkoutManager.manager.hasOngoingWorkout()) {
            observeDiscardOngoingWorkoutConfirm(WorkoutManager.manager.ongoingWorkout()!!)
        }
    }

    private fun handleActivityPaused() {
        navigator.scheduleAppUseNotification()
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
                        R.id.action_create_plan -> {
                            navigator.openEditPlan(null)
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
        AnalyticsManager.manager.userIdentify(getUserAccount()!!.id, getUserAccount()!!.email, getUserAccount()!!.displayName)
        ErrorManager.manager.userIdentify(getUserAccount()!!.id, getUserAccount()!!.email, getUserAccount()!!.displayName)
        Customerly.registerUser(getUserAccount()!!.email!!, getUserAccount()!!.id, getUserAccount()!!.displayName)
//        Qonversion.initialize(ELApplication.getInstance(), BuildConfig.QONVERSION_KEY, getUserAccount()!!.id!!)
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(ELConstants.BROADCAST_CURRENT_PLAN_STARTED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mPlanStartedReceiver, filter)
    }
}