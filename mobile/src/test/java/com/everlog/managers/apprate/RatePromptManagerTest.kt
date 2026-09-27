package com.everlog.managers.apprate

import com.everlog.testutil.InMemorySharedPreferences
import com.everlog.testutil.at
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class RatePromptManagerTest {

    private val manager = RatePromptManager(RatePromptPacing(actionGoals = listOf(3, 3, 4), cooldownMillis = 30L * 24 * 60 * 60 * 1000))

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `is due on the third action`() {
        val now = at(2026, 9, 1)

        assertThat(manager.recordAction(now)).isFalse()
        assertThat(manager.recordAction(now)).isFalse()
        assertThat(manager.recordAction(now)).isTrue()
    }

    @Test
    fun `numbers prompts from 1`() {
        val now = at(2026, 9, 1)

        assertThat(manager.promptLaunched(now)).isEqualTo(1)
        assertThat(manager.promptLaunched(now)).isEqualTo(2)
    }

    @Test
    fun `launching a prompt restarts the count towards the next one`() {
        repeat(3) { manager.recordAction(at(2026, 9, 1)) }
        manager.promptLaunched(at(2026, 9, 1))

        repeat(2) { manager.recordAction(at(2026, 10, 5)) }
        assertThat(manager.isDue(at(2026, 10, 5))).isFalse()
        assertThat(manager.recordAction(at(2026, 10, 5))).isTrue()
    }

    @Test
    fun `actions within the cooldown still count once it's over`() {
        repeat(3) { manager.recordAction(at(2026, 9, 1)) }
        manager.promptLaunched(at(2026, 9, 1))

        repeat(5) { manager.recordAction(at(2026, 9, 10)) }
        assertThat(manager.isDue(at(2026, 9, 10))).isFalse()
        assertThat(manager.isDue(at(2026, 10, 1))).isTrue()
    }

    @Test
    fun `stays due when a prompt couldn't be launched`() {
        repeat(3) { manager.recordAction(at(2026, 9, 1)) }

        // e.g. the user left the screen before the dialog launched
        assertThat(manager.recordAction(at(2026, 9, 2))).isTrue()
    }

    @Test
    fun `logging out starts again from the first prompt`() {
        repeat(3) { manager.recordAction(at(2026, 9, 1)) }
        manager.promptLaunched(at(2026, 9, 1))

        AppLaunchManager.manager.clearAppUserData()

        assertThat(manager.promptLaunched(at(2026, 9, 1))).isEqualTo(1)
    }
}
