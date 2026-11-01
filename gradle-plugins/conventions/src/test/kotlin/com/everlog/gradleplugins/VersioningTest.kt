package com.everlog.gradleplugins

import com.google.common.truth.Truth.assertThat
import org.gradle.api.GradleException
import org.junit.Assert.assertThrows
import org.junit.Test

class VersioningTest {

    @Test
    fun `the base code is the version with the type and sequence left at zero`() {
        assertThat(baseVersionCode("2.22.0")).isEqualTo(2_220_000)
        assertThat(baseVersionCode("2.22.1")).isEqualTo(2_221_000)
        assertThat(baseVersionCode("2.9.0")).isEqualTo(2_090_000)
        assertThat(baseVersionCode("3.0.0")).isEqualTo(3_000_000)
    }

    @Test
    fun `a version that doesn't fit seven digits fails`() {
        assertThrows(GradleException::class.java) { baseVersionCode("2.22") }
        assertThrows(GradleException::class.java) { baseVersionCode("2.100.0") }
        assertThrows(GradleException::class.java) { baseVersionCode("2.22.10") }
        assertThrows(GradleException::class.java) { baseVersionCode("10.0.0") }
    }
}
