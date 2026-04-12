package com.everlog.data.datastores.history

import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserWorkoutStore : ELDocumentStore<ELWorkout>() {

    override fun getType(): Class<ELWorkout> {
        return ELWorkout::class.java
    }

    override fun getQuery(itemId: String): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), itemId)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePathManager.workoutsCollection
    }

    override fun getTag(): String {
        return "ELUserWorkoutStore"
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELWorkout?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELWorkout?> {
        return ELDocStoreWorkoutLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStoreWorkoutLoadedEvent internal constructor(item: ELWorkout?,
                                                            hasPendingWrites: Boolean,
                                                            fromCache: Boolean,
                                                            error: Throwable?) : ELDocStoreItemLoadedEvent<ELWorkout?>(item, hasPendingWrites, fromCache, error)
}