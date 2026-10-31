package com.everlog.android.workout.details

interface WorkoutDetailsAnalytics {
    fun shared()

    fun deleted()

    fun nameModified()

    fun dateModified()

    fun notesModified()

    fun exercisesModified()

    fun savedAsTemplate()
}

// The workout's text that the details screen edits in a prompt
enum class WorkoutTextField {
    Name,
    Note,
}
