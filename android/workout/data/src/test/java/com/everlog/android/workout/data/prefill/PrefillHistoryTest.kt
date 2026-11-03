package com.everlog.android.workout.data.prefill

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.at
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.plannedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class PrefillHistoryTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val now = at(2026, 10, 1)
    private val recentFrom = now - 97 * day
    private val yearFrom = now - 372 * day

    private val bench = ELExercise(uuid = "bench", name = "Bench press")
    private val squat = ELExercise(uuid = "squat", name = "Squat")
    private val row = ELExercise(uuid = "row", name = "Row")

    private val history = mutableListOf<ELWorkout>()
    private val reads = mutableListOf<Pair<Long, Long?>>()

    // Main journeys

    @Test
    fun `exercises done in the last months read only those months`() = runBlocking<Unit> {
        val lastWeek = done(now - 7 * day, bench, squat)
        done(now - 200 * day, bench)

        val kept = scan(bench, squat)

        assertThat(reads).containsExactly(recentFrom to null)
        assertThat(kept).containsExactly(lastWeek)
    }

    @Test
    fun `an exercise not done lately reads the rest of the year`() = runBlocking<Unit> {
        done(now - 7 * day, bench)
        val months = done(now - 200 * day, row)

        val kept = scan(bench, row)

        assertThat(reads).containsExactly(recentFrom to null, yearFrom to recentFrom).inOrder()
        assertThat(kept).contains(months)
    }

    @Test
    fun `an exercise never done reads no further than a year`() = runBlocking<Unit> {
        done(now - 3 * 365 * day, squat)

        val kept = scan(squat)

        assertThat(reads).containsExactly(recentFrom to null, yearFrom to recentFrom).inOrder()
        assertThat(kept).isEmpty()
    }

    // Options and less common paths

    @Test
    fun `workouts without the exercises it's looking for aren't kept`() = runBlocking<Unit> {
        val withBench = done(now - 7 * day, bench)
        done(now - 8 * day, row)
        done(now - 200 * day, row)

        assertThat(scan(bench)).containsExactly(withBench)
    }

    @Test
    fun `an exercise in the workout but never done doesn't count as a session`() = runBlocking<Unit> {
        history += workout(now - 3 * day, squat to listOf(plannedSet())).apply { createdDate = completedDate - hour }
        val done = done(now - 200 * day, squat)

        assertThat(scan(squat)).containsExactly(done)
    }

    @Test
    fun `fails when the rest of the year can't be read`() = runBlocking<Unit> {
        val error = IOException("offline")
        done(now - 7 * day, bench)

        val result = prefillHistory(listOf(squat), now) { _, until ->
            if (until == null) Either.Right(history.toList()) else Either.Left(error)
        }

        assertThat(result.leftOrNull()).isSameInstanceAs(error)
    }

    // Edge cases

    @Test
    fun `a session found close to the last months' start reads the rest of the year, for a longer workout created before it`() = runBlocking<Unit> {
        done(recentFrom + 2 * day, squat)
        // Started before the last months, but finished after the session above
        val longer = workout(recentFrom + 3 * day, squat to listOf(loggedSet(5, 140f))).apply { createdDate = recentFrom - day }
        history += longer

        val kept = scan(squat)

        assertThat(reads).hasSize(2)
        assertThat(kept).contains(longer)
    }

    @Test
    fun `fills in the same as from the whole history, for any history`() = runBlocking<Unit> {
        val exercises = listOf(bench, squat, row, ELExercise(uuid = "press"))
        repeat(200) { seed ->
            val random = Random(seed)
            history.clear()
            repeat(random.nextInt(0, 60)) {
                val completed = now - random.nextLong(0, 3 * 365 * day)
                val logged = exercises.shuffled(random).take(random.nextInt(1, 3))
                history += workout(completed, *logged.map { it to listOf(loggedSet(random.nextInt(1, 12), random.nextInt(20, 200).toFloat())) }.toTypedArray())
                    .apply { createdDate = completed - random.nextLong(0, 5 * day) }
            }
            val asked = exercises.shuffled(random).take(random.nextInt(1, 4))

            val fromScan = WorkoutPrefillController.buildPrefillSources(asked, scan(*asked.toTypedArray()), now, WeightUnit.KILOGRAM)
            val fromAll = WorkoutPrefillController.buildPrefillSources(asked, history, now, WeightUnit.KILOGRAM)

            asked.forEach { exercise ->
                val scanned = fromScan.getValue(exercise.uuid!!)
                val all = fromAll.getValue(exercise.uuid!!)
                assertThat(scanned.lastSession?.workout).isSameInstanceAs(all.lastSession?.workout)
                assertThat(scanned.orm).isEqualTo(all.orm)
            }
        }
    }

    private suspend fun scan(vararg exercises: ELExercise): List<ELWorkout> {
        reads.clear()
        return prefillHistory(exercises.toList(), now) { from, until ->
            reads += from to until
            Either.Right(history.filter { it.createdDate >= from && (until == null || it.createdDate < until) })
        }.getOrNull()!!
    }

    private fun done(completed: Long, vararg exercises: ELExercise): ELWorkout =
        workout(completed, *exercises.map { it to listOf(loggedSet(8, 60f)) }.toTypedArray())
            .apply { createdDate = completed - hour }
            .also { history += it }
}
