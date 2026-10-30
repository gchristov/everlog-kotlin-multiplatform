package com.everlog.ui.activities.home

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.constants.ELConstants
import com.everlog.managers.WorkoutManager
import com.everlog.testutil.E2EAccount
import com.everlog.ui.activities.home.workout.WorkoutActivity
import com.everlog.ui.activities.workoutlog.WorkoutLogActivity
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
 * Opening the app from the launcher while a workout is ongoing, as after swiping it away mid-workout: the real launch
 * through splash and home, signed in with the E2E test account. Run it on an emulator, not on a phone mid-workout, as
 * it replaces the app's ongoing workout.
 */
@RunWith(AndroidJUnit4::class)
class OngoingWorkoutLaunchTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(GrantPermissionRule.grant(*permissions()))
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val monitors = mutableListOf<Instrumentation.ActivityMonitor>()

    @Before
    fun setUp() {
        E2EAccount.signIn()
        WorkoutManager.manager.clearOngoingWorkout()
    }

    @After
    fun tearDown() {
        WorkoutManager.manager.clearOngoingWorkout()
        instrumentation.runOnMainSync { resumedActivities().forEach(Activity::finish) }
        monitors.forEach(instrumentation::removeMonitor)
    }

    @Test
    fun theOngoingWorkoutOpensWithItsSetsAndNotification() {
        val workout = workout(screenV2 = true)
        WorkoutManager.manager.setOngoingWorkout(workout)
        val workoutScreen = watch(WorkoutLogActivity::class.java)
        val home = watch(HomeActivity::class.java)

        launchFromLauncher()

        val opened = awaitOpened(workoutScreen)
        assertThat((opened.intent.getSerializableExtra(ELConstants.EXTRA_WORKOUT) as ELWorkout).uuid).isEqualTo(workout.uuid)
        assertThat(home.hits).isEqualTo(1)
        compose.onNodeWithText("Bench press").assertExists()
        compose.waitUntil(TIMEOUT) { workoutNotificationShows() }
        assertThat(WorkoutManager.manager.ongoingWorkout()?.getCompletedSetsCount()).isEqualTo(1)

        // Discarding goes back to home, which doesn't open the workout again
        Espresso.pressBack()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        compose.waitUntil(TIMEOUT) { opened.isDestroyed }
        compose.waitUntil(TIMEOUT) { home.lastActivity?.let(::isResumed) == true }
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNull()
        assertThat(workoutScreen.hits).isEqualTo(1)
        compose.waitUntil(TIMEOUT) { !workoutNotificationShows() }
    }

    @Test
    fun aWorkoutStartedOnTheOldScreenOpensThere() {
        WorkoutManager.manager.setOngoingWorkout(workout(screenV2 = false))
        val oldScreen = watch(WorkoutActivity::class.java)
        val newScreen = watch(WorkoutLogActivity::class.java)

        launchFromLauncher()

        awaitOpened(oldScreen)
        assertThat(newScreen.hits).isEqualTo(0)
    }

    @Test
    fun withoutAnOngoingWorkoutTheAppOpensOnHome() {
        val home = watch(HomeActivity::class.java)
        val workoutScreens = listOf(watch(WorkoutLogActivity::class.java), watch(WorkoutActivity::class.java))

        launchFromLauncher()

        compose.waitUntil(TIMEOUT) { home.lastActivity?.let(::isResumed) == true }
        assertThat(workoutScreens.sumOf { it.hits }).isEqualTo(0)
    }

    @Test
    fun homeShownAgainWithTheWorkoutOngoingPromptsInsteadOfLooping() {
        // The workout screen opened from home closing without ending the workout, e.g. if it ever couldn't load
        WorkoutManager.manager.setOngoingWorkout(workout(screenV2 = true))
        val workoutScreen = watch(WorkoutLogActivity::class.java)
        val home = watch(HomeActivity::class.java)
        launchFromLauncher()
        val opened = awaitOpened(workoutScreen)

        instrumentation.runOnMainSync { opened.finish() }

        compose.waitUntil(TIMEOUT) { home.lastActivity?.let(::isResumed) == true }
        Espresso.onView(withText("Resume Workout")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
        assertThat(workoutScreen.hits).isEqualTo(1)
        assertThat(WorkoutManager.manager.ongoingWorkout()).isNotNull()
    }

    // As the launcher does, in a task of its own
    private fun launchFromLauncher() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    private fun watch(activity: Class<out Activity>) =
        instrumentation.addMonitor(activity.name, null, false).also { monitors += it }

    private fun awaitOpened(monitor: Instrumentation.ActivityMonitor): Activity {
        compose.waitUntil(LAUNCH_TIMEOUT) { monitor.lastActivity?.let(::isResumed) == true }
        return monitor.lastActivity
    }

    private fun isResumed(activity: Activity): Boolean {
        var resumed = false
        instrumentation.runOnMainSync { resumed = activity in resumedActivities() }
        return resumed
    }

    private fun resumedActivities(): Collection<Activity> =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)

    private fun workoutNotificationShows(): Boolean =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.packageName == context.packageName }

    private fun workout(screenV2: Boolean) = ELWorkout(
        uuid = "e2e-workout-${System.currentTimeMillis()}",
        createdDate = System.currentTimeMillis(),
        routine = ELRoutine(name = "E2E", exerciseGroups = mutableListOf(
            ELExerciseGroup(
                uuid = "group-e2e-bench",
                type = ELSetType.SINGLE.name,
                exercises = mutableListOf(ELRoutineExercise("e2e-bench", ELExercise(uuid = "e2e-bench", name = "Bench press"), mutableListOf(
                    ELSet(requiredReps = 8, reps = 8, completedDate = System.currentTimeMillis()),
                    ELSet(requiredReps = 8),
                ))),
            ),
        )),
    ).apply { workoutScreenV2 = screenV2 }

    private companion object {
        const val TIMEOUT = 10_000L

        // Splash waits for the signed-in user before opening home
        const val LAUNCH_TIMEOUT = 30_000L

        fun permissions(): Array<String> = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.toTypedArray()
    }
}
