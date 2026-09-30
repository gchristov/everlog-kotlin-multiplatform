package com.everlog.data.datastores.history

import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.datastores.events.document.ELDocStoreItemLoadedEvent
import com.everlog.data.model.workout.ELWorkout

// Events posted by the workout stores, both the Firestore and the in-memory ones

class ELDocStoreWorkoutLoadedEvent internal constructor(item: ELWorkout?,
                                                        hasPendingWrites: Boolean,
                                                        fromCache: Boolean,
                                                        error: Throwable?) : ELDocStoreItemLoadedEvent<ELWorkout?>(item, hasPendingWrites, fromCache, error)

class ELColStoreWorkoutAddedEvent internal constructor(position: Int,
                                                       item: ELWorkout,
                                                       hasPendingWrites: Boolean,
                                                       fromCache: Boolean) : ELColStoreItemAddedEvent<ELWorkout>(position, item, hasPendingWrites, fromCache)

class ELColStoreWorkoutModifiedEvent internal constructor(oldPosition: Int,
                                                          newPosition: Int,
                                                          item: ELWorkout,
                                                          hasPendingWrites: Boolean,
                                                          fromCache: Boolean) : ELColStoreItemModifiedEvent<ELWorkout>(oldPosition, newPosition, item, hasPendingWrites, fromCache)

class ELColStoreWorkoutRemovedEvent internal constructor(position: Int,
                                                         item: ELWorkout,
                                                         hasPendingWrites: Boolean,
                                                         fromCache: Boolean) : ELColStoreItemRemovedEvent<ELWorkout>(position, item, hasPendingWrites, fromCache)

class ELColStoreWorkoutsLoadedEvent internal constructor(items: List<ELWorkout>?,
                                                         error: Throwable?,
                                                         fromCache: Boolean) : ELColStoreItemsLoadedEvent<ELWorkout>(items, error, fromCache)
