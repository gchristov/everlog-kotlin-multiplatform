package com.everlog.managers.apprate

import com.everlog.testutil.at
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RatePromptPacingTest {

    private val pacing = RatePromptPacing(actionGoals = listOf(3, 3, 4), cooldownMillis = 30L * 24 * 60 * 60 * 1000)
    private val now = at(2026, 9, 27)
    private val neverPrompted = -1L

    @Test
    fun `isn't due before the first goal`() {
        assertThat(pacing.isDue(actionsSinceLastPrompt = 2, promptsSoFar = 0, lastPromptDate = neverPrompted, now = now)).isFalse()
    }

    @Test
    fun `is due at the first goal when never prompted`() {
        assertThat(pacing.isDue(actionsSinceLastPrompt = 3, promptsSoFar = 0, lastPromptDate = neverPrompted, now = now)).isTrue()
    }

    @Test
    fun `stays due past the goal`() {
        assertThat(pacing.isDue(actionsSinceLastPrompt = 7, promptsSoFar = 0, lastPromptDate = neverPrompted, now = now)).isTrue()
    }

    @Test
    fun `second prompt needs 3 more actions`() {
        val lastPrompt = at(2026, 8, 1)

        assertThat(pacing.isDue(actionsSinceLastPrompt = 2, promptsSoFar = 1, lastPromptDate = lastPrompt, now = now)).isFalse()
        assertThat(pacing.isDue(actionsSinceLastPrompt = 3, promptsSoFar = 1, lastPromptDate = lastPrompt, now = now)).isTrue()
    }

    @Test
    fun `third prompt needs 4 more actions`() {
        val lastPrompt = at(2026, 8, 1)

        assertThat(pacing.isDue(actionsSinceLastPrompt = 3, promptsSoFar = 2, lastPromptDate = lastPrompt, now = now)).isFalse()
        assertThat(pacing.isDue(actionsSinceLastPrompt = 4, promptsSoFar = 2, lastPromptDate = lastPrompt, now = now)).isTrue()
    }

    @Test
    fun `never due after the last goal is used`() {
        val lastPrompt = at(2025, 1, 1)

        assertThat(pacing.isDue(actionsSinceLastPrompt = 100, promptsSoFar = 3, lastPromptDate = lastPrompt, now = now)).isFalse()
    }

    @Test
    fun `isn't due within 30 days of the last prompt, even with the goal met`() {
        val lastPrompt = at(2026, 8, 29)

        assertThat(pacing.isDue(actionsSinceLastPrompt = 10, promptsSoFar = 1, lastPromptDate = lastPrompt, now = now)).isFalse()
    }

    @Test
    fun `is due 30 days after the last prompt`() {
        val lastPrompt = at(2026, 8, 28)

        assertThat(pacing.isDue(actionsSinceLastPrompt = 3, promptsSoFar = 1, lastPromptDate = lastPrompt, now = now)).isTrue()
    }
}
