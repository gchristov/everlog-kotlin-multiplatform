package com.everlog.android.exercise.data.store

// The exercise stores, one of each until destroy()
object ExerciseStores {

    private var exercises: ELExercisesStore? = null
    private var exercise: ELUserExerciseStore? = null

    @JvmStatic
    @Synchronized
    fun exercisesStore(): ELExercisesStore = exercises ?: ELExercisesStore().also { exercises = it }

    @JvmStatic
    @Synchronized
    fun exerciseStore(): ELUserExerciseStore = exercise ?: ELUserExerciseStore().also { exercise = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        exercises?.destroy()
        exercises = null
        exercise?.destroy()
        exercise = null
    }
}
