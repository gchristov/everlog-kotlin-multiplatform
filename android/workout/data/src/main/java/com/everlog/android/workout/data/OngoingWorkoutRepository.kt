package com.everlog.android.workout.data

import com.everlog.android.common.data.model.workout.ELWorkout

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
