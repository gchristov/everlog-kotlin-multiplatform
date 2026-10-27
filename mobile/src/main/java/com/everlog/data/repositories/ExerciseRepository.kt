package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.model.exercise.library.ExerciseRegistryDto
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.exercise.library.LibraryExerciseDto
import com.everlog.data.sync.CollectionSource
import com.everlog.data.sync.DataSync
import com.everlog.data.sync.DocumentSource
import com.everlog.data.sync.FirestoreCollection
import com.everlog.data.sync.FirestoreDocument
import com.everlog.data.sync.Load
import com.everlog.data.sync.SyncPlan
import com.everlog.data.sync.map
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

interface ExerciseRepository {
    /**
     * The global exercise library, the exercises everyone has. A one-off read, unlike
     * ELExercisesStore, which also listens for changes and merges in the user's own exercises.
     */
    suspend fun globalExercises(): Either<Throwable, List<ELExercise>>

    /**
     * The global library with the fields [ELExercise] doesn't read (family, equipment, muscles and
     * so on), without hidden exercises, which stay readable by history but leave the library.
     */
    fun libraryExercises(): Flow<Load<List<LibraryExercise>>>

    /**
     * One exercise of the global library, hidden or not (history and routines still show hidden ones),
     * or null when the library doesn't have it, e.g. a user's own exercise.
     */
    fun libraryExercise(uuid: String): Flow<Load<LibraryExercise?>>

    // The user's own exercises, which have only a name, an image and a category. None without a signed-in user.
    fun ownExercises(): Flow<Load<List<LibraryExercise>>>

    /**
     * The user's own exercises as they change, for as long as it's collected, like ELExercisesStore's
     * listener: one created, edited or deleted elsewhere (e.g. the create exercise screen) arrives
     * straight away, as Firestore reports local writes before the server confirms them. Errors are
     * skipped, keeping the last list.
     */
    fun ownExerciseUpdates(): Flow<List<LibraryExercise>>

    // The ids the library uses and their names, or null when there's no registry
    fun registry(): Flow<Load<ExerciseRegistry?>>
}

class RealExerciseRepository(
    private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
) : ExerciseRepository {

    private val librarySource = CollectionSource(
        "globalExercises",
        userId = null,
        SyncPlan.Whole,
        FirestoreCollection(FirestorePathManager.globalExercisesCollection, dispatcher = dispatcher) { it.toStoredExercise(own = false) },
    )

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> = withContext(dispatcher) {
        Either.catch {
            FirestorePathManager.globalExercisesCollection.get().await().toObjects(ELExercise::class.java)
        }
    }

    override fun libraryExercises(): Flow<Load<List<LibraryExercise>>> =
        dataSync.observe(librarySource).map { load -> load.map { it.visible() } }

    // Its own document, so opening one exercise doesn't sync the whole library
    override fun libraryExercise(uuid: String): Flow<Load<LibraryExercise?>> = dataSync.observe(
        DocumentSource("globalExercise/$uuid", userId = null, FirestoreDocument(FirestorePathManager.globalExercisesCollection.document(uuid), dispatcher) {
            it.toStoredExercise(own = false)?.exercise
        })
    )

    override fun ownExercises(): Flow<Load<List<LibraryExercise>>> {
        val userId = LocalUserManager.getUser()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(
            CollectionSource("exercises", userId, SyncPlan.Whole, FirestoreCollection(FirestorePathManager.exercisesCollection, dispatcher = dispatcher) {
                it.toStoredExercise(own = true)
            })
        ).map { load -> load.map { it.visible() } }
    }

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> = callbackFlow {
        // Throws without a signed-in user: then there's nothing to follow
        val collection = runCatching { FirestorePathManager.exercisesCollection }
            .onFailure { Timber.tag(TAG).w(it, "No user's exercises to follow") }
            .getOrNull()
        if (collection == null) {
            close()
            return@callbackFlow
        }
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                Timber.tag(TAG).w(error, "Couldn't follow the user's exercises")
                return@addSnapshotListener
            }
            trySend(snapshot.documents)
        }
        awaitClose { registration.remove() }
    }
        .map { documents -> documents.mapNotNull { it.toVisibleOwnExerciseOrNull() } }
        .flowOn(dispatcher)

    override fun registry(): Flow<Load<ExerciseRegistry?>> = dataSync.observe(
        DocumentSource("exerciseRegistry", userId = null, FirestoreDocument(FirestorePathManager.exerciseRegistryDocument, dispatcher) {
            it.toObject(ExerciseRegistryDto::class.java)?.toRegistry()
        })
    )

    // Hidden ones included: history still shows them. DataSync skips one that doesn't parse.
    private fun DocumentSnapshot.toStoredExercise(own: Boolean): StoredExercise? =
        toObject(LibraryExerciseDto::class.java)?.let { dto -> dto.toLibraryExercise(documentId = id, own = own)?.let { StoredExercise(it, dto.hidden) } }

    private fun List<StoredExercise>.visible() = filterNot { it.hidden }.map { it.exercise }

    // The listener's for the workout screen, which follows the user's own exercises live
    private fun DocumentSnapshot.toVisibleOwnExerciseOrNull(): LibraryExercise? =
        runCatching { toStoredExercise(own = true) }
            .onFailure { Timber.tag(TAG).w(it, "Skipped exercise %s, which doesn't parse", id) }
            .getOrNull()
            ?.takeUnless { it.hidden }
            ?.exercise

    // Hidden ones leave the library but stay readable by history
    private data class StoredExercise(val exercise: LibraryExercise, val hidden: Boolean)
}

private const val TAG = "ExerciseRepository"
