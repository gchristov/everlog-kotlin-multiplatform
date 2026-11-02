package com.everlog.android.template.data.store

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.data.CommonData
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.data.store.DocumentStore

// The routine stores, one of each until destroy(). Test Lab runs keep routines in memory, so robots don't create data.
object TemplateStores {

    // Set by the app at startup, until the app's graph builds the stores
    lateinit var session: UserSession

    private var routines: CollectionStore<ELRoutine>? = null
    private var routine: DocumentStore<ELRoutine>? = null

    @JvmStatic
    @Synchronized
    fun routinesStore(): CollectionStore<ELRoutine> =
        routines ?: (if (inMemory()) ELUserRoutinesInMemoryStore(session) else ELUserRoutinesStore(session)).also { routines = it }

    @JvmStatic
    @Synchronized
    fun routineStore(): DocumentStore<ELRoutine> =
        routine ?: (if (inMemory()) ELUserRoutineInMemoryStore(session) else ELUserRoutineStore(session)).also { routine = it }

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
