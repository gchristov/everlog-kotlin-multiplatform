package com.everlog.android.home.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.SavedStateHandle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.espresso.Espresso
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.composetest.FeatureTest
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.workout
import com.everlog.android.plan.data.PlanBroadcasts
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.testfixtures.FakePlanProgress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Test

/**
 * The Compose Home driven as a user would, with its real view-model over fakes and stand-ins for the tabs (each has
 * its own feature test): moving between tabs, each tab's button and its menu, back, a workout left going, an app
 * update, and a plan started elsewhere.
 */
class HomeFeatureTest : FeatureTest() {

    private val services = FakeHomeServices()
    private val planProgress = FakePlanProgress()
    private val analytics = FakeHomeAnalytics()
    private val opened = mutableListOf<String>()
    private var restarts = 0
    private lateinit var viewModel: HomeViewModel

    private val leftGoing: ELWorkout = workout(1_000L, ELExercise(uuid = "bench", name = "Bench press") to listOf(ELSet(reps = 8, completedDate = 1_000L)))

    // Main journeys

    @Test
    fun movingBetweenTabsFromTheBarAndBySwiping() {
        show()
        compose.onNodeWithText("Week tab").assertExists()

        compose.onNodeWithTag(HomeTags.tab(HomeTab.Workouts)).performClick()
        waitUntil(WaitMillis) { shows("Workouts tab") }
        compose.onNodeWithText("New").assertExists()

        compose.onNodeWithTag(HomeTags.Pager).performTouchInput { swipeLeft() }
        waitUntil(WaitMillis) { viewModel.state.value.tab == HomeTab.Activity }
        compose.onNodeWithText("Start workout").assertExists()

        // Back goes to Week first, then leaves Home to close
        Espresso.pressBack()
        waitUntil(WaitMillis) { viewModel.state.value.tab == HomeTab.Week }
        assertThat(viewModel.onBack()).isFalse()
    }

    @Test
    fun startingAWorkoutFromTheStartMenu() {
        show(startOn = HomeTab.Activity)

        compose.onNodeWithTag(HomeTags.Button).performClick()
        compose.onNode(hasText("From your templates") and hasAnyAncestor(isDialog())).performClick()
        waitUntil(WaitMillis) { opened == listOf("template picker") }
        waitUntil(WaitMillis) { viewModel.state.value.sheet == null }

        compose.onNodeWithTag(HomeTags.Button).performClick()
        compose.onNodeWithTag(HomeTags.StartEmpty).performClick()
        waitUntil(WaitMillis) { opened.last() == "empty workout" }
        assertThat(analytics.startTaps).containsExactly(false, false)
    }

    @Test
    fun creatingATemplateOrAPlanFromTheWorkoutsTab() {
        show(startOn = HomeTab.Workouts)

        compose.onNodeWithTag(HomeTags.Button).performClick()
        compose.onNodeWithTag(HomeTags.NewTemplate).performClick()
        waitUntil(WaitMillis) { opened == listOf("new template") }

        compose.onNodeWithTag(HomeTags.Button).performClick()
        compose.onNodeWithTag(HomeTags.NewPlan).performClick()
        waitUntil(WaitMillis) { opened.last() == "new plan" }
    }

    // Options

    @Test
    fun theWeekTabHasNoButtonWhileItsEmptyAndItsOwnButtonOpensTheStartMenu() {
        show()
        compose.onNodeWithText("Start workout").assertDoesNotExist()

        compose.onNodeWithText("Week: start a workout").performClick()
        waitUntil(WaitMillis) { shows("From your templates") }
        assertThat(analytics.startTaps).containsExactly(true)
    }

    @Test
    fun theWeekTabsLinksOpenOtherTabs() {
        show()

        compose.onNodeWithText("Week: see statistics").performClick()
        waitUntil(WaitMillis) { viewModel.state.value.tab == HomeTab.Activity }

        compose.onNodeWithTag(HomeTags.tab(HomeTab.Week)).performClick()
        waitUntil(WaitMillis) { shows("Week tab") }
        compose.onNodeWithText("Week: see plans").performClick()
        waitUntil(WaitMillis) { viewModel.state.value.tab == HomeTab.Workouts }
    }

