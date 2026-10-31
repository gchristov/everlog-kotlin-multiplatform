package com.everlog.android.plan.data.store

import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.data.store.InMemoryDocumentStore
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent

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
