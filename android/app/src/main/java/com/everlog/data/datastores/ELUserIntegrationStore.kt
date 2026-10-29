package com.everlog.data.datastores

import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.ELIntegration
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query

class ELUserIntegrationStore : ELDocumentStore<ELIntegration>() {

    override fun getType(): Class<ELIntegration> {
        return ELIntegration::class.java
    }

    override fun getQuery(integrationType: String): Query {
        return parentCollection
                .whereEqualTo(ELConstants.FIELD_CREATED_BY_USER_ID, LocalUserManager.getUser()!!.id)
                .whereEqualTo(ELConstants.FIELD_TYPE, integrationType)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePathManager.integrationsCollection
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