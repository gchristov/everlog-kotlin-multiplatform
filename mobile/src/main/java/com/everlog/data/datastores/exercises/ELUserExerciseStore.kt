package com.everlog.data.datastores.exercises

import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.firebase.FirestorePathManager
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
        return FirestorePathManager.exercisesCollection
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