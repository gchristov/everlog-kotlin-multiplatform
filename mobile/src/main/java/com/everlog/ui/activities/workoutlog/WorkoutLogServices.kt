package com.everlog.ui.activities.workoutlog

import android.content.Context
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.controllers.workoutprefill.WorkoutPrefillController
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemsListener
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.PlanManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.navigator.Navigator
import com.everlog.utils.SoundUtils
import com.everlog.utils.VibrationUtils
import com.google.firebase.firestore.SetOptions
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal interface WorkoutLogStore {
    // Null unless it's the ongoing workout
    fun savedCopyOf(workout: ELWorkout): ELWorkout?

    fun saveOngoing(workout: ELWorkout)

    fun clearOngoing()

    // Also checks it off the ongoing plan
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
 * Fills sets in from the user's history with WorkoutPrefillController: the 1RM target for the muscle goal, or else
 * each set's value from the exercise's last session.
 */
internal interface WorkoutLogPrefill {
    // Whether the muscle goal in Settings fills sets in at all
    fun fillsSets(): Boolean

    // By exercise uuid
    suspend fun sources(exercises: List<ELExercise>): Map<String, PrefillSource>

    // Fills the set's empty values. [index] is the set's place in the exercise.
    fun fill(source: PrefillSource, set: ELSet, index: Int)

    // Replaces the values a set just added copied with the last session's set at its place. False when the last
    // session had no such set.
    fun fillAdded(source: PrefillSource, set: ELSet, index: Int): Boolean
}

internal class RealWorkoutLogPrefill : WorkoutLogPrefill {
    override fun fillsSets(): Boolean = SettingsManager.manager.muscleGoal().canPrefill()

    override suspend fun sources(exercises: List<ELExercise>): Map<String, PrefillSource> {
        val history = suspendCancellableCoroutine { continuation ->
            ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
                override fun onItemsLoaded(items: MutableList<ELWorkout>, fromCache: Boolean) {
                    // Copied, as the store keeps updating its list
                    if (continuation.isActive) continuation.resume(ArrayList(items))
                }

                override fun onItemsLoadingError(throwable: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(throwable)
                }
            })
        }
        return withContext(Dispatchers.Default) {
            WorkoutPrefillController.buildPrefillSources(exercises, history, System.currentTimeMillis())
        }
    }

    override fun fill(source: PrefillSource, set: ELSet, index: Int) = WorkoutPrefillController.prefillSet(source, set, index)

    override fun fillAdded(source: PrefillSource, set: ELSet, index: Int): Boolean =
        WorkoutPrefillController.prefillAddedSet(source, set, index)
}

/**
 * Read again each time the screen shows, as they can change in Settings meanwhile.
 */
internal interface WorkoutLogSettings {
    fun weightUnit(): SettingsManager.WeightUnit

    // In the user's unit
    fun weightIncrease(): Float

    fun keepScreenOn(): Boolean
}

internal class RealWorkoutLogSettings : WorkoutLogSettings {
    override fun weightUnit(): SettingsManager.WeightUnit = SettingsManager.manager.weightUnit()

    override fun weightIncrease(): Float = SettingsManager.manager.weightIncrease()

    override fun keepScreenOn(): Boolean = SettingsManager.manager.keepScreenOn()
}

/**
 * WorkoutService's notification, while the screen is open. It's sent the workout after every change and a rest's
 * countdown every second; its buttons come back as broadcasts (WorkoutLogNotificationReceiver).
 */
internal interface WorkoutLogNotification {
    fun start(workout: ELWorkout)

    fun stop()

    fun update(workout: ELWorkout)

    fun showRest(remainingPercent: Int, remainingSeconds: Int)
}

internal class RealWorkoutLogNotification(private val navigator: Navigator) : WorkoutLogNotification {
    override fun start(workout: ELWorkout) = navigator.startWorkoutService(workout)

    override fun stop() = navigator.stopWorkoutService()

    override fun update(workout: ELWorkout) = navigator.notifyWorkoutServiceSetUpdated(workout)

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
}
