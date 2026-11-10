package com.everlog.android.home.workoutstab

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.everlog.android.common.composetest.FeatureTest
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.kotlin.Load
import com.everlog.android.home.data.workouts.TemplateHistory
import com.everlog.android.home.data.workouts.WorkoutsTabLists
import com.everlog.android.plan.data.PlanWithRoutines
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.testfixtures.FakePlanProgress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.threeten.bp.LocalDate

/**
 * The Workouts tab driven as a user would, with its real view-model over fakes: the user's templates and what's in
 * them, the card on top for each case (a suggestion, the plan's next day, where to start, getting started), plans,
 * and loading, failing and the history arriving late.
 */
class WorkoutsTabFeatureTest : FeatureTest() {

    private val lists = FakeLists()
    private val recent = FakeHistory()
    private val planProgress = FakePlanProgress()
    private val analytics = FakeAnalytics()
    private val opened = mutableListOf<String>()
    private lateinit var viewModel: WorkoutsTabViewModel

    private val push = routine(
        "push", "Push day", createdDate = 2,
        group(exercise("bench", "Bench press", reps = listOf(8, 8, 8))),
        group(exercise("ohp", "Overhead press", reps = listOf(12, 10, 8)), exercise("raise", "Lateral raise", reps = listOf(15, 15, 15)), type = ELSetType.SUPER),
    )
    private val pull = routine("pull", "Pull day", createdDate = 1, group(exercise("row", "Barbell row", reps = listOf(6, 6, 6, 6))))
    private val legs = routine("legs", "Legs", createdDate = 3, group(exercise("plank", "Plank", seconds = listOf(30, 45))))
    private val rotation = history("push", "pull", "legs", "push", "pull", "legs", "push", "pull", "legs", "push")

    // Main journeys

    @Test
    fun aTemplateOpensToItsExercisesAndStarts() {
        show(templates = listOf(push, pull, legs), history = rotation)

        compose.onNodeWithText("Push day").performClick()
        // A single exercise with its sets, then a superset under its type, lettered
        compose.waitUntil(WaitMillis) { shows("3 × 8") }
        compose.onNodeWithText("SUPER SET").assertExists()
        compose.onNodeWithText("3 × 8–12").assertExists()
        compose.onNodeWithText("Lateral raise").assertExists()

        compose.onNodeWithTag(WorkoutsTabTags.templateStart("push")).performScrollTo().performClick()
        compose.onNodeWithTag(WorkoutsTabTags.templateDetails("push")).performClick()
        assertThat(opened).containsExactly("start push", "details push").inOrder()
        assertThat(analytics.starts.single()).isEqualTo(Triple("template", "suggested", false))
    }

    @Test
    fun theSuggestedNextTemplateStartsFromTheCard() {
        show(templates = listOf(push, pull, legs), history = rotation)

        compose.onNodeWithText("✦ Suggested next").assertExists()
        compose.onNodeWithText("You've done it after Push day 3 of the last 3 times.").assertExists()
        compose.onNodeWithText("Start").performClick()

        assertThat(opened).containsExactly("start pull")
        assertThat(analytics.starts.single()).isEqualTo(Triple("card", "suggested", true))
    }

    @Test
    fun thePlansNextDayComesFirstAndStartsFromThePlan() {
        planProgress.plan = plan("strength", push, legs, done = 0)
        show(templates = listOf(push, pull, legs), history = rotation, plans = listOf(planProgress.plan!!))

        compose.onNodeWithText("Next in your plan").assertExists()
        compose.onNodeWithText("Strength block · week 1, day 1").assertExists()
        compose.onNodeWithText("Start").performClick()
        assertThat(opened).containsExactly("start push from the plan")

        // The plan being followed is its cover card, and its templates say they're in it
        compose.onNodeWithTag(WorkoutsTabTags.ActivePlan).performScrollTo()
        compose.onNodeWithText("Active").assertExists()
        compose.onNodeWithText("Week 1 of 1 · 0 of 2 this week").assertExists()
        assertThat(anyInPlan()).isTrue()
    }

    // Options

    @Test
    fun noTemplatesGetsStartedWithAnExample() {
        show(templates = emptyList(), history = history())

        compose.onNodeWithText("Save the workouts you repeat").assertExists()
        compose.onNodeWithText("EXAMPLE · NOT YOUR DATA", ignoreCase = true).assertExists()
        compose.onNodeWithText("New template").performClick()
        compose.onNodeWithText("Start empty").performClick()
        assertThat(opened).containsExactly("new template", "start empty").inOrder()
        compose.onNodeWithText("Your plans").assertDoesNotExist()
    }

