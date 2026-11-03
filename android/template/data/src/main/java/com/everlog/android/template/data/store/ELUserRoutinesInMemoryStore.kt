package com.everlog.android.template.data.store

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.InMemoryCollectionStore
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent

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
