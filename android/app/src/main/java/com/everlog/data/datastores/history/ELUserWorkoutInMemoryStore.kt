package com.everlog.data.datastores.history

import com.everlog.data.datastores.base.InMemoryDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.datastores.routines.ELRoutineDecorator
import com.everlog.data.model.workout.ELWorkout

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
