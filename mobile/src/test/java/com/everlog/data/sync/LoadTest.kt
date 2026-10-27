package com.everlog.data.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LoadTest {

    private val error = IllegalStateException("offline")

    @Test
    fun `two loads join once both are ready`() {
        assertThat(Load.Ready(1).zip(Load.Ready("a")) { n, s -> "$n$s" }).isEqualTo(Load.Ready("1a"))
        assertThat(Load.Ready(1).zip(Load.Loading) { n, _: Any -> n }).isEqualTo(Load.Loading)
        assertThat(Load.Loading.zip(Load.Ready(1)) { _: Any, n -> n }).isEqualTo(Load.Loading)
    }

    @Test
    fun `a join is unavailable as soon as either part is`() {
        assertThat(Load.Unavailable(error).zip(Load.Loading) { _: Any, _: Any -> 1 }).isEqualTo(Load.Unavailable(error))
        assertThat(Load.Loading.zip(Load.Unavailable(error)) { _: Any, _: Any -> 1 }).isEqualTo(Load.Unavailable(error))
        assertThat(Load.Ready(1).zip(Load.Unavailable(error)) { n, _: Any -> n }).isEqualTo(Load.Unavailable(error))
    }

    @Test
    fun `an optional part that can't be read is null, and still waits while it's loading`() {
        assertThat(Load.Unavailable(error).optional()).isEqualTo(Load.Ready(null))
        assertThat(Load.Ready(1).optional()).isEqualTo(Load.Ready(1))
        assertThat(Load.Loading.optional()).isEqualTo(Load.Loading)
    }
}
