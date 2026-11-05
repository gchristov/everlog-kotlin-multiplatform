package com.everlog.android.history.ui.page

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.kotlin.Load
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.WorkoutHistory
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import com.jakewharton.threetenabp.AndroidThreeTen
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

/**
 * The Activity tab's history page driven as a user would, with the real view-model over a fake history: picking days
 * on the calendar, scrolling back through the list, and no workouts yet. A few journeys rather than a test per detail:
 * the view-model's unit tests cover those.
 */
@RunWith(AndroidJUnit4::class)
class HistoryPageScenarioTest {

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
    private val workouts = FakeWorkoutRepository()
    private val opened = mutableListOf<String>()

    @Test
    fun pickingDaysOnTheCalendar() {
        val yesterday = today.minusDays(1)
        show()
        compose.onNodeWithTag(HistoryPageTags.Loading).assertExists()

        shown(listOf(finished(yesterday, "Legs")))
        compose.waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Calendar)) }
        // On the 1st, yesterday is in the month above
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(yesterday)))
        compose.waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.WorkoutDot)) }

        // Today has no workouts: it says so and stays on the calendar
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(today)))
        compose.onNodeWithTag(HistoryPageTags.day(today)).performClick()
        compose.waitUntil(WaitMillis) { exists(hasText("No events found for this day")) }
        compose.onNodeWithTag(HistoryPageTags.Calendar).assertExists()

        // A day with workouts opens the list on it, and its workout opens
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(yesterday)))
        compose.onNodeWithTag(HistoryPageTags.day(yesterday)).performClick()
        compose.waitUntil(WaitMillis) { exists(hasText("Legs") and hasAnyAncestor(hasTestTag(HistoryPageTags.List))) }
        compose.onNodeWithText("Legs").performClick()
        assertThat(opened).containsExactly("Legs")
    }

    @Test
    fun scrollingBackThroughTheList() {
        val days = (1..30).map { today.minusDays(it.toLong()) }
        show()
        pickView("List")

        // Newest first from the read; the list opens on the newest, at the bottom
        shown(days.map { finished(it, "Workout $it") })
        compose.waitUntil(WaitMillis) { exists(hasText("Workout ${days.first()}")) }
        assertThat(workouts.history.loadMoreCalls).isEqualTo(0)

        // Reaching the oldest asks for the next page, which goes in above
        compose.onNodeWithTag(HistoryPageTags.List).performScrollToIndex(0)
        compose.waitUntil(WaitMillis) { workouts.history.loadMoreCalls == 1 }
        val older = today.minusDays(60)
        shown(days.map { finished(it, "Workout $it") } + finished(older, "The first"), complete = true)
        compose.onNodeWithTag(HistoryPageTags.List).performScrollToIndex(0)
        compose.waitUntil(WaitMillis) { exists(hasText("The first")) }
    }

    @Test
    fun scrollingBackThroughTheCalendar() {
        val earlier = today.minusMonths(2).withDayOfMonth(10)
        show()
        shown(listOf(finished(today, "Today"), finished(earlier, "Earlier")))
        compose.waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Calendar)) }

        // The oldest month shown asks for the next page, which adds months above it without moving it
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(earlier)))
        compose.waitUntil(WaitMillis) { workouts.history.loadMoreCalls >= 1 }
        val oldest = today.minusMonths(5).withDayOfMonth(10)
        shown(listOf(finished(today, "Today"), finished(earlier, "Earlier"), finished(oldest, "Oldest")), complete = true)
        compose.waitForIdle()
        compose.onNodeWithTag(HistoryPageTags.day(earlier), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(oldest)))
    }

    @Test
    fun noWorkoutsYet() {
        show()
        pickView("List")

        shown(emptyList(), complete = true)

        compose.waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Empty)) }
        pickView("Calendar")
        compose.onNodeWithTag(HistoryPageTags.Calendar).assertExists()
    }

    private fun shown(history: List<ELWorkout>, complete: Boolean = false) {
        workouts.history.loads.value = Load.Ready(WorkoutHistory(history, loadingMore = false, complete = complete))
    }

    private fun pickView(name: String) {
        compose.onNodeWithTag(HistoryPageTags.View).performClick()
        compose.onNode(hasText(name) and !hasAnyAncestor(hasTestTag(HistoryPageTags.View)) and !hasTestTag(HistoryPageTags.View)).performClick()
    }

    private fun finished(day: LocalDate, name: String): ELWorkout {
        val completed = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ELWorkout(completedDate = completed, routine = ELRoutine(name = name)).apply { createdDate = completed - 3_600_000 }
    }

    private fun show() {
        val viewModel = HistoryPageViewModel(Dispatchers.Main, workouts, FakeUserSettings()) { today }
        compose.setContent {
            Theme {
                HistoryPageScreen(viewModel = viewModel, onWorkout = { opened += it.getName().orEmpty() })
            }
        }
    }

    // Unmerged, as a day's dot is inside its clickable cell
    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L
    }
}
