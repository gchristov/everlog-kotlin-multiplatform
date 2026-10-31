package com.everlog.ui.activities.templateeditor

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.exercise.picker.ExercisePickerActivity
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.firebase.FirestorePathManager
import com.everlog.testutil.E2EAccount
import com.everlog.android.template.details.RoutineDetailsActivity
import com.everlog.android.template.editor.TemplateEditorActivity
import com.everlog.android.template.editor.TemplateEditorTags
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
 * The template editor end to end, through the real activity and everything it's wired to: reading the template
 * through DataSync, the exercise picker's result, saving to Firestore, the result it hands back and the details of a
 * new template. TemplateEditorScenarioTest covers what happens on the screen itself.
 *
 * It signs in with the E2E test account and deletes the templates it saves. The picker and routine details aren't
 * opened: their launches are intercepted.
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
    private val monitors = mutableListOf<Instrumentation.ActivityMonitor>()
    private val saved = mutableListOf<ELRoutine>()

    @Before
    fun setUp() {
        E2EAccount.signIn()
    }

    @After
    fun tearDown() {
        monitors.forEach(instrumentation::removeMonitor)
        saved.forEach { ELDatastore.routineStore().delete(it) }
    }

    @Test
    fun aNewTemplateFromLaunchToItsDetails() {
        val screen = ActivityScenario.launchActivityForResult<TemplateEditorActivity>(
            TemplateEditorActivity.launchIntent(context, uuid = null, showDetailsOnSuccess = true),
        )
        compose.onNodeWithText("New template").assertExists()

        // Add exercise opens the picker, whose picks come back at 3 sets of 10
        val pickerOpened = returnFromPicker(picked("e2e-bench", "Bench press"))
        compose.onNode(hasText("Add exercise") and hasAnyAncestor(hasTestTag(TemplateEditorTags.Keypad))).performClick()
        compose.waitUntil(TIMEOUT) { pickerOpened() }
        compose.onNodeWithTag(TemplateEditorTags.List).performScrollToNode(hasText("Bench press"))

        // Save writes it to Firestore, opens its details and hands it back
        val details = intercept(RoutineDetailsActivity::class.java, null)
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { screen.state == Lifecycle.State.DESTROYED }

        val routine = returned(screen)
        saved += routine
        assertThat(details.hits).isEqualTo(1)
        val stored = stored(routine.uuid!!)
        assertThat(stored.name).isEqualTo("My template")
        assertThat(stored.exerciseGroups.single().exercises.single().sets.map { it.getRequiredReps() }).containsExactly(10, 10, 10)
    }

    @Test
    fun editingASavedTemplate() {
        val routine = savedRoutine()

        val screen = ActivityScenario.launchActivityForResult<TemplateEditorActivity>(
            TemplateEditorActivity.launchIntent(context, routine.uuid, showDetailsOnSuccess = true),
        )

        // Read through DataSync by its uuid, a set added, and saved over it without opening its details
        compose.waitUntil(TIMEOUT) { shows("E2E push") }
        val details = intercept(RoutineDetailsActivity::class.java, null)
        compose.onNodeWithText("Add set").performClick()
        compose.onNodeWithTag(TemplateEditorTags.Save).performClick()
        compose.waitUntil(TIMEOUT) { screen.state == Lifecycle.State.DESTROYED }

        assertThat(returned(screen).uuid).isEqualTo(routine.uuid)
        assertThat(details.hits).isEqualTo(0)
        assertThat(stored(routine.uuid!!).exerciseGroups.single().exercises.single().sets).hasSize(3)
    }

    @Test
    fun anEditKeptThroughRecreationThenDiscarded() {
        val routine = savedRoutine()
        val screen = ActivityScenario.launchActivityForResult<TemplateEditorActivity>(
            TemplateEditorActivity.launchIntent(context, routine.uuid, showDetailsOnSuccess = true),
        )
        compose.waitUntil(TIMEOUT) { shows("E2E push") }

        // A set added stays when Android rebuilds the screen, e.g. turning the phone
        compose.onNodeWithText("Add set").performClick()
        screen.recreate()
        compose.waitUntil(TIMEOUT) { shows("E2E push") }
        compose.onNodeWithTag(TemplateEditorTags.set(0, 2, 0)).assertExists()

        // The system back button asks; Discard closes without saving and hands nothing back
        Espresso.pressBack()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(TIMEOUT) { screen.state == Lifecycle.State.DESTROYED }

        assertThat(screen.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(stored(routine.uuid!!).exerciseGroups.single().exercises.single().sets).hasSize(2)
    }

    @Test
    fun aTemplateThatDoesntExistSaysSo() {
        ActivityScenario.launch<TemplateEditorActivity>(
            TemplateEditorActivity.launchIntent(context, "e2e-missing-${System.currentTimeMillis()}", showDetailsOnSuccess = false),
        ).use {
            compose.waitUntil(TIMEOUT) { shows("This template has been deleted.") }
        }
    }

    // E2E push: bench press, 2 sets of 8, saved to the E2E account and deleted after the test
    private fun savedRoutine(): ELRoutine {
        val routine = ELRoutine(
            uuid = "e2e-template-${System.currentTimeMillis()}",
            name = "E2E push",
            createdDate = System.currentTimeMillis(),
            exerciseGroups = mutableListOf(group("e2e-bench", "Bench press", sets = 2)),
        )
        runBlocking { RealRoutineRepository(Dispatchers.IO, AppDataSync.instance) { ELDatastore.routineStore() }.saveRoutines(listOf(routine)) }
        saved += routine
        return routine
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun returned(screen: ActivityScenario<TemplateEditorActivity>): ELRoutine {
        assertThat(screen.result.resultCode).isEqualTo(Activity.RESULT_OK)
        @Suppress("DEPRECATION")
        return screen.result.resultData.getSerializableExtra(ELConstants.EXTRA_ROUTINE) as ELRoutine
    }

    // As saved, from the cache, which a save updates straight away
    private fun stored(uuid: String): ELRoutine =
        Tasks.await(FirestorePathManager.routinesCollection.document(uuid).get(), 10, TimeUnit.SECONDS).toObject(ELRoutine::class.java)!!

    private fun group(uuid: String, name: String, sets: Int) = ELExerciseGroup(
        uuid = "group-$uuid",
        type = ELSetType.SINGLE.name,
        exercises = mutableListOf(ELRoutineExercise(uuid, ELExercise(uuid = uuid, name = name), MutableList(sets) { ELSet(requiredReps = 8) })),
    )

    // As the picker hands an exercise over: one empty set
    private fun picked(uuid: String, name: String) = ELExerciseGroup(
        uuid = "group-$uuid",
        type = ELSetType.SINGLE.name,
        exercises = mutableListOf(ELRoutineExercise.buildRoutineExercise(ELExercise(uuid = uuid, name = name))),
    )

    // Returns whether the picker was opened
    private fun returnFromPicker(vararg picked: ELExerciseGroup): () -> Boolean {
        val data = Intent().putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, ArrayList(picked.toList()))
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, data)
        val picker = intercept(ExercisePickerActivity::class.java, result)
        return { picker.hits > 0 }
    }

    // Stops the activity from opening, and answers with [result]
    private fun intercept(activity: Class<out Activity>, result: Instrumentation.ActivityResult?) =
        instrumentation.addMonitor(activity.name, result, true).also { monitors += it }

    private companion object {
        const val TIMEOUT = 15_000L
    }
}
