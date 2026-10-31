package com.everlog.android.workout.data.prefill

import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.android.workout.data.prefill.goalWeight
import com.everlog.android.workout.data.prefill.rounded1RM
import com.everlog.android.common.kotlin.estimate1RM
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GoalWeightTest {

    @Test
    fun `a goal takes its share of the 1RM, rounded up to a whole kg`() {
        assertThat(goalWeight(101.26f, MuscleGoal.GROWTH)).isEqualTo(82f)
        assertThat(goalWeight(101.26f, MuscleGoal.POWER)).isEqualTo(92f)
        assertThat(goalWeight(101.26f, MuscleGoal.EXPLOSIVENESS)).isEqualTo(51f)
    }

    @Test
    fun `float noise in the estimate doesn't add a kg`() {
        // Brzycki gives 90.01 for 80 x 5, and 80% of that would round up to 73
        val oneRepMax = estimate1RM(80f, 5)

        assertThat(oneRepMax).isGreaterThan(90f)
        assertThat(goalWeight(oneRepMax, MuscleGoal.GROWTH)).isEqualTo(72f)
    }

    @Test
    fun `an exact share stays as it is`() {
        assertThat(goalWeight(100f, MuscleGoal.GROWTH)).isEqualTo(80f)
        assertThat(goalWeight(202.5f, MuscleGoal.GROWTH)).isEqualTo(162f)
    }

    @Test
    fun `the 1RM is shown to one decimal`() {
        assertThat(rounded1RM(90.0088f)).isEqualTo(90f)
        assertThat(rounded1RM(202.52f)).isEqualTo(202.5f)
    }
}
