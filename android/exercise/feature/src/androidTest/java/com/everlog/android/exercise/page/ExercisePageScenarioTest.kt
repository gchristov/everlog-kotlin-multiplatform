package com.everlog.android.exercise.page

import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.kotlin.AppContext
import com.jakewharton.threetenabp.AndroidThreeTen
import arrow.core.Either
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.kotlin.Load
import com.everlog.android.exercise.data.DeleteExerciseUseCase
import com.everlog.android.exercise.data.LoadExerciseGuideUseCase
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.exercisehistory.ExerciseHistory
import com.everlog.android.workout.data.exercisehistory.ExerciseHistoryRead
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsPreferences
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

/**
 * An exercise's page driven as a user would, with the real view-model over a fake history. A few journeys rather than
 * a test per detail: the view-model's and the rules' unit tests cover those.
 */
@RunWith(AndroidJUnit4::class)
class ExercisePageScenarioTest {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    init {
        // Set by the app at startup, which a library's tests don't run, and needed before the fields below
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        AppContext = context
        AndroidThreeTen.init(context)
    }

    private val today = LocalDate.now()
    private val bench = ELExercise(uuid = "bench", name = "Bench press", category = "CHEST")
    private val own = ELExercise(uuid = "own", name = "Landmine press", category = "SHOULDERS", createdByUserId = "user")
    private val reads = mutableListOf<YearsBack>()
    private val ownExercises = MutableSharedFlow<List<LibraryExercise>>(replay = 1)
    private val deleted = mutableListOf<ELExercise>()
    private val ranges = mutableListOf<ExerciseStatsRange>()
    private var edited: ELExercise? = null
    private var feedbackSent = 0
    private var closed = false

    @Test
    fun browsingAnExercise() {
        show(bench, ExercisePageTab.History, recent = listOf(session("this-week", today, 5, 100f), session("last-month", today.minusMonths(1), 5, 95f)))

        // History opens on the newest session, at the bottom
        compose.onNodeWithText("This week workout").assertExists()

        // Statistics from its chip: this month by default, then the last 12 months from the range menu
        compose.onNodeWithTag(ExercisePageTags.tab(ExercisePageTab.Statistics)).performClick()
        compose.waitUntil(WaitMillis) { exists("times performed") }
        compose.onNodeWithTag(ExercisePageTags.Range).performClick()
        compose.onNodeWithText("Last 12 months").performClick()
        compose.waitUntil(WaitMillis) { ranges.isNotEmpty() }
        assertThat(ranges).containsExactly(ExerciseStatsRange.Last12Months)
        compose.onNodeWithTag(ExercisePageTags.Tiles).assertExists()
        compose.onNodeWithText("2").assertExists()

        // The feedback row asks for more statistics
        scrollStatisticsTo(ExercisePageTags.Feedback)
        compose.onNodeWithTag(ExercisePageTags.Feedback).performClick()
        assertThat(feedbackSent).isEqualTo(1)

        // Info, with the library's steps
        compose.onNodeWithTag(ExercisePageTags.tab(ExercisePageTab.Info)).performClick()
        compose.waitUntil(WaitMillis) { exists("Lower the bar to your chest.") }
    }

    @Test
    fun scrollingBackThroughTheHistory() {
        show(
            bench,
            ExercisePageTab.History,
            recent = listOf(session("recent", today, 5, 100f)),
            older = listOf(
                emptyList(),
                listOf(session("two-years-ago", today.minusYears(2), 5, 80f)),
            ),
        )

        // The oldest in view asks for the year before, and a year without the exercise for the one before that
        compose.onNodeWithTag(ExercisePageTags.History).performScrollToIndex(0)
        compose.waitUntil(WaitMillis) { exists("Two years ago workout") }
        compose.waitUntil(WaitMillis) { reads.single().complete }
        compose.onNodeWithText("Recent workout").assertExists()
    }

    @Test
    fun editingAndDeletingAnOwnExercise() {
        show(own, ExercisePageTab.Info, recent = emptyList())

        // Edit hands the exercise to the create screen, and the edit shows when it's saved
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Edit").performClick()
        assertThat(edited?.uuid).isEqualTo("own")
        ownExercises.tryEmit(listOf(LibraryExercise(uuid = "own", name = "Landmine row", category = "BACK", own = true)))
        compose.waitUntil(WaitMillis) { exists("Landmine row") }

        // Delete asks first
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertThat(deleted).isEmpty()

        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Delete").performClick()
        // The dialog's own Delete, next to Cancel
        compose.onAllNodes(hasText("Delete") and hasClickAction()).onLast().performClick()
        compose.waitUntil(WaitMillis) { closed }
        assertThat(deleted.single().name).isEqualTo("Landmine row")
    }

