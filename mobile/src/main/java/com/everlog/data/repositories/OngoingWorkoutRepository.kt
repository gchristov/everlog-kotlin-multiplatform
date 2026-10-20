package com.everlog.data.repositories

import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.PlanManager
import com.everlog.managers.WorkoutManager
import com.everlog.managers.apprate.AppLaunchManager
import com.google.firebase.firestore.SetOptions
import java.util.Date

/**
 * The workout in progress, saved on the device so it can be resumed, and saved to Firestore once it's finished.
 */
interface OngoingWorkoutRepository {
    // Null unless it's the ongoing workout
    fun savedCopyOf(workout: ELWorkout): ELWorkout?

    fun saveOngoing(workout: ELWorkout)

    fun clearOngoing()

    // Also checks it off the ongoing plan
    fun saveFinished(workout: ELWorkout, fromPlan: Boolean)
}

class RealOngoingWorkoutRepository : OngoingWorkoutRepository {
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
