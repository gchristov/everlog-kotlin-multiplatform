package com.everlog.android.workout.notification

// The workout notification's buttons
interface WorkoutServiceAnalytics {
    fun weightModified()

    fun repsModified()

    fun timerStartedExercise()

    fun timerStoppedExercise()

    fun timerStoppedRest()

    fun nextExercise()
}
