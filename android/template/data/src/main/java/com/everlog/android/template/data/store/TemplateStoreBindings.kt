package com.everlog.android.template.data.store

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.DocumentStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

// The routine store for the graph, until the graph builds the stores
@ContributesTo(AppScope::class)
@BindingContainer
object TemplateStoreBindings {
    @Provides
    fun routineStore(): DocumentStore<ELRoutine> = TemplateStores.routineStore()
}
