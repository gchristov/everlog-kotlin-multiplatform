package com.everlog.application.features

import android.content.Context
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.session.ValueField
import com.everlog.android.workout.data.notification.ShownSet
import com.everlog.android.workout.WorkoutScreenNames
import com.everlog.android.workout.log.KeypadHiddenBy
import com.everlog.android.workout.log.KeypadShownFrom
import com.everlog.android.workout.log.WorkoutLogAlerts
import com.everlog.android.workout.log.WorkoutLogAnalytics
import com.everlog.android.workout.log.WorkoutLogNotification
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.navigator.Navigator
import com.everlog.utils.SoundUtils
import com.everlog.utils.VibrationUtils

class AppWorkoutLogNotification(private val navigator: Navigator) : WorkoutLogNotification {
    override fun start(workout: ELWorkout, shown: ShownSet?) = navigator.startWorkoutService(workout, shown)

    override fun stop() = navigator.stopWorkoutService()

    override fun update(workout: ELWorkout, shown: ShownSet?) = navigator.notifyWorkoutServiceSetUpdated(workout, shown)

    override fun showRest(remainingPercent: Int, remainingSeconds: Int) =
        navigator.notifyWorkoutServiceShowRestTimer(remainingPercent, remainingSeconds)
}

class AppWorkoutLogAlerts(private val context: Context) : WorkoutLogAlerts {
    override fun timerRanOut() {
        SoundUtils.playTimerSound(context)
        VibrationUtils.vibrate(context)
    }
}

class AppWorkoutLogAnalytics : WorkoutLogAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun screenViewed() = analytics.screenName(WorkoutScreenNames.LOG)

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
