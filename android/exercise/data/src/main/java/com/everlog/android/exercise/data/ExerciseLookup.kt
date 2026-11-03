package com.everlog.android.exercise.data

import android.os.Looper
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.exercise.data.model.library.toELExercise
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * The library's exercises and the user's own, hidden ones included, by uuid, for code that resolves exercises an
 * item at a time and can't wait on a Flow, like the old stores' routine decorator. It follows them while the app
 * runs, so a rename reaches the next item resolved.
 */
interface ExerciseLookup {
    /**
     * The ones of [uuids] it has. The first call waits a few seconds for the exercises to load, except on the main
     * thread. Ones it doesn't have are fetched in the background, for later calls.
     */
    fun find(uuids: Set<String>): Map<String, ELExercise>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RealExerciseLookup(
    private val repository: ExerciseRepository,
    private val scope: CoroutineScope,
    private val firstLoadTimeoutMillis: Long,
    private val fetchDelayMillis: Long,
    private val retryAfterMillis: Long,
    private val onMainThread: () -> Boolean,
) : ExerciseLookup {

    @Inject
    constructor(repository: ExerciseRepository, @IoDispatcher dispatcher: CoroutineDispatcher) : this(
        repository,
        CoroutineScope(SupervisorJob() + dispatcher),
        firstLoadTimeoutMillis = 5_000,
        fetchDelayMillis = 500,
        retryAfterMillis = 60_000,
        onMainThread = { Looper.myLooper() == Looper.getMainLooper() },
    )

    private sealed interface Exercises {
        data object Loading : Exercises
        data class Ready(val byUuid: Map<String, ELExercise>) : Exercises
        // Waited for long enough, or couldn't load: items keep the copies they were saved with
        data object Missing : Exercises
    }

    private val exercises = MutableStateFlow<Exercises>(Exercises.Loading)
    private var following: Job? = null

    // Fetched or being fetched while the exercises are followed, so each is asked for once
    private val asked = HashSet<String>()
    private val toFetch = HashSet<String>()
    private var fetching: Job? = null

    override fun find(uuids: Set<String>): Map<String, ELExercise> {
        follow()
        val found = when (val current = exercises.value) {
            is Exercises.Ready -> current.byUuid
            Exercises.Missing -> return emptyMap()
            Exercises.Loading -> if (onMainThread()) return emptyMap() else awaitFirstLoad() ?: return emptyMap()
        }
        fetchLater(uuids - found.keys)
        return uuids.mapNotNull { uuid -> found[uuid]?.let { uuid to it } }.toMap()
    }

    private fun awaitFirstLoad(): Map<String, ELExercise>? = runBlocking {
        val loaded = withTimeoutOrNull(firstLoadTimeoutMillis) { exercises.first { it != Exercises.Loading } }
        if (loaded == null) exercises.compareAndSet(Exercises.Loading, Exercises.Missing)
        (loaded as? Exercises.Ready)?.byUuid
    }

    // Again after sign-out, which ends the reads, for the next user
    @Synchronized
    private fun follow() {
        if (following?.isActive == true) return
        exercises.value = Exercises.Loading
        asked.clear()
        following = scope.launch {
            repository.allExercises().collect { load ->
                when (load) {
                    is Load.Ready -> exercises.value = Exercises.Ready(load.value.associate { it.uuid to it.toELExercise() })
                    is Load.Unavailable -> if (exercises.value == Exercises.Loading) exercises.value = Exercises.Missing
                    Load.Loading -> Unit
                }
            }
        }
    }

    // Gathered for a moment, as items are resolved one after another
    @Synchronized
    private fun fetchLater(uuids: Set<String>) {
        val new = uuids - asked
        if (new.isEmpty()) return
        asked += new
        toFetch += new
        if (fetching != null) return
        // Started once it's recorded, so a fetch that finishes straight away doesn't leave a finished job behind
        val job = scope.launch(start = CoroutineStart.LAZY) { fetchUntilDone() }
        fetching = job
        job.start()
    }

    private suspend fun fetchUntilDone() {
        while (true) {
            delay(fetchDelayMillis)
            val batch = takeBatch() ?: return
            val fetched = repository.fetchExercises(batch)
            if (fetched is Load.Unavailable) {
                Timber.tag(TAG).w("Couldn't fetch %d exercises: %s", batch.size, fetched.error.message)
                scope.launch {
                    delay(retryAfterMillis)
                    synchronized(this@RealExerciseLookup) { asked -= batch }
                }
            }
        }
    }

    // Null when there's nothing left, which ends the fetching
    @Synchronized
    private fun takeBatch(): Set<String>? {
        if (toFetch.isEmpty()) {
            fetching = null
            return null
        }
        return toFetch.toSet().also { toFetch.clear() }
    }

    private companion object {
        const val TAG = "ExerciseLookup"
    }
}