    @Test
    fun aPlanStartedElsewhereShowsTheWeekTab() {
        show(startOn = HomeTab.Settings)
        planProgress.plan = ELPlan.newPlan("user")

        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(PlanBroadcasts.CURRENT_PLAN_STARTED))
        waitUntil(WaitMillis) { viewModel.state.value.tab == HomeTab.Week }
        assertThat(viewModel.state.value.button).isNull()
    }

    @Test
    fun anUpdateReadyOffersARestart() {
        show()

        compose.runOnIdle { viewModel.onUpdateReady() }

        waitUntil(WaitMillis) { shows("An update is ready to install") }
        compose.onNodeWithText("Restart").performClick()
        waitUntil(WaitMillis) { restarts == 1 }
    }

    // A workout left going

    @Test
    fun aWorkoutLeftGoingIsResumedFromThePrompt() {
        services.ongoing = leftGoing
        show()
        compose.runOnIdle {
            viewModel.onResumed()
            viewModel.onResumed()
        }

        waitUntil(WaitMillis) { shows("You have an ongoing workout with 1 completed set. Discarding deletes it.") }
        compose.onNodeWithTag(HomeTags.Resume).performClick()

        waitUntil(WaitMillis) { opened == listOf("resume ${leftGoing.uuid}") }
        assertThat(services.ongoing).isNotNull()
    }

    @Test
    fun aWorkoutLeftGoingIsDiscardedFromThePrompt() {
        services.ongoing = leftGoing
        show()
        compose.runOnIdle {
            viewModel.onResumed()
            viewModel.onResumed()
        }

        waitUntil(WaitMillis) { shows("Resume Workout") }
        compose.onNodeWithTag(HomeTags.Discard).performClick()

        waitUntil(WaitMillis) { services.ongoing == null }
        waitUntil(WaitMillis) { !shows("Resume Workout") }
        assertThat(opened).isEmpty()
    }

    // Edge cases

    @Test
    fun opensOnTheTabItWasOnBeforeTheAppWasKilled() {
        show(startOn = HomeTab.Settings)

        compose.onNodeWithText("Settings tab").assertExists()
        compose.onNodeWithText("Start workout").assertExists()
    }

    private fun show(startOn: HomeTab = HomeTab.Week) {
        viewModel = HomeViewModel(
            dispatcher = Dispatchers.Main,
            savedState = SavedStateHandle(mapOf("tab" to startOn.name)),
            services = services,
            planProgress = planProgress,
            analytics = analytics,
        )
        setContent {
            HomeScreen(
                viewModel = viewModel,
                sheetActions = HomeSheetActions(
                    startFromTemplate = { opened += "template picker" },
                    startEmpty = { opened += "empty workout" },
                    newTemplate = { opened += "new template" },
                    newPlan = { opened += "new plan" },
                    resume = { opened += "resume ${it.uuid}" },
                ),
                onRestartForUpdate = { restarts++ },
                tab = { tab, _, links -> StandIn(tab, links) },
            )
        }
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L
    }
}

// A tab's stand-in: its name, and for the Week tab the links it gives Home, as buttons
@Composable
private fun StandIn(tab: HomeTab, links: HomeTabLinks) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppText(text = "${tab.name} tab")
        if (tab == HomeTab.Week) {
            // An empty week, as when its workouts have loaded
            LaunchedEffect(Unit) { links.week.onWeekEmpty(true) }
            AppButton(onClick = links.week.showStartMenu, text = "Week: start a workout")
            AppButton(onClick = links.week.showStatistics, text = "Week: see statistics")
            AppButton(onClick = links.week.showPlans, text = "Week: see plans")
        }
    }
}
