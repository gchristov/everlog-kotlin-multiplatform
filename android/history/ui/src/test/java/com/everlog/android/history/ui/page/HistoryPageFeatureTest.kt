package com.everlog.android.history.ui.page

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.composetest.FeatureTest
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.history.data.HistoryPreferences
import com.everlog.android.history.data.HistoryView
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.WorkoutHistory
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

/**
 * The Activity tab's history page driven as a user would, with the real view-model over a fake history: picking days
 * on the calendar, scrolling back through the list, and no workouts yet. A few journeys rather than a test per detail:
 * the view-model's unit tests cover those.
 */
class HistoryPageFeatureTest : FeatureTest() {

    private val today = LocalDate.now()
    private val workouts = FakeWorkoutRepository()
    private val opened = mutableListOf<String>()

    // The journeys start on the calendar, as picked last time; the view-model's tests cover opening on the list
    private val preferences = object : HistoryPreferences {
        var saved = HistoryView.Calendar

        override fun view() = saved

        override fun setView(view: HistoryView) {
            saved = view
        }
    }

    @Test
    fun pickingDaysOnTheCalendar() {
        val yesterday = today.minusDays(1)
        show()
        compose.onNodeWithTag(HistoryPageTags.Loading).assertExists()

        shown(listOf(finished(yesterday, "Legs")))
        waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Calendar)) }
        // On the 1st, yesterday is in the month above
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(yesterday)))
        waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.WorkoutDot)) }

        // Today has no workouts: it says so and stays on the calendar
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(today)))
        compose.onNodeWithTag(HistoryPageTags.day(today)).performClick()
        waitUntil(WaitMillis) { exists(hasText("No workouts on this day.")) }
        compose.onNodeWithTag(HistoryPageTags.Calendar).assertExists()

        // A day with workouts opens the list on it, and its workout opens
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(yesterday)))
        compose.onNodeWithTag(HistoryPageTags.day(yesterday)).performClick()
        waitUntil(WaitMillis) { exists(hasText("Legs") and hasAnyAncestor(hasTestTag(HistoryPageTags.List))) }
        compose.onNodeWithText("Legs").performClick()
        assertThat(opened).containsExactly("Legs")
    }

    @Test
    fun scrollingBackThroughTheList() {
        val days = (1..30).map { today.minusDays(it.toLong()) }
        val viewModel = show()
        pickView("List")

        // Newest first from the read; the list opens on the newest, at the bottom
        shown(days.map { finished(it, "Workout $it") })
        waitUntil(WaitMillis) { exists(hasText("Workout ${days.first()}")) }
        assertThat(workouts.history.loadMoreCalls).isEqualTo(0)

        // Reaching the oldest asks for the next page, which goes in above
        compose.onNodeWithTag(HistoryPageTags.List).performScrollToIndex(0)
        waitUntil(WaitMillis) { workouts.history.loadMoreCalls == 1 }
        val older = today.minusDays(60)
        shown(days.map { finished(it, "Workout $it") } + finished(older, "The first"), complete = true)
        compose.onNodeWithTag(HistoryPageTags.List).performScrollToIndex(0)
        waitUntil(WaitMillis) { exists(hasText("The first")) }

        // Another Home tab and back: the newest page shows straight away, on the newest, while it's read again
        compose.runOnIdle {
            viewModel.onLeft()
            viewModel.refresh()
        }
        compose.onNodeWithTag(HistoryPageTags.Loading).assertDoesNotExist()
        waitUntil(WaitMillis) { displayed("Workout ${days.first()}") }
        assertThat(workouts.historyReads).isEqualTo(2)
    }

    @Test
    fun scrollingBackThroughTheCalendar() {
        val earlier = today.minusMonths(2).withDayOfMonth(10)
        show()
        shown(listOf(finished(today, "Today"), finished(earlier, "Earlier")))
        waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Calendar)) }

        // The oldest month shown asks for the next page, which adds months above it without moving it
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(earlier)))
        waitUntil(WaitMillis) { workouts.history.loadMoreCalls >= 1 }
        val oldest = today.minusMonths(5).withDayOfMonth(10)
        shown(listOf(finished(today, "Today"), finished(earlier, "Earlier"), finished(oldest, "Oldest")), complete = true)
        compose.waitForIdle()
        compose.onNodeWithTag(HistoryPageTags.day(earlier), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(HistoryPageTags.Calendar).performScrollToNode(hasTestTag(HistoryPageTags.day(oldest)))
    }

    @Test
    fun tryingAgainWhenTheHistoryCantLoad() {
        show()
        workouts.history.loads.value = Load.Unavailable(IOException("timed out"))
        waitUntil(WaitMillis) { exists(hasText("Couldn't load your history.")) }

        workouts.history.loads.value = Load.Loading
        compose.onNodeWithText("Try again").performClick()
        waitUntil(WaitMillis) { workouts.historyReads == 2 && !exists(hasText("Couldn't load your history.")) }

        shown(listOf(finished(today, "Legs")))
        waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.WorkoutDot)) }
    }

    @Test
    fun aWorkoutSavedAtTheNewestIsFollowed() {
        val days = (1..20).map { today.minusDays(it.toLong()) }
        show()
        pickView("List")
        shown(days.map { finished(it, "Workout $it") })
        waitUntil(WaitMillis) { exists(hasText("Workout ${days.first()}")) }

        // At the newest, workouts saved today come into view, down to the last of them, though the day is taller than
        // the room under the list
        val todays = (1..4).map { finished(today, "Today's $it") }
        shown(days.map { finished(it, "Workout $it") } + todays)
        waitUntil(WaitMillis) { displayed("Today's 4") }

        // Further back, another stays out of view
        compose.onNodeWithTag(HistoryPageTags.List).performScrollToIndex(0)
        shown(days.map { finished(it, "Workout $it") } + todays + finished(today, "Later"))
        compose.waitForIdle()
        assertThat(displayed("Later")).isFalse()
    }

    @Test
    fun noWorkoutsYet() {
        show()
        pickView("List")

        // Checking the server for older workouts, which the user didn't scroll back for
        shown(emptyList(), loadingMore = true)

        // The example shows in either view, under its label, faded and out of the semantics tree
        waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Example)) }
        compose.onNodeWithTag(HistoryPageTags.List).assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag(HistoryPageTags.LoadingMore).assertDoesNotExist()
        shown(emptyList(), complete = true)
        pickView("Calendar")
        compose.onNodeWithTag(HistoryPageTags.Example).assertExists()
        compose.onNodeWithTag(HistoryPageTags.Calendar).assertDoesNotExist()

        // The first workout replaces it
        shown(listOf(finished(today, "Legs")), complete = true)
        waitUntil(WaitMillis) { exists(hasTestTag(HistoryPageTags.Calendar)) }
        compose.onNodeWithTag(HistoryPageTags.Example).assertDoesNotExist()
    }

    private fun shown(history: List<ELWorkout>, complete: Boolean = false, loadingMore: Boolean = false) {
        workouts.history.loads.value = Load.Ready(WorkoutHistory(history, loadingMore = loadingMore, complete = complete))
    }

    private fun pickView(name: String) {
        compose.onNodeWithTag(HistoryPageTags.View).performClick()
        compose.onNode(hasText(name) and !hasAnyAncestor(hasTestTag(HistoryPageTags.View)) and !hasTestTag(HistoryPageTags.View)).performClick()
    }

    private fun finished(day: LocalDate, name: String): ELWorkout {
        val completed = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ELWorkout(completedDate = completed, routine = ELRoutine(name = name)).apply { createdDate = completed - 3_600_000 }
    }

    private fun show(): HistoryPageViewModel {
        val viewModel = HistoryPageViewModel(Dispatchers.Main, workouts, FakeUserSettings(), preferences, listOf("Legs", "Push", "Pull")) { today }
        compose.setContent {
            Theme {
                HistoryPageScreen(viewModel = viewModel, onWorkout = { opened += it.getName().orEmpty() })
            }
        }
        return viewModel
    }

    private fun displayed(text: String) = runCatching { compose.onNodeWithText(text).assertIsDisplayed() }.isSuccess

    // Unmerged, as a day's dot is inside its clickable cell
    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L
    }
}
