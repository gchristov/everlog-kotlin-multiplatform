package com.everlog.ui.activities.workoutlog

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.swipeLeft
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import arrow.core.Either
import arrow.core.right
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.controllers.workoutprefill.LoadPrefillSourcesUseCase
import com.everlog.data.controllers.workoutsession.RealWorkoutSetPrefill
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELExerciseHistory
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.repositories.OngoingWorkoutRepository
import com.everlog.managers.WorkoutManager
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.services.workout.ShownSet
import com.everlog.services.workout.WorkoutService
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.UnitUtils
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
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

    // A scenario that hangs (e.g. the screen never settles) fails with where it was stuck, rather than holding up CI.
    // The longest takes over 2 minutes on a slow emulator (the API 37 tablet).
    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(4, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val store = DeviceStore()
    private val analytics = RecordingAnalytics()
    private val notification = RecordingNotification()
    private var exit: WorkoutLogExit? = null
    // The exercise whose details were opened, and on which tab
    private var opened: Pair<String?, ExerciseTab>? = null
    private var setSuggestionsOpened = 0
    private val settings = PoundSettings()
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
                    onOpenExercise = { exercise, tab -> opened = exercise.name to tab },
                    onOpenSetSuggestions = { setSuggestionsOpened++ },
                    onExit = { exit = it },
                )
            }
        }

        // Bench press was last done at 8 × 100 lb: its reps and weights are filled in from that, as suggestions, and
        // its last time shows beside each set
        cell(0, 0, 0, "count").assertTextEquals("8", "✦")
        cell(0, 0, 0, "weight").assertTextEquals("100", "✦")
        compose.onAllNodesWithText("8 × 100 lb")[0].assertExists()

        // Bench press, set 1: the suggested reps say where they come from and are typed over, Next to the suggested
        // weight, typed over in lb, then Complete set
        cell(0, 0, 0, "count").performClick()
        compose.onNodeWithText("✦ Suggested from last time").assertExists()
        keys("10")
        cell(0, 0, 0, "count").assertTextEquals("10")
        key("Next").performClick()
        compose.onNodeWithText("✦ Suggested from last time").assertExists()
        keys("135")
        cell(0, 0, 0, "weight").assertTextEquals("135")
        key("Complete set").performClick()
        tick(0, 0, 0).assertContentDescriptionEquals("Undo set")

        // Set 2 is open on the keypad: switched to time, typed like a microwave, and its key starts the timer
        compose.onNodeWithText("SET 2 OF 2 · REPS").assertExists()
        key("Time").performClick()
        keys("130")
        cell(0, 1, 0, "count").assertTextEquals("1:30")
        key("Start timer").performClick()
        compose.onNodeWithText("SET TIMER").assertExists()
        // Each value is logged once, as the keypad leaves it, in lb
        assertThat(analytics.events.filter { "modified" in it })
            .containsExactly("reps_modified 10.0", "weight_modified 135.0", "time_modified 90.0").inOrder()

        // +10s adds to its time, as does the notification's +, and Stop leaves the set to be completed, which starts
        // its rest
        key("+10s").performClick()
        cell(0, 1, 0, "count").assertTextEquals("1:40")
        val receiver = WorkoutLogNotificationReceiver(viewModel)
        assertThat(notification.shownSetName()).isEqualTo("Bench press")
        notificationButton(receiver, WorkoutService.BROADCAST_REQUEST_INCREASE_TIME)
        cell(0, 1, 0, "count").assertTextEquals("1:50")
        key("Stop").performClick()
        key("Complete set").performClick()
        tick(0, 1, 0).assertContentDescriptionEquals("Undo set")
        compose.onNodeWithText("REST").assertExists()

        // Edited, the done timed set runs its timer again in place of the rest, and Done stops it and moves on to
        // the set to complete next
        cell(0, 1, 0, "count").performClick()
        key("Start timer").performClick()
        compose.onNodeWithText("SET TIMER").assertExists()
        key("Done").performClick()
        compose.onNodeWithText("SET TIMER").assertDoesNotExist()
        tick(0, 1, 0).assertContentDescriptionEquals("Undo set")
        compose.onNodeWithText("SET 1 OF 2 · 1 OF 2 · REPS").assertExists()
        compose.onNodeWithContentDescription("Hide keypad").performClick()

        // Its weight's key is Done, its time's runs the timer again, and moving on to another set stops it
        cell(0, 1, 0, "weight").performClick()
        key("Done").assertExists()
        cell(0, 1, 0, "count").performClick()
        key("Start timer").performClick()
        compose.onNodeWithText("SET TIMER").assertExists()
        cell(0, 0, 0, "count").performClick()
        compose.onNodeWithText("SET TIMER").assertDoesNotExist()
        compose.onNodeWithContentDescription("Hide keypad").performClick()
        tick(0, 1, 0).assertContentDescriptionEquals("Undo set")

        // The superset's menu, beside its last exercise, has each exercise's history, and each name opens its info
        showExercise(1)
        val menu = compose.onNodeWithTag(WorkoutLogTags.menu(1)).getUnclippedBoundsInRoot()
        val raise = compose.onNode(hasText("Raise") and hasAnyAncestor(hasTestTag(WorkoutLogTags.Table))).getUnclippedBoundsInRoot()
        assertThat(((menu.top + menu.bottom) / 2).value).isWithin(1f).of(((raise.top + raise.bottom) / 2).value)
        compose.onNodeWithTag(WorkoutLogTags.menu(1)).performClick()
        compose.onNode(hasText("Fly history") and hasAnyAncestor(isPopup())).assertExists()
        compose.onNode(hasText("Raise history") and hasAnyAncestor(isPopup())).performClick()
        assertThat(opened).isEqualTo("Raise" to ExerciseTab.History)
        compose.onNode(hasText("Fly") and hasAnyAncestor(hasTestTag(WorkoutLogTags.Table))).performClick()
        assertThat(opened).isEqualTo("Fly" to ExerciseTab.Info)

        // The closed keypad's Complete set goes through the superset exercise by exercise
        key("Complete set").performClick()
        key("Complete set").performClick()
        tick(1, 0, 0).assertContentDescriptionEquals("Undo set")
        tick(1, 0, 1).assertContentDescriptionEquals("Undo set")
        assertThat(analytics.events).contains("set_group_completed SUPER 2")

        // A tick undoes a done set
        tick(1, 0, 1).performClick()
        tick(1, 0, 1).assertContentDescriptionEquals("Complete set")

        // The notification's buttons, sent as WorkoutService sends them, for the set it shows: the one just undone.
        // + reps, then Complete set, which starts the rest, and Skip rest.
        assertThat(notification.shownSetName()).isEqualTo("Raise")
        notificationButton(receiver, WorkoutService.BROADCAST_REQUEST_INCREASE_REPS)
        cell(1, 0, 1, "count").assertTextEquals("13")
        assertThat(analytics.events.last()).isEqualTo("reps_modified 13.0")
        notificationButton(receiver, WorkoutService.BROADCAST_REQUEST_COMPLETE_SET)
        tick(1, 0, 1).assertContentDescriptionEquals("Undo set")
        compose.onNodeWithText("REST").assertExists()
        assertThat(analytics.events).contains("set_completed SUPER notification")
        notificationButton(receiver, WorkoutService.BROADCAST_REQUEST_TIMER_REST_STOP)
        compose.onNodeWithText("REST").assertDoesNotExist()

        // The notification follows the set selected on the screen, and its buttons act on that one
        showExercise(1)
        cell(1, 1, 0, "count").performClick()
        compose.waitForIdle()
        assertThat(notification.shownSetName()).isEqualTo("Fly")
        cell(1, 1, 1, "count").performClick()
        compose.onNodeWithContentDescription("Hide keypad").performClick()
        assertThat(notification.shownSetName()).isEqualTo("Raise")
        notificationButton(receiver, WorkoutService.BROADCAST_REQUEST_DECREASE_REPS)
        cell(1, 1, 1, "count").assertTextEquals("11")

        // The app is killed: the screen comes back from the saved workout with everything done so far
        viewModel = viewModel(template)
        compose.waitForIdle()
        cell(0, 0, 0, "count").assertTextEquals("10")
        cell(0, 0, 0, "weight").assertTextEquals("135")
        tick(0, 0, 0).assertContentDescriptionEquals("Undo set")
        cell(0, 1, 0, "count").assertTextEquals("1:50")

        // A set added to bench press. A swipe only shows its Delete, which deletes it; Undo puts it back, then it's
        // deleted for good.
        showExercise(0)
        compose.onAllNodes(hasText("Add set") and hasAnyAncestor(hasTestTag(WorkoutLogTags.Table)))[0].performClick()
        row(0, 2, 0).assertExists().performTouchInput { swipeLeft() }
        row(0, 2, 0).assertExists()
        // Opening another set's Delete closes the first
        val closedLeft = row(0, 0, 0).getUnclippedBoundsInRoot().left
        assertThat(row(0, 2, 0).getUnclippedBoundsInRoot().left).isLessThan(closedLeft)
        row(0, 1, 0).performTouchInput { swipeLeft() }
        assertThat(row(0, 1, 0).getUnclippedBoundsInRoot().left).isLessThan(closedLeft)
        assertThat(row(0, 2, 0).getUnclippedBoundsInRoot().left).isEqualTo(closedLeft)
        row(0, 2, 0).performTouchInput { swipeLeft() }
        assertThat(row(0, 1, 0).getUnclippedBoundsInRoot().left).isEqualTo(closedLeft)
        deleteSet(0, 2, 0)
        row(0, 2, 0).assertDoesNotExist()
        compose.onNodeWithText("Undo").performClick()
        row(0, 2, 0).assertExists().performTouchInput { swipeLeft() }
        deleteSet(0, 2, 0)
        row(0, 2, 0).assertDoesNotExist()
        assertThat(analytics.events).containsAtLeast("set_added", "set_deleted", "set_deleted").inOrder()

        // An exercise added at the end, which may be below the screen
        key("Add exercise").performClick()
        compose.onNodeWithTag(WorkoutLogTags.Table).performScrollToNode(hasText("Squat"))
        compose.onNodeWithText("Squat").assertExists()

        // Deleting its last set takes the exercise with it, and Undo brings it back to stay
        showExercise(2)
        row(2, 1, 0).performTouchInput { swipeLeft() }
        deleteSet(2, 1, 0)
        row(2, 0, 0).performTouchInput { swipeLeft() }
        deleteSet(2, 0, 0)
        compose.onNodeWithText("Squat").assertDoesNotExist()
        compose.onNodeWithText("Undo").performClick()
        compose.mainClock.advanceTimeBy(2_000)
        showExercise(2)
        row(2, 0, 0).assertExists()

        // Its name opens its info, its menu its history, then deletes it after asking
        compose.onNodeWithText("Squat").performClick()
        assertThat(opened).isEqualTo("Squat" to ExerciseTab.Info)
        compose.onNodeWithTag(WorkoutLogTags.menu(2)).performClick()
        compose.onNode(hasText("History") and hasAnyAncestor(isPopup())).performClick()
        assertThat(opened).isEqualTo("Squat" to ExerciseTab.History)
        compose.onNodeWithTag(WorkoutLogTags.menu(2)).performClick()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isPopup())).performClick()
        compose.onNodeWithText("Delete Squat?").assertExists()
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText("Squat").assertDoesNotExist()

        // The workout's options: the rest after each set up by 10s, then Edit workout to drag bench press below the
        // superset by its grip, and Done
        compose.onNodeWithContentDescription("Workout options").performClick()
        compose.onNodeWithText("Rest timer").performClick()
        compose.onNodeWithContentDescription("More rest").performClick()
        compose.onNode(hasText("Done") and hasAnyAncestor(isDialog())).performClick()
        assertThat(analytics.events).contains("rest_changed 70")
        compose.onNodeWithContentDescription("Workout options").performClick()
        compose.onNodeWithText("Edit workout").performClick()
        compose.onNodeWithTag(WorkoutLogTags.editGrip(0)).performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 4f), delayMillis = 200)
            up()
        }
        compose.onNode(hasText("Fly", substring = true) and hasAnyAncestor(hasTestTag(WorkoutLogTags.editCard(0)))).assertExists()
        compose.onNode(hasText("Done") and hasAnyAncestor(hasTestTag(WorkoutLogTags.EditFooter))).performClick()
        compose.onNodeWithTag(WorkoutLogTags.EditList).assertDoesNotExist()

        // Finish asks, as sets aren't ticked, then saves the workout as it is
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog()).not()).performClick()
        compose.onNodeWithText("Finish workout?").assertExists()
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()

        val finished = store.finished!!
        val bench = finished.getExerciseGroups()[1].exercises[0].sets
        assertThat(bench[0].getReps()).isEqualTo(10)
        assertThat(bench[0].getWeightKg()).isWithin(0.01f).of(61.23f)
        assertThat(bench[0].getCompletedDate()).isEqualTo(now)
        assertThat(bench[1].getTimeSeconds()).isEqualTo(110)
        assertThat(finished.getExerciseGroups().map { it.exercises.first().exercise?.name }).containsExactly("Fly", "Bench press").inOrder()
        // Which screen it ran on stays on the device
        assertThat(finished.asMap().keys).doesNotContain("workoutScreenV2")
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
        assertThat(exit).isEqualTo(WorkoutLogExit.Finished(finished))
        assertThat(analytics.events.last()).isEqualTo("workout_completed")
    }

    @Test
    fun changingSetSuggestionsMidWorkout() {
        val viewModel = viewModel(template())
        compose.setContent {
            Theme {
                WorkoutLogScreen(
                    viewModel = viewModel,
                    onAddExercise = {},
                    onOpenExercise = { _, _ -> },
                    onOpenSetSuggestions = { setSuggestionsOpened++ },
                    onExit = { exit = it },
                )
            }
        }

        // The suggested weight's note opens Set suggestions with Change
        cell(0, 0, 0, "weight").performClick()
        compose.onNodeWithTag(WorkoutLogTags.SuggestionNote).assertExists()
        compose.onNodeWithText("✦ Suggested from last time").assertExists()
        compose.onNode(hasText("Change") and hasAnyAncestor(hasTestTag(WorkoutLogTags.SuggestionNote))).performClick()
        assertThat(setSuggestionsOpened).isEqualTo(1)
        assertThat(analytics.events).contains("set_suggestions_opened")
        compose.onNodeWithContentDescription("Hide keypad").performClick()

        // So does Workout options, which shows what fills sets in
        compose.onNodeWithContentDescription("Workout options").performClick()
        compose.onNode(hasTestTag(WorkoutLogTags.SetSuggestionsOption) and hasText("From last time", substring = true)).performClick()
        // It opens once the sheet has slid away, which can take a while on a slow emulator
        compose.waitUntil(10_000L) { setSuggestionsOpened == 2 }

        // Turned off there, the suggested values go once the workout shows again; the targets stay
        settings.goal = MuscleGoal.NONE
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.onResume() }
        compose.waitUntil(5_000L) { compose.onAllNodes(hasText("✦")).fetchSemanticsNodes().isEmpty() }
        cell(1, 0, 0, "count").assertTextEquals("12")
    }

    @Test
    fun leavingAWorkout() {
        // An empty workout: nothing to lose, so Discard just goes
        val empty = ELWorkout(uuid = "empty", createdDate = now, routine = ELRoutine(name = "Quick"))
        var viewModel by mutableStateOf(viewModel(empty))
        compose.setContent {
            Theme { WorkoutLogScreen(viewModel = viewModel, onAddExercise = {}, onOpenExercise = { _, _ -> }, onOpenSetSuggestions = {}, onExit = { exit = it }) }
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

        // Discard throws the done sets away too, after the system back button asks
        exit = null
        store.finished = null
        viewModel = viewModel(template())
        compose.waitForIdle()
        tick(0, 0, 0).performClick()
        Espresso.pressBack()
        compose.onNodeWithText("You've completed 1 set.", substring = true).assertExists()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForIdle()
        assertThat(exit).isEqualTo(WorkoutLogExit.Discarded)
        assertThat(store.finished).isNull()
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
    }

    private fun viewModel(launched: ELWorkout): WorkoutLogViewModel {
        val loaded = WorkoutLogViewModel.load(launched, store)
        return WorkoutLogViewModel(
            dispatcher = Dispatchers.Main,
            workout = loaded.workout,
            resumed = loaded.resumed,
            fromPlan = false,
            repository = store,
            settings = settings,
            analytics = analytics,
            alerts = object : WorkoutLogAlerts {
                override fun timerRanOut() = Unit
            },
            notification = notification,
            loadPrefillSources = BenchHistory,
            prefill = RealWorkoutSetPrefill { settings.goal.canPrefill() },
            clock = { now },
            tickMillis = null,
        )
    }

    // Bench press, 2 sets with no reps target, then a superset of fly and raise, 2 rounds of 12
    private fun template() = ELWorkout(
        uuid = "workout",
        createdDate = now,
        routine = ELRoutine(
            name = "Push",
            exerciseGroups = mutableListOf(
                group(ELSetType.SINGLE, "Bench press" to 0),
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

    // Scrolls an exercise to the top of the table, clear of the keypad over its bottom, on any screen size
    private fun showExercise(block: Int) {
        compose.onNodeWithTag(WorkoutLogTags.Table).performScrollToIndex(block)
    }

    private fun row(block: Int, round: Int, exercise: Int) = compose.onNodeWithTag(WorkoutLogTags.set(block, round, exercise))

    private fun deleteSet(block: Int, round: Int, exercise: Int) =
        compose.onNodeWithTag(WorkoutLogTags.delete(WorkoutLogTags.set(block, round, exercise))).performClick()

    private fun tick(block: Int, round: Int, exercise: Int) =
        compose.onNodeWithTag(WorkoutLogTags.tick(WorkoutLogTags.set(block, round, exercise)))

    // A key or button on the docked keypad, rather than the same text in the table
    private fun key(text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(WorkoutLogTags.Keypad)))

    // A notification button, delivered on the main thread like WorkoutService's local broadcast, with the set the
    // notification shows
    private fun notificationButton(receiver: WorkoutLogNotificationReceiver, action: String) {
        val intent = Intent(action).putExtra(ELConstants.EXTRA_WORKOUT_STATE, notification.shownSet?.position)
        compose.runOnUiThread { receiver.onReceive(InstrumentationRegistry.getInstrumentation().targetContext, intent) }
        compose.waitForIdle()
    }

    private fun keys(digits: String) = digits.forEach { key(it.toString()).performClick() }

    /**
     * The ongoing workout as the app keeps it, on this device. The finished one is kept here instead of Firestore.
     */
    private class DeviceStore : OngoingWorkoutRepository {
        var finished: ELWorkout? = null

        override fun savedCopyOf(workout: ELWorkout): ELWorkout? = WorkoutManager.manager.savedCopyOf(workout)

        override fun saveOngoing(workout: ELWorkout) = WorkoutManager.manager.setOngoingWorkout(workout)

        override fun clearOngoing() = WorkoutManager.manager.clearOngoingWorkout()

        override fun saveFinished(workout: ELWorkout, fromPlan: Boolean) {
            finished = workout
        }
    }

    /**
     * What the notification is sent, in place of WorkoutService.
     */
    private class RecordingNotification : WorkoutLogNotification {
        // The workout it was last sent, and the set in it to show
        var shown: ELWorkout? = null
        var shownSet: ShownSet? = null

        override fun start(workout: ELWorkout, shown: ShownSet?) {
            this.shown = workout
            shownSet = shown
        }

        override fun stop() = Unit

        override fun update(workout: ELWorkout, shown: ShownSet?) {
            this.shown = workout
            shownSet = shown
        }

        override fun showRest(remainingPercent: Int, remainingSeconds: Int) = Unit

        // The exercise of the set it shows
        fun shownSetName(): String? {
            val workout = shown ?: return null
            val position = shownSet?.position ?: return null
            return workout.getExerciseGroups()[position.groupIndex].getExercisesForSetIndex(position.setIndex)[position.exerciseIndex].getName()
        }
    }

    /**
     * The user's history: bench press last done at 8 × 100 lb for two sets. Sets are filled in from it by the old
     * screen's prefill, as on the device.
     */
    private object BenchHistory : LoadPrefillSourcesUseCase {
        override suspend fun invoke(exercises: List<ELExercise>): Either<Throwable, Map<String, PrefillSource>> {
            val bench = exercises.firstOrNull { it.uuid == "Bench press" } ?: return emptyMap<String, PrefillSource>().right()
            val sets = MutableList(2) { ELSet(reps = 8, weight = UnitUtils.lbToKg(100f)) }
            val last = ELExerciseHistory(ELRoutineExercise(bench.uuid, bench, sets), ELWorkout())
            return mapOf(bench.uuid!! to PrefillSource(last, orm = 0f)).right()
        }
    }

    private class PoundSettings : WorkoutLogSettings {
        var goal = MuscleGoal.HISTORY

        override fun weightUnit() = WeightUnit.POUND

        override fun weightIncrease() = 5f

        override fun keepScreenOn() = false

        override fun muscleGoal() = goal
    }


    private class RecordingAnalytics : WorkoutLogAnalytics {
        val events = mutableListOf<String>()

        override fun screenViewed() = Unit

        override fun setCompleted(type: ELSetType?, timed: Boolean, fromNotification: Boolean) {
            events += "set_completed $type" + if (fromNotification) " notification" else ""
        }

        override fun setGroupCompleted(type: ELSetType?, exercises: Int, fromNotification: Boolean) {
            events += "set_group_completed $type $exercises"
        }

        override fun setEdited(type: ELSetType?, timed: Boolean) {
            events += "set_edited $type"
        }

        override fun exercisesAdded() {
            events += "exercises_added"
        }

        override fun setAdded() {
            events += "set_added"
        }

        override fun setDeleted() {
            events += "set_deleted"
        }

        override fun restChanged(seconds: Int) {
            events += "rest_changed $seconds"
        }

        override fun setSuggestionsOpened() {
            events += "set_suggestions_opened"
        }

        override fun valueChanged(field: ValueField, value: Float?) {
            events += "${field.name.lowercase()}_modified $value"
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
