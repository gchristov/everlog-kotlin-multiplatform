package com.everlog.data.datastores

import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.ELConsent
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserConsentStore : ELDocumentStore<ELConsent>() {

    override fun getType(): Class<ELConsent> {
        return ELConsent::class.java
    }

    override fun getQuery(unused: String?): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), LocalUserManager.getUser()!!.id)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePathManager.consentCollection
    }

    override fun getTag(): String {
        return "ELUserConsentStore"
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELConsent?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELConsent?> {
        return ELDocStoreConsentLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStoreConsentLoadedEvent internal constructor(item: ELConsent?,
                                                            hasPendingWrites: Boolean,
                                                            fromCache: Boolean,
                                                            error: Throwable?) : ELDocStoreItemLoadedEvent<ELConsent?>(item, hasPendingWrites, fromCache, error)
}