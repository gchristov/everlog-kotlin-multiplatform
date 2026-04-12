package com.everlog.data.datastores.history

import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query

class ELUserWorkoutsStore : ELCollectionStore<ELWorkout>() {

    override fun getType(): Class<ELWorkout> {
        return ELWorkout::class.java
    }

    override fun getQuery(): Query {
        return FirestorePathManager.workoutsCollection
                .orderBy(ELConstants.FIELD_CREATED_DATE, Query.Direction.DESCENDING)
    }

    override fun getTag(): String {
        return "ELUserWorkoutsStore"
    }

    // Events

    override fun getCollectionStoreItemAddedEvent(position: Int,
                                                  item: ELWorkout,
                                                  hasPendingWrites: Boolean,
                                                  fromCache: Boolean): ELColStoreItemAddedEvent<ELWorkout> {
        return ELColStoreWorkoutAddedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemModifiedEvent(oldPosition: Int,
                                                     newPosition: Int,
                                                     item: ELWorkout,
                                                     hasPendingWrites: Boolean,
                                                     fromCache: Boolean): ELColStoreItemModifiedEvent<ELWorkout> {
        return ELColStoreWorkoutModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemRemovedEvent(position: Int,
                                                    item: ELWorkout,
                                                    hasPendingWrites: Boolean,
                                                    fromCache: Boolean): ELColStoreItemRemovedEvent<ELWorkout> {
        return ELColStoreWorkoutRemovedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELWorkout>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELWorkout> {
        return ELColStoreWorkoutsLoadedEvent(items, error, fromCache)
    }

    class ELColStoreWorkoutAddedEvent internal constructor(position: Int,
                                                           item: ELWorkout,
                                                           hasPendingWrites: Boolean,
                                                           fromCache: Boolean) : ELColStoreItemAddedEvent<ELWorkout>(position, item, hasPendingWrites, fromCache)

    class ELColStoreWorkoutModifiedEvent internal constructor(oldPosition: Int,
                                                              newPosition: Int,
                                                              item: ELWorkout,
                                                              hasPendingWrites: Boolean,
                                                              fromCache: Boolean) : ELColStoreItemModifiedEvent<ELWorkout>(oldPosition, newPosition, item, hasPendingWrites, fromCache)

    class ELColStoreWorkoutRemovedEvent internal constructor(position: Int,
                                                             item: ELWorkout,
                                                             hasPendingWrites: Boolean,
                                                             fromCache: Boolean) : ELColStoreItemRemovedEvent<ELWorkout>(position, item, hasPendingWrites, fromCache)

    class ELColStoreWorkoutsLoadedEvent internal constructor(items: List<ELWorkout>?,
                                                             error: Throwable?,
                                                             fromCache: Boolean) : ELColStoreItemsLoadedEvent<ELWorkout>(items, error, fromCache)
}