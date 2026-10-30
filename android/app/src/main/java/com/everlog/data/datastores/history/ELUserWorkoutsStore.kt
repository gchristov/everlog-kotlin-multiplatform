package com.everlog.data.datastores.history

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.common.data.store.ELCollectionStore
import com.everlog.android.common.data.store.events.ELColStoreItemAddedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemModifiedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemRemovedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.routines.ELRoutineDecorator
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query

class ELUserWorkoutsStore : ELCollectionStore<ELWorkout>() {

    override fun getType(): Class<ELWorkout> {
        return ELWorkout::class.java
    }

    override fun getQuery(): Query {
        return FirestorePathManager.workoutsCollection
                .orderBy(FirestoreFields.CREATED_DATE, Query.Direction.DESCENDING)
    }

    override fun getTag(): String {
        return "ELUserWorkoutsStore"
    }

    override fun decorateItem(item: ELWorkout) {
        ELRoutineDecorator().decorate(item)
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
}