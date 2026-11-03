package com.everlog.android.exercise.data.store

import com.everlog.android.auth.data.UserSession
import com.everlog.android.exercise.data.ExerciseLookup

// The exercise stores, one of each until destroy()
object ExerciseStores {

    // Set by the app at startup, until the app's graph builds the stores
    lateinit var session: UserSession

    // Set by the app at startup, for the routine decorator. Built when first used: building it opens Firestore,
    // which mustn't happen before the app has configured it.
    lateinit var lookup: Lazy<ExerciseLookup>

    private var exercises: ELExercisesStore? = null
    private var exercise: ELUserExerciseStore? = null

    @JvmStatic
    @Synchronized
    fun exercisesStore(): ELExercisesStore = exercises ?: ELExercisesStore(session).also { exercises = it }

    @JvmStatic
    @Synchronized
    fun exerciseStore(): ELUserExerciseStore = exercise ?: ELUserExerciseStore(session).also { exercise = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        exercises?.destroy()
        exercises = null
        exercise?.destroy()
        exercise = null
    }
}
