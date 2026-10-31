package com.everlog.android.workout.data.store

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.InMemoryDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.template.data.store.ELRoutineDecorator

/**
 * [ELUserWorkoutStore] for Firebase Test Lab runs, kept in memory.
 */
class ELUserWorkoutInMemoryStore : InMemoryDocumentStore<ELWorkout>() {

    override fun getType(): Class<ELWorkout> {
        return ELWorkout::class.java
    }

    override fun getCollection(): String {
        return ELUserWorkoutsInMemoryStore.COLLECTION
    }

    override fun decorateItem(item: ELWorkout) {
        ELRoutineDecorator().decorate(item)
    }

    override fun getDocumentStoreItemLoadedEvent(item: ELWorkout?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELWorkout?> {
        return ELDocStoreWorkoutLoadedEvent(item, hasPendingWrites, fromCache, error)
    }
}
