package com.everlog.android.plan.data.store

import com.everlog.android.common.firebase.FirestoreFields
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.data.store.ELCollectionStore
import com.everlog.android.common.data.store.events.ELColStoreItemAddedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemModifiedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemRemovedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.firebase.FirestorePaths
import com.google.firebase.firestore.Query

class ELUserPlansStore : ELCollectionStore<ELPlan>() {

    override fun getType(): Class<ELPlan> {
        return ELPlan::class.java
    }

    override fun getQuery(): Query {
        return FirestorePaths.plans
                .whereEqualTo(FirestoreFields.CREATED_BY_USER_ID, LocalUserManager.getUser()!!.id)
                .orderBy(FirestoreFields.NAME)
    }

    override fun getTag(): String {
        return "ELUserPlansStore"
    }

    override fun decorateItem(item: ELPlan) {
        ELPlanDecorator().decoratePlan(item)
    }

    // Events

    override fun getCollectionStoreItemAddedEvent(position: Int,
                                                  item: ELPlan,
                                                  hasPendingWrites: Boolean,
                                                  fromCache: Boolean): ELColStoreItemAddedEvent<ELPlan> {
        return ELColStorePlanAddedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemModifiedEvent(oldPosition: Int,
                                                     newPosition: Int,
                                                     item: ELPlan,
                                                     hasPendingWrites: Boolean,
                                                     fromCache: Boolean): ELColStoreItemModifiedEvent<ELPlan> {
        return ELColStorePlanModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemRemovedEvent(position: Int,
                                                    item: ELPlan,
                                                    hasPendingWrites: Boolean,
                                                    fromCache: Boolean): ELColStoreItemRemovedEvent<ELPlan> {
        return ELColStorePlanRemovedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELPlan>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELPlan> {
        return ELColStorePlansLoadedEvent(items, error, fromCache)
    }
}