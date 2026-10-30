package com.everlog.data.controllers.workoutprefill

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datasync.Range
import com.everlog.android.common.datatest.at
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.settings.testfixtures.install
import com.everlog.android.settings.testfixtures.uninstall
import com.everlog.android.workout.data.WorkoutRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RealLoadPrefillSourcesUseCaseTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val now = at(2026, 10, 1)
    private val repository = FakeWorkoutRepository()
    private val useCase = RealLoadPrefillSourcesUseCase(FakeCoroutineDispatcher, repository) { now }

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
        SettingsManager.manager.setWeightUnit(WeightUnit.KILOGRAM)
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `an exercise last done months ago still has its last session, from the whole history`() = runBlocking<Unit> {
        val longAgo = workout(at(2026, 3, 14), bench to listOf(loggedSet(8, 60f)))
        repository.history.value = Load.Ready(listOf(longAgo))

        val sources = useCase(listOf(bench)).getOrNull()!!

        assertThat(repository.ranges).containsExactly(Range.All)
        assertThat(sources.getValue("bench").lastSession?.workout).isSameInstanceAs(longAgo)
    }

    @Test
    fun `waits for the full history the first time`() = runBlocking<Unit> {
        val loading = async { useCase(listOf(bench)) }
        yield()
        assertThat(loading.isCompleted).isFalse()

        repository.history.value = Load.Ready(listOf(workout(at(2026, 9, 29), bench to listOf(loggedSet(8, 60f)))))

        assertThat(loading.await().getOrNull()!!.getValue("bench").lastSession).isNotNull()
    }

    @Test
    fun `fails when the history can't be synced`() = runBlocking<Unit> {
        val error = IOException("offline")
        repository.history.value = Load.Unavailable(error)

        assertThat(useCase(listOf(bench)).leftOrNull()).isSameInstanceAs(error)
    }

    private class FakeWorkoutRepository : WorkoutRepository {
        val history = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading)
        val ranges = mutableListOf<Range>()

        override fun workouts(range: Range): Flow<Load<List<ELWorkout>>> {
            ranges += range
            return history
        }

        override suspend fun exerciseLastDone(workouts: Int): Either<Throwable, Map<String, Long>> = Either.Right(emptyMap())
    }
}
