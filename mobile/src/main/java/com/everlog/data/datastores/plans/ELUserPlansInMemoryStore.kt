package com.everlog.data.datastores.plans

import com.everlog.data.datastores.base.InMemoryCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.plans.ELUserPlansStore.ELColStorePlansLoadedEvent
import com.everlog.data.model.plan.ELPlan

/**
 * [ELUserPlansStore] for Firebase Test Lab runs, kept in memory.
 */
class ELUserPlansInMemoryStore : InMemoryCollectionStore<ELPlan>() {

    companion object {
        const val COLLECTION = "plans"
    }

    override fun getCollection(): String {
        return COLLECTION
    }

    override fun getOrder(): Comparator<ELPlan> {
        return compareBy(nullsFirst()) { it.name }
    }

    override fun decorateItem(item: ELPlan) {
        ELPlanInMemoryDecorator().decoratePlan(item)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELPlan>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELPlan> {
        return ELColStorePlansLoadedEvent(items, error, fromCache)
    }
}
