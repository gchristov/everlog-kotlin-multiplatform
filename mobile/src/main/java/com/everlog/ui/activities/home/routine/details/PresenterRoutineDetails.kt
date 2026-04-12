package com.everlog.ui.activities.home.routine.details

import android.content.DialogInterface
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.routines.ELUserRoutineStore
import com.everlog.data.model.ELRoutine
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.adapters.exercise.group.ExerciseGroupSummaryAdapter
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open class PresenterRoutineDetails<T : MvpViewRoutineDetails> : BaseActivityPresenter<T>() {

    private var toEdit: ELRoutine? = null
    private var loadedItemOnce = false
    @JvmField
    protected val mAdapter = RecyclerAdapter()
    @JvmField
    protected val mDataListManager = DataListManager<Any>(mAdapter)

    override fun init() {
        super.init()
        setupListView()
    }

    override fun onReady() {
        setupEditedItem()
        observeEditClick()
        observeDeleteClick()
        observePerformClick()
        loadRoutine()
    }

    override fun shouldCloseOnWorkoutStart(): Boolean {
        return true
    }

    override fun detachView() {
        ELDatastore.routineStore().destroy()
        super.detachView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRoutineLoaded(event: ELUserRoutineStore.ELDocStoreRoutineLoadedEvent) {
        if (isAttachedToView) {
            if (event.error != null) {
                handleError(event.error)
            } else {
                if (!loadedItemOnce || event.isHasPendingWrites) {
                    toEdit = event.item
                    loadViewData()
                }
                loadedItemOnce = true
            }
        }
    }

    fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    // Observers

    private fun observeEditClick() {
        subscriptions.add(mvpView.onClickEdit()
                .compose(applyUISchedulers())
                .subscribe {
                    navigator.openEditRoutine(CreateRoutineActivity.Companion.Properties()
                            .routine(toEdit)
                            .showDetailsOnSuccess(false))
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

    private fun observeDeleteConfirm(routine: ELRoutine) {
        subscriptions.add(mvpView.showPrompt(R.string.delete_title, R.string.delete_prompt, R.string.delete, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe { action: Int ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        handleDeleteRoutine(routine)
                    }
                })
    }

    private fun observePerformClick() {
        subscriptions.add(mvpView.onClickPerform()
                .compose(applyUISchedulers())
                .subscribe {
                    if (toEdit?.canBePerformed() == true) {
                        navigator.startWorkout(toEdit, true, performingFromPlan())
                    } else {
                        mvpView.showOK(R.string.routines_cannot_perform_title, R.string.routines_cannot_perform_prompt)
                    }
                })
    }

    // Loading

    private fun loadRoutine() {
        loadViewData()
        // Listen for item changes
        ELDatastore.routineStore().getItem(toEdit!!.uuid)
    }

    protected open fun loadViewData() {
        // Show routine immediately
        mvpView?.loadItemDetails(toEdit!!)
        mDataListManager.clear()
        mDataListManager.addAll(toEdit!!.exerciseGroups)
        mAdapter.notifyDataSetChanged()
        handleCheckEmptyState()
    }

    // Handlers

    private fun handleCheckEmptyState() {
        mvpView?.toggleEmptyViewVisible(mDataListManager.isEmpty)
    }

    private fun handleDeleteRoutine(routine: ELRoutine) {
        ELDatastore.routineStore().delete(routine)
        AnalyticsManager.manager.routineDeleted()
        // Replace routine for ongoing plan
        PlanManager.manager.deleteRoutineForPlan(mvpView.context, routine)
        mvpView?.closeScreen()
    }

    protected open fun performingFromPlan(): Boolean {
        return false
    }

    // Setup

    protected open fun setupListView() {
        mAdapter.addDataManager(mDataListManager)
        mAdapter.registerBinders(ExerciseGroupSummaryAdapter().build(ExerciseGroupSummaryAdapter.Builder()
                .setShowTemplates(true)
        ))
    }

    private fun setupEditedItem() {
        toEdit = mvpView.getItemToEdit()
    }
}