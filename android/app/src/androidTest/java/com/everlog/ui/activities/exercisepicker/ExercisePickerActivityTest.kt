package com.everlog.ui.activities.exercisepicker

import android.app.Activity
import android.app.Instrumentation
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.constants.ELConstants
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.testutil.E2EAccount
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
 * The exercise picker end to end, through the real activity: the library from Firestore (the dev project's, signed in
 * with the E2E account), and the picks handed back as the activity's result. Exercise details aren't opened: the
 * launch is intercepted. ExercisePickerScenarioTest covers the screen itself.
 */
@RunWith(AndroidJUnit4::class)
class ExercisePickerActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var details: Instrumentation.ActivityMonitor? = null

    @Before
    fun setUp() {
        E2EAccount.signIn()
        details = instrumentation.addMonitor(ExerciseDetailsActivity::class.java.name, null, true)
    }

    @After
    fun tearDown() {
        details?.let(instrumentation::removeMonitor)
    }

    @Test
    fun pickingAnExerciseFromTheLibrary() {
        val screen = ActivityScenario.launchActivityForResult<ExercisePickerActivity>(
            ExercisePickerActivity.launchIntent(instrumentation.targetContext, pick = true),
        )
        // The search bar is enabled once the library has loaded
        compose.waitUntil(LoadMillis) { compose.onAllNodes(hasSetTextAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        val search = compose.onNode(hasSetTextAction())
        search.performTextInput("barbell bench press")
        search.performImeAction()
        compose.waitUntil(WaitMillis) { exists(Exercise) }

        // A long press opens the exercise rather than picking it
        compose.onNodeWithText(Exercise).performTouchInput { longClick() }
        compose.waitUntil(WaitMillis) { details!!.hits > 0 }

        compose.onNodeWithText(Exercise).performClick()
        compose.onNodeWithText("Add 1 exercise").performClick()

        // The result only arrives once the picker has closed
        compose.waitUntil(WaitMillis) { screen.state == Lifecycle.State.DESTROYED }

        val result = screen.result
        assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val groups = result.resultData.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as List<ELExerciseGroup>
        assertThat(groups.single().exercises.single().exercise?.name).isEqualTo(Exercise)
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        // In the dev library, which the debug build reads
        const val Exercise = "Barbell Bench Press"
        const val LoadMillis = 30_000L
        const val WaitMillis = 5_000L
    }
}
