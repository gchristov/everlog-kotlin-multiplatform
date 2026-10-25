package com.everlog.ui.activities.workoutlog

import android.content.Context
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.services.workout.ShownSet
import com.everlog.ui.navigator.Navigator
import com.everlog.utils.SoundUtils
import com.everlog.utils.VibrationUtils

/**
 * Read again each time the screen shows, as they can change in Settings meanwhile.
 */
internal interface WorkoutLogSettings {
    fun weightUnit(): SettingsManager.WeightUnit

    // In the user's unit
    fun weightIncrease(): Float

    fun keepScreenOn(): Boolean

    // What fills sets in: off, last time or a 1RM goal
    fun muscleGoal(): SettingsManager.MuscleGoal
}

internal class RealWorkoutLogSettings : WorkoutLogSettings {
    override fun weightUnit(): SettingsManager.WeightUnit = SettingsManager.manager.weightUnit()

    override fun weightIncrease(): Float = SettingsManager.manager.weightIncrease()

    override fun keepScreenOn(): Boolean = SettingsManager.manager.keepScreenOn()

    override fun muscleGoal(): SettingsManager.MuscleGoal = SettingsManager.manager.muscleGoal()
}

/**
 * WorkoutService's notification, while the screen is open. It's sent the workout and the set it shows after every
 * change and a rest's countdown every second; its buttons come back as broadcasts (WorkoutLogNotificationReceiver).
 */
internal interface WorkoutLogNotification {
    fun start(workout: ELWorkout, shown: ShownSet?)

    fun stop()

    fun update(workout: ELWorkout, shown: ShownSet?)

    fun showRest(remainingPercent: Int, remainingSeconds: Int)
}

internal class RealWorkoutLogNotification(private val navigator: Navigator) : WorkoutLogNotification {
    override fun start(workout: ELWorkout, shown: ShownSet?) = navigator.startWorkoutService(workout, shown)

    override fun stop() = navigator.stopWorkoutService()

    override fun update(workout: ELWorkout, shown: ShownSet?) = navigator.notifyWorkoutServiceSetUpdated(workout, shown)

    override fun showRest(remainingPercent: Int, remainingSeconds: Int) =
        navigator.notifyWorkoutServiceShowRestTimer(remainingPercent, remainingSeconds)
}

/**
 * The sound and vibration when a countdown runs out. Called from the view-model rather than the screen, so they
 * play with the phone locked.
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

internal interface WorkoutLogAnalytics {
    // Logged as the screen shows, before its other events, so they're credited to it
    fun screenViewed()

    fun setCompleted(type: ELSetType?, timed: Boolean, fromNotification: Boolean)

    // A superset's (or circuit's) round done
    fun setGroupCompleted(type: ELSetType?, exercises: Int, fromNotification: Boolean)

    // A done set undone
    fun setEdited(type: ELSetType?, timed: Boolean)

    fun exercisesAdded()

    fun setAdded()

    fun setDeleted()

    fun restChanged(seconds: Int)

    // Weight in the user's unit
    fun valueChanged(field: ValueField, value: Float?)

    fun restStarted()

    // Skipped, not run out
    fun restStopped()

    fun setTimerStarted()

    // Stopped, not run out
    fun setTimerStopped()

    fun workoutCompleted()

    fun workoutStopped()

    fun discardPromptShown(setsCompleted: Int)

    fun discardPromptFinished()

    fun discardPromptCancelled()

    // From Workout options or the keypad note
    fun setSuggestionsOpened()

    // Opened or closed by the user, not as sets are completed or the workout is edited
    fun keypadShown(source: KeypadShownFrom)

    fun keypadHidden(source: KeypadHiddenBy)
}

internal enum class KeypadShownFrom(val id: String) {
    Cell("cell"),
    Button("button"),
}

internal enum class KeypadHiddenBy(val id: String) {
    Collapse("collapse"),
    Back("back"),
}

internal class RealWorkoutLogAnalytics : WorkoutLogAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed() = analytics.screenName(AnalyticsConstants.SCREEN_WORKOUT_V2)

    override fun setCompleted(type: ELSetType?, timed: Boolean, fromNotification: Boolean) =
        analytics.setCompleted(source(fromNotification), type, timed)

    override fun setGroupCompleted(type: ELSetType?, exercises: Int, fromNotification: Boolean) =
        analytics.setGroupCompleted(source(fromNotification), type, exercises)

    private fun source(fromNotification: Boolean) =
        if (fromNotification) AnalyticsConstants.SET_COMPLETED_SOURCE_NOTIFICATION else AnalyticsConstants.SET_COMPLETED_SOURCE_SCREEN

    override fun setEdited(type: ELSetType?, timed: Boolean) = analytics.setEdited(type, timed)

    override fun exercisesAdded() = analytics.routineExerciseGroupAdded()

    override fun setAdded() = analytics.setAdded()

    override fun setDeleted() = analytics.setDeleted()

    override fun restChanged(seconds: Int) = analytics.setRestTimeModified(seconds)

    override fun valueChanged(field: ValueField, value: Float?) = when (field) {
        ValueField.Weight -> analytics.setWeightModified(value)
        ValueField.Reps -> analytics.setRepsModified(value?.toInt())
        ValueField.Time -> analytics.setTimeModified(value?.toInt())
    }

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

    // The old workout screen's event for its muscle goal button
    override fun setSuggestionsOpened() = analytics.workoutChangeMuscleGoal()

    override fun keypadShown(source: KeypadShownFrom) = analytics.workoutKeypadShown(source.id)

    override fun keypadHidden(source: KeypadHiddenBy) = analytics.workoutKeypadHidden(source.id)
}
