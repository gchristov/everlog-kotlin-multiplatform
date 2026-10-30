package com.everlog.utils.format

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutSetFormatTest {

    @Test
    fun `last time reads count then weight, and is empty when nothing was logged`() {
        assertThat(lastTimeText(WorkoutSet(reps = 15, weightKg = 17f), WeightUnit.KILOGRAM, "KG")).isEqualTo("15 × 17 kg")
        assertThat(lastTimeText(WorkoutSet(mode = SetMode.Time, timeSeconds = 30, weightKg = 10f), WeightUnit.KILOGRAM, "KG"))
            .isEqualTo("0:30 × 10 kg")
        assertThat(lastTimeText(WorkoutSet(reps = 12), WeightUnit.KILOGRAM, "KG")).isEqualTo("12")
        assertThat(lastTimeText(WorkoutSet(), WeightUnit.KILOGRAM, "KG")).isNull()
    }
}
