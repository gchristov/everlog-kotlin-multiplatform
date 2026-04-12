package com.everlog.ui.activities.home.plan.details

import android.content.DialogInterface
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.plans.ELUserPlanStore
import com.everlog.data.model.plan.ELPlan
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PresenterPlanDetails : BaseActivityPresenter<MvpViewPlanDetails>() {

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
        ELDatastore.planStore().destroy()
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
    fun onPlanLoaded(event: ELUserPlanStore.ELDocStorePlanLoadedEvent) {
        if (isAttachedToView) {
            mvpView?.toggleLoadingOverlay(false)
            if (event.error != null) {
                handleError(event.error)
            } else {
                if (!mLoadedItemOnce || event.isHasPendingWrites) {
                    toEdit = event.item
                    mRealPlan = event.item
                    if (PlanManager.manager.isOngoing(toEdit)) {
                        toEdit = PlanManager.manager.ongoingPlan()
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
                        navigator.openEditPlan(toEdit)
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
        subscriptions.add(mvpView.showPrompt(R.string.delete_title, R.string.delete_prompt, R.string.delete, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleDeletePlan(plan)
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeStopConfirm() {
        subscriptions.add(mvpView.showPrompt(R.string.plan_details_stop, R.string.plan_details_stop_prompt, R.string.stop, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        PlanManager.manager.clearOngoingPlan()
                        toEdit = mRealPlan
                        loadViewData()
                        AnalyticsManager.manager.planStopped()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observeSwitchConfirm() {
        subscriptions.add(mvpView.showPrompt(R.string.plan_details_switch, R.string.plan_details_switch_prompt, R.string.plan_details_switch, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleStartPlan()
                    }
                }, { throwable -> handleError(throwable) }))
    }

    private fun observePlanNotFoundConfirm() {
        val planUuid = mvpView.getItemToEditUuid()
        val currentPlanUuid = PlanManager.manager.ongoingPlan()?.uuid
        if (planUuid.equals(currentPlanUuid)) {
            // We didn't find the current user plan
            subscriptions.add(mvpView.showPrompt(R.string.plan_details_not_found, R.string.plan_details_not_found_prompt_current, R.string.stop, R.string.continue_)
                    .compose(applyUISchedulers())
                    .subscribe({ action ->
                        if (action == DialogInterface.BUTTON_POSITIVE) {
                            PlanManager.manager.clearOngoingPlan()
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
        ELDatastore.planStore().getItem(mvpView.getItemToEditUuid())
    }

    private fun loadViewData() {
        mvpView?.loadPlanDetails(toEdit!!, isThisPlanStarted())
    }

    private fun isThisPlanStarted(): Boolean {
        return toEdit?.uuid?.equals(PlanManager.manager.ongoingPlan()?.uuid) == true
    }

    // Handlers

    private fun handleToggleStartPlan() {
        when {
            isThisPlanStarted() ->
                // Stop plan
                observeStopConfirm()
            PlanManager.manager.hasOngoingPlan() -> {
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
        PlanManager.manager.setOngoingPlan(toEdit)
        AnalyticsManager.manager.planStarted()
        LocalBroadcastManager.getInstance(mvpView.context).sendBroadcast(Intent(ELConstants.BROADCAST_CURRENT_PLAN_STARTED))
        navigator.openHome()
    }

    private fun handleDeletePlan(plan: ELPlan) {
        // Delete plan from database
        ELDatastore.planStore().delete(plan)
        AnalyticsManager.manager.planDeleted()
        // If local plan is the same, clear it too
        if (PlanManager.manager.isOngoing(plan)) {
            PlanManager.manager.clearOngoingPlan()
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