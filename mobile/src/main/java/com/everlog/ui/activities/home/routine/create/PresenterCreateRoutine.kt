package com.everlog.ui.activities.home.routine.create

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import com.ahamed.multiviewadapter.DataListManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.ELRoutine.Companion.buildNewRoutine
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.activities.home.exercisegroup.PresenterCreateExerciseGroups
import com.everlog.ui.adapters.routine.RoutineCreateHeaderAdapter
import com.google.firebase.firestore.SetOptions
// import icepick.State
import java.util.*

class PresenterCreateRoutine : PresenterCreateExerciseGroups<MvpViewCreateRoutine>() {

    // @State
    @JvmField
    var toEdit: ELRoutine? = null
    // @State
    @JvmField
    var mLocalChangesMade = true // So that the user can create an empty routine
    private var mEditMode = false

    private val mRoutineDataListManager = DataListManager<ELRoutine>(mAdapter)

    override fun onReady() {
        super.onReady()
        setupEditedItem()
        loadRoutine()
    }

    override fun onBackPressedConsumed(): Boolean {
        return if (mLocalChangesMade) {
            val superConsumed = super.onBackPressedConsumed()
            if (!superConsumed) {
                observeDiscard()
            }
            true
        } else {
            super.onBackPressedConsumed()
        }
    }

    override fun changesMade(refreshList: Boolean) {
        super.changesMade(refreshList)
        toEdit?.exerciseGroups = ArrayList(mSelectedGroups)
        mLocalChangesMade = true
    }

    // Observers

    private fun observeEditNameConfirm() {
        subscriptions.add(mvpView.showNamePrompt(toEdit?.name)
                .compose(applyUISchedulers())
                .subscribe { newName ->
                    toEdit?.name = newName
                    loadRoutineDetails()
                    mLocalChangesMade = true
                })
    }

    private fun observeDiscard() {
        subscriptions.add(mvpView.showPrompt(R.string.discard_title, R.string.discard_prompt, R.string.discard, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action: Int ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        mvpView?.closeScreen()
                    }
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    // Loading

    private fun loadRoutine() {
        loadRoutineDetails()
        loadExistingGroupsData(ArrayList(toEdit!!.exerciseGroups))
    }

    private fun loadRoutineDetails() {
        mRoutineDataListManager.clear()
        mRoutineDataListManager.add(toEdit!!)
        mAdapter.notifyDataSetChanged()
    }

    // Handlers

    override fun performSave(exerciseGroups: ArrayList<ELExerciseGroup>) {
        val name = toEdit?.name
        if (name?.isEmpty() == true) {
            mvpView?.showToast(R.string.create_routine_error_no_title)
        } else {
            if (mLocalChangesMade) {
                val toSave = buildChangedItem(exerciseGroups)
                ELDatastore.routineStore().create(toSave, SetOptions.merge())
                mvpView?.showToast(R.string.create_routine_saved)
                if (mEditMode) {
                    AnalyticsManager.manager.routineModified()
                    // Replace routine for ongoing plan
                    PlanManager.manager.updateRoutineForPlan(mvpView.context, toSave!!)
                } else {
                    AnalyticsManager.manager.routineCreated()
                    AppLaunchManager.manager.rateActionTrigger()
                    if (mvpView?.shouldOpenDetailsOnSuccess() == true) {
                        navigator.openRoutineDetails(toSave, false)
                    }
                }
                val i = Intent()
                i.putExtra(ELConstants.EXTRA_ROUTINE, toSave)
                mvpView?.setViewResult(Activity.RESULT_OK, i)
            }
            mvpView?.closeScreen()
        }
    }

    // Item changes

    private fun buildChangedItem(exerciseGroups: List<ELExerciseGroup>): ELRoutine? {
        toEdit?.exerciseGroups?.clear()
        toEdit?.exerciseGroups?.addAll(exerciseGroups)
        return toEdit
    }

    // Setup

    private fun setupEditedItem() {
        mEditMode = mvpView?.getItemToEdit() != null
        if (toEdit == null) {
            toEdit = mvpView?.getItemToEdit()
            if (toEdit == null) {
                toEdit = buildNewRoutine(getUserAccount()?.id!!)
            } else {
                // If we're editing a routine, don't assume changes made
                mLocalChangesMade = false
            }
        }
    }

    override fun setupListView() {
        super.setupListView()
        mAdapter.addDataManager(0, mRoutineDataListManager)
        mAdapter.registerBinder(RoutineCreateHeaderAdapter.Binder { _, _ ->
            observeEditNameConfirm()
        })
    }
}