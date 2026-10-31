package com.everlog.ui.activities.onboarding

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.testutil.E2EAccount
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.android.template.feature.editor.TemplateEditorActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The onboarding end to end, through the real activity: templates built from the dev project's exercise library
 * (signed in with the E2E account), the routine builder's result, and the screen closing with RESULT_OK whichever
 * way it ends. The questions are skipped, so the device's settings stay as they are, and nothing is saved to the
 * account: the routine builder isn't opened, its launch is intercepted and answers as if a routine was saved.
 * OnboardingScenarioTest covers the screen itself.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    // The template editor is whichever one Remote Config has on
    private val routineBuilders = mutableListOf<Instrumentation.ActivityMonitor>()

    @Before
    fun setUp() {
        E2EAccount.signIn()
        listOf(CreateRoutineActivity::class.java, TemplateEditorActivity::class.java).forEach { editor ->
            routineBuilders += instrumentation.addMonitor(editor.name, Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()), true)
        }
    }

    @After
    fun tearDown() {
        routineBuilders.forEach(instrumentation::removeMonitor)
    }

    @Test
    fun buildingTemplatesThenTheUsersOwn() {
        val screen = launch()

        compose.onNodeWithText("Let's go").performClick()
        repeat(5) { compose.onNodeWithText("Skip").performClick() }
        compose.onNodeWithText("Build my templates").performClick()
        compose.waitUntil(LoadMillis) { exists("Build my own template") }

        compose.onNodeWithText("Build my own template").performClick()
        compose.waitUntil(WaitMillis) { routineBuilders.sumOf { it.hits } > 0 }

        compose.waitUntil(WaitMillis) { screen.state == Lifecycle.State.DESTROYED }
        assertThat(screen.result.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun skippingSetup() {
        val screen = launch()

        Espresso.pressBack()
        compose.onNode(hasText("Skip setup") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(WaitMillis) { screen.state == Lifecycle.State.DESTROYED }
        assertThat(screen.result.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    private fun launch(): ActivityScenario<OnboardingActivity> {
        val screen = ActivityScenario.launchActivityForResult<OnboardingActivity>(
            Intent(instrumentation.targetContext, OnboardingActivity::class.java),
        )
        compose.waitUntil(WaitMillis) { exists("Let's go") }
        return screen
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val LoadMillis = 30_000L
        const val WaitMillis = 10_000L
    }
}
