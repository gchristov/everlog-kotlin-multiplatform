package com.everlog.android.common.kotlin.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// For reads and writes: disk, network, Firestore
@Qualifier
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class IoDispatcher

// For work on the CPU, like building a session or a library from what was read
@Qualifier
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class DefaultDispatcher

@ContributesTo(AppScope::class)
@BindingContainer
object DispatcherBindings {
    @Provides
    @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun default(): CoroutineDispatcher = Dispatchers.Default
}
