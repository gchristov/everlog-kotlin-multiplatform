package com.everlog.android.home.data.workouts

import com.everlog.android.common.data.model.workout.ELWorkout
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TemplateHistoryTest {

    @Test
    fun `lists each workout's template, oldest first`() {
        val workouts = workoutsFrom("push", "pull", "legs").reversed()

        assertThat(templateHistory(workouts).order).containsExactly("push", "pull", "legs").inOrder()
    }

    @Test
    fun `leaves out empty workouts`() {
        val empty = ELWorkout(uuid = "empty", createdDate = 5 * DayMillis, fromRoutine = false)

        assertThat(templateHistory(workoutsFrom("push") + empty).order).containsExactly("push")
    }

    @Test
    fun `knows when each template was last done, and its latest name`() {
        val workouts = workoutsFrom("push", "pull", "push")
        workouts.last().routine!!.name = "Push day"

        val history = templateHistory(workouts)

        assertThat(history.lastDone).containsExactly("push", 2 * DayMillis, "pull", DayMillis)
        assertThat(history.names["push"]).isEqualTo("Push day")
    }
}
