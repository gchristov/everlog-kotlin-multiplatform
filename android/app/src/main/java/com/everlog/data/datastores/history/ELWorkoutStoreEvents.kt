package com.everlog.data.datastores.history

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.events.ELColStoreItemAddedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemModifiedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemRemovedEvent
import com.everlog.android.common.data.store.events.ELColStoreItemsLoadedEvent
import com.everlog.android.common.data.store.events.ELDocStoreItemLoadedEvent

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
