package com.everlog.ui.activities.home.exercisegroup

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.ahamed.multiviewadapter.DataListManager
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.config.AppConfig
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.exercises.ELExercisesStore.ELColStoreExerciseModifiedEvent
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.util.ExerciseGroupCreateHeader
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.base.BaseActivityPresenter
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.adapters.exercise.group.ExerciseGroupCreateAdapter
import com.everlog.ui.adapters.exercise.group.ExerciseGroupCreateHeaderAdapter
import com.everlog.ui.dialog.DialogBuilder
// import icepick.State
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class PresenterCreateExerciseGroups<T : MvpViewCreateExerciseGroups> : BaseActivityPresenter<T>() {

    // @State
    @JvmField
    var mChangesMade = false
    // @State
    @JvmField
    var mSelectedGroups = ArrayList<ELExerciseGroup>()
    // @State
    @JvmField
    var mSelectedGroup: ELExerciseGroup? = null
    // @State
    @JvmField
    var mSelectedCommonRestTimeSeconds = AppConfig.configuration.defaultRestTimeSeconds
    // @State
    @JvmField
    var mSelectedCommonExerciseTimeSeconds = 0 // -1 - mixed, 0 - not set, >0 is the value
    // IcePick doesn't like the LinkedHashMap here so it will be a known bug that selection doesn't persist if activity is killed
    internal var mSelectedExercises = LinkedHashMap<ELRoutineExercise, ELExerciseGroup>()

    internal val mAdapter = RecyclerAdapter()
    private val mGroupSettingsDataListManager = DataListManager<ExerciseGroupCreateHeader>(mAdapter)
    internal val mGroupsDataListManager = DataListManager<ELExerciseGroup>(mAdapter)

    override fun init() {
        super.init()
        setupListView()
    }

    override fun onReady() {
        setupState()
        observeSaveClick()
        observeAddClick()
        observeSelectionCancelClick()
        observeSelectionInfoClick()
        observeSelectionLinkClick()
        observeSelectionDeleteClick()
        checkEditingGroups()
        loadWorkoutSettings()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Persist the order of the exercises
        buildExerciseGroups()
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressedConsumed(): Boolean {
        return when {
            mSelectedExercises.isNotEmpty() -> {
                handleSelectionCancel()
                true
            }
            mChangesMade -> {
                observeDiscard()
                true
            }
            else -> super.onBackPressedConsumed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_EXERCISES) {
            if (resultCode == Activity.RESULT_OK) {
                handleExerciseGroupsSelected(data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as ArrayList<ELExerciseGroup>)
            }
        } else if (requestCode == ELActivityRequestCodes.REQUEST_PICK_SET_TYPE) {
            if (resultCode == Activity.RESULT_OK) {
                handleEditSetType(data?.getSerializableExtra(ELConstants.EXTRA_SET_TYPE) as ELSetType)
            }
        }
        mSelectedGroup = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onExerciseModified(event: ELColStoreExerciseModifiedEvent) {
        handleEditExerciseEvent(event.item)
    }

    internal fun getListAdapter(): RecyclerAdapter {
        return mAdapter
    }

    internal fun isInSelectionMode(): Boolean {
        return mSelectedExercises.isNotEmpty()
    }

    internal open fun allowSetCompletion(): Boolean {
        return false
    }

    internal open fun allowSetTemplates(): Boolean {
        return true
    }

    internal open fun allowWorkoutOptions(): Boolean {
        return true
    }

    // Observers

    private fun observeSaveClick() {
        subscriptions.add(mvpView.onClickSave()
                .compose(applyUISchedulers())
                .subscribe {
                    handleSave()
                })
    }

    private fun observeAddClick() {
        subscriptions.add(mvpView.onClickAdd()
                .compose(applyUISchedulers())
                .subscribe { navigator.openExercisePicker() })
    }

    private fun observeRestTimeConfirm() {
        subscriptions.add(mvpView.showPickerDuration(mSelectedCommonRestTimeSeconds, if (!allowSetTemplates()) DialogBuilder.DurationPickerDialogType.REST_TIME else DialogBuilder.DurationPickerDialogType.REST_TIME_REQUIRED)
                .compose(applyUISchedulers())
                .subscribe({ value: Int ->
                    mSelectedCommonRestTimeSeconds = value
                    AnalyticsManager.manager.setRestTimeModified(value)
                    changesMade(false)
                    loadWorkoutSettings()
                }) { throwable: Throwable? -> handleError(throwable) } )
    }

    private fun observeExerciseTimeConfirm() {
        val default = if (mSelectedCommonExerciseTimeSeconds <= 0) AppConfig.configuration.defaultExerciseTimeSeconds else mSelectedCommonExerciseTimeSeconds
        subscriptions.add(mvpView.showPickerDuration(default, if (!allowSetTemplates()) DialogBuilder.DurationPickerDialogType.EXERCISE_TIME else DialogBuilder.DurationPickerDialogType.EXERCISE_TIME_REQUIRED)
                .compose(applyUISchedulers())
                .subscribe({ value: Int ->
                    mSelectedCommonExerciseTimeSeconds = value
                    AnalyticsManager.manager.setRequiredTimeModified(value)
                    changesMade(false)
                    // Force the exercise time to all sets
                    buildExerciseGroups()
                    mAdapter.notifyDataSetChanged()
                    loadWorkoutSettings()
                }) { throwable: Throwable? -> handleError(throwable) } )
    }

    private fun observeSelectionCancelClick() {
        subscriptions.add(mvpView.onClickSelectionCancel()
                .compose(applyUISchedulers())
                .subscribe {
                    handleSelectionCancel()
                })
    }

    private fun observeSelectionInfoClick() {
        subscriptions.add(mvpView.onClickSelectionInfo()
                .compose(applyUISchedulers())
                .subscribe {
                    if (mSelectedExercises.isNotEmpty()) {
                        navigator.openExerciseDetails(ExerciseDetailsActivity.Companion.Properties()
                                .exercise(mSelectedExercises.keys.first().exercise!!))
                    }
                })
    }

    private fun observeSelectionLinkClick() {
        subscriptions.add(mvpView.onClickSelectionLink()
                .compose(applyUISchedulers())
                .subscribe {
                    navigator.openSetTypePicker(mSelectedExercises.size)
                })
    }

    private fun observeSelectionDeleteClick() {
        subscriptions.add(mvpView.onClickSelectionDelete()
                .compose(applyUISchedulers())
                .subscribe {
                    handleSelectionDelete()
                })
    }

    private fun observeDiscard() {
        subscriptions.add(mvpView.showPrompt(R.string.discard_title, R.string.discard_prompt, R.string.discard, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action: Int ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        mvpView.setViewResult(Activity.RESULT_CANCELED)
                        mvpView.closeScreen()
                    }
                }) { throwable: Throwable? -> handleError(throwable) } )
    }

    // Loading

    protected fun loadExistingGroupsData(items: List<ELExerciseGroup>) {
        mGroupsDataListManager.clear()
        mGroupsDataListManager.addAll(items)
        mSelectedGroups.clear()
        mSelectedGroups.addAll(items)
        checkOnboarding(true)
        if (items.isNotEmpty()) {
            // Rest time (all groups have the same rest time)
            mSelectedCommonRestTimeSeconds = items.first().restTimeSeconds
            loadWorkoutSettings()
        }
    }

    private fun loadWorkoutSettings() {
        // Recalculate exercise time
        var selectedTime: Int? = null
        mSelectedGroups.forEach {
            if (selectedTime == null) {
                // Initial state
                selectedTime = it.getCommonExerciseTime(allowSetTemplates())
            } else if (selectedTime != it.getCommonExerciseTime(allowSetTemplates())) {
                // Safely return -1 if content is mixed
                selectedTime = -1
                return@forEach
            }
        }
        mSelectedCommonExerciseTimeSeconds = selectedTime ?: mSelectedCommonExerciseTimeSeconds
        // Refresh settings in list
        mGroupSettingsDataListManager.clear()
        if (allowWorkoutOptions()) {
            val settingsHeader = ExerciseGroupCreateHeader(mSelectedCommonRestTimeSeconds, mSelectedCommonExerciseTimeSeconds, mSelectedCommonExerciseTimeSeconds == -1)
            mGroupSettingsDataListManager.add(settingsHeader)
        }
        mAdapter.notifyDataSetChanged()
    }

    // Handlers

    private fun handleSave() {
        buildExerciseGroups()
        performSave(mSelectedGroups)
    }

    protected open fun performSave(exerciseGroups: ArrayList<ELExerciseGroup>) {
        val intent = Intent()
        intent.putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, exerciseGroups)
        mvpView.setViewResult(Activity.RESULT_OK, intent)
        mvpView.closeScreen()
    }

    protected open fun handleExerciseGroupsSelected(groups: List<ELExerciseGroup>) {
        groups.forEach { group ->
            // Add common group settings
            group.exercises.forEach { routineExercise ->
                if (allowWorkoutOptions()) {
                    // Required exercise time
                    routineExercise.setCommonSettings(mSelectedCommonExerciseTimeSeconds, allowSetTemplates())
                }
            }
            mGroupsDataListManager.add(group)
            mSelectedGroups.add(group)
        }
        AnalyticsManager.manager.routineExerciseGroupAdded()
        mvpView.scrollToBottom()
        checkOnboarding(false)
        changesMade(true)
    }

    private fun handleEditSetType(type: ELSetType) {
        if (mSelectedGroup == null) {
            mergeSelection(type)
        } else {
            mSelectedGroup?.changeSetType(type)
        }
        changesMade(true)
    }

    private fun handleSelectionCancel() {
        mSelectedExercises.clear()
        refreshGroupSelection()
    }

    private fun handleSelectionDelete() {
        mSelectedExercises.forEach {
            val exercise = it.key
            val group = it.value
            if (group.isWithOnlyOneExercise()) {
                mGroupsDataListManager.remove(group)
                mSelectedGroups.remove(group)
            } else {
                group.exercises.remove(exercise)
                if (group.isWithOnlyOneExercise()) {
                    // Make sure sets with one exercise have appropriate type
                    group.changeSetType(ELSetType.SINGLE)
                }
            }
        }
        handleSelectionCancel()
        loadWorkoutSettings()
        changesMade(false)
    }

    protected open fun handleEditExerciseEvent(exercise: ELExercise) {
        if (isAttachedToView && mSelectedGroups.isNotEmpty()) {
            mSelectedGroups.forEach { it.updateExercise(exercise) }
            changesMade(true)
        }
    }

    private fun buildExerciseGroups() {
        mSelectedGroups.clear()
        for (i in 0 until mGroupsDataListManager.count) {
            val group = mGroupsDataListManager[i]
            if (allowWorkoutOptions()) {
                // Required rest time
                group.restTimeSeconds = mSelectedCommonRestTimeSeconds
                // Required exercise time, only if it's not mixed
                group.setCommonSettings(mSelectedCommonExerciseTimeSeconds, allowSetTemplates())
            }
            mSelectedGroups.add(mGroupsDataListManager[i])
        }
    }

    protected open fun changesMade(refreshList: Boolean) {
        mChangesMade = true
        if (refreshList) {
            mAdapter.notifyDataSetChanged()
        }
    }

    private fun checkEditingGroups() {
        val groups = mvpView.getItemsToEdit()
        if (groups != null
                && groups.isNotEmpty()
                && mSelectedGroups.isEmpty() // This state gets saved so if it's empty here it means we never had a state loaded
        ) {
            loadExistingGroupsData(groups)
        }
    }

    // Selection

    private fun mergeSelection(type: ELSetType) {
        val newGroup = ELExerciseGroup.buildDefault(type)
        var first: ELExerciseGroup? = null
        mSelectedExercises.forEach {
            if (first == null) {
                // Keep track of first item
                first = it.value
            }
            newGroup.exercises.add(it.key)
        }
        newGroup.ensureEqualSets()
        // Add new group at the position of the first linked exercise to make it easier for the user
        val insertIndex = mGroupsDataListManager.indexOf(first)
        if (insertIndex >= 0) {
            mGroupsDataListManager.add(insertIndex, newGroup)
            mSelectedGroups.add(insertIndex, newGroup)
            handleSelectionDelete()
        }
    }

    private fun refreshGroupSelection(refreshList: Boolean? = true) {
        mvpView?.toggleContextToolbar(mSelectedExercises.isNotEmpty(), mSelectedExercises.size)
        if (refreshList == true) {
            mAdapter.notifyDataSetChanged()
        }
        changesMade(false)
    }

    private fun toggleSelectedExercise(exercise: ELRoutineExercise, group: ELExerciseGroup) {
        if (mSelectedExercises.contains(exercise)) {
            mSelectedExercises.remove(exercise)
        } else {
            mSelectedExercises[exercise] = group
        }
    }

    // Onboarding

    private fun checkOnboarding(atTop: Boolean) {
        mvpView?.getOnboardingController()?.setAtTop(atTop)
        mvpView?.getOnboardingController()?.checkOnboarding()
    }

    // Timer

    internal open fun setTimerStarted(exercise: ELRoutineExercise, set: ELSet) {
        // No-op
    }

    internal open fun setTimerChanged(exercise: ELRoutineExercise, set: ELSet) {
        // No-op
    }

    // Set changes

    internal open fun setCompleted(group: ELExerciseGroup?) {
        // No-op
    }

    // Setup

    private fun setupState() {
        mGroupsDataListManager.clear()
        mGroupsDataListManager.addAll(mSelectedGroups)
    }

    internal open fun setupListView() {
        // Group settings
        mAdapter.addDataManager(mGroupSettingsDataListManager)
        mAdapter.registerBinder(ExerciseGroupCreateHeaderAdapter.Binder(object : ExerciseGroupCreateHeaderAdapter.OnExerciseGroupCreateHeaderListener {
            override fun onItemClicked(item: ExerciseGroupCreateHeader, position: Int) {}

            override fun onRestTimeClick() {
                observeRestTimeConfirm()
            }

            override fun onExerciseTimeClick() {
                observeExerciseTimeConfirm()
            }
        }))
        // Exercise groups
        mAdapter.addDataManager(mGroupsDataListManager)
        mAdapter.registerBinders(ExerciseGroupCreateAdapter().build(ExerciseGroupCreateAdapter.Builder()
                .setTouchHelper(mAdapter.itemTouchHelper)
                .setAllowSetCompletion(allowSetCompletion())
                .setAllowSetTemplates(allowSetTemplates())
                .setListener(object : ExerciseGroupCreateAdapter.OnExerciseGroupListener {
                    override fun onGroupEdited() {
                        changesMade(false)
                        loadWorkoutSettings()
                    }

                    override fun onGroupDeleted(group: ELExerciseGroup) {
                        mGroupsDataListManager.remove(group)
                        loadWorkoutSettings()
                    }

                    override fun onClickExercise(group: ELExerciseGroup, exercise: ELRoutineExercise) {
                        val wasNotInSelectMode = !hasSelections()
                        toggleSelectedExercise(exercise, group)
                        val isNotInSelectMode = !hasSelections()
                        refreshGroupSelection(wasNotInSelectMode || isNotInSelectMode)
                    }

                    override fun hasSelections(): Boolean {
                        return mSelectedExercises.isNotEmpty()
                    }

                    override fun isSelected(exercise: ELRoutineExercise): Boolean {
                        return mSelectedExercises.containsKey(exercise)
                    }

                    override fun onStartTimer(exercise: ELRoutineExercise, set: ELSet) {
                        setTimerStarted(exercise, set)
                    }

                    override fun onTimeChanged(exercise: ELRoutineExercise, set: ELSet) {
                        setTimerChanged(exercise, set)
                    }

                    override fun onSetCompleted(group: ELExerciseGroup) {
                        setCompleted(group)
                    }
                })))
    }
}

class DefaultPresenterCreateExerciseGroups : PresenterCreateExerciseGroups<MvpViewCreateExerciseGroups>() {
    override fun allowWorkoutOptions(): Boolean {
        return false
    }

    override fun allowSetTemplates(): Boolean {
        return false
    }
}