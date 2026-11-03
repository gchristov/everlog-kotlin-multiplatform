package com.everlog.android.workout

import com.everlog.android.common.data.model.workout.ELWorkout

// The statistics the workout screens show, which the app works out
interface WorkoutStats {
    // How many of [workouts] were done this week, as the statistics count them
    fun workoutsThisWeek(workouts: List<ELWorkout>, onCount: (Int) -> Unit, onError: (Throwable) -> Unit)
}
