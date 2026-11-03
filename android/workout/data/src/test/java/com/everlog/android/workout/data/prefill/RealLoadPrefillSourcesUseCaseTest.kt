package com.everlog.android.workout.data.prefill

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.at
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.WorkoutRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test
import java.io.IOException

class RealLoadPrefillSourcesUseCaseTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val now = at(2026, 10, 1)
    private val repository = FakeWorkoutRepository()
    private val useCase = RealLoadPrefillSourcesUseCase(FakeCoroutineDispatcher, repository, FakeUserSettings(unit = WeightUnit.KILOGRAM)) { now }

    @Test
    fun `an exercise last done months ago still has its last session, from the rest of the year`() = runBlocking<Unit> {
        val longAgo = workout(at(2026, 3, 14), bench to listOf(loggedSet(8, 60f))).apply { createdDate = completedDate }
        repository.history += longAgo

        val sources = useCase(listOf(bench)).getOrNull()!!

        assertThat(sources.getValue("bench").lastSession?.workout).isSameInstanceAs(longAgo)
        assertThat(repository.reads).hasSize(2)
    }

    @Test
    fun `an exercise done lately reads only the last months`() = runBlocking<Unit> {
        repository.history += workout(at(2026, 9, 29), bench to listOf(loggedSet(8, 60f))).apply { createdDate = completedDate }

        useCase(listOf(bench))

        assertThat(repository.reads).hasSize(1)
    }

    @Test
    fun `waits for the history the first time`() = runBlocking<Unit> {
        val synced = CompletableDeferred<Unit>()
        repository.beforeRead = { synced.await() }
        repository.history += workout(at(2026, 9, 29), bench to listOf(loggedSet(8, 60f))).apply { createdDate = completedDate }
        val loading = async { useCase(listOf(bench)) }
        yield()
        assertThat(loading.isCompleted).isFalse()

        synced.complete(Unit)

        assertThat(loading.await().getOrNull()!!.getValue("bench").lastSession).isNotNull()
    }

    @Test
    fun `fails when the history can't be synced`() = runBlocking<Unit> {
        val error = IOException("offline")
        repository.failure = error

        assertThat(useCase(listOf(bench)).leftOrNull()).isSameInstanceAs(error)
    }

    private class FakeWorkoutRepository : WorkoutRepository {
        val history = mutableListOf<ELWorkout>()
        val reads = mutableListOf<Pair<Long, Long?>>()
        var failure: Throwable? = null
        var beforeRead: suspend () -> Unit = {}

        override suspend fun workoutsCreatedBetween(from: Long, until: Long?): Either<Throwable, List<ELWorkout>> {
            beforeRead()
            reads += from to until
            failure?.let { return Either.Left(it) }
            return Either.Right(history.filter { it.createdDate >= from && (until == null || it.createdDate < until) })
        }

        override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> = Either.Right(emptyMap())
    }
}
