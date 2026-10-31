package com.everlog.android.auth.data.store

import com.everlog.android.auth.data.model.ELConsent
import com.everlog.android.common.data.store.ELDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.firebase.FirestorePaths
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
        return FirestorePaths.consent
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