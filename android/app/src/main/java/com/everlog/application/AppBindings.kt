package com.everlog.application

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.exercise.data.ExerciseUsage
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.android.workout.data.RealWorkoutRepository
import com.everlog.application.features.AppExerciseUsage
import com.everlog.config.AppConfig
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.sync.AppDataSync
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineDispatcher

// What the app builds itself for the graph: its own singletons, and classes not yet on the graph
@ContributesTo(AppScope::class)
@BindingContainer
object AppBindings {
    @Provides
    fun dataSync(): DataSync = AppDataSync.instance

    @Provides
    fun workoutsStore(): CollectionStore<ELWorkout> = ELDatastore.workoutsStore()

    // The routine and workout repositories join the graph with their features
    @Provides
    fun exerciseUsage(@IoDispatcher io: CoroutineDispatcher, dataSync: DataSync, session: UserSession): ExerciseUsage =
        AppExerciseUsage(
            RealRoutineRepository(io, dataSync, session) { ELDatastore.routineStore() },
            RealWorkoutRepository(io, dataSync, AppConfig.configuration.workoutSyncMonths, session),
        )
}
