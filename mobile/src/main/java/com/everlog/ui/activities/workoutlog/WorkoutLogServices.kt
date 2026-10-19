package com.everlog.ui.activities.workoutlog

import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.PlanManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.preferences.SettingsManager
import com.google.firebase.firestore.SetOptions
import java.util.Date

/**
 * Where the workout screen's workout is kept: the ongoing copy, which resumes it, and the finished one. As the old
 * screen's PresenterWorkout does it.
 */
internal interface WorkoutLogStore {
    // The saved copy of the given workout if it's the ongoing one, with everything done since it started
    fun savedCopyOf(workout: ELWorkout): ELWorkout?

    fun saveOngoing(workout: ELWorkout)

    fun clearOngoing()

    // Saves the finished workout and checks it off the ongoing plan
    fun saveFinished(workout: ELWorkout, fromPlan: Boolean)

    fun weightUnit(): SettingsManager.WeightUnit

    // The keypad's ± for weight, in the user's unit
    fun weightIncrease(): Float
}

internal class RealWorkoutLogStore : WorkoutLogStore {
    override fun savedCopyOf(workout: ELWorkout): ELWorkout? = WorkoutManager.manager.savedCopyOf(workout)

    override fun saveOngoing(workout: ELWorkout) = WorkoutManager.manager.setOngoingWorkout(workout)

    override fun clearOngoing() = WorkoutManager.manager.clearOngoingWorkout()

    override fun saveFinished(workout: ELWorkout, fromPlan: Boolean) {
        // Pushes back the app usage reminder, the user may not pass through the home screen for a while
        AppLaunchManager.manager.recordActivity(Date())
        ELDatastore.workoutStore().create(workout, SetOptions.merge())
        PlanManager.manager.checkCompletedWorkout(workout, fromPlan)
    }

    override fun weightUnit(): SettingsManager.WeightUnit = SettingsManager.manager.weightUnit()

    override fun weightIncrease(): Float = SettingsManager.manager.weightIncrease()
}

/**
 * The workout screen's events, the same ones the old screen logs.
 */
internal interface WorkoutLogAnalytics {
    fun setCompleted(type: ELSetType?, timed: Boolean)

    // A superset's round (or another set of several exercises) done
    fun setGroupCompleted(type: ELSetType?, exercises: Int)

    // A done set undone
    fun setEdited(type: ELSetType?, timed: Boolean)

    fun exercisesAdded()

    fun workoutCompleted()

    fun workoutStopped()

    fun discardPromptShown(setsCompleted: Int)

    fun discardPromptFinished()

    fun discardPromptCancelled()
}

internal class RealWorkoutLogAnalytics : WorkoutLogAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun setCompleted(type: ELSetType?, timed: Boolean) =
        analytics.setCompleted(AnalyticsConstants.SET_COMPLETED_SOURCE_SCREEN, type, timed)

    override fun setGroupCompleted(type: ELSetType?, exercises: Int) =
        analytics.setGroupCompleted(AnalyticsConstants.SET_COMPLETED_SOURCE_SCREEN, type, exercises)

    override fun setEdited(type: ELSetType?, timed: Boolean) = analytics.setEdited(type, timed)

    override fun exercisesAdded() = analytics.routineExerciseGroupAdded()

    override fun workoutCompleted() = analytics.workoutCompleted()

    override fun workoutStopped() = analytics.workoutStopped()

    override fun discardPromptShown(setsCompleted: Int) =
        analytics.workoutDiscardPromptShown(AnalyticsConstants.DISCARD_PROMPT_SOURCE_WORKOUT, setsCompleted)

    override fun discardPromptFinished() = analytics.workoutDiscardPromptFinished()

    override fun discardPromptCancelled() = analytics.workoutDiscardPromptCancelled(AnalyticsConstants.DISCARD_PROMPT_SOURCE_WORKOUT)
}
