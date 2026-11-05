package com.everlog.android.statistics.ui.page

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.kotlin.Load
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.statistics.data.StatisticsPreferences
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.ui.StatisticsAnalytics
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import com.jakewharton.threetenabp.AndroidThreeTen
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

/**
 * The Activity tab's statistics page driven as a user would, with the real view-model over fakes: the synced week and
 * month, and the last 12 months failing and tried again. A few journeys rather than a test per detail: the
 * view-model's unit tests cover those.
 */
@RunWith(AndroidJUnit4::class)
class StatisticsPageScenarioTest {

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
    private val year = MutableStateFlow<Load<List<ELWorkout>>>(Load.Loading).also { workouts.byMonths[12] = it }
    private val picked = mutableListOf<StatsPeriod>()
    private var feedbackOpened = false

    @Test
    fun seeingTheWeekAndMonth() {
        show()

        compose.onNodeWithTag(StatisticsPageTags.Loading).assertExists()

        workouts.recent.value = Load.Ready(listOf(finishedToday(), finishedToday()))
        compose.waitUntil(WaitMillis) { workoutsShown("2") }
        compose.onNodeWithTag(StatisticsPageTags.Loading).assertDoesNotExist()

        // The month counts from the same synced workouts, with no read of its own
        pick("This month")
        compose.waitUntil(WaitMillis) { exists("This month") && workoutsShown("2") }
        assertThat(workouts.reads[12]).isNull()

        compose.onNodeWithText("Let us know").performScrollTo().performClick()
        assertThat(feedbackOpened).isTrue()
        assertThat(picked).containsExactly(StatsPeriod.Month)
    }

    @Test
    fun tryingTheLast12MonthsAgain() {
        workouts.recent.value = Load.Ready(listOf(finishedToday()))
        show()
        compose.waitUntil(WaitMillis) { workoutsShown("1") }

        pick("Last 12 months")
        compose.onNodeWithTag(StatisticsPageTags.Loading).assertExists()

        year.value = Load.Unavailable(IOException("timed out"))
        compose.waitUntil(WaitMillis) { exists("Couldn't load the last 12 months.") }

        year.value = Load.Loading
        compose.onNodeWithText("Try again").performClick()
        compose.waitUntil(WaitMillis) { workouts.reads[12] == 2 }
        compose.waitUntil(WaitMillis) { !exists("Couldn't load the last 12 months.") }

        year.value = Load.Ready(List(3) { finishedToday() })
        compose.waitUntil(WaitMillis) { workoutsShown("3") }

        // Back to the week, which never stopped following the synced workouts
        pick("This week")
        compose.waitUntil(WaitMillis) { workoutsShown("1") }
    }

    @Test
    fun anEmptyWeekShowsAnExample() {
        workouts.recent.value = Load.Ready(emptyList())
        show()

        // Under its label, faded and out of the semantics tree, so the real numbers aren't there
        compose.waitUntil(WaitMillis) { exists(hasTestTag(StatisticsPageTags.Example)) }
        compose.onNodeWithTag(StatisticsPageTags.Workouts).assertDoesNotExist()

        workouts.recent.value = Load.Ready(listOf(finishedToday()))
        compose.waitUntil(WaitMillis) { workoutsShown("1") }
        compose.onNodeWithTag(StatisticsPageTags.Example).assertDoesNotExist()
    }

    private fun pick(period: String) {
        compose.onNodeWithTag(StatisticsPageTags.Period).performClick()
        compose.onNode(hasText(period) and !hasAnyAncestor(hasTestTag(StatisticsPageTags.Period)) and !hasTestTag(StatisticsPageTags.Period))
            .performClick()
    }

    private fun finishedToday(): ELWorkout {
        val completed = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ELWorkout(completedDate = completed).apply { createdDate = completed - 3_600_000 }
    }

    private fun show() {
        val viewModel = StatisticsPageViewModel(Dispatchers.Main, workouts, FakeUserSettings(), Analytics(picked), Preferences(), { true }) { today }
        compose.setContent {
            Theme {
                StatisticsPageScreen(viewModel = viewModel, onFeedback = { feedbackOpened = true })
            }
        }
    }

    private fun workoutsShown(count: String) =
        compose.onAllNodes(hasText(count) and hasAnyAncestor(hasTestTag(StatisticsPageTags.Workouts))).fetchSemanticsNodes().isNotEmpty()

    private fun exists(text: String) = exists(hasText(text))

    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private class Preferences : StatisticsPreferences {
        private var period = StatsPeriod.Week

        override fun period() = period

        override fun setPeriod(period: StatsPeriod) {
            this.period = period
        }
    }

    private class Analytics(private val picked: MutableList<StatsPeriod>) : StatisticsAnalytics {
        override fun statisticsRangeModified(range: RangeType) = Unit

        override fun statisticsPeriodPicked(period: StatsPeriod) {
            picked += period
        }
    }

    private companion object {
        const val WaitMillis = 5_000L
    }
}
