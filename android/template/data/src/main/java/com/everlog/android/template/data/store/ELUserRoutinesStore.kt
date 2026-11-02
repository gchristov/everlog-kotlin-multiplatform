package com.everlog.android.template.data.store

import com.everlog.android.auth.data.UserSession
import com.everlog.android.auth.data.requireUserId
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.common.data.store.ELCollectionStore
import com.everlog.android.common.data.store.events.ELColStoreItemAddedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemModifiedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemRemovedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.google.firebase.firestore.Query

class ELUserRoutinesStore(private val session: UserSession) : ELCollectionStore<ELRoutine>() {

    override fun getType(): Class<ELRoutine> {
        return ELRoutine::class.java
    }

    override fun getQuery(): Query {
        return FirestorePaths.routines(session.requireUserId())
                .orderBy(FirestoreFields.NAME)
    }

    override fun getTag(): String {
        return "ELUserRoutinesStore"
    }

    override fun decorateItem(item: ELRoutine) {
        ELRoutineDecorator(session).decorate(item)
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
}