    @Test
    fun aLibraryExerciseHasNoMenu() {
        show(bench, ExercisePageTab.Info, recent = emptyList())

        compose.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun theLast12MonthsThatCantLoadCanBeTriedAgain() {
        show(bench, ExercisePageTab.Statistics, recent = null)

        compose.waitUntil(WaitMillis) { exists("Couldn't load the last 12 months.") }
        compose.onNodeWithText("Try again").performClick()
        compose.waitUntil(WaitMillis) { reads.size == 2 }
    }

    private fun show(
        exercise: ELExercise,
        tab: ExercisePageTab,
        recent: List<ExerciseSession>?,
        older: List<List<ExerciseSession>> = emptyList(),
    ) {
        var attempts = 0
        val viewModel = ExercisePageViewModel(
            dispatcher = Dispatchers.Main,
            exercise = exercise,
            userId = "user",
            readHistory = {
                // The first read of one that can't load fails, the next one loads
                val sessions = recent ?: if (attempts++ == 0) null else emptyList()
                YearsBack(sessions, older).also { reads += it }
            },
            loadGuide = object : LoadExerciseGuideUseCase {
                override fun invoke(uuid: String): Flow<Load<ExerciseGuide>> =
                    flowOf(Load.Ready(ExerciseGuide(instructions = listOf("Lower the bar to your chest."), tips = emptyList())))
            },
            ownExerciseUpdates = { ownExercises },
            deleteExercise = object : DeleteExerciseUseCase {
                override suspend fun invoke(exercise: ELExercise): Either<Throwable, Unit> {
                    deleted += exercise
                    return Either.Right(Unit)
                }
            },
            settings = FakeUserSettings(),
            preferences = object : ExerciseStatsPreferences {
                override fun range() = ExerciseStatsRange.Month
                override fun setRange(range: ExerciseStatsRange) = Unit
            },
            analytics = object : ExercisePageAnalytics {
                override fun screenViewed() = Unit
                override fun tabViewed(tab: ExercisePageTab) = Unit
                override fun rangePicked(range: ExerciseStatsRange) {
                    ranges += range
                }
                override fun exerciseDeleted() = Unit
            },
            feedback = { true },
            exampleNames = listOf("Chest", "Upper body", "Full body"),
            today = { today },
        )
        compose.setContent {
            Theme {
                ExercisePageScreen(
                    viewModel = viewModel,
                    tab = tab,
                    onEdit = { edited = it },
                    onFeedback = { feedbackSent++ },
                    onClose = { closed = true },
                )
            }
        }
    }

    private fun scrollStatisticsTo(tag: String) {
        compose.onNode(hasScrollToNodeAction() and hasAnyDescendant(hasTestTag(ExercisePageTags.Tiles)))
            .performScrollToNode(hasTestTag(tag))
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    // The last 12 months as [recent] says (null: unavailable), then [older] a year at a time
    private class YearsBack(recent: List<ExerciseSession>?, private val older: List<List<ExerciseSession>>) : ExerciseHistoryRead {
        private val recentSessions = recent.orEmpty()
        private var years = 0
        var complete = older.isEmpty()
            private set

        override val loads = MutableStateFlow<Load<ExerciseHistory>>(
            if (recent == null) Load.Unavailable(IllegalStateException("offline")) else Load.Ready(history()),
        )

        // As the real read does, a year without the exercise reads the one before straight away
        override fun loadMore() {
            if (complete) return
            do {
                years++
                complete = years >= older.size
            } while (!complete && older[years - 1].isEmpty())
            loads.value = Load.Ready(history())
        }

        private fun history() = ExerciseHistory(recentSessions + older.take(years).flatten(), loadingMore = false, complete = complete, failed = false)
    }

    private fun session(name: String, day: LocalDate, reps: Int, weight: Float) = ExerciseSession(
        workoutUuid = name,
        workoutName = name.replace('-', ' ').replaceFirstChar { it.uppercase() } + " workout",
        completedDate = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        exercise = ELRoutineExercise(bench.uuid, bench, mutableListOf(ELSet(reps = reps, weight = weight))),
    )

    private companion object {
        const val WaitMillis = 5_000L
    }
}
