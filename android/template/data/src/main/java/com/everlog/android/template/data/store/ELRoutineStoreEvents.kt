package com.everlog.android.template.data.store

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.events.ELColStoreItemAddedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemModifiedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemRemovedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent

// Events posted by the routine stores, both the Firestore and the in-memory ones

class ELDocStoreRoutineLoadedEvent internal constructor(item: ELRoutine?,
                                                        hasPendingWrites: Boolean,
                                                        fromCache: Boolean,
                                                        error: Throwable?) : ELDocStoreItemLoadedEvent<ELRoutine?>(item, hasPendingWrites, fromCache, error)

class ELColStoreRoutineAddedEvent internal constructor(position: Int,
                                                       item: ELRoutine,
                                                       hasPendingWrites: Boolean,
                                                       fromCache: Boolean) : ELColStoreItemAddedEvent<ELRoutine>(position, item, hasPendingWrites, fromCache)

class ELColStoreRoutineModifiedEvent internal constructor(oldPosition: Int,
                                                          newPosition: Int,
                                                          item: ELRoutine,
                                                          hasPendingWrites: Boolean,
                                                          fromCache: Boolean) : ELColStoreItemModifiedEvent<ELRoutine>(oldPosition, newPosition, item, hasPendingWrites, fromCache)

class ELColStoreRoutineRemovedEvent internal constructor(position: Int,
                                                         item: ELRoutine,
                                                         hasPendingWrites: Boolean,
                                                         fromCache: Boolean) : ELColStoreItemRemovedEvent<ELRoutine>(position, item, hasPendingWrites, fromCache)

class ELColStoreRoutinesLoadedEvent internal constructor(items: List<ELRoutine>?,
                                                         error: Throwable?,
                                                         fromCache: Boolean) : ELColStoreItemsLoadedEvent<ELRoutine>(items, error, fromCache)
