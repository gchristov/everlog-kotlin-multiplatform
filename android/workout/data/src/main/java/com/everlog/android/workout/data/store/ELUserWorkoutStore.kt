package com.everlog.android.workout.data.store

import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.template.data.store.ELRoutineDecorator
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
        return FirestorePaths.workouts(LocalUserManager.requireUserId())
    }

    override fun getTag(): String {
        return "ELUserWorkoutStore"
    }

    override fun decorateItem(item: ELWorkout) {
        ELRoutineDecorator().decorate(item)
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELWorkout?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELWorkout?> {
        return ELDocStoreWorkoutLoadedEvent(item, hasPendingWrites, fromCache, error)
    }
}