package com.everlog.data.datastores.routines

import com.everlog.data.datastores.base.InMemoryDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.ELRoutine

/**
 * [ELUserRoutineStore] for Firebase Test Lab runs, kept in memory.
 */
class ELUserRoutineInMemoryStore : InMemoryDocumentStore<ELRoutine>() {

    override fun getType(): Class<ELRoutine> {
        return ELRoutine::class.java
    }

    override fun getCollection(): String {
        return ELUserRoutinesInMemoryStore.COLLECTION
    }

    override fun decorateItem(item: ELRoutine) {
        ELRoutineDecorator().decorate(item)
    }

    override fun getDocumentStoreItemLoadedEvent(item: ELRoutine?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELRoutine?> {
        return ELDocStoreRoutineLoadedEvent(item, hasPendingWrites, fromCache, error)
    }
}
