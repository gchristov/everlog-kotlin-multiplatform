package com.everlog.data.datastores.routines

import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.ELRoutine

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
