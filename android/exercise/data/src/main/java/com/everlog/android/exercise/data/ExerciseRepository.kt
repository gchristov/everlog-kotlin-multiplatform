package com.everlog.android.exercise.data

import arrow.core.Either
import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.datasync.CollectionSource
import com.everlog.android.datasync.DataSync
import com.everlog.android.datasync.DocumentSource
import com.everlog.android.datasync.FirestoreCollection
import com.everlog.android.datasync.FirestoreDocument
import com.everlog.android.datasync.SyncPlan
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.common.kotlin.map
import com.everlog.android.common.kotlin.optional
import com.everlog.android.common.kotlin.zip
import com.everlog.android.exercise.data.model.library.ExerciseRegistry
import com.everlog.android.exercise.data.model.library.ExerciseRegistryDto
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.data.model.library.LibraryExerciseDto
import com.google.firebase.firestore.DocumentSnapshot
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    /**
     * The library and the user's own exercises, hidden ones included, as routines and history point at them. Ready
     * once the library is, with the own exercises when they're available.
     */
    fun allExercises(): Flow<Load<List<LibraryExercise>>>

    /**
     * Brings these exercises from the server into the cache, from the library or the user's own, so [allExercises]
     * shows them: for ones it doesn't have yet, e.g. added to the library since its last sync. Ready with the uuids
     * found.
     */
    suspend fun fetchExercises(uuids: Set<String>): Load<Set<String>>
}

@ContributesBinding(AppScope::class)
@Inject
class RealExerciseRepository(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val dataSync: DataSync,
    private val session: UserSession,
) : ExerciseRepository {

    private val librarySource = CollectionSource(
        "globalExercises",
        userId = null,
        SyncPlan.Whole,
        FirestoreCollection(FirestorePaths.globalExercises, dispatcher = dispatcher) { it.toStoredExercise(own = false) },
        // libraryExercise(uuid) and routines (ELRoutineDecorator) cache single exercises, and the picker searches the whole library
        completeOnly = true,
    )

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> = withContext(dispatcher) {
        Either.catch {
            FirestorePaths.globalExercises.get().await().toObjects(ELExercise::class.java)
        }
    }

    override fun libraryExercises(): Flow<Load<List<LibraryExercise>>> =
        dataSync.observe(librarySource).map { load -> load.map { it.visible() } }

    // Its own document, so opening one exercise doesn't sync the whole library
    override fun libraryExercise(uuid: String): Flow<Load<LibraryExercise?>> = dataSync.observe(
        DocumentSource("globalExercise/$uuid", userId = null, FirestoreDocument(FirestorePaths.globalExercises.document(uuid), dispatcher) {
            it.toStoredExercise(own = false)?.exercise
        })
    )

    override fun ownExercises(): Flow<Load<List<LibraryExercise>>> = ownStored().map { load -> load.map { it.visible() } }

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> =
        ownExercises().transform { load -> if (load is Load.Ready) emit(load.value) }

    override fun allExercises(): Flow<Load<List<LibraryExercise>>> =
        combine(dataSync.observe(librarySource), ownStored()) { library, own ->
            library.zip(own.optional()) { libraryExercises, ownExercises -> (libraryExercises + ownExercises.orEmpty()).map { it.exercise } }
        }

    override suspend fun fetchExercises(uuids: Set<String>): Load<Set<String>> {
        val inLibrary = when (val fetched = dataSync.fetchIds(librarySource, uuids)) {
            is Load.Ready -> fetched.value
            else -> return fetched
        }
        val rest = uuids - inLibrary
        val userId = session.user()?.id
        if (rest.isEmpty() || userId == null) return Load.Ready(inLibrary)
        return dataSync.fetchIds(ownSource(userId), rest).map { inLibrary + it }
    }

    private fun ownStored(): Flow<Load<List<StoredExercise>>> {
        val userId = session.user()?.id ?: return flowOf(Load.Ready(emptyList()))
        return dataSync.observe(ownSource(userId))
    }

    private fun ownSource(userId: String) = CollectionSource(
        "exercises",
        userId,
        SyncPlan.Whole,
        FirestoreCollection(FirestorePaths.exercises(userId), dispatcher = dispatcher) { it.toStoredExercise(own = true) },
        // Routines read single exercises (ELRoutineDecorator), and the picker searches them with the library
        completeOnly = true,
    )

    override fun registry(): Flow<Load<ExerciseRegistry?>> = dataSync.observe(
        DocumentSource("exerciseRegistry", userId = null, FirestoreDocument(FirestorePaths.exerciseRegistry, dispatcher) {
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

