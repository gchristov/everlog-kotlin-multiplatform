package com.everlog.data.datastores.plans

import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.model.plan.ELPlan
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query

class ELUserPlansStore : ELCollectionStore<ELPlan>() {

    override fun getType(): Class<ELPlan> {
        return ELPlan::class.java
    }

    override fun getQuery(): Query {
        return FirestorePathManager.plansCollection
                .whereEqualTo(ELConstants.FIELD_CREATED_BY_USER_ID, LocalUserManager.getUser()!!.id)
                .orderBy(ELConstants.FIELD_NAME)
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

    class ELColStorePlanAddedEvent internal constructor(position: Int,
                                                        item: ELPlan,
                                                        hasPendingWrites: Boolean,
                                                        fromCache: Boolean) : ELColStoreItemAddedEvent<ELPlan>(position, item, hasPendingWrites, fromCache)

    class ELColStorePlanModifiedEvent internal constructor(oldPosition: Int,
                                                           newPosition: Int,
                                                           item: ELPlan,
                                                           hasPendingWrites: Boolean,
                                                           fromCache: Boolean) : ELColStoreItemModifiedEvent<ELPlan>(oldPosition, newPosition, item, hasPendingWrites, fromCache)

    class ELColStorePlanRemovedEvent internal constructor(position: Int,
                                                          item: ELPlan,
                                                          hasPendingWrites: Boolean,
                                                          fromCache: Boolean) : ELColStoreItemRemovedEvent<ELPlan>(position, item, hasPendingWrites, fromCache)

    class ELColStorePlansLoadedEvent internal constructor(items: List<ELPlan>?,
                                                          error: Throwable?,
                                                          fromCache: Boolean) : ELColStoreItemsLoadedEvent<ELPlan>(items, error, fromCache)
}