package com.everlog.android.home.weektab

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.common.kotlin.Load
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.home.HomeConfig
import com.everlog.android.home.data.HomeNotificationTap
import com.everlog.android.home.data.HomeNotifications
import com.everlog.android.home.data.model.HomeNotification
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.testfixtures.FakePlanProgress
import com.everlog.android.settings.testfixtures.FakeUserSettings
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
 * Home's Week tab driven as a user would, with the real view-model over fakes: the week, an empty week, the banner and
 * a plan. A few journeys rather than a test per detail: the view-model's unit tests cover those.
 */
@RunWith(AndroidJUnit4::class)
class WeekTabScenarioTest {

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
    private val settings = FakeUserSettings(workoutsGoal = 3)
    private val planProgress = FakePlanProgress()
    private val notifications = DeviceNotifications()
    private val config = DeviceConfig()

    private val opened = mutableListOf<String>()
    private var tapped: HomeNotificationTap? = null
    private var started: WeekTabPlanStart? = null

    @Test
    fun seeingTheWeek() {
        show()

        // The days show while the week's workouts load
        compose.onNodeWithTag(WeekTabTags.Days).assertExists()
        compose.onNodeWithTag(WeekTabTags.Loading).assertExists()

        workouts.recent.value = Load.Ready(listOf(finishedToday(), finishedToday()))
        compose.waitUntil(WaitMillis) { exists("more to go!") }
        compose.onNodeWithTag(WeekTabTags.Empty).assertDoesNotExist()

        // The goal opens the settings, the days and the numbers the statistics
        compose.onNodeWithTag(WeekTabTags.Goal).performClick()
        compose.onNodeWithTag(WeekTabTags.Days).performClick()
        assertThat(opened).containsExactly("settings", "statistics").inOrder()
    }

    @Test
    fun anEmptyWeek() {
        show()

        workouts.recent.value = Load.Ready(emptyList())

        compose.waitUntil(WaitMillis) { exists("Ready to train?") }
        compose.onNodeWithTag(WeekTabTags.Empty).performScrollTo()
        compose.onNodeWithText("Start workout").performClick()
        assertThat(opened).containsExactly("start workout")
    }

    @Test
    fun closingTheBanner() {
        config.banner = Survey
        show()

        compose.onNodeWithText("Tell us").assertExists()
        compose.onNodeWithTag(WeekTabTags.BannerClose).performClick()

        compose.waitUntil(WaitMillis) { !exists("Tell us") }
        assertThat(notifications.dismissedIds).containsExactly("survey")
        assertThat(tapped).isNull()
    }

    @Test
    fun tappingTheBanner() {
        config.banner = Survey
        show()

        compose.onNodeWithTag(WeekTabTags.Banner).performClick()

        assertThat(tapped).isEqualTo(HomeNotificationTap.OpenUrl("https://example.com"))
        compose.waitUntil(WaitMillis) { !exists("Tell us") }
        assertThat(notifications.dismissedIds).containsExactly("survey")
    }

    @Test
    fun followingAPlan() {
        planProgress.plan = plan()
        show()

        compose.onNodeWithText("Plan").assertExists()
        compose.onNodeWithText("Week 1 of 1").assertExists()

        // Starting a workout day opens its template
        compose.onNodeWithTag(WeekTabTags.StartDay).performClick()
        assertThat(started).isEqualTo(WeekTabPlanStart.Workout(legs))

        // Skipping asks first, then moves on to the rest day
        compose.onNodeWithTag(WeekTabTags.SkipDay).performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitUntil(WaitMillis) { !exists("Cancel") }
        assertThat(planProgress.plan!!.getNextDay()!!.complete).isFalse()
        compose.onNodeWithTag(WeekTabTags.SkipDay).performClick()
        compose.onNodeWithTag(WeekTabTags.SkipConfirm).performClick()
        compose.waitUntil(WaitMillis) { exists("Complete") }

        // A rest day explains itself, and completing it marks it done in place
        compose.onNodeWithText("Day 3").performClick()
        compose.waitUntil(WaitMillis) { exists("Make sure", substring = true) }
        compose.onNodeWithText("OK").performClick()
        compose.waitUntil(WaitMillis) { !exists("Make sure", substring = true) }
        compose.onNodeWithTag(WeekTabTags.StartDay).performClick()
        assertThat(planProgress.plan!!.weeks[0].getDays()[1].complete).isTrue()
        assertThat(started).isEqualTo(WeekTabPlanStart.Nothing)
    }

    @Test
    fun finishingAPlan() {
        planProgress.plan = plan(done = 7)
        show()

        compose.onNodeWithTag(WeekTabTags.FinishPlan).performScrollTo().performClick()

        assertThat(started).isEqualTo(WeekTabPlanStart.Finished)
        assertThat(planProgress.plan).isNull()
        // Back to the week, as the plan is over
        compose.waitUntil(WaitMillis) { exists("Week") }
    }

    private val legs = ELRoutine(uuid = "legs", name = "Legs")

    // One week with legs on its first day and rest on the others, and [done] days done
    private fun plan(done: Int = 0): ELPlan {
        val plan = ELPlan.newPlan("user").apply { name = "Strength" }
        plan.weeks.single().getDays().forEachIndexed { index, day -> if (index == 0) day.setRoutine(legs) else day.setRest(true) }
        plan.weeks.single().getDays().take(done).forEach { it.complete = true }
        return plan
    }

    private fun finishedToday(): ELWorkout {
        val completed = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ELWorkout(completedDate = completed).apply { createdDate = completed - 3_600_000 }
    }

    private fun show() {
        val viewModel = WeekTabViewModel(Dispatchers.Main, workouts, settings, planProgress, notifications, config, Analytics) { today }
        compose.setContent {
            Theme {
                WeekTabScreen(
                    viewModel = viewModel,
                    actions = WeekTabActions(
                        showStatistics = { opened += "statistics" },
                        showSettings = { opened += "settings" },
                        showStartWorkout = { opened += "start workout" },
                        open = { tapped = it },
                        showPlan = { opened += "plan" },
                        showTemplate = { opened += "template ${it.name}" },
                        startPlanDay = { started = it },
                    ),
                )
            }
        }
    }

    private fun exists(text: String, substring: Boolean = false) =
        compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()

    private class DeviceNotifications : HomeNotifications {
        val dismissedIds = mutableListOf<String>()

        override fun shouldShow(notification: HomeNotification?) =
            notification != null && notification.canShow() && notification.dismissalId() !in dismissedIds

        override fun dismissed(notification: HomeNotification) {
            dismissedIds += notification.dismissalId()
        }

        override fun clear() = dismissedIds.clear()
    }

    private class DeviceConfig : HomeConfig {
        var banner: HomeNotification? = null

        override fun notification() = banner

        override val appVersionCode = Int.MAX_VALUE

        override fun statisticsV2() = false

        override fun historyV2() = false
    }

    private object Analytics : HomeAnalytics {
        override fun planDayCompleted() = Unit

        override fun planCompleted() = Unit

        override fun notificationShown(title: String?) = Unit

        override fun notificationDismissed(title: String?) = Unit

        override fun notificationTapped(title: String?) = Unit
    }

    private companion object {
        const val WaitMillis = 5_000L

        val Survey = HomeNotification(id = "survey", title = "Tell us", description = "Two minutes", actionUrl = "https://example.com")
    }
}
