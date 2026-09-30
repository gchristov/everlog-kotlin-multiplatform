package com.everlog.data.datastores.routines

import com.everlog.data.datastores.base.InMemoryCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.model.ELRoutine

/**
 * [ELUserRoutinesStore] for Firebase Test Lab runs, kept in memory.
 */
class ELUserRoutinesInMemoryStore : InMemoryCollectionStore<ELRoutine>() {

    companion object {
        const val COLLECTION = "routines"
    }

    override fun getCollection(): String {
        return COLLECTION
    }

    override fun getOrder(): Comparator<ELRoutine> {
        return compareBy(nullsFirst()) { it.name }
    }

    override fun decorateItem(item: ELRoutine) {
        ELRoutineDecorator().decorate(item)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELRoutine>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELRoutine> {
        return ELColStoreRoutinesLoadedEvent(items, error, fromCache)
    }
}
