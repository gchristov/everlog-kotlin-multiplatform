package com.everlog.android.workout.data

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import kotlinx.coroutines.flow.Flow

/**
 * The user's workouts shown so far, newest first, growing a page at a time as the user scrolls back.
 *
 * @param loadingMore An older page is on its way from the server.
 * @param complete Nothing older: the first workout is in [workouts].
 * @param failed The last older page couldn't load: [WorkoutHistoryRead.loadMore] tries it again.
 */
data class WorkoutHistory(val workouts: List<ELWorkout>, val loadingMore: Boolean, val complete: Boolean, val failed: Boolean = false)

// One screen's read of the history, following the cache, so workouts saved or deleted anywhere show in place
interface WorkoutHistoryRead {
    val loads: Flow<Load<WorkoutHistory>>

    // For a screen scrolled to its oldest workout. A page that failed is tried again.
    fun loadMore()
}
