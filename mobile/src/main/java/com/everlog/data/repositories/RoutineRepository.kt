package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.utils.device.DeviceUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

interface RoutineRepository {
    /**
     * Saves the routines to the user's routines in one write, so either all of them are saved or
     * none are. Saving a routine again overwrites it, as routines are saved by their uuid.
     *
     * Like the rest of the app, it works offline: Firestore applies the write to its cache straight
     * away and sends it when back online. So if the server hasn't confirmed it within
     * [ServerTimeout], it counts as saved. It fails if the write is rejected, e.g. with no user.
     */
    suspend fun saveRoutines(routines: List<ELRoutine>): Either<Throwable, Unit>

    companion object {
        const val ServerTimeout = 5_000L
    }
}

class RealRoutineRepository(
    private val dispatcher: CoroutineDispatcher,
) : RoutineRepository {

    override suspend fun saveRoutines(routines: List<ELRoutine>): Either<Throwable, Unit> = withContext(dispatcher) {
        Either.catch {
            // Firebase Test Lab runs keep routines in memory, so robots don't create data
            if (DeviceUtils.isFirebaseTestLabRun()) {
                routines.forEach { ELDatastore.routineStore().create(it, SetOptions.merge()) }
                return@catch
            }
            val collection = FirestorePathManager.routinesCollection
            val batch = FirebaseFirestore.getInstance().batch()
            routines.forEach { batch.set(collection.document(it.documentId()), it.asMap(), SetOptions.merge()) }
            val confirmed = withTimeoutOrNull(RoutineRepository.ServerTimeout) { batch.commit().await() }
            if (confirmed == null) Timber.tag(TAG).w("Saved %d routines offline, they sync once back online", routines.size)
        }
    }

    private companion object {
        const val TAG = "RoutineRepository"
    }
}
