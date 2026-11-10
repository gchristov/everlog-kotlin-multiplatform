package com.everlog.android.home.data.workouts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NextTemplateRuleTest {

    private val all = setOf("push", "pull", "legs", "arms")

    @Test
    fun `a rotation suggests what comes after the latest one`() {
        val suggestion = suggestNextTemplate(history("push", "pull", "legs", "push", "pull", "legs", "push", "pull", "legs", "push"), all)

        assertThat(suggestion).isEqualTo(SuggestedTemplate(uuid = "pull", afterUuid = "push", times = 3, of = 3))
    }

    @Test
    fun `repeating one template suggests it again`() {
        assertThat(suggestNextTemplate(history("legs", "legs", "legs", "legs"), all)?.uuid).isEqualTo("legs")
    }

    @Test
    fun `nothing until the latest one was followed at least three times`() {
        assertThat(suggestNextTemplate(history("push", "pull", "push", "pull", "push"), all)).isNull()
    }

    @Test
    fun `nothing when it came next less than 70 percent of the time`() {
        // After push: pull three times, legs twice, so 60%
        val history = history("push", "pull", "push", "pull", "push", "legs", "push", "pull", "push", "legs", "push")

        assertThat(suggestNextTemplate(history, all)).isNull()
    }

    @Test
    fun `a suggestion at 80 percent`() {
        val history = history("push", "pull", "push", "pull", "push", "legs", "push", "pull", "push", "pull", "push")

        assertThat(suggestNextTemplate(history, all)).isEqualTo(SuggestedTemplate(uuid = "pull", afterUuid = "push", times = 4, of = 5))
    }

    @Test
    fun `nothing for a template since deleted`() {
        val history = history("push", "pull", "push", "pull", "push", "pull", "push")

        assertThat(suggestNextTemplate(history, setOf("push", "legs"))).isNull()
    }

    @Test
    fun `nothing without history`() {
        assertThat(suggestNextTemplate(history(), all)).isNull()
    }
}
