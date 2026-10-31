package com.everlog.android.template.data.store

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.InMemoryDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent

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
