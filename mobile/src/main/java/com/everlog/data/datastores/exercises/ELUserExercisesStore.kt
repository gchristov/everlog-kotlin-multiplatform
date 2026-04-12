package com.everlog.data.datastores.exercises

import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.exercises.ELExercisesStore.*
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query

internal class ELUserExercisesStore : ELCollectionStore<ELExercise>() {

    override fun getType(): Class<ELExercise> {
        return ELExercise::class.java
    }

    override fun getQuery(): Query {
        return FirestorePathManager.exercisesCollection
                .orderBy(ELConstants.FIELD_NAME)
    }

    override fun getTag(): String {
        return "ELUserExercisesStore"
    }

    // Events

    override fun getCollectionStoreItemAddedEvent(position: Int,
                                                  item: ELExercise,
                                                  hasPendingWrites: Boolean,
                                                  fromCache: Boolean): ELColStoreItemAddedEvent<ELExercise> {
        return ELColStoreExerciseAddedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemModifiedEvent(oldPosition: Int,
                                                     newPosition: Int,
                                                     item: ELExercise,
                                                     hasPendingWrites: Boolean,
                                                     fromCache: Boolean): ELColStoreItemModifiedEvent<ELExercise> {
        return ELColStoreExerciseModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemRemovedEvent(position: Int,
                                                    item: ELExercise,
                                                    hasPendingWrites: Boolean,
                                                    fromCache: Boolean): ELColStoreItemRemovedEvent<ELExercise> {
        return ELColStoreExerciseRemovedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELExercise>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELExercise> {
        return ELColStoreExercisesLoadedEvent(items, error, fromCache)
    }
}