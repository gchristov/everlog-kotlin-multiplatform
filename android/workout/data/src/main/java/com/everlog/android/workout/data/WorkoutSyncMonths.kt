package com.everlog.android.workout.data

import dev.zacsweers.metro.Qualifier

// How many months of workouts to keep synced, which the app sets
@Qualifier
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class WorkoutSyncMonths
