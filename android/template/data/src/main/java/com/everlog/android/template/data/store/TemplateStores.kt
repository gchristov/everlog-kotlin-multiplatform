package com.everlog.android.template.data.store

import com.everlog.android.common.data.CommonData
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.data.store.DocumentStore

// The routine stores, one of each until destroy(). Test Lab runs keep routines in memory, so robots don't create data.
object TemplateStores {

    private var routines: CollectionStore<ELRoutine>? = null
    private var routine: DocumentStore<ELRoutine>? = null

    @JvmStatic
    @Synchronized
    fun routinesStore(): CollectionStore<ELRoutine> =
        routines ?: (if (inMemory()) ELUserRoutinesInMemoryStore() else ELUserRoutinesStore()).also { routines = it }

    @JvmStatic
    @Synchronized
    fun routineStore(): DocumentStore<ELRoutine> =
        routine ?: (if (inMemory()) ELUserRoutineInMemoryStore() else ELUserRoutineStore()).also { routine = it }

    @JvmStatic
    @Synchronized
    fun destroy() {
        routines?.destroy()
        routines = null
        routine?.destroy()
        routine = null
    }

    private fun inMemory() = CommonData.dependencies.isTestLabRun()
}
