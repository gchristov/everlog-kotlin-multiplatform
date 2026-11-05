package com.everlog.android.home.weektab

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
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
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import java.io.IOException

class WeekTabViewModelTest {

    private val bench = ELExercise(uuid = "bench")

    // Wednesday
    private var today = LocalDate.of(2026, 11, 4)

    private val workouts = FakeWorkoutRepository()
    private val settings = FakeUserSettings(workoutsGoal = 3)
    private val notifications = FakeHomeNotifications()
    private val config = FakeHomeConfig()
    private val analytics = FakeHomeAnalytics()
    private val planProgress = FakePlanProgress()

    private fun viewModel() = WeekTabViewModel(FakeCoroutineDispatcher, workouts, settings, planProgress, notifications, config, analytics) { today }

    private fun finishedOn(day: LocalDate): ELWorkout {
        val completed = day.atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return workout(completed, bench to listOf(loggedSet(5, 100f))).apply { createdDate = completed - 3_600_000 }
    }

    private val banner = HomeNotification(id = "survey", title = "Tell us", description = "Two minutes", actionUrl = "https://example.com")

    // Week

    @Test
    fun `shows the week's days while its workouts load`() {
        val state = viewModel().state.value

        assertThat(state.loading).isTrue()
        assertThat(state.empty).isFalse()
        assertThat(state.stats.days.first()).isEqualTo(LocalDate.of(2026, 11, 2))
    }

    @Test
    fun `counts this week's workouts once they load`() {
        val viewModel = viewModel()

        workouts.recent.value = Load.Ready(listOf(finishedOn(today), finishedOn(LocalDate.of(2026, 11, 2)), finishedOn(LocalDate.of(2026, 10, 30))))

        val state = viewModel.state.value
        assertThat(state.loading).isFalse()
        assertThat(state.stats.workouts).isEqualTo(2)
        assertThat(state.stats.workoutDays).containsExactly(today, LocalDate.of(2026, 11, 2))
        assertThat(state.workoutsLeft).isEqualTo(1)
        assertThat(state.goalProgress).isWithin(0.001f).of(2f / 3)
    }

    @Test
    fun `a week without workouts is empty`() {
        val viewModel = viewModel()

        workouts.recent.value = Load.Ready(emptyList())

        assertThat(viewModel.state.value.empty).isTrue()
    }

