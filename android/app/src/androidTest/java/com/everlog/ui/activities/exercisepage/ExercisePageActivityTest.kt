package com.everlog.ui.activities.exercisepage

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.page.ExercisePageActivity
import com.everlog.android.exercise.page.ExercisePageTab
import com.everlog.android.exercise.page.ExercisePageTags
import com.everlog.testutil.E2EAccount
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * An exercise's page end to end, through the real activity: the E2E account's history from Firestore (the dev
 * project's), opening on the tab it's asked for and closing with back. ExercisePageScenarioTest covers the screen
 * itself.
 */
@RunWith(AndroidJUnit4::class)
class ExercisePageActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        E2EAccount.signIn()
    }

    @Test
    fun openingAnExercisesHistory() {
        val screen = ActivityScenario.launch<ExercisePageActivity>(
            ExercisePageActivity.launchIntent(instrumentation.targetContext, Exercise, ExercisePageTab.History),
        )

        // The history loads from the account: its sessions, or the example when it has none
        compose.waitUntil(LoadMillis) { shows(ExercisePageTags.History) || shows(ExercisePageTags.HistoryExample) }

        // Statistics from its chip
        compose.onNodeWithTag(ExercisePageTags.tab(ExercisePageTab.Statistics)).performClick()
        compose.waitUntil(WaitMillis) { shows(ExercisePageTags.Tiles) }

        // Back closes the page
        Espresso.pressBackUnconditionally()
        compose.waitUntil(WaitMillis) { screen.state == Lifecycle.State.DESTROYED }
    }

    private fun shows(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        // The exercise the E2E workouts use (WorkoutLogActivityTest)
        val Exercise = ELExercise(uuid = "e2e-bench", name = "Bench press")
        const val LoadMillis = 30_000L
        const val WaitMillis = 5_000L
    }
}
