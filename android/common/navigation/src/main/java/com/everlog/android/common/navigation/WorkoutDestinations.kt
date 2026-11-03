package com.everlog.android.common.navigation

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout

// Starts a workout from [routine]
data class StartWorkout(val routine: ELRoutine, val fromPlan: Boolean) : Destination

data class WorkoutDetails(
    val workout: ELWorkout,
    val justFinished: Boolean = false,
    val fromPlan: Boolean = false,
) : Destination

// The system share sheet with a summary of [workout]
data class ShareWorkout(val workout: ELWorkout) : Destination

// Asks for a rating after [workout], when the rating prompt's rules allow it, after [delayMillis]
data class RatePrompt(val workout: ELWorkout, val delayMillis: Long) : Destination
