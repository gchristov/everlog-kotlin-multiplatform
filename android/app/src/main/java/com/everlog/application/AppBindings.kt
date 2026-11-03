package com.everlog.application

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.workout.data.WorkoutSyncMonths
import com.everlog.config.AppConfig
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.sync.AppDataSync
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// What the app gives the graph from outside it: its own singletons and settings, and the old stores
@ContributesTo(AppScope::class)
@BindingContainer
object AppBindings {
    @Provides
    fun dataSync(): DataSync = AppDataSync.instance

    @Provides
    fun workoutsStore(): CollectionStore<ELWorkout> = ELDatastore.workoutsStore()

    @Provides
    @WorkoutSyncMonths
    fun workoutSyncMonths(): Int = AppConfig.configuration.workoutSyncMonths
}
