package com.everlog.data.datastores.routines

import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.ELRoutine
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserRoutineStore : ELDocumentStore<ELRoutine>() {

    override fun getType(): Class<ELRoutine> {
        return ELRoutine::class.java
    }

    override fun getQuery(itemId: String): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), itemId)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePathManager.routinesCollection
    }

    override fun getTag(): String {
        return "ELUserRoutineStore"
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELRoutine?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELRoutine?> {
        return ELDocStoreRoutineLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStoreRoutineLoadedEvent internal constructor(item: ELRoutine?,
                                                            hasPendingWrites: Boolean,
                                                            fromCache: Boolean,
                                                            error: Throwable?) : ELDocStoreItemLoadedEvent<ELRoutine?>(item, hasPendingWrites, fromCache, error)
}