package com.everlog.ui.activities.workoutlog

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
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
import androidx.test.rule.GrantPermissionRule
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.WorkoutManager
import com.everlog.testutil.E2EAccount
import com.everlog.ui.activities.exercisepicker.ExercisePickerActivity
import com.everlog.ui.activities.home.exercise.ExercisesActivity
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.activities.home.workout.details.WorkoutDetailsActivity
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
 * The workout screen end to end, through the real activity and everything it's wired to: the saved ongoing workout,
 * the notification, the exercise picker's result, Firestore and workout details on finishing. WorkoutLogScenarioTest
 * covers what happens on the screen itself.
 *
 * Finishing saves to Firestore, so it signs in with the E2E test account (as LoginActivityTest does) and deletes the
 * workout afterwards. The picker and workout details aren't opened: their launches are intercepted. Run it on an
 * emulator, not on a phone mid-workout, as it clears the app's ongoing workout.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutLogActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(GrantPermissionRule.grant(*permissions()))
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val monitors = mutableListOf<Instrumentation.ActivityMonitor>()
    private val workout = ELWorkout(
        uuid = "e2e-workout-${System.currentTimeMillis()}",
        createdDate = System.currentTimeMillis(),
        routine = ELRoutine(name = "E2E", exerciseGroups = mutableListOf(group("e2e-bench", "Bench press"))),
    ).apply { workoutScreenV2 = true }

    @Before
    fun setUp() {
        E2EAccount.signIn()
        WorkoutManager.manager.clearOngoingWorkout()
    }

    @After
    fun tearDown() {
        monitors.forEach(instrumentation::removeMonitor)
        WorkoutManager.manager.clearOngoingWorkout()
        // The finished workout, if it was saved
        ELDatastore.workoutStore().delete(workout)
    }

    @Test
    fun aWorkoutFromLaunchToItsDetails() {
        val screen = ActivityScenario.launch<WorkoutLogActivity>(
            Intent(context, WorkoutLogActivity::class.java).putExtra(ELConstants.EXTRA_WORKOUT, workout),
        )

        // With the permissions granted, the workout's notification shows
        compose.waitUntil(TIMEOUT) { workoutNotificationShows() }

        // A set ticked is saved as the ongoing workout, which resumes it
        compose.onNodeWithTag(WorkoutLogTags.tick(WorkoutLogTags.set(0, 0, 0))).performClick()
        compose.waitUntil(TIMEOUT) { ongoingFirstSetDone() }

        // Its name opens its details on Info, and its menu's History on History
        val details = recordLaunches(ExerciseDetailsActivity::class.java)
        compose.onNodeWithText("Bench press").performClick()
        compose.onNodeWithTag(WorkoutLogTags.menu(0)).performClick()
        compose.onNode(hasText("History") and hasAnyAncestor(isPopup())).performClick()
        compose.waitUntil(TIMEOUT) { details.intents.size == 2 }
        assertThat(details.intents.map { it.getSerializableExtra(ELConstants.EXTRA_TYPE) })
            .containsExactly(ExerciseDetailsActivity.Companion.Type.INFO, ExerciseDetailsActivity.Companion.Type.HISTORY).inOrder()

        // Add exercise opens the picker, whose picks come back to the table
        val pickerOpened = returnFromPicker(group("e2e-squat", "Squat"))
        compose.onNode(hasText("Add exercise") and hasAnyAncestor(hasTestTag(WorkoutLogTags.Keypad))).performClick()
        compose.waitUntil(TIMEOUT) { pickerOpened() }
        compose.onNodeWithTag(WorkoutLogTags.Table).performScrollToNode(hasText("Squat"))

        // The system back button asks about leaving; Finish instead saves the workout and opens its details
        val workoutDetails = interceptLaunch(WorkoutDetailsActivity::class.java)
        Espresso.pressBack()
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(TIMEOUT) { workoutDetails.hits > 0 }
        compose.waitUntil(TIMEOUT) { screen.state == Lifecycle.State.DESTROYED }
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
        compose.waitUntil(TIMEOUT) { !workoutNotificationShows() }
    }

    @Test
    fun discardingAWorkout() {
        val screen = ActivityScenario.launch<WorkoutLogActivity>(
            Intent(context, WorkoutLogActivity::class.java).putExtra(ELConstants.EXTRA_WORKOUT, workout),
        )
        compose.onNodeWithText("Bench press").assertExists()

        Espresso.pressBack()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()

        compose.waitUntil(TIMEOUT) { screen.state == Lifecycle.State.DESTROYED }
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
        compose.waitUntil(TIMEOUT) { !workoutNotificationShows() }
    }

    private fun group(uuid: String, name: String) = ELExerciseGroup(
        uuid = "group-$uuid",
        type = ELSetType.SINGLE.name,
        exercises = mutableListOf(ELRoutineExercise(uuid, ELExercise(uuid = uuid, name = name), MutableList(2) { ELSet(requiredReps = 8) })),
    )

    // The picker is whichever one Remote Config has on. Returns whether it was opened.
    private fun returnFromPicker(vararg picked: ELExerciseGroup): () -> Boolean {
        val data = Intent().putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, ArrayList(picked.toList()))
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, data)
        val new = intercept(ExercisePickerActivity::class.java, result)
        val old = intercept(ExercisesActivity::class.java, result)
        return { new.hits + old.hits > 0 }
    }

    private fun interceptLaunch(activity: Class<out Activity>) = intercept(activity, null)

    // Stops the activity from opening, keeping the intents it was launched with
    private fun recordLaunches(activity: Class<out Activity>) = LaunchRecorder(activity).also {
        instrumentation.addMonitor(it)
        monitors += it
    }

    private class LaunchRecorder(private val activity: Class<out Activity>) : Instrumentation.ActivityMonitor() {
        val intents = mutableListOf<Intent>()

        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            if (intent.component?.className != activity.name) return null
            intents += intent
            return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        }
    }

    // Stops the activity from opening, and answers with [result]
    private fun intercept(activity: Class<out Activity>, result: Instrumentation.ActivityResult?) =
        instrumentation.addMonitor(activity.name, result, true).also { monitors += it }

    private fun workoutNotificationShows(): Boolean =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.packageName == context.packageName }

    private fun ongoingFirstSetDone(): Boolean =
        WorkoutManager.manager.ongoingWorkout()?.getExerciseGroups()?.firstOrNull()?.exercises?.firstOrNull()?.sets?.firstOrNull()
            ?.getCompletedDate()?.let { it > 0 } == true

    private companion object {
        const val TIMEOUT = 10_000L

        fun permissions(): Array<String> = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.toTypedArray()
    }
}
