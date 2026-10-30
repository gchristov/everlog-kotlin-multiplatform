package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.datasync.CollectionSource
import com.everlog.android.common.datasync.DataSync
import com.everlog.android.common.datasync.DocumentSource
import com.everlog.android.common.datasync.FirestoreCollection
import com.everlog.android.common.datasync.FirestoreDocument
import com.everlog.android.common.datasync.SyncPlan
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.map
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.model.exercise.library.ExerciseRegistryDto
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.exercise.library.LibraryExerciseDto
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
     * The user's own exercises as they change, for as long as it's collected, once they've loaded: one created,
     * edited or deleted on this phone (e.g. the create exercise screen) arrives straight away, as Firestore puts
     * local writes in the cache; other phones' changes at the next sync.
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

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> =
        ownExercises().transform { load -> if (load is Load.Ready) emit(load.value) }

    override fun registry(): Flow<Load<ExerciseRegistry?>> = dataSync.observe(
        DocumentSource("exerciseRegistry", userId = null, FirestoreDocument(FirestorePathManager.exerciseRegistryDocument, dispatcher) {
            it.toObject(ExerciseRegistryDto::class.java)?.toRegistry()
        })
    )

    // Hidden ones included: history still shows them. DataSync skips one that doesn't parse.
    private fun DocumentSnapshot.toStoredExercise(own: Boolean): StoredExercise? =
        toObject(LibraryExerciseDto::class.java)?.let { dto -> dto.toLibraryExercise(documentId = id, own = own)?.let { StoredExercise(it, dto.hidden) } }

    private fun List<StoredExercise>.visible() = filterNot { it.hidden }.map { it.exercise }

    // Hidden ones leave the library but stay readable by history
    private data class StoredExercise(val exercise: LibraryExercise, val hidden: Boolean)
}

