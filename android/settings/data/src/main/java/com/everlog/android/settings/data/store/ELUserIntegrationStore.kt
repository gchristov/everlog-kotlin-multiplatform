package com.everlog.android.settings.data.store

import com.everlog.android.auth.data.requireUserId
import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.settings.data.model.ELIntegration
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.common.firebase.FirestorePaths
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query

class ELUserIntegrationStore(private val session: UserSession) : ELDocumentStore<ELIntegration>() {

    override fun getType(): Class<ELIntegration> {
        return ELIntegration::class.java
    }

    override fun getQuery(integrationType: String): Query {
        return parentCollection
                .whereEqualTo(FirestoreFields.CREATED_BY_USER_ID, session.requireUserId())
                .whereEqualTo(FirestoreFields.TYPE, integrationType)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePaths.integrations
    }

    override fun getTag(): String {
        return "ELUserIntegrationStore"
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELIntegration?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELIntegration?> {
        return ELDocStoreIntegrationLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStoreIntegrationLoadedEvent internal constructor(item: ELIntegration?,
                                                                hasPendingWrites: Boolean,
                                                                fromCache: Boolean,
                                                                error: Throwable?) : ELDocStoreItemLoadedEvent<ELIntegration?>(item, hasPendingWrites, fromCache, error)
}