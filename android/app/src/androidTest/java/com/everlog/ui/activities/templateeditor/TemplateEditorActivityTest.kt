package com.everlog.ui.activities.templateeditor

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.android.template.details.RoutineDetailsActivity
import com.everlog.android.template.editor.TemplateEditorActivity
import com.everlog.android.template.editor.TemplateEditorTags
import com.everlog.application.ELApplication
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.testutil.E2EAccount
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The template editor end to end, through the real activity: a saved template read through DataSync, an edit kept
 * when Android rebuilds the screen, and saved back to Firestore. TemplateEditorFeatureTest covers the screen itself.
 * It signs in with the E2E test account and deletes the template afterwards. Routine details' launch is intercepted.
 */
@RunWith(AndroidJUnit4::class)
class TemplateEditorActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val routine = ELRoutine(
        uuid = "e2e-template-${System.currentTimeMillis()}",
        name = "E2E push",
        createdDate = System.currentTimeMillis(),
        exerciseGroups = mutableListOf(
            ELExerciseGroup(
                uuid = "group-e2e-bench",
                type = ELSetType.SINGLE.name,
                exercises = mutableListOf(
                    ELRoutineExercise("e2e-bench", ELExercise(uuid = "e2e-bench", name = "Bench press"), MutableList(2) { ELSet(requiredReps = 8) }),
                ),
            ),
        ),
    )
    private var details: Instrumentation.ActivityMonitor? = null

    @Before
    fun setUp() {
        E2EAccount.signIn()
        runBlocking {
            RealRoutineRepository(Dispatchers.IO, AppDataSync.instance, ELApplication.graph.userSession) { ELDatastore.routineStore() }
                .saveRoutines(listOf(routine))
        }
        details = instrumentation.addMonitor(RoutineDetailsActivity::class.java.name, null, true)
    }

    @After
    fun tearDown() {
        details?.let(instrumentation::removeMonitor)
        ELDatastore.routineStore().delete(routine)
    }

    @Test
    fun editingASavedTemplate() {
        val screen = ActivityScenario.launchActivityForResult<TemplateEditorActivity>(
            TemplateEditorActivity.launchIntent(context, routine.uuid, showDetailsOnSuccess = true),
        )
        compose.waitUntil(TIMEOUT) { shows("E2E push") }

        // A set added stays when Android rebuilds the screen, e.g. turning the phone
        compose.onNodeWithText("Add set").performClick()
        screen.recreate()
        compose.waitUntil(TIMEOUT) { shows("E2E push") }
        compose.onNodeWithTag(TemplateEditorTags.set(0, 2, 0)).assertExists()

        // Saved over it and handed back, without opening its details
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { screen.state == Lifecycle.State.DESTROYED }

        assertThat(screen.result.resultCode).isEqualTo(Activity.RESULT_OK)
        @Suppress("DEPRECATION")
        val returned = screen.result.resultData.getSerializableExtra(ELConstants.EXTRA_ROUTINE) as ELRoutine
        assertThat(returned.uuid).isEqualTo(routine.uuid)
        assertThat(details?.hits).isEqualTo(0)
        assertThat(stored().exerciseGroups.single().exercises.single().sets).hasSize(3)
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    // As saved, from the cache, which a save updates straight away
    private fun stored(): ELRoutine =
        Tasks.await(FirestorePathManager.routinesCollection.document(routine.uuid!!).get(), 10, TimeUnit.SECONDS).toObject(ELRoutine::class.java)!!

    private companion object {
        const val TIMEOUT = 15_000L
    }
}
