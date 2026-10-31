package com.everlog.android.plan.data.store

import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.data.store.InMemoryCollectionStore
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent

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
