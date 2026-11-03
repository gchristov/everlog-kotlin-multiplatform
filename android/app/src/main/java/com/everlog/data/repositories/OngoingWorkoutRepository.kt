package com.everlog.data.repositories

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.workout.data.OngoingWorkoutRepository
import com.everlog.android.workout.data.WorkoutManager
import com.everlog.android.workout.data.store.WorkoutStores
import com.everlog.application.ELApplication
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.google.firebase.firestore.SetOptions
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.util.Date

@ContributesBinding(AppScope::class)
@Inject
class RealOngoingWorkoutRepository : OngoingWorkoutRepository {
    override fun savedCopyOf(workout: ELWorkout): ELWorkout? = WorkoutManager.manager.savedCopyOf(workout)

    override fun saveOngoing(workout: ELWorkout) = WorkoutManager.manager.setOngoingWorkout(workout)

    override fun clearOngoing() = WorkoutManager.manager.clearOngoingWorkout()

    override fun saveFinished(workout: ELWorkout, fromPlan: Boolean) {
        // Pushes back the app usage reminder, the user may not pass through the home screen for a while
        AppLaunchManager.manager.recordActivity(Date())
        WorkoutStores.workoutStore().create(workout, SetOptions.merge())
        if (ELApplication.graph.planProgress.checkCompletedWorkout(workout, fromPlan)) AnalyticsManager.manager.planDayComplete()
    }
}
