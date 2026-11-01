package com.everlog.android.template.data

import arrow.core.Either
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.store.DocumentStore
import com.everlog.android.common.datasync.CollectionSource
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.common.datasync.DocumentSource
import com.everlog.android.common.datasync.FirestoreCollection
import com.everlog.android.common.datasync.FirestoreDocument
import com.everlog.android.common.datasync.SyncPlan
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.common.kotlin.Load
import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.data.CommonData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

/**
 * @param testLabRoutines Where routines go during Test Lab runs, which keep them in memory so robots don't
 * create data
 */
class RealRoutineRepository(
    private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
    private val testLabRoutines: () -> DocumentStore<ELRoutine>,
) : RoutineRepository {

    // Its own document: the routines' cache may hold only some of them, so missing from it doesn't mean deleted
    override fun routine(uuid: String): Flow<Load<ELRoutine?>> {
        val userId = LocalUserManager.getUser()?.id ?: return flowOf(Load.Ready(null))
        return dataSync.observe(
            DocumentSource("routine/$uuid", userId, FirestoreDocument(FirestorePaths.routines(userId).document(uuid), dispatcher) {
                it.toObject(ELRoutine::class.java)
            }),
        )
    }

    private fun routines(): Flow<Load<List<ELRoutine>>> {
        val userId = LocalUserManager.getUser()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(
            CollectionSource(
                "routines",
                userId,
                SyncPlan.Whole,
                FirestoreCollection(FirestorePaths.routines(userId), dispatcher = dispatcher) { it.toObject(ELRoutine::class.java) },
                // The routine screens cache single routines (ELUserRoutineStore), and counts need all of them
                completeOnly = true,
            ),
        )
    }

    override suspend fun saveRoutines(routines: List<ELRoutine>): Either<Throwable, Unit> = withContext(dispatcher) {
        Either.catch {
            if (CommonData.dependencies.isTestLabRun()) {
                routines.forEach { testLabRoutines().create(it, SetOptions.merge()) }
                return@catch
            }
            val collection = FirestorePaths.routines(LocalUserManager.requireUserId())
            val batch = FirebaseFirestore.getInstance().batch()
            routines.forEach { batch.set(collection.document(it.documentId()), it.asMap(), SetOptions.merge()) }
            // commit() completes with null, so a plain await() can't tell a confirmed write from a timeout
            val confirmed = withTimeoutOrNull(RoutineRepository.ServerTimeout) { batch.commit().await(); true }
            if (confirmed == null) Timber.tag(TAG).w("Saved %d routines offline, they sync once back online", routines.size)
        }
    }

    override suspend fun exerciseTemplateCounts(): Either<Throwable, Map<String, Int>> =
        when (val load = routines().first { it != Load.Loading }) {
            is Load.Ready -> Either.Right(exerciseTemplateCounts(load.value))
            is Load.Unavailable -> Either.Left(load.error)
            Load.Loading -> error("Filtered out")
        }

    private companion object {
        const val TAG = "RoutineRepository"
    }
}

// How many of the routines use each exercise, by the exercise's uuid. An exercise in a routine twice counts once.
internal fun exerciseTemplateCounts(routines: List<ELRoutine>): Map<String, Int> =
    routines
        .flatMap { routine -> routine.exerciseGroups.flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }.toSet() }
        .groupingBy { it }
        .eachCount()
