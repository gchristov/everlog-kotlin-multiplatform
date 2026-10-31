package com.everlog.ui.navigator

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.ui.activities.home.workout.WorkoutActivity
import com.everlog.android.workout.feature.log.WorkoutLogActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ELNavigatorWorkoutActivityTest {

    @Test
    fun `a workout opens the screen it started on`() {
        assertThat(ELNavigator.workoutActivity(ELWorkout().apply { workoutScreenV2 = true })).isEqualTo(WorkoutLogActivity::class.java)
        assertThat(ELNavigator.workoutActivity(ELWorkout())).isEqualTo(WorkoutActivity::class.java)
    }

    @Test
    fun `no workout opens the old screen`() {
        assertThat(ELNavigator.workoutActivity(null)).isEqualTo(WorkoutActivity::class.java)
    }
}
