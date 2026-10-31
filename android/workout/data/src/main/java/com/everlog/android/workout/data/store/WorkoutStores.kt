package com.everlog.android.workout.data.store

import com.everlog.android.common.data.CommonData
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.data.store.DocumentStore

// The workout stores, one of each until destroy(). Test Lab runs keep workouts in memory, so robots don't create data.
object WorkoutStores {

    private var workouts: CollectionStore<ELWorkout>? = null
    private var workout: DocumentStore<ELWorkout>? = null

    @JvmStatic
    @Synchronized
    fun workoutsStore(): CollectionStore<ELWorkout> =
        workouts ?: (if (inMemory()) ELUserWorkoutsInMemoryStore() else ELUserWorkoutsStore()).also { workouts = it }

    @JvmStatic
    @Synchronized
    fun workoutStore(): DocumentStore<ELWorkout> =
        workout ?: (if (inMemory()) ELUserWorkoutInMemoryStore() else ELUserWorkoutStore()).also { workout = it }

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
