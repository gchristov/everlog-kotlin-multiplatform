package com.everlog.android.auth.data.store

import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.auth.data.model.ELDevice
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.common.firebase.FirestorePaths
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserDeviceStore : ELDocumentStore<ELDevice>() {

    override fun getType(): Class<ELDevice> {
        return ELDevice::class.java
    }

    override fun getQuery(itemId: String): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), itemId)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePaths.devices(LocalUserManager.requireUserId())
    }

    override fun getTag(): String {
        return "ELUserDeviceStore"
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELDevice?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELDevice?> {
        return ELDocStoreDeviceLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStoreDeviceLoadedEvent internal constructor(item: ELDevice?,
                                                           hasPendingWrites: Boolean,
                                                           fromCache: Boolean,
                                                           error: Throwable?) : ELDocStoreItemLoadedEvent<ELDevice?>(item, hasPendingWrites, fromCache, error)
}