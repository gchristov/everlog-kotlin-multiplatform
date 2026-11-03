package com.everlog.android.workout.data.store

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.InMemoryCollectionStore
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.everlog.android.template.data.store.ELRoutineDecorator

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
