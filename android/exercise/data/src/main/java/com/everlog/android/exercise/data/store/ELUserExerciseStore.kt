package com.everlog.android.exercise.data.store

import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.common.firebase.FirestorePaths
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserExerciseStore : ELDocumentStore<ELExercise>() {

    override fun getType(): Class<ELExercise> {
        return ELExercise::class.java
    }

    override fun getQuery(itemId: String): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), itemId)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePaths.exercises(LocalUserManager.requireUserId())
    }

    override fun getTag(): String {
        return "ELUserExerciseStore"
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELExercise?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELExercise?> {
        return ELDocStoreExerciseLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStoreExerciseLoadedEvent internal constructor(item: ELExercise?,
                                                             hasPendingWrites: Boolean,
                                                             fromCache: Boolean,
                                                             error: Throwable?) : ELDocStoreItemLoadedEvent<ELExercise?>(item, hasPendingWrites, fromCache, error)
}