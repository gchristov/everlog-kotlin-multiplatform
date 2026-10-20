package com.everlog.ui.activities.workoutlog

import android.content.Context
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.PlanManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.utils.SoundUtils
import com.everlog.utils.VibrationUtils
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
}

/**
 * The settings the workout screen follows. Read again each time it shows, as they can change in Settings meanwhile.
 */
internal interface WorkoutLogSettings {
    fun weightUnit(): SettingsManager.WeightUnit

    // The keypad's ± for a weight, in the user's unit
    fun weightIncrease(): Float

    fun keepScreenOn(): Boolean
}

internal class RealWorkoutLogSettings : WorkoutLogSettings {
    override fun weightUnit(): SettingsManager.WeightUnit = SettingsManager.manager.weightUnit()

    override fun weightIncrease(): Float = SettingsManager.manager.weightIncrease()

    override fun keepScreenOn(): Boolean = SettingsManager.manager.keepScreenOn()
}

/**
 * The sound and vibration when a countdown runs out. Called from the view-model rather than the screen, so they
 * play with the phone locked, as the old screen's do.
 */
internal interface WorkoutLogAlerts {
    fun timerRanOut()
}

internal class RealWorkoutLogAlerts(private val context: Context) : WorkoutLogAlerts {
    override fun timerRanOut() {
        SoundUtils.playTimerSound(context)
        VibrationUtils.vibrate(context)
    }
}

/**
 * The workout screen's events, the same ones the old screen logs.
 */
internal interface WorkoutLogAnalytics {
    // Logged as the screen shows, before its other events, so they're credited to it
    fun screenViewed()

    fun setCompleted(type: ELSetType?, timed: Boolean)

    // A superset's round (or another set of several exercises) done
    fun setGroupCompleted(type: ELSetType?, exercises: Int)

    // A done set undone
    fun setEdited(type: ELSetType?, timed: Boolean)

    fun exercisesAdded()

    fun restStarted()

    // Rest skipped (not run out)
    fun restStopped()

    fun setTimerStarted()

    // A set timer stopped early (not run out)
    fun setTimerStopped()

    fun workoutCompleted()

    fun workoutStopped()

    fun discardPromptShown(setsCompleted: Int)

    fun discardPromptFinished()

    fun discardPromptCancelled()
}

internal class RealWorkoutLogAnalytics : WorkoutLogAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed() = analytics.screenName(AnalyticsConstants.SCREEN_WORKOUT_V2)

    override fun setCompleted(type: ELSetType?, timed: Boolean) =
        analytics.setCompleted(AnalyticsConstants.SET_COMPLETED_SOURCE_SCREEN, type, timed)

    override fun setGroupCompleted(type: ELSetType?, exercises: Int) =
        analytics.setGroupCompleted(AnalyticsConstants.SET_COMPLETED_SOURCE_SCREEN, type, exercises)

    override fun setEdited(type: ELSetType?, timed: Boolean) = analytics.setEdited(type, timed)

    override fun exercisesAdded() = analytics.routineExerciseGroupAdded()

    override fun restStarted() = analytics.workoutTimerStartedRest()

    override fun restStopped() = analytics.workoutTimerStoppedRest()

    override fun setTimerStarted() = analytics.workoutTimerStartedExercise()

    override fun setTimerStopped() = analytics.workoutTimerStoppedExercise()

    override fun workoutCompleted() = analytics.workoutCompleted()

    override fun workoutStopped() = analytics.workoutStopped()

    override fun discardPromptShown(setsCompleted: Int) =
        analytics.workoutDiscardPromptShown(AnalyticsConstants.DISCARD_PROMPT_SOURCE_WORKOUT, setsCompleted)

    override fun discardPromptFinished() = analytics.workoutDiscardPromptFinished()

    override fun discardPromptCancelled() = analytics.workoutDiscardPromptCancelled(AnalyticsConstants.DISCARD_PROMPT_SOURCE_WORKOUT)
}
