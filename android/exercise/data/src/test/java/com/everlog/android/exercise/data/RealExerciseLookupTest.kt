package com.everlog.android.exercise.data

import com.everlog.android.common.kotlin.Load
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.testfixtures.FakeExerciseRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.takeWhile
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RealExerciseLookupTest {

    private val bench = LibraryExercise(uuid = "bench", name = "Bench press")
    private val row = LibraryExercise(uuid = "row", name = "Cable row", own = true)

    private val repository = FakeExerciseRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var onMainThread = false
    private val lookup = RealExerciseLookup(
        repository,
        scope,
        firstLoadTimeoutMillis = 300,
        fetchDelayMillis = 50,
        retryAfterMillis = 100,
        onMainThread = { onMainThread },
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    // Main journeys

    @Test
    fun `finds the library's and the user's own exercises it has`() {
        loaded(bench, row)

        val found = lookup.find(setOf("bench", "row", "missing"))

        assertThat(found.keys).containsExactly("bench", "row")
        assertThat(found.getValue("bench").name).isEqualTo("Bench press")
        assertThat(found.getValue("row").privateToUser).isTrue()
    }

    @Test
    fun `the first find waits for the exercises to load`() {
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Loading)
        repository.allExercises = mutableListOf(exercises)
        Executors.newSingleThreadScheduledExecutor().schedule({ exercises.value = Load.Ready(listOf(bench)) }, 100, TimeUnit.MILLISECONDS)

        assertThat(lookup.find(setOf("bench")).keys).containsExactly("bench")
    }

    @Test
    fun `a rename reaches the next find`() {
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Ready(listOf(bench)))
        repository.allExercises = mutableListOf(exercises)
        lookup.find(setOf("bench"))

        exercises.value = Load.Ready(listOf(bench.copy(name = "Barbell bench press")))

        awaitUntil { lookup.find(setOf("bench")).getValue("bench").name == "Barbell bench press" }
    }

    // Exercises it doesn't have

    @Test
    fun `missing exercises are fetched together, once`() {
        loaded(bench)

        lookup.find(setOf("bench", "squat"))
        lookup.find(setOf("deadlift"))
        awaitUntil { repository.fetches.isNotEmpty() }
        Thread.sleep(150)
        lookup.find(setOf("squat", "deadlift"))
        Thread.sleep(150)

        assertThat(repository.fetches).containsExactly(setOf("squat", "deadlift"))
    }

    @Test
    fun `a fetched exercise is found by a later find`() {
        val squat = LibraryExercise(uuid = "squat", name = "Squat")
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Ready(listOf(bench)))
        repository.allExercises = mutableListOf(exercises)
        repository.fetch = { uuids ->
            exercises.value = Load.Ready(listOf(bench, squat))
            Load.Ready(uuids)
        }

        assertThat(lookup.find(setOf("squat"))).isEmpty()

        awaitUntil { lookup.find(setOf("squat")).containsKey("squat") }
    }

    @Test
    fun `a fetch that fails is tried again later`() {
        loaded(bench)
        repository.fetch = { Load.Unavailable(IOException("offline")) }
        lookup.find(setOf("squat"))
        awaitUntil { repository.fetches.size == 1 }

        Thread.sleep(250)
        lookup.find(setOf("squat"))

        awaitUntil { repository.fetches.size == 2 }
    }

    // Edge cases

    @Test
    fun `doesn't wait on the main thread`() {
        repository.allExercises = mutableListOf(MutableStateFlow(Load.Loading))
        onMainThread = true

        val started = System.currentTimeMillis()

        assertThat(lookup.find(setOf("bench"))).isEmpty()
        assertThat(System.currentTimeMillis() - started).isLessThan(200)
    }

    @Test
    fun `waits only once, so exercises that don't load in time leave items with their saved copies`() {
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Loading)
        repository.allExercises = mutableListOf(exercises)
        assertThat(lookup.find(setOf("bench"))).isEmpty()

        val started = System.currentTimeMillis()
        assertThat(lookup.find(setOf("bench"))).isEmpty()
        assertThat(System.currentTimeMillis() - started).isLessThan(200)

        exercises.value = Load.Ready(listOf(bench))
        awaitUntil { lookup.find(setOf("bench")).isNotEmpty() }
    }

    @Test
    fun `exercises that can't load don't hold items up`() {
        repository.allExercises = mutableListOf(flowOf(Load.Unavailable(IOException("offline"))))

        val started = System.currentTimeMillis()

        assertThat(lookup.find(setOf("bench"))).isEmpty()
        assertThat(System.currentTimeMillis() - started).isLessThan(200)
        assertThat(repository.fetches).isEmpty()
    }

    @Test
    fun `after sign-out ends the reads, the next user's exercises load`() {
        val first = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Ready(listOf(bench)))
        repository.allExercises = mutableListOf(first.endWhen { it is Load.Unavailable }, flowOf(Load.Ready(listOf(row))))
        assertThat(lookup.find(setOf("bench")).keys).containsExactly("bench")

        first.value = Load.Unavailable(IOException("signed out"))

        awaitUntil { lookup.find(setOf("row")).containsKey("row") }
    }

    @Test
    fun `blank ids aren't fetched`() {
        loaded(bench)

        lookup.find(setOf("", " ", "squat"))
        awaitUntil { repository.fetches.isNotEmpty() }

        assertThat(repository.fetches).containsExactly(setOf("squat"))
    }

    // Exercises arriving late

    @Test
    fun `exercises arriving after finds went without them let what was resolved be resolved again`() {
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Loading)
        repository.allExercises = mutableListOf(exercises)
        var lateArrivals = 0
        lookup.onLateArrival { lateArrivals++ }
        assertThat(lookup.find(setOf("bench"))).isEmpty()

        exercises.value = Load.Ready(listOf(bench))
        exercises.value = Load.Ready(listOf(bench, row))

        awaitUntil { lateArrivals == 1 }
        Thread.sleep(50)
        assertThat(lateArrivals).isEqualTo(1)
    }

    @Test
    fun `a find on the main thread before they load is resolved again once they do`() {
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Loading)
        repository.allExercises = mutableListOf(exercises)
        var lateArrivals = 0
        lookup.onLateArrival { lateArrivals++ }
        onMainThread = true
        lookup.find(setOf("bench"))

        exercises.value = Load.Ready(listOf(bench))

        awaitUntil { lateArrivals == 1 }
    }

    @Test
    fun `exercises loaded in time aren't late`() {
        val exercises = MutableStateFlow<Load<List<LibraryExercise>>>(Load.Loading)
        repository.allExercises = mutableListOf(exercises)
        var lateArrivals = 0
        lookup.onLateArrival { lateArrivals++ }
        Executors.newSingleThreadScheduledExecutor().schedule({ exercises.value = Load.Ready(listOf(bench)) }, 100, TimeUnit.MILLISECONDS)

        assertThat(lookup.find(setOf("bench")).keys).containsExactly("bench")
        Thread.sleep(100)
        assertThat(lateArrivals).isEqualTo(0)
    }

    @Test
    fun `exercises that couldn't load are read again later, without holding items up again`() {
        repository.allExercises = mutableListOf(stillOpen(Load.Unavailable(IOException("offline"))), MutableStateFlow(Load.Ready(listOf(bench))))
        var lateArrivals = 0
        lookup.onLateArrival { lateArrivals++ }
        assertThat(lookup.find(setOf("bench"))).isEmpty()

        awaitUntil { lookup.find(setOf("bench")).isNotEmpty() }
        assertThat(lateArrivals).isEqualTo(1)
    }

    // Followed, like DataSync's reads, which only end on sign-out
    private fun loaded(vararg exercises: LibraryExercise) {
        repository.allExercises = mutableListOf(MutableStateFlow(Load.Ready(exercises.toList())))
    }

    private fun <T> stillOpen(value: T): Flow<T> = flow {
        emit(value)
        awaitCancellation()
    }

    private fun <T> MutableStateFlow<T>.endWhen(end: (T) -> Boolean): Flow<T> = takeWhile { !end(it) }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertThat(condition()).isTrue()
    }
}
