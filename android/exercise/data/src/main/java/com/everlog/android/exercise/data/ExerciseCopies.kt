package com.everlog.android.exercise.data

import com.everlog.android.common.data.model.exercise.ELExercise

// Puts an exercise's new name and image into the user's routines and workouts, which keep copies of it
interface ExerciseCopies {
    fun update(exercise: ELExercise)
}
