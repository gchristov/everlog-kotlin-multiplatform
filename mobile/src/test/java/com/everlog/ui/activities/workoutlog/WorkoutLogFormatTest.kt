package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutLogFormatTest {

    @Test
    fun `a time is typed like a microwave's, its last two digits seconds`() {
        assertThat(typedSeconds("5")).isEqualTo(5)
        assertThat(typedSeconds("90")).isEqualTo(90)
        assertThat(typedSeconds("130")).isEqualTo(90)
        assertThat(typedSeconds("1000")).isEqualTo(600)
        assertThat(typedSeconds("")).isEqualTo(0)
    }

    @Test
    fun `a time backspaces as the digits that type it`() {
        assertThat(secondsTyped(45)).isEqualTo("45")
        assertThat(secondsTyped(90)).isEqualTo("130")
        assertThat(secondsTyped(600)).isEqualTo("1000")
        assertThat(typedSeconds(secondsTyped(754))).isEqualTo(754)
    }

    @Test
    fun `nothing typed clears a value, and reps take whole numbers only`() {
        assertThat(parse(ValueField.Weight, "")).isNull()
        assertThat(parse(ValueField.Reps, "")).isNull()
        assertThat(parse(ValueField.Time, "")).isNull()
        assertThat(parse(ValueField.Weight, "62.")).isEqualTo(62f)
        assertThat(parse(ValueField.Reps, "1.5")).isNull()
        assertThat(parse(ValueField.Time, "130")).isEqualTo(90f)
    }

    @Test
    fun `a value backspaces from what it shows`() {
        val set = WorkoutSet(reps = 12, timeSeconds = 90, weightKg = 62.5f)

        assertThat(rawText(set, ValueField.Reps, WeightUnit.KILOGRAM)).isEqualTo("12")
        assertThat(rawText(set, ValueField.Time, WeightUnit.KILOGRAM)).isEqualTo("130")
        assertThat(rawText(set, ValueField.Weight, WeightUnit.KILOGRAM)).isEqualTo("62.5")
        assertThat(rawText(WorkoutSet(), ValueField.Weight, WeightUnit.KILOGRAM)).isEmpty()
    }
}
