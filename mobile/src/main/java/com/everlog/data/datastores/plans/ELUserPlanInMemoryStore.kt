package com.everlog.data.datastores.plans

import com.everlog.data.datastores.base.InMemoryDocumentStore
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.plan.ELPlan

/**
 * [ELUserPlanStore] for Firebase Test Lab runs, kept in memory.
 */
class ELUserPlanInMemoryStore : InMemoryDocumentStore<ELPlan>() {

    override fun getType(): Class<ELPlan> {
        return ELPlan::class.java
    }

    override fun getCollection(): String {
        return ELUserPlansInMemoryStore.COLLECTION
    }

    override fun decorateItem(item: ELPlan) {
        ELPlanInMemoryDecorator().decoratePlan(item)
    }

    override fun getDocumentStoreItemLoadedEvent(item: ELPlan?,
                                                 hasPendingWrites: Boolean,
                                                 fromCache: Boolean,
                                                 error: Throwable?): ELDocStoreItemLoadedEvent<ELPlan?> {
        return ELDocStorePlanLoadedEvent(item, hasPendingWrites, fromCache, error)
    }
}
