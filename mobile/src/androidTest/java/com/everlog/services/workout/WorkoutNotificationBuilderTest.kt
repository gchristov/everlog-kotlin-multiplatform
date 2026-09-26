package com.everlog.services.workout

import android.app.Notification
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.R
import com.everlog.data.model.set.ELSet
import com.everlog.managers.preferences.SettingsManager
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.services.workout.WorkoutNotificationState.Done
import com.everlog.services.workout.WorkoutNotificationState.NextSet
import com.everlog.services.workout.WorkoutNotificationState.NoExercises
import com.everlog.services.workout.WorkoutNotificationState.Rest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders each [WorkoutNotificationState] and checks what the system will be given. Runs on a device
 * because rendering needs real resources. Derivation of the state itself is covered by the JVM
 * WorkoutNotificationStateTest.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutNotificationBuilderTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val builder = WorkoutNotificationBuilder(context)
    private val channelId = "test_channel"

    private lateinit var originalWeightUnit: WeightUnit

    @Before
    fun setUp() {
        originalWeightUnit = SettingsManager.manager.weightUnit()
        SettingsManager.manager.setWeightUnit(WeightUnit.KILOGRAM)
    }

    @After
    fun tearDown() {
        SettingsManager.manager.setWeightUnit(originalWeightUnit)
    }

    // Common

    @Test
    fun every_state_is_an_ongoing_silent_notification() {
        listOf(nextSet(), rest(), Done, NoExercises).forEach { state ->
            val notification = build(state)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Channels don't exist before API 26
                assertThat(NotificationCompat.getChannelId(notification)).isEqualTo(channelId)
            }
            assertThat(notification.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
            assertThat(notification.flags and Notification.FLAG_ONLY_ALERT_ONCE).isNotEqualTo(0)
            assertThat(notification.flags and Notification.FLAG_AUTO_CANCEL).isEqualTo(0)
            assertThat(notification.defaults).isEqualTo(0)
            assertThat(notification.contentIntent).isNotNull()
        }
    }

    @Test
    fun rebuilding_reuses_the_same_pending_intents() {
        val first = build(nextSet())
        val second = build(nextSet(reps = 9))

        assertThat(second.contentIntent).isEqualTo(first.contentIntent)
    }

    @Test
    fun pending_intents_are_immutable() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

        val notification = build(rest())

        assertThat(notification.contentIntent.isImmutable).isTrue()
        assertThat(notification.actions.single().actionIntent.isImmutable).isTrue()
    }

    // Next set

    @Test
    fun next_set_uses_the_decorated_custom_layouts() {
        // Below API 24 NotificationCompat builds the decoration itself, so the layouts are nested
        // inside its own template rather than set directly.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

        val notification = build(nextSet())

        assertThat(notification.extras.getString(Notification.EXTRA_TEMPLATE))
                .isEqualTo(Notification.DecoratedCustomViewStyle::class.java.name)
        assertThat(notification.contentView.layoutId).isEqualTo(R.layout.notification_workout_set)
        assertThat(notification.bigContentView.layoutId).isEqualTo(R.layout.notification_workout_set_big)
    }

    @Test
    fun next_set_title_is_the_exercise() {
        val notification = build(nextSet())

        assertThat(title(notification)).isEqualTo("Bench press")
    }

    // The values match a completed set's row in the workout screen (ELSet.getExerciseSetSummary)

    @Test
    fun next_set_text_summarises_reps_and_weight() {
        val notification = build(nextSet())

        assertThat(text(notification)).isEqualTo("Set 2/3 • 8 x 60 kg")
    }

    @Test
    fun next_set_text_shows_reps_without_weight() {
        val notification = build(nextSet(weight = 0f))

        assertThat(text(notification)).isEqualTo("Set 2/3 • 8 reps")
    }

    @Test
    fun next_set_text_uses_singular_rep() {
        val notification = build(nextSet(weight = 0f, reps = 1))

        assertThat(text(notification)).isEqualTo("Set 2/3 • 1 rep")
    }

    @Test
    fun next_set_text_shows_time_for_timed_sets() {
        val notification = build(nextSet(weight = 0f, reps = null, timeSeconds = 40))

        assertThat(text(notification)).isEqualTo("Set 2/3 • 40 sec")
    }

    @Test
    fun next_set_text_shows_time_and_weight_for_weighted_timed_sets() {
        val notification = build(nextSet(weight = 20f, reps = null, timeSeconds = 90))

        assertThat(text(notification)).isEqualTo("Set 2/3 • 1 min 30 sec • 20 kg")
    }

    @Test
    fun next_set_text_counts_down_while_the_exercise_timer_runs() {
        val notification = build(nextSet(weight = 0f, reps = null, timeSeconds = 40, remainingSeconds = 12))

        assertThat(text(notification)).isEqualTo("Set 2/3 • 12 sec")
    }

    @Test
    fun next_set_shows_the_total_for_a_single_set() {
        val notification = build(nextSet(setNumber = 1, totalSets = 1, weight = 0f))

        assertThat(text(notification)).isEqualTo("Set 1/1 • 8 reps")
    }

    @Test
    fun next_set_without_data_prompts_to_edit() {
        val notification = build(nextSet(setNumber = 1, totalSets = 1, weight = 0f, reps = null))

        assertThat(text(notification)).isEqualTo("Set 1/1 • ${context.getString(R.string.workout_notification_tap_to_edit)}")
    }

    @Test
    fun super_set_shows_exercise_position_and_set_type() {
        val notification = build(nextSet(exerciseName = "Squat", exercisePosition = 2, exercisesInGroup = 2, setType = "SUPER"))

        assertThat(title(notification)).isEqualTo("2/2 Squat")
        assertThat(text(notification)).isEqualTo("${context.getString(R.string.set_super)} 2/3 • 8 x 60 kg")
    }

    // Rest

    @Test
    fun rest_uses_the_standard_template_with_progress() {
        val notification = build(rest(remainingSeconds = 45, remainingPercent = 75))

        assertThat(notification.contentView).isNull()
        assertThat(title(notification)).isEqualTo("Rest • 00:45")
        assertThat(notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS_MAX)).isEqualTo(100)
        assertThat(notification.extras.getInt(NotificationCompat.EXTRA_PROGRESS)).isEqualTo(75)
    }

    @Test
    fun rest_can_be_skipped() {
        val notification = build(rest())

        assertThat(notification.actions.single().title.toString())
                .isEqualTo(context.getString(R.string.workout_notification_skip_rest))
    }

    @Test
    fun rest_without_remaining_time_shows_rest() {
        val notification = build(rest(remainingSeconds = 0))

        assertThat(title(notification)).isEqualTo(context.getString(R.string.workout_notification_rest))
    }

    @Test
    fun rest_shows_the_next_set() {
        val notification = build(rest())

        assertThat(text(notification)).isEqualTo("Next: Bench press • Set 2/3 • 8 x 60 kg")
    }

    @Test
    fun rest_shows_the_next_set_alongside_progress_when_expanded() {
        // The expanded standard template hides the text when there's a progress bar on API 31+
        val notification = build(rest())

        assertThat(notification.extras.getString(Notification.EXTRA_TEMPLATE))
                .isEqualTo(Notification.BigTextStyle::class.java.name)
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
                .isEqualTo("Next: Bench press • Set 2/3 • 8 x 60 kg")
    }

    @Test
    fun rest_does_not_prompt_to_edit_an_empty_next_set() {
        val notification = build(Rest(remainingSeconds = 45, remainingPercent = 75, upNext = nextSet(weight = 0f, reps = null)))

        assertThat(text(notification)).isEqualTo("Next: Bench press • Set 2/3")
    }

    @Test
    fun rest_after_the_last_set_shows_workout_complete() {
        val notification = build(Rest(remainingSeconds = 45, remainingPercent = 75, upNext = null))

        assertThat(text(notification)).isEqualTo(context.getString(R.string.workout_notification_complete))
    }

    // Done / no exercises

    @Test
    fun done_uses_the_standard_template() {
        val notification = build(Done)

        assertThat(notification.contentView).isNull()
        assertThat(title(notification)).isEqualTo(context.getString(R.string.workout_empty_done))
        assertThat(text(notification)).isEqualTo(context.getString(R.string.workout_empty_done_subtitle))
    }

    @Test
    fun no_exercises_uses_the_standard_template() {
        val notification = build(NoExercises)

        assertThat(notification.contentView).isNull()
        assertThat(title(notification)).isEqualTo(context.getString(R.string.workout_empty_no_sets))
        assertThat(text(notification)).isEqualTo(context.getString(R.string.workout_empty_no_sets_subtitle))
    }

    // Helpers

    private fun build(state: WorkoutNotificationState) = builder.build(channelId, state, null)

    private fun title(notification: Notification) = notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()

    private fun text(notification: Notification) = notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    private fun nextSet(exerciseName: String = "Bench press",
                        exercisePosition: Int = 1,
                        exercisesInGroup: Int = 1,
                        setType: String = "SINGLE",
                        setNumber: Int = 2,
                        totalSets: Int = 3,
                        weight: Float = 60f,
                        reps: Int? = 8,
                        timeSeconds: Int? = null,
                        remainingSeconds: Int? = null) = NextSet(
            exerciseName = exerciseName,
            exercisePosition = exercisePosition,
            exercisesInGroup = exercisesInGroup,
            setType = setType,
            setNumber = setNumber,
            totalSets = totalSets,
            weight = weight,
            reps = reps,
            timeSeconds = remainingSeconds ?: timeSeconds,
            timerRunning = remainingSeconds != null,
            set = ELSet(requiredReps = reps, reps = reps, weight = weight, requiredTimeSeconds = timeSeconds, timeSeconds = timeSeconds)
                    .apply { remainingTimeSeconds = remainingSeconds })

    private fun rest(remainingSeconds: Int = 45, remainingPercent: Int = 75) =
            Rest(remainingSeconds, remainingPercent, nextSet())
}
