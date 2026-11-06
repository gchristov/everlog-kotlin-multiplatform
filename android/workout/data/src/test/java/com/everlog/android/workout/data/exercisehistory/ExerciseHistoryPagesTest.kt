package com.everlog.android.workout.data.exercisehistory

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.plannedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExerciseHistoryPagesTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val year = TimeUnit.DAYS.toMillis(365)
    private val now = 1_790_000_000_000L
    private val recentFrom = now - year
    private val squat = ELExercise(uuid = "squat", name = "Squat")
    private val bench = ELExercise(uuid = "bench", name = "Bench press")

    private val recent = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading)
    private val history = mutableListOf<ELWorkout>()
    private var oldest: Either<Throwable, Long?> = Either.Right(null)
    private var failure: Throwable? = null
    private var oldestReads = 0
    private val reads = mutableListOf<Pair<Long, Long>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val pages = ExerciseHistoryPages(
        exerciseUuid = squat.uuid!!,
        recent = recent,
        recentFrom = recentFrom,
        oldest = { oldestReads++; oldest },
        readBetween = { from, until ->
            reads += from to until
            failure?.let { Either.Left(it) } ?: Either.Right(history.filter { it.createdDate in from until until })
        },
        dispatcher = FakeCoroutineDispatcher,
    )

    @After
    fun tearDown() {
        scope.cancel()
    }

    // Main journeys

    @Test
    fun `the last months show each time the exercise was logged, most recent first`() {
        val older = done("a", now - 40 * day, squat to listOf(loggedSet(5, 100f)))
        val newer = done("b", now - 2 * day, squat to listOf(loggedSet(5, 105f)), bench to listOf(loggedSet(8, 60f)))
        val benchOnly = done("c", now - 5 * day, bench to listOf(loggedSet(8, 60f)))
        val loads = collect()

        recent.value = Load.Ready(listOf(older, benchOnly, newer))

        assertThat(loads.last().history().sessions.map { it.workoutUuid }).containsExactly("b", "a").inOrder()
        assertThat(loads.last().history().sessions.first().exercise.getSetsWithData().single().weight).isEqualTo(105f)
    }

    @Test
    fun `a session only counts once the workout is finished and the exercise has a logged set`() {
        val unfinished = done("a", now - day, squat to listOf(loggedSet(5, 100f))).apply { completedDate = 0 }
        val skipped = done("b", now - 2 * day, squat to listOf(plannedSet()))
        val loads = collect()

        recent.value = Load.Ready(listOf(unfinished, skipped))

        assertThat(loads.last().history().sessions).isEmpty()
    }

    @Test
    fun `a workout saved while the screen is open shows in place`() {
        val loads = collect()
        recent.value = Load.Ready(listOf(done("a", now - 40 * day, squat to listOf(loggedSet(5, 100f)))))

        recent.value = Load.Ready(listOf(done("a", now - 40 * day, squat to listOf(loggedSet(5, 100f))), done("b", now, squat to listOf(loggedSet(5, 110f)))))

        assertThat(loads.last().history().sessions.map { it.workoutUuid }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun `scrolling to the end reads the year before, and the first workout's date once`() {
        oldest = Either.Right(now - 5 * year)
        history += done("old", recentFrom - 100 * day, squat to listOf(loggedSet(5, 90f)))
        val loads = collect()
        recent.value = Load.Ready(listOf(done("a", now - day, squat to listOf(loggedSet(5, 100f)))))

        pages.loadMore()

        assertThat(reads).containsExactly(recentFrom - year to recentFrom)
        assertThat(loads.last().history().sessions.map { it.workoutUuid }).containsExactly("a", "old").inOrder()
        assertThat(loads.last().history().complete).isFalse()

        pages.loadMore()

        assertThat(oldestReads).isEqualTo(1)
        assertThat(reads[1]).isEqualTo(recentFrom - 2 * year to recentFrom - year)
    }

    @Test
    fun `years without the exercise are skipped, until one has it`() {
        oldest = Either.Right(now - 6 * year)
        history += done("bench", recentFrom - 100 * day, bench to listOf(loggedSet(8, 60f)))
        history += done("old", recentFrom - 2 * year - 100 * day, squat to listOf(loggedSet(5, 90f)))
        val loads = collect()
        recent.value = Load.Ready(emptyList())

        pages.loadMore()

        assertThat(reads).hasSize(3)
        assertThat(loads.last().history().sessions.map { it.workoutUuid }).containsExactly("old")
        assertThat(loads.last().history().loadingMore).isFalse()
    }

    @Test
    fun `the history is complete at the first workout, and reads nothing more`() {
        oldest = Either.Right(recentFrom - year - 30 * day)
        val loads = collect()
        recent.value = Load.Ready(emptyList())

        pages.loadMore()

        assertThat(reads).containsExactly(
            recentFrom - year to recentFrom,
            recentFrom - year - 30 * day to recentFrom - year,
        ).inOrder()
        assertThat(loads.last().history().complete).isTrue()

        pages.loadMore()

        assertThat(reads).hasSize(2)
    }

    // Edge cases

    @Test
    fun `without workouts the history is complete without reading`() {
        val loads = collect()
        recent.value = Load.Ready(emptyList())

        pages.loadMore()

        assertThat(reads).isEmpty()
        assertThat(loads.last().history().complete).isTrue()
    }

    @Test
    fun `a first workout within the last months leaves nothing older to read`() {
        oldest = Either.Right(recentFrom + day)
        val loads = collect()
        recent.value = Load.Ready(emptyList())

        pages.loadMore()

        assertThat(reads).isEmpty()
        assertThat(loads.last().history().complete).isTrue()
    }

    @Test
    fun `a workout read both ways shows once`() {
        oldest = Either.Right(now - 5 * year)
        val edge = done("edge", recentFrom - day, squat to listOf(loggedSet(5, 100f)))
        history += edge
        val loads = collect()
        recent.value = Load.Ready(listOf(edge))

        pages.loadMore()

        assertThat(loads.last().history().sessions.map { it.workoutUuid }).containsExactly("edge")
    }

    @Test
    fun `a year that can't be read fails, and asking again reads it again`() {
        oldest = Either.Right(now - 5 * year)
        failure = IllegalStateException("offline")
        val loads = collect()
        recent.value = Load.Ready(emptyList())

        pages.loadMore()

        assertThat(loads.last().history().failed).isTrue()
        assertThat(loads.last().history().loadingMore).isFalse()

        failure = null
        pages.loadMore()

        assertThat(reads).contains(recentFrom - year to recentFrom)
        assertThat(reads.count { it == recentFrom - year to recentFrom }).isEqualTo(2)
        assertThat(loads.last().history().failed).isFalse()
    }

    @Test
    fun `the first workout's date that can't be read fails, and is read again next time`() {
        oldest = Either.Left(IllegalStateException("offline"))
        val loads = collect()
        recent.value = Load.Ready(emptyList())

        pages.loadMore()

        assertThat(loads.last().history().failed).isTrue()

        oldest = Either.Right(null)
        pages.loadMore()

        assertThat(oldestReads).isEqualTo(2)
        assertThat(loads.last().history().complete).isTrue()
    }

    @Test
    fun `the history loads with the last months, and is unavailable when they are`() {
        val loads = collect()

        assertThat(loads.last()).isEqualTo(Load.Loading)

        recent.value = Load.Unavailable(IllegalStateException("offline"))

        assertThat(loads.last()).isInstanceOf(Load.Unavailable::class.java)
    }

    private fun collect(): List<Load<ExerciseHistory>> {
        val loads = mutableListOf<Load<ExerciseHistory>>()
        scope.launch { pages.loads.collect { loads += it } }
        return loads
    }

    private fun Load<ExerciseHistory>.history(): ExerciseHistory = (this as Load.Ready).value

    private fun done(uuid: String, date: Long, vararg exercises: Pair<ELExercise, List<ELSet>>): ELWorkout =
        workout(date, *exercises).apply {
            this.uuid = uuid
            createdDate = date
        }
}
