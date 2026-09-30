package com.everlog.data.datastores.plans

import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.plan.ELPlan

// Events posted by the plan stores, both the Firestore and the in-memory ones

class ELDocStorePlanLoadedEvent internal constructor(item: ELPlan?,
                                                     hasPendingWrites: Boolean,
                                                     fromCache: Boolean,
                                                     error: Throwable?) : ELDocStoreItemLoadedEvent<ELPlan?>(item, hasPendingWrites, fromCache, error)

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
