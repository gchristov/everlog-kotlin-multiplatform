package com.everlog.android.statistics.ui.page

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.statistics.data.StatisticsPreferences
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.ui.StatisticsAnalytics
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class StatisticsPageViewModelTest {

    private val bench = ELExercise(uuid = "bench", category = "CHEST")

    // Wednesday
    private var today = LocalDate.of(2026, 11, 4)

    private val workouts = FakeWorkoutRepository()
    private val settings = FakeUserSettings()
    private val analytics = FakeAnalytics()
    private var feedback = false

    private val preferences = FakeStatisticsPreferences()

    private fun viewModel() = StatisticsPageViewModel(FakeCoroutineDispatcher, workouts, settings, analytics, preferences, { feedback }) { today }

    private fun finishedOn(day: LocalDate): ELWorkout {
        val completed = day.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return workout(completed, bench to listOf(loggedSet(5, 100f))).apply { createdDate = completed - 3_600_000 }
    }

    private val history = listOf(
        finishedOn(LocalDate.of(2026, 11, 3)),
        finishedOn(LocalDate.of(2026, 11, 1)),
        finishedOn(LocalDate.of(2026, 6, 10)),
        finishedOn(LocalDate.of(2025, 10, 10)),
    )

    @Test
    fun `shows this week while the workouts load`() {
        val state = viewModel().state.value

        assertThat(state.loading).isTrue()
        assertThat(state.period).isEqualTo(StatsPeriod.Week)
        assertThat(state.stats.workouts).isEqualTo(0)
    }

    private val year = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading)

    @Test
    fun `counts the week and month from the synced months`() {
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(1)

        viewModel.onPeriodPicked(StatsPeriod.Month)
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(2)
        assertThat(workouts.reads).isEmpty()
    }

    @Test
    fun `the last 12 months wait for a read of their own`() {
        workouts.byMonths[12] = year
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)

        viewModel.onPeriodPicked(StatsPeriod.Last12Months)
        assertThat(viewModel.state.value.loading).isTrue()

        year.value = Load.Ready(history)
        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(3)
        assertThat(analytics.picked).containsExactly(StatsPeriod.Last12Months)
    }

    @Test
    fun `when the last 12 months can't load, trying again reads them again`() {
        workouts.byMonths[12] = year
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)
        viewModel.onPeriodPicked(StatsPeriod.Last12Months)

        year.value = Load.Unavailable(IOException("timed out"))
        assertThat(viewModel.state.value.failed).isTrue()
        assertThat(viewModel.state.value.loading).isFalse()

        year.value = Load.Loading
        viewModel.onTryAgain()
        assertThat(viewModel.state.value.failed).isFalse()
        assertThat(viewModel.state.value.loading).isTrue()
        assertThat(workouts.reads[12]).isEqualTo(2)

        year.value = Load.Ready(history)
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(3)
    }

    @Test
    fun `leaving the last 12 months goes back to the synced months`() {
        workouts.byMonths[12] = year
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)
        viewModel.onPeriodPicked(StatsPeriod.Last12Months)
        year.value = Load.Unavailable(IOException("timed out"))

        viewModel.onPeriodPicked(StatsPeriod.Week)

        assertThat(viewModel.state.value.failed).isFalse()
        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(1)
    }

    @Test
    fun `picking the period shown changes nothing`() {
        val viewModel = viewModel()

        viewModel.onPeriodPicked(StatsPeriod.Week)

        assertThat(analytics.picked).isEmpty()
    }

    @Test
    fun `a workout saved while the page is open counts straight away`() {
        val viewModel = viewModel()
        viewModel.onPeriodPicked(StatsPeriod.Month)
        workouts.recent.value = Load.Ready(history)

        workouts.recent.value = Load.Ready(history + finishedOn(today))

        assertThat(viewModel.state.value.stats.workouts).isEqualTo(3)
        assertThat(viewModel.state.value.period).isEqualTo(StatsPeriod.Month)
    }

    @Test
    fun `offline before the workouts ever synced shows nothing to count`() {
        val viewModel = viewModel()

        workouts.recent.value = Load.Unavailable(IOException("offline"))

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(0)
    }

    @Test
    fun `a refresh picks up changed settings, a new day and the feedback form`() {
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)

        settings.setFirstDayOfWeek(DayOfWeek.SUNDAY)
        settings.setWeightUnit(WeightUnit.POUND)
        today = LocalDate.of(2026, 11, 7)
        feedback = true
        viewModel.refresh()

        val state = viewModel.state.value
        assertThat(state.stats.from).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(state.stats.workouts).isEqualTo(2)
        assertThat(state.weightUnit).isEqualTo(WeightUnit.POUND)
        assertThat(state.today).isEqualTo(today)
        assertThat(state.feedback).isTrue()
    }

    @Test
    fun `a period without workouts shows a full example of it, and a period with them doesn't`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.example).isNull()

        workouts.recent.value = Load.Ready(emptyList())

        val example = viewModel.state.value.example!!
        assertThat(example.period).isEqualTo(StatsPeriod.Week)
        assertThat(example.workouts).isEqualTo(3)
        assertThat(example.muscles).isNotEmpty()

        workouts.recent.value = Load.Ready(history)
        assertThat(viewModel.state.value.example).isNull()
    }

    @Test
    fun `the last 12 months that failed and then load, synced by another screen, show without the error`() {
        workouts.byMonths[12] = year
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)
        viewModel.onPeriodPicked(StatsPeriod.Last12Months)
        year.value = Load.Unavailable(IOException("timed out"))

        year.value = Load.Ready(history)

        assertThat(viewModel.state.value.failed).isFalse()
        assertThat(viewModel.state.value.example).isNull()
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(3)
    }

    @Test
    fun `when the last 12 months can't load, their example shows with the way to try again`() {
        workouts.byMonths[12] = year
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)
        viewModel.onPeriodPicked(StatsPeriod.Last12Months)
        assertThat(viewModel.state.value.example).isNull()

        year.value = Load.Unavailable(IOException("timed out"))

        assertThat(viewModel.state.value.failed).isTrue()
        assertThat(viewModel.state.value.example!!.period).isEqualTo(StatsPeriod.Last12Months)
        assertThat(viewModel.state.value.example!!.weightLifted).hasSize(12)
    }

    @Test
    fun `opens on the period last picked, reading the last 12 months straight away`() {
        workouts.byMonths[12] = year
        preferences.saved = StatsPeriod.Last12Months
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(history)

        assertThat(viewModel.state.value.period).isEqualTo(StatsPeriod.Last12Months)
        assertThat(workouts.reads[12]).isEqualTo(1)
        assertThat(viewModel.state.value.loading).isTrue()

        year.value = Load.Ready(history)
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(3)
    }

    @Test
    fun `picking a period keeps it for next time`() {
        val viewModel = viewModel()

        viewModel.onPeriodPicked(StatsPeriod.Month)

        assertThat(preferences.saved).isEqualTo(StatsPeriod.Month)
    }

    private class FakeStatisticsPreferences : StatisticsPreferences {
        var saved = StatsPeriod.Week

        override fun period() = saved

        override fun setPeriod(period: StatsPeriod) {
            saved = period
        }
    }

    private class FakeAnalytics : StatisticsAnalytics {
        val picked = mutableListOf<StatsPeriod>()


        override fun statisticsPeriodPicked(period: StatsPeriod) {
            picked += period
        }
    }
}