    @Test
    fun templatesButNothingDoneLatelyStartWithTheFirstSaved() {
        show(templates = listOf(push, pull, legs), history = history())

        compose.onNodeWithText("Start here").assertExists()
        compose.onNodeWithText("Start").performClick()
        assertThat(opened).containsExactly("start pull")
    }

    @Test
    fun plansAndExercisesOpen() {
        val other = plan("cut", push, legs).apply { name = "Summer cut" }
        show(templates = listOf(push), history = history(), plans = listOf(other))

        compose.onNodeWithText("Exercises").performClick()
        compose.onNodeWithText("Summer cut").performScrollTo().performClick()
        compose.onNodeWithText("1 week · 2 days a week").assertExists()
        assertThat(opened).containsExactly("exercises", "plan cut").inOrder()
    }

    @Test
    fun noPlansSaysSo() {
        show(templates = listOf(push), history = history())

        compose.onNodeWithText("No plans yet").performScrollTo().assertExists()
    }

    // Edge cases

    @Test
    fun loadingThenTheLists() {
        show(templates = null, history = null)
        compose.onNodeWithText("Your templates").assertDoesNotExist()

        lists.lists.value = Load.Ready(WorkoutsTabLists(listOf(push), emptyList()))

        compose.waitUntil(WaitMillis) { shows("Your templates") }
    }

    @Test
    fun listsThatCantLoadOfferToTryAgain() {
        show(templates = null, history = null)
        lists.lists.value = Load.Unavailable(IllegalStateException("offline"))

        compose.waitUntil(WaitMillis) { shows("Couldn't load your templates") }
        compose.onNodeWithTag(WorkoutsTabTags.Retry).performClick()
        lists.lists.value = Load.Ready(WorkoutsTabLists(listOf(push), emptyList()))

        compose.waitUntil(WaitMillis) { shows("Push day") }
        assertThat(lists.reads).isEqualTo(2)
    }

    @Test
    fun theCardArrivesWithTheHistoryAfterTheLists() {
        show(templates = listOf(push, pull, legs), history = null)
        compose.onNodeWithText("✦ Suggested next").assertDoesNotExist()
        // Nothing says when they were last done until the history is there
        compose.onNodeWithText("Not done recently").assertDoesNotExist()

        recent.history.value = Load.Ready(rotation)

        compose.waitUntil(WaitMillis) { shows("✦ Suggested next") }
    }

    @Test
    fun aTemplateWithoutExercisesStillShowsAndOpens() {
        val empty = routine("empty", "Empty template", createdDate = 4)
        show(templates = listOf(empty), history = history())

        compose.onNodeWithTag(WorkoutsTabTags.template("empty")).performScrollTo().performClick()
        compose.onNodeWithTag(WorkoutsTabTags.templateStart("empty")).performScrollTo().assertExists()
    }

    private fun show(
        templates: List<ELRoutine>?,
        history: TemplateHistory?,
        plans: List<ELPlan> = emptyList(),
    ) {
        templates?.let { lists.lists.value = Load.Ready(WorkoutsTabLists(it, plans.map { plan -> PlanWithRoutines(plan, emptyMap()) })) }
        history?.let { recent.history.value = Load.Ready(it) }
        viewModel = WorkoutsTabViewModel(Dispatchers.Main, lists, recent, planProgress, analytics) { LocalDate.of(2026, 11, 10) }
        setContent {
            WorkoutsTabScreen(
                viewModel = viewModel,
                actions = WorkoutsTabActions(
                    showExercises = { opened += "exercises" },
                    showTemplate = { opened += "details ${it.uuid}" },
                    start = { opened += "start ${it.routine.uuid}" + if (it.fromPlan) " from the plan" else "" },
                    startEmpty = { opened += "start empty" },
                    newTemplate = { opened += "new template" },
                    showPlan = { opened += "plan $it" },
                ),
            )
        }
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun anyInPlan() = compose.onAllNodes(hasText("In your plan", substring = true)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L

        fun exercise(uuid: String, name: String, reps: List<Int> = emptyList(), seconds: List<Int> = emptyList()) = ELRoutineExercise(
            uuid,
            ELExercise(uuid = uuid, name = name),
            (reps.map { ELSet().apply { updateRequiredReps(it) } } + seconds.map { ELSet().apply { updateRequiredTimeSeconds(it) } }).toMutableList(),
        )

        fun group(vararg exercises: ELRoutineExercise, type: ELSetType = ELSetType.SINGLE) =
            ELExerciseGroup(uuid = exercises.first().uuid, type = type.name, exercises = exercises.toMutableList())

        fun routine(uuid: String, name: String, createdDate: Long, vararg groups: ELExerciseGroup) =
            ELRoutine(uuid = uuid, name = name, exerciseGroups = groups.toMutableList(), createdDate = createdDate)
    }
}
