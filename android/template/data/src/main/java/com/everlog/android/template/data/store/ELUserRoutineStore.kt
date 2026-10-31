package com.everlog.android.template.data.store

import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.common.firebase.FirestorePaths
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
        return FirestorePaths.routines(LocalUserManager.requireUserId())
    }

    override fun getTag(): String {
        return "ELUserRoutineStore"
    }

    override fun decorateItem(item: ELRoutine) {
        ELRoutineDecorator().decorate(item)
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELRoutine?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELRoutine?> {
        return ELDocStoreRoutineLoadedEvent(item, hasPendingWrites, fromCache, error)
    }
}