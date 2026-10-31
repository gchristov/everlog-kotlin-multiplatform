package com.everlog.android.template.feature.details

import com.everlog.android.common.design.R as DesignR
import android.content.DialogInterface
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.template.data.store.ELDocStoreRoutineLoadedEvent
import com.everlog.android.template.data.store.TemplateStores
import com.everlog.android.template.feature.R
import com.everlog.android.template.feature.TemplateFeature
import com.everlog.android.common.mvp.activity.BaseActivityPresenter
import com.everlog.android.common.mvp.list.ExerciseGroupSummaryAdapter
import com.everlog.android.settings.data.SettingsManager
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
        TemplateStores.routineStore().destroy()
        super.detachView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRoutineLoaded(event: ELDocStoreRoutineLoadedEvent) {
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
                    mvpView.getActivity()?.let { TemplateFeature.dependencies.openTemplateEditor(it, toEdit) }
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
        subscriptions.add(mvpView.showDestructivePrompt(DesignR.string.delete_title, DesignR.string.delete_prompt, DesignR.string.delete, DesignR.string.cancel)
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
                        mvpView.getActivity()?.let { TemplateFeature.dependencies.startWorkout(it, toEdit!!, performingFromPlan()) }
                    } else {
                        mvpView.showOK(R.string.routines_cannot_perform_title, R.string.routines_cannot_perform_prompt)
                    }
                })
    }

    // Loading

    private fun loadRoutine() {
        loadViewData()
        // Listen for item changes
        TemplateStores.routineStore().getItem(toEdit!!.uuid)
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
        TemplateStores.routineStore().delete(routine)
        TemplateFeature.dependencies.templateAnalytics().routineDeleted()
        // Replace routine for ongoing plan
        mvpView.getActivity()?.let { TemplateFeature.dependencies.removeFromOngoingPlan(it, routine) }
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
                .setWeightUnit(SettingsManager.manager.weightUnit())
                .setOnExerciseClicked { context, exercise -> TemplateFeature.dependencies.openExerciseDetails(context, exercise) }
        ))
    }

    private fun setupEditedItem() {
        toEdit = mvpView.getItemToEdit()
    }
}