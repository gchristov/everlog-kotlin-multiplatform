package com.everlog.data.datastores.history

import com.everlog.data.datastores.base.InMemoryCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.routines.ELRoutineDecorator
import com.everlog.data.model.workout.ELWorkout

/**
 * [ELUserWorkoutsStore] for Firebase Test Lab runs, kept in memory.
 */
class ELUserWorkoutsInMemoryStore : InMemoryCollectionStore<ELWorkout>() {

    companion object {
        const val COLLECTION = "workouts"
    }

    override fun getCollection(): String {
        return COLLECTION
    }

    override fun getOrder(): Comparator<ELWorkout> {
        return compareByDescending { it.createdDate }
    }

    override fun decorateItem(item: ELWorkout) {
        ELRoutineDecorator().decorate(item)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELWorkout>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELWorkout> {
        return ELColStoreWorkoutsLoadedEvent(items, error, fromCache)
    }
}
