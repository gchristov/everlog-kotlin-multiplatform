package com.everlog.android.common.kotlin.format

import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeightFormatTest {

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
    fun `a weight in pounds is stored in kg and shows as it was typed`() {
        val kg = storedWeight(135f, WeightUnit.POUND)

        assertThat(kg).isWithin(0.01f).of(61.23f)
        assertThat(formatWeight(displayWeight(kg, WeightUnit.POUND))).isEqualTo("135")
        assertThat(storedWeight(60f, WeightUnit.KILOGRAM)).isEqualTo(60f)
    }
}
