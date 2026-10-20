package com.everlog.ui.activities.workoutlog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.WorkoutManager
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.ui.design.theme.Theme
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The workout screen driven as a user would, through its table and keypad, with the real view-model. A few high
 * level scenarios rather than a test per detail (the view-model's unit tests cover those), as each one is slow.
 *
 * The ongoing workout is saved and loaded through the device's real WorkoutManager, so resuming is the real thing.
 * Only Firestore and the plan are left out: the finished workout is kept here instead. It runs in the installed debug
 * app, so it clears that app's ongoing workout: run it on an emulator, not on a phone mid-workout.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutLogScenarioTest {

    val compose = createComposeRule()

    // A scenario that hangs (e.g. the screen never settles) fails with where it was stuck, rather than holding up CI
    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val store = DeviceStore()
    private val analytics = RecordingAnalytics()
    private var exit: WorkoutLogExit? = null
    private val now = 1_800_000_000_000L

    @Before
    fun setUp() {
        WorkoutManager.manager.clearOngoingWorkout()
    }

    @After
    fun tearDown() {
        WorkoutManager.manager.clearOngoingWorkout()
    }

    @Test
    fun aWorkoutFromStartToFinish() {
        val template = template()
        var viewModel by mutableStateOf(viewModel(template))
        compose.setContent {
            Theme {
                WorkoutLogScreen(
                    viewModel = viewModel,
                    // The picker is another screen, so its picks are handed straight in
                    onAddExercise = { viewModel.onExercisesPicked(listOf(group(ELSetType.SINGLE, "Squat" to 5))) },
                    onExit = { exit = it },
                )
            }
        }

        // Bench press, set 1: reps typed over the template's 8, Next to the weight in lb, then Complete set
        cell(0, 0, 0, "count").performClick()
        keys("10")
        cell(0, 0, 0, "count").assertTextEquals("10")
        key("Next").performClick()
        keys("135")
        cell(0, 0, 0, "weight").assertTextEquals("135")
        key("Complete set").performClick()
        tick(0, 0, 0).assertContentDescriptionEquals("Undo set")

        // Set 2 is open on the keypad: switched to time, typed like a microwave
        compose.onNodeWithText("SET 2 OF 2 · REPS").assertExists()
        key("Time").performClick()
        keys("130")
        cell(0, 1, 0, "count").assertTextEquals("1:30")
        compose.onNodeWithContentDescription("Hide keypad").performClick()

        // A timed set starts its timer from its row. +10s adds to its time, and Stop leaves the set to be ticked, which
        // starts its rest
        tick(0, 1, 0).assertContentDescriptionEquals("Start timer").performClick()
        compose.onNodeWithText("SET TIMER").assertExists()
        key("+10s").performClick()
        cell(0, 1, 0, "count").assertTextEquals("1:40")
        key("Stop").performClick()
        tick(0, 1, 0).assertContentDescriptionEquals("Complete set").performClick()
        tick(0, 1, 0).assertContentDescriptionEquals("Undo set")
        compose.onNodeWithText("REST").assertExists()
        key("Skip").performClick()
        compose.onNodeWithText("REST").assertDoesNotExist()

        // The closed keypad's Complete set goes through the superset exercise by exercise
        key("Complete set").performClick()
        key("Complete set").performClick()
        tick(1, 0, 0).assertContentDescriptionEquals("Undo set")
        tick(1, 0, 1).assertContentDescriptionEquals("Undo set")
        assertThat(analytics.events).contains("set_group_completed SUPER 2")

        // A tick undoes a done set
        tick(1, 0, 1).performClick()
        tick(1, 0, 1).assertContentDescriptionEquals("Complete set")

        // The app is killed: the screen comes back from the saved workout with everything done so far
        viewModel = viewModel(template)
        compose.waitForIdle()
        cell(0, 0, 0, "count").assertTextEquals("10")
        cell(0, 0, 0, "weight").assertTextEquals("135")
        tick(0, 0, 0).assertContentDescriptionEquals("Undo set")
        cell(0, 1, 0, "count").assertTextEquals("1:40")

        // An exercise added at the end, which may be below the screen
        key("Add exercise").performClick()
        compose.onNodeWithTag(WorkoutLogTags.Table).performScrollToNode(hasText("Squat"))
        compose.onNodeWithText("Squat").assertExists()

        // Finish asks, as sets aren't ticked, then saves the workout as it is
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog()).not()).performClick()
        compose.onNodeWithText("Finish workout?").assertExists()
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()

        val finished = store.finished!!
        val bench = finished.getExerciseGroups()[0].exercises[0].sets
        assertThat(bench[0].getReps()).isEqualTo(10)
        assertThat(bench[0].getWeightKg()).isWithin(0.01f).of(61.23f)
        assertThat(bench[0].getCompletedDate()).isEqualTo(now)
        assertThat(bench[1].getTimeSeconds()).isEqualTo(100)
        assertThat(finished.getExerciseGroups().map { it.exercises.first().exercise?.name }).containsExactly("Bench press", "Fly", "Squat").inOrder()
        // Which screen it ran on stays on the device
        assertThat(finished.asMap().keys).doesNotContain("workoutScreenV2")
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
        assertThat(exit).isEqualTo(WorkoutLogExit.Finished(finished))
        assertThat(analytics.events.last()).isEqualTo("workout_completed")
    }

    @Test
    fun leavingAWorkout() {
        // An empty workout: nothing to lose, so Discard just goes
        val empty = ELWorkout(uuid = "empty", createdDate = now, routine = ELRoutine(name = "Quick"))
        var viewModel by mutableStateOf(viewModel(empty))
        compose.setContent {
            Theme { WorkoutLogScreen(viewModel = viewModel, onAddExercise = {}, onExit = { exit = it }) }
        }
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Nothing's been logged yet.").assertExists()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertThat(exit).isEqualTo(WorkoutLogExit.Discarded)
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
        assertThat(analytics.events).contains("workout_stopped")

        // A set done: Keep going leaves the workout as it was, and Finish saves it without asking again
        exit = null
        viewModel = viewModel(template())
        compose.waitForIdle()
        tick(0, 0, 0).performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNode(hasText("Keep going") and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        tick(0, 0, 0).assertContentDescriptionEquals("Undo set")
        assertThat(exit).isNull()

        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertThat(store.finished).isNotNull()
        assertThat(exit).isInstanceOf(WorkoutLogExit.Finished::class.java)
    }

    private fun viewModel(launched: ELWorkout) = WorkoutLogViewModel(
        dispatcher = Dispatchers.Main,
        workout = WorkoutLogViewModel.load(launched, store),
        fromPlan = false,
        store = store,
        settings = PoundSettings,
        analytics = analytics,
        alerts = object : WorkoutLogAlerts {
            override fun timerRanOut() = Unit
        },
        clock = { now },
        tickMillis = null,
    )

    // Bench press, 2 sets of 8, then a superset of fly and raise, 2 rounds of 12
    private fun template() = ELWorkout(
        uuid = "workout",
        createdDate = now,
        routine = ELRoutine(
            name = "Push",
            exerciseGroups = mutableListOf(
                group(ELSetType.SINGLE, "Bench press" to 8),
                group(ELSetType.SUPER, "Fly" to 12, "Raise" to 12),
            ),
        ),
    )

    private fun group(type: ELSetType, vararg exercises: Pair<String, Int>) = ELExerciseGroup(
        uuid = "group-${exercises.first().first}",
        type = type.name,
        exercises = exercises.map { (name, reps) ->
            ELRoutineExercise(name, ELExercise(uuid = name, name = name), MutableList(2) { ELSet(requiredReps = reps) })
        }.toMutableList(),
    )

    private fun cell(block: Int, round: Int, exercise: Int, field: String): SemanticsNodeInteraction {
        val set = WorkoutLogTags.set(block, round, exercise)
        return compose.onNodeWithTag(if (field == "count") WorkoutLogTags.count(set) else WorkoutLogTags.weight(set))
    }

    private fun tick(block: Int, round: Int, exercise: Int) =
        compose.onNodeWithTag(WorkoutLogTags.tick(WorkoutLogTags.set(block, round, exercise)))

    // A key or button on the docked keypad, rather than the same text in the table
    private fun key(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(WorkoutLogTags.Keypad)))

    private fun keys(digits: String) = digits.forEach { key(it.toString()).performClick() }

    /**
     * The ongoing workout as the app keeps it, on this device. The finished one is kept here instead of Firestore.
     */
    private class DeviceStore : WorkoutLogStore {
        var finished: ELWorkout? = null

        override fun savedCopyOf(workout: ELWorkout): ELWorkout? = WorkoutManager.manager.savedCopyOf(workout)

        override fun saveOngoing(workout: ELWorkout) = WorkoutManager.manager.setOngoingWorkout(workout)

        override fun clearOngoing() = WorkoutManager.manager.clearOngoingWorkout()

        override fun saveFinished(workout: ELWorkout, fromPlan: Boolean) {
            finished = workout
        }
    }

    private object PoundSettings : WorkoutLogSettings {
        override fun weightUnit() = WeightUnit.POUND

        override fun weightIncrease() = 5f

        override fun keepScreenOn() = false
    }

    private class RecordingAnalytics : WorkoutLogAnalytics {
        val events = mutableListOf<String>()

        override fun screenViewed() = Unit

        override fun setCompleted(type: ELSetType?, timed: Boolean) {
            events += "set_completed $type"
        }

        override fun setGroupCompleted(type: ELSetType?, exercises: Int) {
            events += "set_group_completed $type $exercises"
        }

        override fun setEdited(type: ELSetType?, timed: Boolean) {
            events += "set_edited $type"
        }

        override fun exercisesAdded() {
            events += "exercises_added"
        }

        override fun restStarted() {
            events += "rest_started"
        }

        override fun restStopped() {
            events += "rest_stopped"
        }

        override fun setTimerStarted() {
            events += "set_timer_started"
        }

        override fun setTimerStopped() {
            events += "set_timer_stopped"
        }

        override fun workoutCompleted() {
            events += "workout_completed"
        }

        override fun workoutStopped() {
            events += "workout_stopped"
        }

        override fun discardPromptShown(setsCompleted: Int) {
            events += "discard_prompt_shown $setsCompleted"
        }

        override fun discardPromptFinished() {
            events += "discard_prompt_finished"
        }

        override fun discardPromptCancelled() {
            events += "discard_prompt_cancelled"
        }
    }
}
