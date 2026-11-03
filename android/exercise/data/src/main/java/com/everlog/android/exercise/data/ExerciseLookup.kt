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

    /**
     * [action] runs whenever the exercises arrive after finds had to go without them, e.g. after a slow first load
     * or while offline, so what was resolved then can be resolved again.
     */
    fun onLateArrival(action: () -> Unit)
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
    private var retrying: Job? = null

    // Fetched or being fetched while the exercises are followed, so each is asked for once
    private val asked = HashSet<String>()
    private val toFetch = HashSet<String>()
    private var fetching: Job? = null

    private var lateArrival: List<() -> Unit> = emptyList()
    private var wentWithout = false

    override fun find(uuids: Set<String>): Map<String, ELExercise> {
        follow()
        val found = when (val current = exercises.value) {
            is Exercises.Ready -> current.byUuid
            Exercises.Missing -> null
            Exercises.Loading -> if (onMainThread()) null else awaitFirstLoad()
        }
        if (found == null) {
            synchronized(this) { wentWithout = true }
            return emptyMap()
        }
        fetchLater(uuids.filterTo(HashSet()) { it.isNotBlank() } - found.keys)
        return uuids.mapNotNull { uuid -> found[uuid]?.let { uuid to it } }.toMap()
    }

    @Synchronized
    override fun onLateArrival(action: () -> Unit) {
        lateArrival = lateArrival + action
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
        wentWithout = false
        following = collect()
    }

    private fun collect(): Job = scope.launch {
        repository.allExercises().collect { load ->
            when (load) {
                is Load.Ready -> arrived(load.value.associate { it.uuid to it.toELExercise() })
                is Load.Unavailable -> {
                    exercises.compareAndSet(Exercises.Loading, Exercises.Missing)
                    readAgainLater()
                }
                Load.Loading -> Unit
            }
        }
    }

    private fun arrived(byUuid: Map<String, ELExercise>) {
        exercises.value = Exercises.Ready(byUuid)
        val late = synchronized(this) { lateArrival.takeIf { wentWithout }.orEmpty().also { wentWithout = false } }
        late.forEach { it() }
    }

    // DataSync tries a failed sync again when it's read again, e.g. the library's first sync while offline
    @Synchronized
    private fun readAgainLater() {
        if (retrying?.isActive == true) return
        retrying = scope.launch {
            delay(retryAfterMillis)
            synchronized(this@RealExerciseLookup) {
                following?.cancel()
                following = collect()
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
