package com.everlog.application.features

import com.everlog.android.workout.notification.WorkoutServiceAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppWorkoutServiceAnalytics : WorkoutServiceAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun weightModified() = analytics.workoutServiceWeightModified()

    override fun repsModified() = analytics.workoutServiceRepsModified()

    override fun timerStartedExercise() = analytics.workoutServiceTimerStartedExercise()

    override fun timerStoppedExercise() = analytics.workoutServiceTimerStoppedExercise()

    override fun timerStoppedRest() = analytics.workoutServiceTimerStoppedRest()

    override fun nextExercise() = analytics.workoutServiceNextExercise()
}
