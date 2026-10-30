package com.everlog.ui.components.exercisetable

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CellTypingTest {

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

    @Test
    fun `the first key replaces the value and later keys add to it, up to the field's length`() {
        assertThat(typedAfterKey(null, ValueField.Reps, '8')).isEqualTo("8")
        assertThat(typedAfterKey("1", ValueField.Reps, '2')).isEqualTo("12")
        assertThat(typedAfterKey("100", ValueField.Reps, '0')).isEqualTo("100")
        assertThat(typedAfterKey("130", ValueField.Time, '5')).isEqualTo("1305")
        assertThat(typedAfterKey("1305", ValueField.Time, '5')).isEqualTo("1305")
    }

    @Test
    fun `a point only goes into a weight, once`() {
        assertThat(typedAfterKey("62", ValueField.Weight, '.')).isEqualTo("62.")
        assertThat(typedAfterKey(null, ValueField.Weight, '.')).isEqualTo(".")
        assertThat(typedAfterKey("62.5", ValueField.Weight, '.')).isNull()
        assertThat(typedAfterKey("12", ValueField.Reps, '.')).isNull()
        assertThat(typedAfterKey("130", ValueField.Time, '.')).isNull()
    }

    @Test
    fun `backspace takes a digit off what's typed, or off the value shown when nothing is`() {
        val set = WorkoutSet(reps = 12, timeSeconds = 90, weightKg = 62.5f)

        assertThat(typedAfterBackspace("8", set, ValueField.Reps, WeightUnit.KILOGRAM)).isEmpty()
        assertThat(typedAfterBackspace(null, set, ValueField.Reps, WeightUnit.KILOGRAM)).isEqualTo("1")
        assertThat(typedAfterBackspace(null, set, ValueField.Time, WeightUnit.KILOGRAM)).isEqualTo("13")
        assertThat(typedAfterBackspace(null, set, ValueField.Weight, WeightUnit.KILOGRAM)).isEqualTo("62.")
        assertThat(typedAfterBackspace(null, WorkoutSet(), ValueField.Reps, WeightUnit.KILOGRAM)).isEmpty()
    }

    @Test
    fun `minus and plus step by a rep, the weight step or the timer's seconds, never below zero`() {
        val set = WorkoutSet(reps = 10, timeSeconds = 30, weightKg = 60f)
        val timeStep = AppConfig.configuration.defaultRestTimeOffsetSeconds.toFloat()

        assertThat(steppedValue(set, ValueField.Reps, WeightUnit.KILOGRAM, 2.5f, 1)).isEqualTo(11f)
        assertThat(steppedValue(set, ValueField.Weight, WeightUnit.KILOGRAM, 2.5f, -1)).isEqualTo(57.5f)
        assertThat(steppedValue(set, ValueField.Time, WeightUnit.KILOGRAM, 2.5f, 1)).isEqualTo(30f + timeStep)
        assertThat(steppedValue(WorkoutSet(), ValueField.Reps, WeightUnit.KILOGRAM, 2.5f, -1)).isEqualTo(0f)
        assertThat(steppedValue(WorkoutSet(), ValueField.Weight, WeightUnit.KILOGRAM, 2.5f, 1)).isEqualTo(2.5f)
    }
}
