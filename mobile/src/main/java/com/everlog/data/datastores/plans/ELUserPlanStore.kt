package com.everlog.data.datastores.plans

import com.everlog.data.datastores.base.ELDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.plan.ELPlan
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

class ELUserPlanStore : ELDocumentStore<ELPlan>() {

    override fun getType(): Class<ELPlan> {
        return ELPlan::class.java
    }

    override fun getQuery(itemId: String): Query {
        return parentCollection
                .whereEqualTo(FieldPath.documentId(), itemId)
    }

    override fun getParentCollection(): CollectionReference {
        return FirestorePathManager.plansCollection
    }

    override fun getTag(): String {
        return "ELUserPlanStore"
    }

    override fun decorateItem(item: ELPlan) {
        ELPlanDecorator().decoratePlan(item)
    }

    // Events

    override fun getDocumentStoreItemLoadedEvent(item: ELPlan?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELPlan?> {
        return ELDocStorePlanLoadedEvent(item, hasPendingWrites, fromCache, error)
    }

    class ELDocStorePlanLoadedEvent internal constructor(item: ELPlan?,
                                                         hasPendingWrites: Boolean,
                                                         fromCache: Boolean,
                                                         error: Throwable?) : ELDocStoreItemLoadedEvent<ELPlan?>(item, hasPendingWrites, fromCache, error)
}