    @Test
    fun `offline before the workouts ever synced shows an empty week`() {
        val viewModel = viewModel()

        workouts.recent.value = Load.Unavailable(IOException("offline"))

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.empty).isTrue()
    }

    @Test
    fun `a workout saved while the tab is open counts straight away`() {
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(emptyList())

        workouts.recent.value = Load.Ready(listOf(finishedOn(today)))

        assertThat(viewModel.state.value.stats.workouts).isEqualTo(1)
        assertThat(viewModel.state.value.empty).isFalse()
    }

    @Test
    fun `the goal is met once the week's workouts reach it`() {
        val viewModel = viewModel()

        workouts.recent.value = Load.Ready(List(4) { finishedOn(today) })

        assertThat(viewModel.state.value.workoutsLeft).isEqualTo(0)
        assertThat(viewModel.state.value.goalProgress).isEqualTo(1f)
    }

    @Test
    fun `a refresh picks up changed settings and a new day`() {
        val viewModel = viewModel()
        workouts.recent.value = Load.Ready(listOf(finishedOn(LocalDate.of(2026, 11, 8))))
        assertThat(viewModel.state.value.stats.workouts).isEqualTo(1)

        settings.setWeeklyWorkoutsGoal(5)
        settings.setFirstDayOfWeek(DayOfWeek.SUNDAY)
        today = LocalDate.of(2026, 11, 9)
        viewModel.refresh()

        val state = viewModel.state.value
        assertThat(state.weeklyGoal).isEqualTo(5)
        assertThat(state.today).isEqualTo(today)
        assertThat(state.stats.days.first()).isEqualTo(LocalDate.of(2026, 11, 8))
        assertThat(state.stats.workouts).isEqualTo(1)
    }

    // Banner

    @Test
    fun `shows the banner Remote Config sets, counting it shown once`() {
        config.banner = banner

        val viewModel = viewModel()
        viewModel.refresh()

        assertThat(viewModel.state.value.banner).isEqualTo(WeekTabBanner("Tell us", "Two minutes", imageUrl = null, updateRequired = false))
        assertThat(analytics.shown).containsExactly("Tell us")
    }

    @Test
    fun `doesn't show a banner the user dismissed`() {
        config.banner = banner
        notifications.dismissedIds += "survey"

        assertThat(viewModel().state.value.banner).isNull()
    }

    @Test
    fun `a new banner set while the app runs shows on the next refresh`() {
        val viewModel = viewModel()

        config.banner = banner
        viewModel.refresh()

        assertThat(viewModel.state.value.banner).isNotNull()
    }

    @Test
    fun `closing the banner dismisses it`() {
        config.banner = banner
        val viewModel = viewModel()

        viewModel.onBannerClosed()

        assertThat(viewModel.state.value.banner).isNull()
        assertThat(notifications.dismissedIds).containsExactly("survey")
        assertThat(analytics.dismissed).containsExactly("Tell us")
    }

    @Test
    fun `tapping the banner opens it and dismisses it`() {
        config.banner = banner
        val viewModel = viewModel()

        val tap = viewModel.onBannerTapped()

        assertThat(tap).isEqualTo(HomeNotificationTap.OpenUrl("https://example.com"))
        assertThat(viewModel.state.value.banner).isNull()
        assertThat(notifications.dismissedIds).containsExactly("survey")
        assertThat(analytics.tapped).containsExactly("Tell us")
    }

    @Test
    fun `a banner needing a newer version says so and stays after a tap`() {
        config.banner = banner.copy(minRequiredVersion = Int.MAX_VALUE)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.banner!!.updateRequired).isTrue()

        val tap = viewModel.onBannerTapped()

        assertThat(tap).isEqualTo(HomeNotificationTap.UpdateApp)
        assertThat(viewModel.state.value.banner).isNotNull()
        assertThat(notifications.dismissedIds).isEmpty()
    }

    @Test
    fun `tapping with no banner does nothing`() {
        assertThat(viewModel().onBannerTapped()).isEqualTo(HomeNotificationTap.Nothing)
        assertThat(analytics.tapped).isEmpty()
    }

    // Plan

    private val legs = ELRoutine(uuid = "legs", name = "Legs")

    // [weeks] weeks with legs on each week's first day and rest on the others, and [done] days done
    private fun plan(weeks: Int = 2, done: Int = 0): ELPlan {
        val plan = ELPlan.newPlan("user").apply { name = "Strength" }
        repeat(weeks - 1) { plan.addNewWeek() }
        plan.weeks.forEach { week ->
            week.getDays().forEachIndexed { index, day -> if (index == 0) day.setRoutine(legs) else day.setRest(true) }
        }
        plan.weeks.flatMap { it.getDays() }.take(done).forEach { it.complete = true }
        return plan
    }

    @Test
    fun `shows the plan the user is following instead of the week`() {
        planProgress.plan = plan(weeks = 2, done = 8)

        val shown = viewModel().state.value.plan!!

        assertThat(shown.name).isEqualTo("Strength")
        assertThat(shown.progress).isEqualTo(8 * 100 / 14)
        assertThat(shown.weekNumber).isEqualTo(2)
        assertThat(shown.weekCount).isEqualTo(2)
        assertThat(shown.days.map { it.complete }).containsExactly(true, false, false, false, false, false, false).inOrder()
        assertThat(shown.days.single { it.next }.number).isEqualTo(2)
        assertThat(shown.days[0].routine).isEqualTo(legs)
        assertThat(shown.done).isFalse()
    }

    @Test
    fun `without a plan there's no plan to show`() {
        assertThat(viewModel().state.value.plan).isNull()
    }

    @Test
    fun `a plan started or stopped elsewhere shows on the next refresh`() {
        val viewModel = viewModel()

        planProgress.plan = plan()
        viewModel.refresh()
        assertThat(viewModel.state.value.plan).isNotNull()

        planProgress.clearOngoingPlan()
        viewModel.refresh()
        assertThat(viewModel.state.value.plan).isNull()
    }

    @Test
    fun `skipping a day asks first, then marks it a done rest day`() {
        planProgress.plan = plan()
        val viewModel = viewModel()

        viewModel.onSkipDay()
        assertThat(viewModel.state.value.prompt).isEqualTo(WeekTabPrompt.SkipDay)
        assertThat(planProgress.plan!!.getNextDay()!!.complete).isFalse()

        viewModel.onSkipDayConfirmed()

        assertThat(viewModel.state.value.prompt).isNull()
        assertThat(planProgress.plan!!.weeks[0].getDays()[0].getRest()).isTrue()
        assertThat(viewModel.state.value.plan!!.days.single { it.next }.number).isEqualTo(2)
    }

    @Test
    fun `dismissing the skip prompt leaves the day as it was`() {
        planProgress.plan = plan()
        val viewModel = viewModel()
        viewModel.onSkipDay()

        viewModel.onPromptDismissed()

        assertThat(viewModel.state.value.prompt).isNull()
        assertThat(planProgress.plan!!.getNextDay()!!.complete).isFalse()
    }

    @Test
    fun `starting a workout day opens its template without changing the plan`() {
        planProgress.plan = plan()
        val viewModel = viewModel()

        assertThat(viewModel.onStartDay()).isEqualTo(WeekTabPlanStart.Workout(legs))
        assertThat(planProgress.plan!!.getNextDay()!!.complete).isFalse()
    }

    @Test
    fun `starting a rest day marks it done in place`() {
        planProgress.plan = plan(done = 1)
        val viewModel = viewModel()

        assertThat(viewModel.onStartDay()).isEqualTo(WeekTabPlanStart.Nothing)

        assertThat(planProgress.plan!!.weeks[0].getDays()[1].complete).isTrue()
        assertThat(viewModel.state.value.plan!!.days.single { it.next }.number).isEqualTo(3)
        assertThat(analytics.planDaysCompleted).isEqualTo(1)
    }

    @Test
    fun `finishing a done plan ends it and goes back to the week`() {
        planProgress.plan = plan(weeks = 1, done = 7)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.plan!!.done).isTrue()

        assertThat(viewModel.onStartDay()).isEqualTo(WeekTabPlanStart.Finished)

        assertThat(planProgress.plan).isNull()
        assertThat(viewModel.state.value.plan).isNull()
        assertThat(analytics.plansCompleted).isEqualTo(1)
    }

    @Test
    fun `tapping a workout day opens its template, a rest day explains itself`() {
        planProgress.plan = plan()
        val viewModel = viewModel()
        val days = viewModel.state.value.plan!!.days

        assertThat(viewModel.onDayTapped(days[0])).isEqualTo(legs)
        assertThat(viewModel.state.value.prompt).isNull()

        assertThat(viewModel.onDayTapped(days[1])).isNull()
        assertThat(viewModel.state.value.prompt).isEqualTo(WeekTabPrompt.RestDay)
    }

    private class FakeHomeNotifications : HomeNotifications {
        val dismissedIds = mutableListOf<String>()

        override fun shouldShow(notification: HomeNotification?) =
            notification != null && notification.canShow() && notification.dismissalId() !in dismissedIds

        override fun dismissed(notification: HomeNotification) {
            dismissedIds += notification.dismissalId()
        }

        override fun clear() = dismissedIds.clear()
    }

    private class FakeHomeConfig : HomeConfig {
        var banner: HomeNotification? = null

        override fun notification() = banner

        override val appVersionCode = 2_240_000

        override fun statisticsV2() = false
    }

    private class FakeHomeAnalytics : HomeAnalytics {
        val shown = mutableListOf<String?>()
        val dismissed = mutableListOf<String?>()
        val tapped = mutableListOf<String?>()
        var planDaysCompleted = 0
        var plansCompleted = 0

        override fun planDayCompleted() {
            planDaysCompleted++
        }

        override fun planCompleted() {
            plansCompleted++
        }

        override fun notificationShown(title: String?) {
            shown += title
        }

        override fun notificationDismissed(title: String?) {
            dismissed += title
        }

        override fun notificationTapped(title: String?) {
            tapped += title
        }
    }
}
