package com.everlog.ui.activities.home

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.history.ui.R as HistoryR
import com.everlog.android.history.ui.page.HistoryPageTags
import com.everlog.android.home.screen.HomeActivity
import com.everlog.android.home.screen.HomeTab
import com.everlog.android.home.screen.HomeTags
import com.everlog.android.home.weektab.WeekTabTags
import com.everlog.android.home.workoutstab.WorkoutsTabTags
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.android.workout.data.WorkoutManager
import com.everlog.android.workout.log.WorkoutLogTags
import com.everlog.android.workoutbuilder.ui.exercisetable.ExerciseTableTags
import com.everlog.application.ELApplication
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.sync.AppDataSync
import com.everlog.testutil.E2EAccount
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
import org.threeten.bp.LocalDate

/**
 * The app's core loop end to end, through the real Compose Home and every screen on the way, signed in with the E2E
 * account: a template started from the Workouts tab, an exercise added from the real exercise picker, a set logged,
 * the workout finished and saved to Firestore, then counted on the Week tab and listed in the history. Each screen's
 * own behaviour is covered by its feature tests. The template and the workout are deleted afterwards.
 */
@RunWith(AndroidJUnit4::class)
class CoreLoopTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(5, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(GrantPermissionRule.grant(*permissions()))
        .around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val templateId = "e2e-core-loop-${System.currentTimeMillis()}"
    private val template = ELRoutine(
        uuid = templateId,
        name = "E2E core loop",
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
    private var workout: ELWorkout? = null

    @Before
    fun setUp() {
        E2EAccount.signIn()
        WorkoutManager.manager.clearOngoingWorkout()
        runBlocking {
            RealRoutineRepository(Dispatchers.IO, AppDataSync.instance, ELApplication.graph.userSession) { ELDatastore.routineStore() }
                .saveRoutines(listOf(template))
        }
    }

    @After
    fun tearDown() {
        WorkoutManager.manager.clearOngoingWorkout()
        ELDatastore.routineStore().delete(template)
        workout?.let { ELDatastore.workoutStore().delete(it) }
    }

    @Test
    fun aTemplateStartedFromHomeIsLoggedAndShowsInTheWeekAndHistory() {
        ActivityScenario.launch(HomeActivity::class.java)
        compose.waitUntil(LoadMillis) { exists(hasTestTag(WeekTabTags.Workouts)) || exists(hasTestTag(WeekTabTags.Empty)) }
        val workoutsBefore = workoutsThisWeek()

        // The template, opened on the Workouts tab, starts the workout
        compose.onNodeWithTag(HomeTags.tab(HomeTab.Workouts)).performClick()
        compose.waitUntil(LoadMillis) { exists(hasTestTag(WorkoutsTabTags.template(templateId))) }
        compose.onNodeWithTag(WorkoutsTabTags.template(templateId)).performScrollTo().performClick()
        compose.onNodeWithTag(WorkoutsTabTags.templateStart(templateId)).performScrollTo().performClick()
        compose.waitUntil(WaitMillis) { exists(hasTestTag(ExerciseTableTags.Table)) }

        // An exercise from the library, through the picker
        compose.onNode(hasText("Add exercise") and hasAnyAncestor(hasTestTag(WorkoutLogTags.Keypad))).performClick()
        compose.waitUntil(LoadMillis) { exists(hasSetTextAction() and isEnabled()) }
        compose.onNode(hasSetTextAction()).run {
            performTextInput("barbell bench press")
            performImeAction()
        }
        compose.waitUntil(WaitMillis) { exists(hasText(PickedExercise)) }
        compose.onNodeWithText(PickedExercise).performClick()
        compose.onNodeWithText("Add 1 exercise").performClick()
        compose.waitUntil(WaitMillis) { exists(hasTestTag(ExerciseTableTags.Table)) }
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToNode(hasText(PickedExercise))

        // A set logged, then finished, which asks first as the other sets aren't ticked
        compose.onNodeWithTag(ExerciseTableTags.Table).performScrollToNode(hasTestTag(ExerciseTableTags.tick(ExerciseTableTags.set(0, 0, 0))))
        compose.onNodeWithTag(ExerciseTableTags.tick(ExerciseTableTags.set(0, 0, 0))).performClick()
        compose.waitUntil(WaitMillis) { WorkoutManager.manager.ongoingWorkout()?.let { workout = it } != null }
        compose.onNode(hasText("Finish") and !hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(WaitMillis) { exists(hasText("Finish") and hasAnyAncestor(isDialog())) }
        compose.onNode(hasText("Finish") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(WaitMillis) { WorkoutManager.manager.ongoingWorkout() == null }

        // Its details, then back on Home
        Espresso.pressBack()
        compose.waitUntil(LoadMillis) { exists(hasTestTag(HomeTags.tab(HomeTab.Week))) }
        compose.onNodeWithTag(HomeTags.tab(HomeTab.Week)).performClick()
        compose.waitUntil(LoadMillis) { workoutsThisWeek() == workoutsBefore + 1 }

        // Today in the history's list
        compose.onNodeWithTag(HomeTags.tab(HomeTab.Activity)).performClick()
        compose.onNodeWithText(context.getString(DesignR.string.history_title)).performClick()
        compose.waitUntil(LoadMillis) { exists(hasTestTag(HistoryPageTags.List)) || exists(hasTestTag(HistoryPageTags.Calendar)) }
        if (exists(hasTestTag(HistoryPageTags.Calendar))) {
            compose.onNodeWithText(context.getString(HistoryR.string.view_type_calendar)).performClick()
            compose.onNodeWithText(context.getString(HistoryR.string.view_type_list)).performClick()
        }
        compose.waitUntil(LoadMillis) {
            exists(hasText(template.name!!) and hasAnyAncestor(hasTestTag(HistoryPageTags.day(LocalDate.now()))))
        }
    }

    // The Week tab's count of workouts done, which shows -- for none
    private fun workoutsThisWeek(): Int {
        if (!exists(hasTestTag(WeekTabTags.Workouts))) return 0
        val texts = compose.onNodeWithTag(WeekTabTags.Workouts).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text).orEmpty()
        return texts.firstNotNullOfOrNull { it.text.toIntOrNull() } ?: 0
    }

    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        // In the dev library, which the debug build reads
        const val PickedExercise = "Barbell Bench Press"
        // A first read on a new emulator syncs the account's last few months first
        const val LoadMillis = 60_000L
        const val WaitMillis = 10_000L

        fun permissions(): Array<String> = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.toTypedArray()
    }
}
