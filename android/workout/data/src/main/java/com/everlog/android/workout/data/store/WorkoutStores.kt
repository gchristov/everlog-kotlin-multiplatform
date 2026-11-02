package com.everlog.android.workout.data.store

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.data.CommonData
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.data.store.DocumentStore

// The workout stores, one of each until destroy(). Test Lab runs keep workouts in memory, so robots don't create data.
object WorkoutStores {

    // Set by the app at startup, until dependency injection builds the stores
    lateinit var session: UserSession

    private var workouts: CollectionStore<ELWorkout>? = null
    private var workout: DocumentStore<ELWorkout>? = null

    @JvmStatic
    @Synchronized
    fun workoutsStore(): CollectionStore<ELWorkout> =
        workouts ?: (if (inMemory()) ELUserWorkoutsInMemoryStore(session) else ELUserWorkoutsStore(session)).also { workouts = it }

    @JvmStatic
    @Synchronized
    fun workoutStore(): DocumentStore<ELWorkout> =
        workout ?: (if (inMemory()) ELUserWorkoutInMemoryStore(session) else ELUserWorkoutStore(session)).also { workout = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        workouts?.destroy()
        workouts = null
        workout?.destroy()
        workout = null
    }

    private fun inMemory() = CommonData.dependencies.isTestLabRun()
}
