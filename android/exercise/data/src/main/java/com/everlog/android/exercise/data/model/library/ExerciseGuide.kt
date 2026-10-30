package com.everlog.android.exercise.data.model.library

/**
 * How to do an exercise, as the library has it: its steps, one each, and form tips. Both are empty for
 * an exercise the library doesn't have, like a user's own.
 */
data class ExerciseGuide(
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
) {
    val isEmpty get() = instructions.isEmpty() && tips.isEmpty()
}
