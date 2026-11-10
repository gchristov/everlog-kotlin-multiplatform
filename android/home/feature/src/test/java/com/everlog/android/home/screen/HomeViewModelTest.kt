package com.everlog.android.home.screen

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.SavedStateHandle
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.testfixtures.FakePlanProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeViewModelTest {

    private val services = FakeHomeServices()
    private val planProgress = FakePlanProgress()
    private val analytics = FakeAnalytics()
    private var savedState = SavedStateHandle()

    private fun viewModel() = HomeViewModel(FakeCoroutineDispatcher, savedState, services, planProgress, analytics)

    private val bench = ELExercise(uuid = "bench")
    private fun doneSet() = ELSet(reps = 8, weight = 60f, completedDate = 1_000L)
    private val twoSetsDone = workout(1_000L, bench to listOf(doneSet(), doneSet(), ELSet()))

    // Tabs

    @Test
    fun `opens on the Week tab, and starts the app's jobs once`() {
        val viewModel = viewModel()

        assertThat(viewModel.state.value.tab).isEqualTo(HomeTab.Week)
        assertThat(services.started).isEqualTo(1)
    }

    @Test
    fun `the tab survives the process being killed`() {
        viewModel().onTabSelected(HomeTab.Activity)

        assertThat(viewModel().state.value.tab).isEqualTo(HomeTab.Activity)
    }

    @Test
    fun `back goes to the Week tab first, then lets Home close`() {
        val viewModel = viewModel()
        viewModel.onTabSelected(HomeTab.Settings)

        assertThat(viewModel.onBack()).isTrue()
        assertThat(viewModel.state.value.tab).isEqualTo(HomeTab.Week)
        assertThat(viewModel.onBack()).isFalse()
    }

    @Test
    fun `a plan started elsewhere shows on the Week tab`() {
        val viewModel = viewModel()
        viewModel.onTabSelected(HomeTab.Workouts)

        viewModel.onPlanStarted()

        assertThat(viewModel.state.value.tab).isEqualTo(HomeTab.Week)
        assertThat(viewModel.state.value.planOngoing).isTrue()
    }

    // The floating button

    @Test
    fun `each tab's button`() {
        val viewModel = viewModel()
        viewModel.onWeekEmpty(false)

        val buttons = HomeTab.entries.associateWith { tab -> viewModel.onTabSelected(tab).let { viewModel.state.value.button } }

        assertThat(buttons).containsExactly(
            HomeTab.Week, HomeButton.StartWorkout,
            HomeTab.Workouts, HomeButton.New,
            HomeTab.Activity, HomeButton.StartWorkout,
            HomeTab.Settings, HomeButton.StartWorkout,
        )
    }

    @Test
    fun `the Week tab has no button while the week is empty or a plan is going, which have their own`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.button).isNull()

        viewModel.onWeekEmpty(false)
        assertThat(viewModel.state.value.button).isEqualTo(HomeButton.StartWorkout)

        planProgress.plan = ELPlan.newPlan("user")
        viewModel.onResumed()
        assertThat(viewModel.state.value.button).isNull()
    }

    @Test
    fun `Start workout opens the start menu, New the new menu`() {
        val viewModel = viewModel()
        viewModel.onTabSelected(HomeTab.Activity)

        viewModel.onButtonTapped()
        assertThat(viewModel.state.value.sheet).isEqualTo(HomeSheet.Start)
        assertThat(analytics.startTaps).containsExactly(false)

        viewModel.onSheetClosed()
        viewModel.onTabSelected(HomeTab.Workouts)
        viewModel.onButtonTapped()
        assertThat(viewModel.state.value.sheet).isEqualTo(HomeSheet.New)
    }

    @Test
    fun `the empty week's own button opens the start menu too`() {
        val viewModel = viewModel()

        viewModel.onStartFromEmptyWeek()

        assertThat(viewModel.state.value.sheet).isEqualTo(HomeSheet.Start)
        assertThat(analytics.startTaps).containsExactly(true)
    }

    // A workout left going

    @Test
    fun `a workout left going opens again the first time Home comes back`() {
        services.ongoing = twoSetsDone
        val viewModel = viewModel()

        assertThat(viewModel.onResumed()).isEqualTo(HomeResume.Reopen(twoSetsDone))
        assertThat(analytics.resumedOnLaunch).containsExactly(2)
    }

    @Test
    fun `after that, Home asks to resume or discard it, once`() {
        services.ongoing = twoSetsDone
        val viewModel = viewModel()
        viewModel.onResumed()

        assertThat(viewModel.onResumed()).isEqualTo(HomeResume.Nothing)
        viewModel.onResumed()

        assertThat(viewModel.state.value.prompt).isEqualTo(HomePrompt.OngoingWorkout(twoSetsDone))
        assertThat(analytics.promptsShown).containsExactly(2)
    }

    @Test
    fun `resuming opens the workout again`() {
        services.ongoing = twoSetsDone
        val viewModel = viewModel()
        viewModel.onResumed()
        viewModel.onResumed()

        assertThat(viewModel.onResumeWorkout()).isEqualTo(twoSetsDone)
        assertThat(viewModel.state.value.prompt).isNull()
        assertThat(analytics.promptsCancelled).isEqualTo(1)
    }

    @Test
    fun `discarding ends the workout`() {
        services.ongoing = twoSetsDone
        val viewModel = viewModel()
        viewModel.onResumed()
        viewModel.onResumed()

        viewModel.onDiscardWorkout()

        assertThat(services.discarded).isEqualTo(1)
        assertThat(analytics.discarded).isEqualTo(1)
        assertThat(viewModel.state.value.prompt).isNull()
    }

    @Test
    fun `closing the prompt keeps the workout for next time`() {
        services.ongoing = twoSetsDone
        val viewModel = viewModel()
        viewModel.onResumed()
        viewModel.onResumed()

        viewModel.onPromptDismissed()

        assertThat(services.discarded).isEqualTo(0)
        assertThat(analytics.promptsCancelled).isEqualTo(1)
    }

    // App updates and the app's jobs

    @Test
    fun `with no workout going, an update can be offered`() {
        val viewModel = viewModel()

        assertThat(viewModel.onResumed()).isEqualTo(HomeResume.CheckForUpdate)

        viewModel.onUpdateReady()
        assertThat(viewModel.state.value.updateReady).isTrue()
        viewModel.onUpdateShown()
        assertThat(viewModel.state.value.updateReady).isFalse()
    }

    @Test
    fun `no restart is offered with a workout going, as it would end it`() {
        val viewModel = viewModel()
        services.ongoing = twoSetsDone

        viewModel.onUpdateReady()

        assertThat(viewModel.state.value.updateReady).isFalse()
    }

    @Test
    fun `resuming and pausing pass on to the app`() {
        val viewModel = viewModel()

        viewModel.onResumed()
        viewModel.onPaused()

        assertThat(services.resumed).isEqualTo(1)
        assertThat(services.paused).isEqualTo(1)
    }

    private class FakeHomeServices : HomeServices {
        var ongoing: ELWorkout? = null
        var started = 0
        var resumed = 0
        var paused = 0
        var discarded = 0

        override fun ongoingWorkout() = ongoing

        override fun discardOngoingWorkout() {
            discarded++
            ongoing = null
        }

        override fun onStarted() {
            started++
        }

        override fun onResumed() {
            resumed++
        }

        override fun onPaused() {
            paused++
        }

        override fun onDestroyed() = Unit

        override fun appUpdates(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>, onReady: () -> Unit) =
            error("Not used by the view-model")
    }

    private class FakeAnalytics : HomeAnalytics {
        val startTaps = mutableListOf<Boolean>()
        val resumedOnLaunch = mutableListOf<Int>()
        val promptsShown = mutableListOf<Int>()
        var promptsCancelled = 0
        var discarded = 0

        override fun planDayCompleted() = Unit

        override fun planCompleted() = Unit

        override fun notificationShown(title: String?) = Unit

        override fun notificationDismissed(title: String?) = Unit

        override fun notificationTapped(title: String?) = Unit

        override fun startWorkoutTapped(fromEmptyWeek: Boolean) {
            startTaps += fromEmptyWeek
        }

        override fun workoutResumedOnLaunch(setsCompleted: Int) {
            resumedOnLaunch += setsCompleted
        }

        override fun ongoingWorkoutPromptShown(setsCompleted: Int) {
            promptsShown += setsCompleted
        }

        override fun ongoingWorkoutPromptCancelled() {
            promptsCancelled++
        }

        override fun ongoingWorkoutDiscarded() {
            discarded++
        }

        override fun workoutsCardShown(kind: String) = Unit

        override fun workoutsTabStarted(from: String, card: String?, suggested: Boolean) = Unit
    }
}
