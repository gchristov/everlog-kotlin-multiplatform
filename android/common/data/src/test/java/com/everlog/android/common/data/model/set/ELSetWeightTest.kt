package com.everlog.android.common.data.model.set

import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ELSetWeightTest {

    @Test
    fun `a weight is stored in kg and read back in either unit`() {
        val set = ELSet()

        set.updateWeight(100f, WeightUnit.KILOGRAM)

        assertThat(set.weight).isEqualTo(100f)
        assertThat(set.getWeight(WeightUnit.KILOGRAM)).isEqualTo(100f)
        assertThat(set.getWeight(WeightUnit.POUND)).isWithin(0.01f).of(220.46f)
    }

    @Test
    fun `a weight entered in pounds is stored in kg`() {
        val set = ELSet()

        set.updateWeight(220.462f, WeightUnit.POUND)

        assertThat(set.weight!!).isWithin(0.001f).of(100f)
    }

    @Test
    fun `no weight reads as -1 and isn't entered`() {
        val set = ELSet()

        assertThat(set.getWeight(WeightUnit.POUND)).isEqualTo(-1f)
        assertThat(set.isWeightEntered()).isFalse()
    }

    @Test
    fun `a weight of zero or less clears it`() {
        val set = ELSet()
        set.updateWeight(50f, WeightUnit.KILOGRAM)

        set.updateWeight(0f, WeightUnit.KILOGRAM)

        assertThat(set.weight).isNull()
        assertThat(set.isWeightEntered()).isFalse()
    }

    @Test
    fun `modifying adds the offset in the user's unit`() {
        val set = ELSet()
        set.updateWeight(100f, WeightUnit.POUND)

        set.modifyWeight(5f, WeightUnit.POUND)

        assertThat(set.getWeight(WeightUnit.POUND)).isWithin(0.001f).of(105f)
    }

    @Test
    fun `modifying up by 1 from no weight starts at 1`() {
        val set = ELSet()

        set.modifyWeight(1f, WeightUnit.KILOGRAM)

        assertThat(set.getWeight(WeightUnit.KILOGRAM)).isEqualTo(1f)
    }

    @Test
    fun `modifying down to zero or below clears the weight`() {
        val set = ELSet()
        set.updateWeight(2.5f, WeightUnit.KILOGRAM)

        set.modifyWeight(-2.5f, WeightUnit.KILOGRAM)

        assertThat(set.isWeightEntered()).isFalse()
    }

    @Test
    fun `the heavier set is better whichever unit it was entered in`() {
        val heavier = ELSet().apply { updateWeight(100f, WeightUnit.KILOGRAM) }
        val lighter = ELSet().apply { updateWeight(200f, WeightUnit.POUND) }

        assertThat(heavier.isBetterThan(lighter, compareWeight = true)).isTrue()
        assertThat(lighter.isBetterThan(heavier, compareWeight = true)).isFalse()
        assertThat(lighter.isBetterThan(ELSet(), compareWeight = true)).isTrue()
    }
}
