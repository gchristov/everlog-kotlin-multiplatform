package com.everlog.android.exercise.page

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.exercise.data.DeleteExerciseUseCase
import com.everlog.android.exercise.data.LoadExerciseGuideUseCase
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.exercisehistory.ExerciseHistory
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class ExercisePageViewModelTest {

    // A Thursday
    private var today = LocalDate.of(2026, 11, 5)
    private val bench = ELExercise(uuid = "bench", name = "Bench press", category = "CHEST")
    private val own = ELExercise(uuid = "own", name = "Landmine press", category = "SHOULDERS", createdByUserId = "user")
    private val workouts = FakeWorkoutRepository()
    private val history = workouts.exerciseHistory
    private val settings = FakeUserSettings()
    private val analytics = FakeAnalytics()
    private val guides = FakeGuides()
    private val ownExercises = MutableSharedFlow<List<LibraryExercise>>(replay = 1)
    private val deletes = FakeDelete()
    private var feedback = true

    // Main journeys

    @Test
    fun `the page loads, then shows this month's statistics and the history`() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.loading).isTrue()

        history.loads.value = ready(session("a", today, loggedSet(5, 100f)), session("b", today.minusMonths(2), loggedSet(5, 90f)))

        val state = viewModel.state.value
        assertThat(state.loading).isFalse()
        assertThat(state.range).isEqualTo(ExerciseStatsRange.Month)
        assertThat(state.stats.timesPerformed).isEqualTo(1)
        assertThat(state.sessions.map { it.workoutUuid }).containsExactly("a", "b").inOrder()
        assertThat(state.statsExample).isNull()
    }

    @Test
    fun `picking a range shows its statistics and logs it`() {
        val viewModel = viewModel()
        history.loads.value = ready(session("a", today, loggedSet(5, 100f)), session("b", today.minusMonths(2), loggedSet(5, 90f)))

        viewModel.onRangePicked(ExerciseStatsRange.Last12Months)

        assertThat(viewModel.state.value.stats.timesPerformed).isEqualTo(2)
        assertThat(analytics.ranges).containsExactly(ExerciseStatsRange.Last12Months)

        viewModel.onRangePicked(ExerciseStatsRange.Last12Months)

        assertThat(analytics.ranges).hasSize(1)
    }

    @Test
    fun `a workout saved while the page is open counts straight away`() {
        val viewModel = viewModel()
        history.loads.value = ready(session("a", today.minusDays(1), loggedSet(5, 100f)))

        history.loads.value = ready(session("b", today, loggedSet(3, 110f)), session("a", today.minusDays(1), loggedSet(5, 100f)))

        assertThat(viewModel.state.value.stats.timesPerformed).isEqualTo(2)
        assertThat(viewModel.state.value.stats.heaviestSet?.weight).isEqualTo(110f)
        assertThat(viewModel.state.value.sessions.first().workoutUuid).isEqualTo("b")
    }

    @Test
    fun `scrolling to the history's oldest session asks for the year before`() {
        val viewModel = viewModel()
        history.loads.value = ready(session("a", today, loggedSet(5, 100f)))

        viewModel.onHistoryEnd()

        assertThat(history.loadMoreCalls).isEqualTo(1)
    }

    @Test
    fun `the page and each tab shown are logged`() {
        val viewModel = viewModel()

        viewModel.onResume()
        viewModel.onTabShown(ExercisePageTab.Statistics)

        assertThat(analytics.events).containsExactly("screen", "tab:Statistics").inOrder()
    }

    // Medium importance

    @Test
    fun `nothing older is asked for while loading, while a year is on its way, or at the first workout`() {
        val viewModel = viewModel()

        viewModel.onHistoryEnd()
        history.loads.value = Load.Ready(ExerciseHistory(emptyList(), loadingMore = true, complete = false, failed = false))
        viewModel.onHistoryEnd()
        history.loads.value = Load.Ready(ExerciseHistory(emptyList(), loadingMore = false, complete = true, failed = false))
        viewModel.onHistoryEnd()

        assertThat(history.loadMoreCalls).isEqualTo(0)
    }

    @Test
    fun `a library exercise shows its guide, and can't be edited or deleted`() {
        guides.guide.value = Load.Ready(ExerciseGuide(instructions = listOf("Lower the bar"), tips = emptyList()))

        val viewModel = viewModel()

        assertThat(viewModel.state.value.guide.instructions).containsExactly("Lower the bar")
        assertThat(viewModel.state.value.own).isFalse()
    }

    @Test
    fun `an own exercise edited while the page is open shows the edit, without reading a guide`() {
        val viewModel = viewModel(own)

        ownExercises.tryEmit(listOf(LibraryExercise(uuid = "own", name = "Landmine row", imageUrl = "image", category = "BACK", own = true)))

        assertThat(viewModel.state.value.own).isTrue()
        assertThat(viewModel.state.value.exercise.name).isEqualTo("Landmine row")
        assertThat(viewModel.state.value.exercise.imageUrl).isEqualTo("image")
        assertThat(viewModel.state.value.exercise.category).isEqualTo("BACK")
        assertThat(viewModel.state.value.exercise.createdByUserId).isEqualTo("user")
        assertThat(guides.reads).isEmpty()
    }

    @Test
    fun `deleting asks first, then deletes the exercise as it is now and closes`() {
        val viewModel = viewModel(own)
        ownExercises.tryEmit(listOf(LibraryExercise(uuid = "own", name = "Landmine row", own = true)))

        viewModel.onDeleteClicked()

        assertThat(viewModel.state.value.deletePrompt).isTrue()

        viewModel.onDeleteConfirmed()

        assertThat(deletes.deleted.map { it.name }).containsExactly("Landmine row")
        assertThat(analytics.events).contains("deleted")
        assertThat(viewModel.state.value.closed).isTrue()
    }

    @Test
    fun `deleting cancelled leaves the exercise`() {
        val viewModel = viewModel(own)

        viewModel.onDeleteClicked()
        viewModel.onDeleteCancelled()

        assertThat(viewModel.state.value.deletePrompt).isFalse()
        assertThat(deletes.deleted).isEmpty()
    }

    // Edge cases

    @Test
    fun `a delete that fails says so and keeps the page open`() {
        deletes.result = Either.Left(IllegalStateException("rejected"))
        val viewModel = viewModel(own)

        viewModel.onDeleteClicked()
        viewModel.onDeleteConfirmed()

        assertThat(viewModel.state.value.deleteFailed).isTrue()
        assertThat(viewModel.state.value.closed).isFalse()
        assertThat(analytics.events).doesNotContain("deleted")

        viewModel.onDeleteFailedShown()

        assertThat(viewModel.state.value.deleteFailed).isFalse()
    }

    @Test
    fun `the last 12 months that can't load fail, and trying again reads them again`() {
        val viewModel = viewModel()

        history.loads.value = Load.Unavailable(IllegalStateException("offline"))

        assertThat(viewModel.state.value.failed).isTrue()
        assertThat(viewModel.state.value.loading).isFalse()

        // A new read starts over
        history.loads.value = Load.Loading
        viewModel.onTryAgain()

        assertThat(workouts.exerciseHistoryReads).containsExactly("bench", "bench")
        assertThat(viewModel.state.value.failed).isFalse()
        assertThat(viewModel.state.value.loading).isTrue()

        history.loads.value = ready(session("a", today, loggedSet(5, 100f)))

        assertThat(viewModel.state.value.sessions).hasSize(1)
    }

    @Test
    fun `a range without sessions shows an example, and the history one once it has none at all`() {
        val viewModel = viewModel()

        history.loads.value = Load.Ready(ExerciseHistory(emptyList(), loadingMore = false, complete = false, failed = false))

        assertThat(viewModel.state.value.statsExample).isNotNull()
        assertThat(viewModel.state.value.historyExample).isNull()

        history.loads.value = Load.Ready(ExerciseHistory(emptyList(), loadingMore = false, complete = true, failed = false))

        assertThat(viewModel.state.value.historyExample).isNotEmpty()
    }

    @Test
    fun `no example while loading`() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.statsExample).isNull()
        assertThat(viewModel.state.value.historyExample).isNull()
    }

    @Test
    fun `coming back reads the weight unit, the feedback form and the date again`() {
        val viewModel = viewModel()
        history.loads.value = ready(session("a", today, loggedSet(5, 100f)))

        settings.setWeightUnit(WeightUnit.POUND)
        feedback = false
        today = today.plusMonths(1)
        viewModel.onResume()

        assertThat(viewModel.state.value.weightUnit).isEqualTo(WeightUnit.POUND)
        assertThat(viewModel.state.value.feedback).isFalse()
        assertThat(viewModel.state.value.stats.timesPerformed).isEqualTo(0)
    }

    @Test
    fun `an older year that couldn't be read is shown, for scrolling to the end to try again`() {
        val viewModel = viewModel()

        history.loads.value = Load.Ready(ExerciseHistory(emptyList(), loadingMore = false, complete = false, failed = true))
        viewModel.onHistoryEnd()

        assertThat(viewModel.state.value.olderFailed).isTrue()
        assertThat(history.loadMoreCalls).isEqualTo(1)
    }

    private fun viewModel(exercise: ELExercise = bench) = ExercisePageViewModel(
        dispatcher = FakeCoroutineDispatcher,
        exercise = exercise,
        userId = "user",
        readHistory = workouts::exerciseHistory,
        loadGuide = guides,
        ownExerciseUpdates = { ownExercises },
        deleteExercise = deletes,
        settings = settings,
        analytics = analytics,
        feedback = { feedback },
        exampleName = "Chest",
        today = { today },
    )

    private fun ready(vararg sessions: ExerciseSession): Load<ExerciseHistory> =
        Load.Ready(ExerciseHistory(sessions.toList(), loadingMore = false, complete = false, failed = false))

    private fun session(workoutUuid: String, day: LocalDate, vararg sets: ELSet) = ExerciseSession(
        workoutUuid = workoutUuid,
        workoutName = "Push",
        completedDate = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        exercise = ELRoutineExercise(bench.uuid, bench, sets.toMutableList()),
    )

    private class FakeAnalytics : ExercisePageAnalytics {
        val events = mutableListOf<String>()
        val ranges = mutableListOf<ExerciseStatsRange>()

        override fun screenViewed() {
            events += "screen"
        }

        override fun tabViewed(tab: ExercisePageTab) {
            events += "tab:${tab.name}"
        }

        override fun rangePicked(range: ExerciseStatsRange) {
            ranges += range
        }

        override fun exerciseDeleted() {
            events += "deleted"
        }
    }

    private class FakeGuides : LoadExerciseGuideUseCase {
        val guide = MutableStateFlow<Load<ExerciseGuide>>(Load.Loading)
        val reads = mutableListOf<String>()

        override fun invoke(uuid: String): Flow<Load<ExerciseGuide>> {
            reads += uuid
            return guide
        }
    }

    private class FakeDelete : DeleteExerciseUseCase {
        var result: Either<Throwable, Unit> = Either.Right(Unit)
        val deleted = mutableListOf<ELExercise>()

        override suspend fun invoke(exercise: ELExercise): Either<Throwable, Unit> {
            deleted += exercise
            return result
        }
    }
}
