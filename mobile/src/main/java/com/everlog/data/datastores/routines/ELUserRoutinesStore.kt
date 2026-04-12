package com.everlog.data.datastores.routines

import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.model.ELRoutine
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query

class ELUserRoutinesStore : ELCollectionStore<ELRoutine>() {

    override fun getType(): Class<ELRoutine> {
        return ELRoutine::class.java
    }

    override fun getQuery(): Query {
        return FirestorePathManager.routinesCollection
                .orderBy(ELConstants.FIELD_NAME)
    }

    override fun getTag(): String {
        return "ELUserRoutinesStore"
    }

    // Events

    override fun getCollectionStoreItemAddedEvent(position: Int,
                                                  item: ELRoutine,
                                                  hasPendingWrites: Boolean,
                                                  fromCache: Boolean): ELColStoreItemAddedEvent<ELRoutine> {
        return ELColStoreRoutineAddedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemModifiedEvent(oldPosition: Int,
                                                     newPosition: Int,
                                                     item: ELRoutine,
                                                     hasPendingWrites: Boolean,
                                                     fromCache: Boolean): ELColStoreItemModifiedEvent<ELRoutine> {
        return ELColStoreRoutineModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemRemovedEvent(position: Int,
                                                    item: ELRoutine,
                                                    hasPendingWrites: Boolean,
                                                    fromCache: Boolean): ELColStoreItemRemovedEvent<ELRoutine> {
        return ELColStoreRoutineRemovedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELRoutine>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELRoutine> {
        return ELColStoreRoutinesLoadedEvent(items, error, fromCache)
    }

    class ELColStoreRoutineAddedEvent internal constructor(position: Int,
                                                           item: ELRoutine,
                                                           hasPendingWrites: Boolean,
                                                           fromCache: Boolean) : ELColStoreItemAddedEvent<ELRoutine>(position, item, hasPendingWrites, fromCache)

    class ELColStoreRoutineModifiedEvent internal constructor(oldPosition: Int,
                                                              newPosition: Int,
                                                              item: ELRoutine,
                                                              hasPendingWrites: Boolean,
                                                              fromCache: Boolean) : ELColStoreItemModifiedEvent<ELRoutine>(oldPosition, newPosition, item, hasPendingWrites, fromCache)

    class ELColStoreRoutineRemovedEvent internal constructor(position: Int,
                                                             item: ELRoutine,
                                                             hasPendingWrites: Boolean,
                                                             fromCache: Boolean) : ELColStoreItemRemovedEvent<ELRoutine>(position, item, hasPendingWrites, fromCache)

    class ELColStoreRoutinesLoadedEvent internal constructor(items: List<ELRoutine>?,
                                                             error: Throwable?,
                                                             fromCache: Boolean) : ELColStoreItemsLoadedEvent<ELRoutine>(items, error, fromCache)
}