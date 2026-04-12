package com.everlog.ui.activities.home.workout

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.R
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.workoutprefill.WorkoutPrefillController
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.managers.PlanManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.home.exercisegroup.PresenterCreateExerciseGroups
import com.everlog.ui.activities.home.workout.utils.*
import com.everlog.ui.activities.home.workout.utils.TickTimeController.OnTimeTick
import com.everlog.utils.Utils
import com.google.firebase.firestore.SetOptions
// import icepick.State
import rx.Observable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class PresenterWorkout : PresenterCreateExerciseGroups<MvpViewWorkout>() {

    private val SERVICE_INTERACTION_DELAY = 150

    private val REQUEST_CODE_POST_NOTIFICATIONS = 151

    // @State
    @JvmField
    var mWorkout: ELWorkout? = null

    // Controllers

    private var mTickTimeController: TickTimeController? = null
    private var mWorkoutSetController: WorkoutSetController? = null
    private var mRestTimeController: RestTimeController? = null
    private var mExerciseTimeController: ExerciseTimeController? = null

    override fun onReady() {
        super.onReady()
        setupState()
        setupControllers()
        observeMuscleGoalClick()
        observeEmptyActionClick()
        // Timers
        observeWorkoutTimersStopped()
        // Workout service
        observeServiceIncreaseWeightClick()
        observeServiceDecreaseWeightClick()
        observeServiceIncreaseRepsClick()
        observeServiceDecreaseRepsClick()
        observeServiceTimerExerciseStartClick()
        observeServiceTimerExerciseStopClick()
        observeServiceTimerRestStopClick()
        observeServiceCompleteSetClick()

        notifyWorkoutServiceStart()
        LocalBroadcastManager.getInstance(mvpView.context).sendBroadcast(Intent(ELConstants.BROADCAST_WORKOUT_STARTED))
        loadWorkout()
    }

    override fun detachView() {
        stopAllTimers()
        notifyWorkoutServiceStop()
        super.detachView()
    }

    override fun onActivityResumed() {
        super.onActivityResumed()
        mTickTimeController?.ensureTimerWhenActivityResumed()
    }

    internal fun onRequestPermissionsResult(requestCode: Int) {
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            navigator.startWorkoutService(mWorkout)
        }
    }

    override fun onBackPressedConsumed(): Boolean {
        if (mSelectedExercises.isNotEmpty()) {
            return super.onBackPressedConsumed()
        }
        observeStopWorkout()
        return true
    }

    override fun allowSetCompletion(): Boolean {
        return true
    }

    override fun allowWorkoutOptions(): Boolean {
        return false
    }

    override fun allowSetTemplates(): Boolean {
        return false
    }

    override fun onPreferencesChanged() {
        super.onPreferencesChanged()
        workoutTimeTick()
        notifyWorkoutServiceSetUpdated()
    }

    // Observers

    private fun observeMuscleGoalClick() {
        subscriptions.add(mvpView.onClickMuscleGoalStatus()
                .compose(applyUISchedulers())
                .subscribe({
                    AnalyticsManager.manager.workoutChangeMuscleGoal()
                    navigator.openMuscleGoal()
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeEmptyActionClick() {
        subscriptions.add(mvpView.onClickEmptyAction()
                .compose(applyUISchedulers())
                .subscribe({
                    navigator.openExercisePicker()
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeWorkoutTimersStopped() {
        subscriptions.add(Observable.merge(mRestTimeController?.onTimeStopped(),
                mExerciseTimeController?.onTimeStopped())
                .compose(applyUISchedulers())
                .subscribe({ workoutTimeTick() })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceIncreaseWeightClick() {
        subscriptions.add(mvpView.onClickServiceIncreaseWeight()
                .onBackpressureDrop()
                .throttleFirst(SERVICE_INTERACTION_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ handleServiceSetModifyWeight(it, SettingsManager.manager.weightIncrease()) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceDecreaseWeightClick() {
        subscriptions.add(mvpView.onClickServiceDecreaseWeight()
                .onBackpressureDrop()
                .throttleFirst(SERVICE_INTERACTION_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ handleServiceSetModifyWeight(it, -SettingsManager.manager.weightIncrease()) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceIncreaseRepsClick() {
        subscriptions.add(mvpView.onClickServiceIncreaseReps()
                .onBackpressureDrop()
                .throttleFirst(SERVICE_INTERACTION_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ handleServiceSetModifyReps(it, 1) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceDecreaseRepsClick() {
        subscriptions.add(mvpView.onClickServiceDecreaseReps()
                .onBackpressureDrop()
                .throttleFirst(SERVICE_INTERACTION_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ handleServiceSetModifyReps(it, -1) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceTimerExerciseStartClick() {
        subscriptions.add(mvpView.onClickServiceTimerExerciseStart()
                .onBackpressureDrop()
                .throttleFirst(SERVICE_INTERACTION_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ handleServiceSetModifyTime(it, true) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceTimerExerciseStopClick() {
        subscriptions.add(mvpView.onClickServiceTimerExerciseStop()
                .onBackpressureDrop()
                .throttleFirst(SERVICE_INTERACTION_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .compose(applyUISchedulers())
                .subscribe({ handleServiceSetModifyTime(it, false) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceTimerRestStopClick() {
        subscriptions.add(mvpView.onClickServiceTimerRestStop()
                .compose(applyUISchedulers())
                .subscribe({ mRestTimeController?.stopTimer(true) })
                { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeServiceCompleteSetClick() {
        subscriptions.add(mvpView.onClickServiceCompleteSet()
                .compose(applyUISchedulers())
                .subscribe({
                    handleServiceSetComplete(it)
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeStopWorkout() {
        subscriptions.add(mvpView.showPrompt(R.string.workout_stop_prompt, R.string.workout_stop_description, R.string.stop, R.string.cancel)
                .compose(applyUISchedulers())
                .subscribe({ action: Int ->
                    if (action == DialogInterface.BUTTON_POSITIVE) {
                        WorkoutManager.manager.clearOngoingWorkout()
                        AnalyticsManager.manager.workoutStopped()
                        mvpView.closeScreen()
                    }
                }) { throwable: Throwable? -> handleError(throwable) })
    }

    private fun observeFinishWorkout() {
        if (mWorkout?.hasExercises() == false
                || mWorkout?.getNextIncompleteState(false) == null) {
            // Everything complete
            saveWorkout()
        } else {
            // Incomplete reps
            subscriptions.add(mvpView.showPrompt(R.string.workout_finish_title, R.string.workout_finish_subtitle, R.string.workout_finish, R.string.cancel)
                    .compose(applyUISchedulers())
                    .subscribe({ action: Int ->
                        if (action == DialogInterface.BUTTON_POSITIVE) {
                            saveWorkout()
                        }
                    }) { throwable: Throwable? -> handleError(throwable) })
        }
    }

    // Loading

    private fun loadWorkout() {
        if (mSelectedGroups.isNotEmpty()) {
            // In case the screen was killed
            (mWorkout?.getExerciseGroups() as? MutableList<ELExerciseGroup>)?.clear()
            (mWorkout?.getExerciseGroups() as? MutableList<ELExerciseGroup>)?.addAll(mSelectedGroups)
        }
        loadExistingGroupsData(ArrayList<ELExerciseGroup>(mWorkout!!.getExerciseGroups()))
        changesMade(false)
    }

    // Time tick

    private fun workoutTimeTick() {
        if (isAttachedToView) {
            mWorkoutSetController?.workoutTimerTick()
            if (mRestTimeController?.isActive() == true) {
                mRestTimeController?.workoutTimerTick()
            } else if (mExerciseTimeController?.isActive() == true) {
                mExerciseTimeController?.workoutTimerTick()
            }
        }
    }

    private fun toggleSetTimer(exercise: ELRoutineExercise,
                               set: ELSet,
                               start: Boolean) {
        // Cancel other timers if running
        stopTimer(mRestTimeController)
        stopTimer(mExerciseTimeController)
        mExerciseTimeController?.setRunningData(exercise, set)
        if (start) {
            mExerciseTimeController?.startTimer(set.getTimeSeconds())
            workoutTimeTick()
        } else {
            stopTimer(mExerciseTimeController)
        }
    }

    private fun stopTimer(timer: BaseWorkoutTimeController?) {
        if (timer?.isActive() == true) {
            timer.stopTimer(true)
        }
    }

    private fun stopAllTimers() {
        stopTimer(mRestTimeController)
        stopTimer(mExerciseTimeController)
        mTickTimeController?.cancelTimer()
    }

    override fun setTimerStarted(exercise: ELRoutineExercise, set: ELSet) {
        super.setTimerStarted(exercise, set)
        toggleSetTimer(exercise, set, true)
    }

    override fun setTimerChanged(exercise: ELRoutineExercise, set: ELSet) {
        super.setTimerChanged(exercise, set)
        // Update exercise timer if already running
        if (mExerciseTimeController?.isActive() == true) {
            toggleSetTimer(exercise, set, true)
        } else {
            mvpView?.getOnboardingController()?.checkOnboarding()
        }
    }

    // Handlers

    override fun handleExerciseGroupsSelected(groups: List<ELExerciseGroup>) {
        super.handleExerciseGroupsSelected(groups)
        prefillWorkout()
    }

    override fun handleEditExerciseEvent(exercise: ELExercise) {
        super.handleEditExerciseEvent(exercise)
        notifyWorkoutServiceSetUpdated()
    }

    private fun handleServiceSetComplete(state: ELWorkoutState) {
        val group = mWorkout?.getExerciseGroups()?.get(state.groupIndex)
        val exercise = group?.getExercisesForSetIndex(state.setIndex)?.get(state.exerciseIndex)
        val set = exercise?.sets?.get(state.setIndex)
        set?.updateStartedDate(Date().time)
        set?.updateCompletedDate(Date().time)
        // Stop exercise timer if already running
        stopTimer(mExerciseTimeController)
        // Only call this is the overall set has been completed
        notifyWorkoutServiceSetUpdated()
        if (group?.setIsComplete(state.setIndex) == true) {
            setCompleted(group)
        }
        Utils.runWithDelay({
            saveOngoingWorkout()
            mAdapter.notifyDataSetChanged()
        }, 300)
    }

    private fun handleServiceSetModifyWeight(state: ELWorkoutState, offset: Float) {
        val group = mWorkout?.getExerciseGroups()?.get(state.groupIndex)
        val exercise = group?.getExercisesForSetIndex(state.setIndex)?.get(state.exerciseIndex)
        exercise?.modifyWeight(offset, state.setIndex)
        mAdapter.notifyDataSetChanged()
        // This causes an extra BROADCAST_SERVICE_SET_UPDATED event but is needed for when the app is in background.
        notifyWorkoutServiceSetUpdated()
        saveOngoingWorkout()
    }

    private fun handleServiceSetModifyReps(state: ELWorkoutState, offset: Int) {
        val group = mWorkout?.getExerciseGroups()?.get(state.groupIndex)
        val exercise = group?.getExercisesForSetIndex(state.setIndex)?.get(state.exerciseIndex)
        exercise?.modifyReps(offset, state.setIndex)
        mAdapter.notifyDataSetChanged()
        // This causes an extra BROADCAST_SERVICE_SET_UPDATED event but is needed for when the app is in background.
        notifyWorkoutServiceSetUpdated()
        saveOngoingWorkout()
    }

    private fun handleServiceSetModifyTime(state: ELWorkoutState, start: Boolean) {
        val group = mWorkout?.getExerciseGroups()?.get(state.groupIndex)
        val exercise = group?.getExercisesForSetIndex(state.setIndex)?.get(state.exerciseIndex)
        val set = exercise?.sets?.get(state.setIndex)
        toggleSetTimer(exercise!!, set!!, start)
    }

    // Workout service

    private fun notifyWorkoutServiceStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(mvpView.context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mvpView.getActivity()!!, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
                return
            }
        }
        navigator.startWorkoutService(mWorkout)
    }

    private fun notifyWorkoutServiceStop() {
        navigator.stopWorkoutService()
    }

    private fun notifyWorkoutServiceSetUpdated() {
        navigator.notifyWorkoutServiceSetUpdated(mWorkout)
    }

    // Set changes

    override fun setCompleted(group: ELExerciseGroup?) {
        super.setCompleted(group)
        if (group?.hasRestTime() == true) {
            mRestTimeController?.startTimer(group.restTimeSeconds)
        }
    }

    // Save

    override fun performSave(exerciseGroups: ArrayList<ELExerciseGroup>) {
        mWorkout?.setExerciseGroups(exerciseGroups)
        observeFinishWorkout()
    }

    private fun saveOngoingWorkout() {
        WorkoutManager.manager.setOngoingWorkout(mWorkout!!)
    }

    private fun saveWorkout() {
        WorkoutManager.manager.clearOngoingWorkout()
        AnalyticsManager.manager.workoutCompleted()
        AppLaunchManager.manager.rateActionTrigger()
        // Save workout
        val toSave = buildChangedItem()
        ELDatastore.workoutStore().create(toSave!!, SetOptions.merge())
        // Check ongoing plan
        PlanManager.manager.checkCompletedWorkout(toSave, mvpView.performingFromPlan())
        // Show summary
        navigator.openWorkoutDetails(toSave, true, mvpView.performingFromPlan())
        mvpView.closeScreen()
    }

    // Workout prefilling

    private fun prefillWorkout() {
        if (mWorkout?.hasExercises() == true && SettingsManager.manager.muscleGoal().canPrefill()) {
            WorkoutPrefillController.prefillWorkout(mWorkout!!, object : WorkoutPrefillController.OnExercisePrefillListener {

                override fun onSuccess() {
                    if (isAttachedToView) {
                        mAdapter.notifyDataSetChanged()
                        notifyWorkoutServiceSetUpdated()
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (isAttachedToView) {
                        handleError(throwable)
                    }
                }
            })
        }
    }

    // Item changes

    override fun changesMade(refreshList: Boolean) {
        super.changesMade(refreshList)
        mWorkout?.setExerciseGroups(mSelectedGroups)
        mvpView?.loadWorkoutDetails(mWorkout!!, mGroupsDataListManager.count > 0)
        saveOngoingWorkout()
        notifyWorkoutServiceSetUpdated()
    }

    private fun buildChangedItem(): ELWorkout? {
        mWorkout?.setCompletedDateAsDate(Date())
        return mWorkout
    }

    // Setup

    private fun setupState() {
        var shouldPrefillWeight = false
        if (mWorkout == null) {
            shouldPrefillWeight = true
            mWorkout = mvpView.getWorkout()
            // Make sure we prefill with our required values
            mWorkout?.prefillRequiredMetrics()
        }
        if (shouldPrefillWeight) {
            prefillWorkout()
        }
    }

    private fun setupControllers() {
        mTickTimeController = TickTimeController(mvpView.context, object : OnTimeTick {

            override fun onTimeTick() {
                workoutTimeTick()
            }
        })
        mWorkoutSetController = WorkoutSetController(mWorkout!!, mvpView, mvpView.getBinding())
        mRestTimeController = RestTimeController(mvpView, mWorkout!!, mvpView.getBinding(), navigator)
        mExerciseTimeController = ExerciseTimeController(mvpView, mWorkout!!, mvpView.getBinding(), mAdapter, navigator)
    }
}