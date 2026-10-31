package com.everlog.android.workout.log

import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.common.session.ValueField
import com.everlog.android.workout.data.notification.ShownSet

/**
 * Read again each time the screen shows, as they can change in Settings meanwhile.
 */
internal interface WorkoutLogSettings {
    fun weightUnit(): WeightUnit

    // In the user's unit
    fun weightIncrease(): Float

    fun keepScreenOn(): Boolean

    // What fills sets in: off, last time or a 1RM goal
    fun muscleGoal(): SettingsManager.MuscleGoal
}

internal class RealWorkoutLogSettings : WorkoutLogSettings {
    override fun weightUnit(): WeightUnit = SettingsManager.manager.weightUnit()

    override fun weightIncrease(): Float = SettingsManager.manager.weightIncrease()

    override fun keepScreenOn(): Boolean = SettingsManager.manager.keepScreenOn()

    override fun muscleGoal(): SettingsManager.MuscleGoal = SettingsManager.manager.muscleGoal()
}

/**
 * WorkoutService's notification, while the screen is open. It's sent the workout and the set it shows after every
 * change and a rest's countdown every second; its buttons come back as broadcasts (WorkoutLogNotificationReceiver).
 */
interface WorkoutLogNotification {
    fun start(workout: ELWorkout, shown: ShownSet?)

    fun stop()

    fun update(workout: ELWorkout, shown: ShownSet?)

    fun showRest(remainingPercent: Int, remainingSeconds: Int)
}

/**
 * The sound and vibration when a countdown runs out. Called from the view-model rather than the screen, so they
 * play with the phone locked.
 */
interface WorkoutLogAlerts {
    fun timerRanOut()
}

interface WorkoutLogAnalytics {
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

enum class KeypadShownFrom(val id: String) {
    Cell("cell"),
    Button("button"),
}

enum class KeypadHiddenBy(val id: String) {
    Collapse("collapse"),
    Back("back"),
}
