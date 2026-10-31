package com.everlog.android.plan.data.store

import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.common.data.store.events.ELColStoreItemAddedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemModifiedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemRemovedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent

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
