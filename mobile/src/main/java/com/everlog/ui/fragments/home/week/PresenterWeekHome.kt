package com.everlog.ui.fragments.home.week

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.data.controllers.statistics.UserStatsController.OnCompleteListener
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.history.ELUserWorkoutsStore.ELColStoreWorkoutsLoadedEvent
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.plan.ELPlan
import com.everlog.data.model.plan.ELPlanState
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.PlanManager
import com.everlog.managers.RemoteConfigManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.activities.home.congratulate.CongratulateActivity
import com.everlog.ui.fragments.base.BaseFragmentPresenter
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.utils.Utils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class PresenterWeekHome : BaseFragmentPresenter<MvpViewWeekHome>() {

    private var mBroadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isAttachedToView) {
                when {
                    ELConstants.BROADCAST_CURRENT_PLAN_CHANGED == intent.action -> {
                        handleCurrentPlanChanged()
                    }
                }
            }
        }
    }

    // Cached

    private var mLatestPlan: ELPlan? = null
    private var mLatestPlanState: ELPlanState? = null
    private var mLatestWeekStats: UserStatsController.StatsResult? = null
    private val mHistory: MutableList<ELWorkout> = ArrayList()

    private val mOnRefreshStats = PublishSubject.create<List<ELWorkout>>()

    override fun init() {
        super.init()
        setupBroadcastReceivers()
    }

    override fun onReady() {
        observeRefreshStats()
        observePlanClick()
        observePlanStartClick()
        observePlanSkipClick()
        observeWeekStatsClick()
        observeWeekGoalClick()
        observeWeekEmptyStateClick()
        loadPlaceholders()
        loadWeekStats()
    }

    override fun detachView() {
        mBroadcastReceiver?.let { LocalBroadcastManager.getInstance(mvpView.context).unregisterReceiver(it) }
        mBroadcastReceiver = null
        super.detachView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_ROUTINE) {
            if (resultCode == Activity.RESULT_OK) {
                handleStartRoutinePicked(data!!.getSerializableExtra(ELConstants.EXTRA_ROUTINE) as ELRoutine, false)
            }
        }
    }

    override fun onFragmentResumed() {
        super.onFragmentResumed()
        if (isAttachedToView) {
            handleFragmentResumed()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoryLoaded(event: ELColStoreWorkoutsLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                mvpView?.toggleLoadingOverlay(false)
            } else {
                mHistory.clear()
                mHistory.addAll(event.items)
                handleHistoryReady(mHistory)
            }
        }
    }

    override fun onRemoteConfigChanged() {
        super.onRemoteConfigChanged()
        mvpView?.showHomeNotification(RemoteConfigManager.manager.notificationHome())
    }

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        handleHistoryReady(mHistory)
    }

    // Observers

    private fun observeRefreshStats() {
        subscriptions.add(mOnRefreshStats
                .debounce(600, TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({
                    UserStatsController().loadStats(StatisticsHomeFragment.RangeType.WEEK,
                            it,
                            object : OnCompleteListener {
                                override fun onComplete(result: UserStatsController.StatsResult) {
                                    mLatestWeekStats = result
                                    handleCurrentPlanChanged()
                                }

                                override fun onError(throwable: Throwable) {
                                    mvpView?.toggleLoadingOverlay(false)
                                }
                    })
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeWeekGoalClick() {
        subscriptions.add(mvpView.onClickWeekGoal()
                .compose(applyUISchedulers())
                .subscribe({ mvpView?.showSettings() }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeWeekStatsClick() {
        subscriptions.add(mvpView.onClickWeekStats()
                .compose(applyUISchedulers())
                .subscribe({ mvpView?.showStatistics() }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeWeekEmptyStateClick() {
        subscriptions.add(mvpView.onClickWeekEmptyState()
                .compose(applyUISchedulers())
                .subscribe({ mvpView?.showCreateActivity() }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observePlanSkipClick() {
        subscriptions.add(mvpView.onClickPlanSkip()
                .compose(applyUISchedulers())
                .subscribe({ observePlanSkipConfirm() }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observePlanSkipConfirm() {
        subscriptions.add(mvpView.showPrompt(R.string.home_week_plan_skip, R.string.home_week_plan_skip_prompt, R.string.home_week_plan_skip, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action: Int ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleSkip()
                    }
                }, { throwable: Throwable? -> handleError(throwable) }))
    }

    private fun observePlanClick() {
        subscriptions.add(mvpView.onClickPlan()
                .compose(applyUISchedulers())
                .subscribe({
                    val plan = PlanManager.manager.ongoingPlan()
                    if (plan != null) {
                        navigator.openPlanDetails(plan)
                    }
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observePlanStartClick() {
        subscriptions.add(mvpView.onClickPlanStart()
                .compose(applyUISchedulers())
                .subscribe({ handleStartNextPlanDay() }) { throwable: Throwable? -> handleError(throwable) })
    }

    // Loading

    private fun loadPlaceholders() {
        handleWeekStatsReady(false)
    }

    private fun loadWeekStats() {
        // Load history
        mvpView?.toggleLoadingOverlay(true)
        Utils.runWithDelay({ ELDatastore.workoutsStore().getItems() }, 600)
    }

    // Handlers

    private fun handleHistoryReady(history: List<ELWorkout>) {
        mOnRefreshStats.onNext(history)
    }

    private fun handleSkip() {
        val plan = PlanManager.manager.ongoingPlan()
        val nextDay = plan!!.getNextDay()
        if (nextDay != null) {
            nextDay.setRest(true)
            nextDay.complete = true
            PlanManager.manager.setOngoingPlan(plan)
            mLatestPlan = plan
            mLatestPlanState = PlanManager.manager.ongoingPlanState()
            checkWeekViews()
        }
    }

    private fun handleStartNextPlanDay() {
        if (PlanManager.manager.hasOngoingPlan()) {
            val plan = PlanManager.manager.ongoingPlan()
            val nextDay = plan!!.getNextDay()
            if (nextDay != null) {
                if (nextDay.getRoutine() != null) {
                    // Perform next plan routine
                    handleStartRoutinePicked(nextDay.getRoutine()!!, true)
                } else {
                    // Rest day, so mark as complete directly
                    nextDay.complete = true
                    nextDay.next = false
                    PlanManager.manager.setOngoingPlan(plan)
                    mLatestPlan = plan
                    mLatestPlanState = PlanManager.manager.ongoingPlanState()
                    checkWeekViews()
                    AnalyticsManager.manager.planDayComplete()
                }
            } else {
                // Finish plan
                PlanManager.manager.clearOngoingPlan()
                navigator.openCongratulate(CongratulateActivity.Type.PLAN_FINISH)
                AnalyticsManager.manager.planCompleted()
                AppLaunchManager.manager.rateActionTrigger()
            }
        }
    }

    private fun handleStartRoutinePicked(routine: ELRoutine, fromPlan: Boolean) {
        navigator.openPerformRoutineConfirmation(routine, fromPlan)
    }

    private fun handleWeekStatsReady(stopLoading: Boolean) {
        if (isAttachedToView) {
            if (stopLoading) {
                mvpView?.toggleLoadingOverlay(false)
            }
            if (PlanManager.manager.hasOngoingPlan()) {
                mvpView?.showWeekData(mLatestPlan, mLatestPlanState)
            } else {
                mvpView?.showWeekData(mLatestWeekStats)
            }
        }
    }

    private fun handleFragmentResumed() {
        checkWeekViews()
    }

    private fun handleCurrentPlanChanged() {
        mLatestPlan = PlanManager.manager.ongoingPlan()
        mLatestPlanState = PlanManager.manager.ongoingPlanState()
        handleWeekStatsReady(true)
    }

    private fun checkWeekViews() {
        if (PlanManager.manager.ongoingPlan()?.uuid != mLatestPlan?.uuid) {
            // Plan was started or stopped, so refresh our stats
            mLatestPlan = null
            mLatestPlanState = null
            mLatestWeekStats = null
            mvpView?.toggleLoadingOverlay(true)
            handleHistoryReady(mHistory)
        }
        // Show whatever was the latest info
        handleWeekStatsReady(false)
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(ELConstants.BROADCAST_CURRENT_PLAN_CHANGED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mBroadcastReceiver!!, filter)
    }
}
