package com.everlog.application.features

import com.everlog.android.workout.details.WorkoutDetailsAnalytics
import com.everlog.managers.analytics.AnalyticsManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppWorkoutDetailsAnalytics : WorkoutDetailsAnalytics {
    private val analytics = AnalyticsManager.manager

    override fun shared() = analytics.workoutDetailsShared()

    override fun deleted() = analytics.workoutDetailsDeleted()

    override fun nameModified() = analytics.workoutDetailsNameModified()

    override fun dateModified() = analytics.workoutDetailsDateModified()

    override fun notesModified() = analytics.workoutDetailsNotesModified()

    override fun exercisesModified() = analytics.workoutDetailsExercisesModified()

    override fun savedAsTemplate() {
        analytics.workoutDetailsSavedAsRoutine()
        analytics.routineCreated()
    }
}
