package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.model.exercise.library.ExerciseRegistryDto
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.exercise.library.LibraryExerciseDto
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
    suspend fun libraryExercises(): Either<Throwable, List<LibraryExercise>>

    /**
     * One exercise of the global library, hidden or not (history and routines still show hidden ones),
     * or null when the library doesn't have it, e.g. a user's own exercise.
     */
    suspend fun libraryExercise(uuid: String): Either<Throwable, LibraryExercise?>

    // The user's own exercises, which have only a name, an image and a category
    suspend fun ownExercises(): Either<Throwable, List<LibraryExercise>>

    /**
     * The user's own exercises as they change, for as long as it's collected, like ELExercisesStore's
     * listener: one created, edited or deleted elsewhere (e.g. the create exercise screen) arrives
     * straight away, as Firestore reports local writes before the server confirms them. Errors are
     * skipped, keeping the last list.
     */
    fun ownExerciseUpdates(): Flow<List<LibraryExercise>>

    // The ids the library uses and their names
    suspend fun registry(): Either<Throwable, ExerciseRegistry>
}

class RealExerciseRepository(
    private val dispatcher: CoroutineDispatcher,
) : ExerciseRepository {

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> = withContext(dispatcher) {
        Either.catch {
            FirestorePathManager.globalExercisesCollection.get().await().toObjects(ELExercise::class.java)
        }
    }

    override suspend fun libraryExercises(): Either<Throwable, List<LibraryExercise>> = withContext(dispatcher) {
        Either.catch {
            FirestorePathManager.globalExercisesCollection.getCacheFirst(refreshKey = "globalExercises")
                .documents
                .mapNotNull { it.toLibraryExerciseOrNull(own = false) }
        }
    }

    override suspend fun libraryExercise(uuid: String): Either<Throwable, LibraryExercise?> = withContext(dispatcher) {
        Either.catch {
            val document = FirestorePathManager.globalExercisesCollection.document(uuid)
                .getCacheFirst(refreshKey = "globalExercise/$uuid")
            if (document.exists()) document.toObject(LibraryExerciseDto::class.java)?.toLibraryExercise(documentId = document.id, own = false) else null
        }
    }

    override suspend fun ownExercises(): Either<Throwable, List<LibraryExercise>> = withContext(dispatcher) {
        Either.catch {
            // ownExerciseUpdates() keeps them current, so the cache is enough here
            FirestorePathManager.exercisesCollection.getCacheFirst(emptyIsAnswer = true)
                .documents
                .mapNotNull { it.toLibraryExerciseOrNull(own = true) }
        }
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
        .map { documents -> documents.mapNotNull { it.toLibraryExerciseOrNull(own = true) } }
        .flowOn(dispatcher)

    override suspend fun registry(): Either<Throwable, ExerciseRegistry> = withContext(dispatcher) {
        Either.catch {
            FirestorePathManager.exerciseRegistryDocument.getCacheFirst(refreshKey = "exerciseRegistry")
                .toObject(ExerciseRegistryDto::class.java)
                ?.toRegistry()
                ?: ExerciseRegistry()
        }
    }

    // One document that doesn't parse is left out, rather than failing the whole library
    private fun DocumentSnapshot.toLibraryExerciseOrNull(own: Boolean): LibraryExercise? =
        runCatching { toObject(LibraryExerciseDto::class.java) }
            .onFailure { Timber.tag(TAG).w(it, "Skipped exercise %s, which doesn't parse", id) }
            .getOrNull()
            ?.takeUnless { it.hidden }
            ?.toLibraryExercise(documentId = id, own = own)
}

private const val TAG = "ExerciseRepository"
