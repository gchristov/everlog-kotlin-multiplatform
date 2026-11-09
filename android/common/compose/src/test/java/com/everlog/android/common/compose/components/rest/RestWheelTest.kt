package com.everlog.android.common.compose.components.rest

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RestWheelTest {

    @Test
    fun `minutes and seconds come from the total`() {
        assertThat(restWheelParts(90, 10)).isEqualTo(1 to 30)
        assertThat(restWheelParts(600, 10)).isEqualTo(10 to 0)
    }

    @Test
    fun `a total off the steps rounds onto them`() {
        assertThat(restWheelTotal(1, 34, 10)).isEqualTo(90)
        assertThat(restWheelTotal(1, 36, 10)).isEqualTo(100)
    }

    @Test
    fun `the total stays between one step and the last minute`() {
        assertThat(restWheelTotal(0, 0, 10)).isEqualTo(10)
        assertThat(restWheelParts(0, 10)).isEqualTo(0 to 10)
        assertThat(restWheelTotal(40, 0, 10)).isEqualTo(30 * 60 + 50)
    }
}
