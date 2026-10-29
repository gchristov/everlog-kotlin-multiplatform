package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.android.common.datasync.CollectionSource
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.common.datasync.FirestoreCollection
import com.everlog.android.common.datasync.SyncPlan
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.map
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.utils.device.DeviceUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

interface RoutineRepository {
    /**
     * One of the user's routines as it changes, read from the cache, or null once it's known not to exist, e.g.
     * deleted on another phone. Null without a signed-in user.
     */
    fun routine(uuid: String): Flow<Load<ELRoutine?>>

    /**
     * Saves the routines to the user's routines in one write, so either all of them are saved or
     * none are. Saving a routine again overwrites it, as routines are saved by their uuid.
     *
     * Like the rest of the app, it works offline: Firestore applies the write to its cache straight
     * away and sends it when back online. So if the server hasn't confirmed it within
     * [ServerTimeout], it counts as saved. It fails if the write is rejected, e.g. with no user.
     */
    suspend fun saveRoutines(routines: List<ELRoutine>): Either<Throwable, Unit>

    /**
     * How many of the user's routines (workout templates) use each exercise, by the exercise's
     * uuid. An exercise in a routine twice counts once.
     */
    suspend fun exerciseTemplateCounts(): Either<Throwable, Map<String, Int>>

    companion object {
        const val ServerTimeout = 5_000L
    }
}

class RealRoutineRepository(
    private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
) : RoutineRepository {

    override fun routine(uuid: String): Flow<Load<ELRoutine?>> = routines().map { load -> load.map { routines -> routines.firstOrNull { it.uuid == uuid } } }

    private fun routines(): Flow<Load<List<ELRoutine>>> {
        val userId = LocalUserManager.getUser()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(
            CollectionSource("routines", userId, SyncPlan.Whole, FirestoreCollection(FirestorePathManager.routinesCollection, dispatcher = dispatcher) {
                it.toObject(ELRoutine::class.java)
            }),
        )
    }

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

    override suspend fun exerciseTemplateCounts(): Either<Throwable, Map<String, Int>> = withContext(dispatcher) {
        Either.catch {
            // The routines store keeps the cache current while the app runs
            FirestorePathManager.routinesCollection.getCacheFirst(emptyIsAnswer = true)
                .documents
                .mapNotNull { document ->
                    runCatching { document.toObject(ELRoutine::class.java) }
                        .onFailure { Timber.tag(TAG).w(it, "Skipped routine %s, which doesn't parse", document.id) }
                        .getOrNull()
                }
                .flatMap { routine ->
                    routine.exerciseGroups.flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }.toSet()
                }
                .groupingBy { it }
                .eachCount()
        }
    }

    private companion object {
        const val TAG = "RoutineRepository"
    }
}
