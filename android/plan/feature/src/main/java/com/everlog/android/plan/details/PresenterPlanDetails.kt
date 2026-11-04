package com.everlog.android.plan.details

import android.content.DialogInterface
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.common.navigation.Home
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.plan.ui.PlanAnalytics
import com.everlog.android.plan.R
import com.everlog.android.plan.create.CreatePlanActivity
import com.everlog.android.plan.data.PlanBroadcasts
import com.everlog.android.plan.data.PlanProgress
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.store.ELDocStorePlanLoadedEvent
import com.everlog.android.plan.data.store.PlanStores
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PresenterPlanDetails(
    private val planProgress: PlanProgress,
    private val analytics: PlanAnalytics,
    private val navigator: Navigator,
) : BaseActivityPresenter<MvpViewPlanDetails>() {


    private var toEdit: ELPlan? = null
    private var mRealPlan: ELPlan? = null
    private var mLoadedItemOnce = false

    override fun onReady() {
        observeToggleStartClick()
        observeEditClick()
        observeDeleteClick()
        loadPlan()
    }

    override fun detachView() {
        PlanStores.planStore().destroy()
        super.detachView()
    }

    override fun handleError(throwable: Throwable?) {
        super.handleError(throwable)
        if (throwable is ELDocumentStore.ItemNotFoundError && !mLoadedItemOnce) {
            observePlanNotFoundConfirm()
        }
    }

    override fun onProChanged() {
        super.onProChanged()
        mLoadedItemOnce = false
        loadPlan()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlanLoaded(event: ELDocStorePlanLoadedEvent) {
        if (isAttachedToView) {
            mvpView?.toggleLoadingOverlay(false)
            if (event.error != null) {
                handleError(event.error)
            } else {
                if (!mLoadedItemOnce || event.isHasPendingWrites) {
                    toEdit = event.item
                    mRealPlan = event.item
                    if (planProgress.isOngoing(toEdit)) {
                        toEdit = planProgress.ongoingPlan()
                    }
                    loadViewData()
                }
                mLoadedItemOnce = true
            }
        }
    }

    // Observers

    private fun observeToggleStartClick() {
        subscriptions.add(mvpView.onClickToggleStartPlan()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit != null) {
                        handleToggleStartPlan()
                    }
                })
    }

    private fun observeEditClick() {
        subscriptions.add(mvpView.onClickEdit()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit != null) {
                        mvpView?.getActivity()?.let { it.startActivity(CreatePlanActivity.launchIntent(it, toEdit)) }
                    }
                })
    }

    private fun observeDeleteClick() {
        subscriptions.add(mvpView.onClickDelete()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit != null) {
                        observeDeleteConfirm(toEdit!!)
                    }
                })
    }

    private fun observeDeleteConfirm(plan: ELPlan) {
        subscriptions.add(mvpView.showDestructivePrompt(DesignR.string.delete_title, DesignR.string.delete_prompt, DesignR.string.delete, DesignR.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleDeletePlan(plan)
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeStopConfirm() {
        subscriptions.add(mvpView.showDestructivePrompt(R.string.plan_details_stop, R.string.plan_details_stop_prompt, R.string.stop, DesignR.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        planProgress.clearOngoingPlan()
                        toEdit = mRealPlan
                        loadViewData()
                        analytics.stopped()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeSwitchConfirm() {
        subscriptions.add(mvpView.showPrompt(R.string.plan_details_switch, R.string.plan_details_switch_prompt, R.string.plan_details_switch, DesignR.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleStartPlan()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observePlanNotFoundConfirm() {
        val planUuid = mvpView.getItemToEditUuid()
        val currentPlanUuid = planProgress.ongoingPlan()?.uuid
        if (planUuid.equals(currentPlanUuid)) {
            // We didn't find the current user plan
            subscriptions.add(mvpView.showDestructivePrompt(R.string.plan_details_not_found, R.string.plan_details_not_found_prompt_current, R.string.stop, R.string.continue_)
                    .compose(applyUISchedulers())
                    .subscribe({ action ->
                        if (action == DialogInterface.BUTTON_POSITIVE) {
                            planProgress.clearOngoingPlan()
                        }
                        mvpView?.closeScreen()
                    }, { throwable -> handleError(throwable) }))
        } else {
            subscriptions.add(mvpView.showOKPrompt(R.string.plan_details_not_found, R.string.plan_details_not_found_prompt)
                    .compose(applyUISchedulers())
                    .subscribe({
                        mvpView?.closeScreen()
                    }, { throwable -> handleError(throwable) }))
        }
    }

    // Loading

    private fun loadPlan() {
        mvpView?.toggleLoadingOverlay(true)
        // Listen for item changes.
        PlanStores.planStore().getItem(mvpView.getItemToEditUuid())
    }

    private fun loadViewData() {
        mvpView?.loadPlanDetails(toEdit!!, isThisPlanStarted())
    }

    private fun isThisPlanStarted(): Boolean {
        return toEdit?.uuid?.equals(planProgress.ongoingPlan()?.uuid) == true
    }

    // Handlers

    private fun handleToggleStartPlan() {
        when {
            isThisPlanStarted() ->
                // Stop plan
                observeStopConfirm()
            planProgress.hasOngoingPlan() -> {
                if (!planCanBeStarted()) {
                    return
                }
                // Switch plan
                observeSwitchConfirm()
            }
            else -> {
                if (!planCanBeStarted()) {
                    return
                }
                // Start plan
                handleStartPlan()
            }
        }
    }

    private fun handleStartPlan() {
        planProgress.setOngoingPlan(toEdit)
        analytics.started()
        LocalBroadcastManager.getInstance(mvpView.context).sendBroadcast(Intent(PlanBroadcasts.CURRENT_PLAN_STARTED))
        mvpView?.getActivity()?.let { navigator.open(it, Home) }
    }

    private fun handleDeletePlan(plan: ELPlan) {
        // Delete plan from database
        PlanStores.planStore().delete(plan)
        analytics.deleted()
        // If local plan is the same, clear it too
        if (planProgress.isOngoing(plan)) {
            planProgress.clearOngoingPlan()
        }
        mvpView?.closeScreen()
    }

    private fun planCanBeStarted(): Boolean {
        if (toEdit?.hasWeeksWithWorkouts() == false) {
            mvpView.showToast(R.string.plan_details_start_no_workouts)
            return false
        }
        return true
    }
}