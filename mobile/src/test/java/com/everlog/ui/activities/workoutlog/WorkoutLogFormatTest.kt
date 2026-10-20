package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutLogFormatTest {

    @Test
    fun `a weight has at most two decimals, without trailing zeros`() {
        assertThat(formatWeight(62.5f)).isEqualTo("62.5")
        assertThat(formatWeight(60f)).isEqualTo("60")
        assertThat(formatWeight(137.7889f)).isEqualTo("137.79")
        assertThat(formatWeight(0.005f)).isEqualTo("0.01")
    }

    @Test
    fun `a clock shows hours only when there are some`() {
        assertThat(formatClock(0)).isEqualTo("0:00")
        assertThat(formatClock(65)).isEqualTo("1:05")
        assertThat(formatClock(3723)).isEqualTo("1:02:03")
    }

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
    fun `a weight in pounds is stored in kg and shows as it was typed`() {
        val kg = storedWeight(135f, WeightUnit.POUND)

        assertThat(kg).isWithin(0.01f).of(61.23f)
        assertThat(formatWeight(displayWeight(kg, WeightUnit.POUND))).isEqualTo("135")
        assertThat(storedWeight(60f, WeightUnit.KILOGRAM)).isEqualTo(60f)
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
    fun `last time reads count then weight, and is empty when nothing was logged`() {
        assertThat(lastTimeText(WorkoutSet(reps = 15, weightKg = 17f), WeightUnit.KILOGRAM, "KG")).isEqualTo("15 × 17 kg")
        assertThat(lastTimeText(WorkoutSet(mode = SetMode.Time, timeSeconds = 30, weightKg = 10f), WeightUnit.KILOGRAM, "KG"))
            .isEqualTo("0:30 × 10 kg")
        assertThat(lastTimeText(WorkoutSet(reps = 12), WeightUnit.KILOGRAM, "KG")).isEqualTo("12")
        assertThat(lastTimeText(WorkoutSet(), WeightUnit.KILOGRAM, "KG")).isNull()
    }
}
