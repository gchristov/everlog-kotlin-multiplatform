package com.everlog.android.home.week

import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.Utils
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.common.navigation.PlanCongratulations
import com.everlog.android.common.navigation.PlanDetails
import com.everlog.android.common.navigation.StartWorkoutConfirmation
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.home.HomeConfig
import com.everlog.android.home.R
import com.everlog.android.plan.data.PlanBroadcasts
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanState
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.statistics.ui.UserStatsController
import com.everlog.android.statistics.ui.UserStatsController.OnCompleteListener
import com.everlog.android.workout.data.store.ELColStoreWorkoutsLoadedEvent
import com.everlog.android.workout.data.store.WorkoutStores
import java.util.concurrent.TimeUnit
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rx.subjects.PublishSubject

class PresenterWeekHome(
    private val settings: UserSettings,
    private val planProgress: PlanProgress,
    private val config: HomeConfig,
    private val analytics: HomeAnalytics,
    private val navigator: Navigator,
) : BaseFragmentPresenter<MvpViewWeekHome>() {

    private var mBroadcastReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isAttachedToView) {
                when {
                    PlanBroadcasts.CURRENT_PLAN_CHANGED == intent.action -> {
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
    private var mLoading = false

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
        renderHomeNotification()
        loadWeekStats()
    }

    override fun detachView() {
        mBroadcastReceiver?.let { LocalBroadcastManager.getInstance(mvpView.context).unregisterReceiver(it) }
        mBroadcastReceiver = null
        super.detachView()
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
                renderReady()
            } else {
                mHistory.clear()
                mHistory.addAll(event.items)
                handleHistoryReady(mHistory)
            }
        }
    }

    override fun onRemoteConfigChanged() {
        super.onRemoteConfigChanged()
        renderHomeNotification()
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
                    UserStatsController(settings.weightUnit(), settings.firstDayOfWeek()).loadStats(RangeType.WEEK,
                            it,
                            object : OnCompleteListener {
                                override fun onComplete(result: UserStatsController.StatsResult) {
                                    mLatestWeekStats = result
                                    handleCurrentPlanChanged()
                                }

                                override fun onError(throwable: Throwable) {
                                    renderReady()
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
        subscriptions.add(mvpView.showPrompt(DesignR.string.home_week_plan_skip, R.string.home_week_plan_skip_prompt, DesignR.string.home_week_plan_skip, DesignR.string.cancel)
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
                    planProgress.ongoingPlan()?.uuid?.let { open(PlanDetails(it)) }
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observePlanStartClick() {
        subscriptions.add(mvpView.onClickPlanStart()
                .compose(applyUISchedulers())
                .subscribe({ handleStartNextPlanDay() }) { throwable: Throwable? -> handleError(throwable) })
    }

    // Loading

    private fun loadWeekStats() {
        // Load history
        mLoading = true
        mvpView?.render(WeekViewState.Loading(planProgress.hasOngoingPlan()))
        Utils.runWithDelay({ WorkoutStores.workoutsStore().getItems() }, 600)
    }

    // Handlers

    private fun renderHomeNotification() {
        mvpView?.showHomeNotification(config.notification())
    }

    private fun handleHistoryReady(history: List<ELWorkout>) {
        mOnRefreshStats.onNext(history)
    }

    private fun handleSkip() {
        val plan = planProgress.ongoingPlan()
        val nextDay = plan!!.getNextDay()
        if (nextDay != null) {
            nextDay.setRest(true)
            nextDay.complete = true
            planProgress.setOngoingPlan(plan)
            mLatestPlan = plan
            mLatestPlanState = planProgress.ongoingPlanState()
            checkWeekViews()
        }
    }

    private fun handleStartNextPlanDay() {
        if (planProgress.hasOngoingPlan()) {
            val plan = planProgress.ongoingPlan()
            val nextDay = plan!!.getNextDay()
            if (nextDay != null) {
                if (nextDay.getRoutine() != null) {
                    // Perform next plan routine
                    handleStartRoutinePicked(nextDay.getRoutine()!!, true)
                } else {
                    // Rest day, so mark as complete directly
                    nextDay.complete = true
                    nextDay.next = false
                    planProgress.setOngoingPlan(plan)
                    mLatestPlan = plan
                    mLatestPlanState = planProgress.ongoingPlanState()
                    checkWeekViews()
                    analytics.planDayCompleted()
                }
            } else {
                // Finish plan
                planProgress.clearOngoingPlan()
                open(PlanCongratulations)
                analytics.planCompleted()
            }
        }
    }

    private fun handleStartRoutinePicked(routine: ELRoutine, fromPlan: Boolean) {
        open(StartWorkoutConfirmation(routine, fromPlan))
    }

    private fun open(destination: Destination) {
        context?.let { navigator.open(it, destination) }
    }

    private fun renderReady() {
        if (isAttachedToView) {
            mLoading = false
            val state = if (planProgress.hasOngoingPlan()) {
                WeekViewState.Plan(mLatestPlan, mLatestPlanState)
            } else {
                WeekViewState.Stats(mLatestWeekStats ?: UserStatsController.StatsResult())
            }
            mvpView?.render(state)
        }
    }

    private fun handleFragmentResumed() {
        checkWeekViews()
    }

    private fun handleCurrentPlanChanged() {
        mLatestPlan = planProgress.ongoingPlan()
        mLatestPlanState = planProgress.ongoingPlanState()
        renderReady()
    }

    private fun checkWeekViews() {
        if (planProgress.ongoingPlan()?.uuid != mLatestPlan?.uuid) {
            // Plan was started or stopped, so refresh our stats
            mLatestPlan = null
            mLatestPlanState = null
            mLatestWeekStats = null
            loadWeekStats()
        } else if (!mLoading) {
            // Nothing changed and no load is in flight, so just re-render the latest known state
            renderReady()
        }
    }

    // Setup

    private fun setupBroadcastReceivers() {
        val filter = IntentFilter()
        filter.addAction(PlanBroadcasts.CURRENT_PLAN_CHANGED)
        LocalBroadcastManager.getInstance(mvpView.context).registerReceiver(mBroadcastReceiver!!, filter)
    }
}
