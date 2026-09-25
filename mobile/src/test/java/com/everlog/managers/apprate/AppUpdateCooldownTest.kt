package com.everlog.managers.apprate

import com.everlog.testutil.InMemorySharedPreferences
import com.everlog.testutil.at
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

class AppUpdateCooldownTest {

    private val manager = AppLaunchManager.manager
    private val versionCode = 1_020_300
    private val declinedAt = Date(at(2026, 9, 24))

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `prompts for a version that was never declined`() {
        assertThat(manager.shouldPromptAppUpdate(versionCode, declinedAt)).isTrue()
    }

    @Test
    fun `doesn't prompt for a declined version on the same day`() {
        manager.appUpdateDeclined(versionCode, declinedAt)

        assertThat(manager.shouldPromptAppUpdate(versionCode, declinedAt)).isFalse()
    }

    @Test
    fun `doesn't prompt for a declined version within 7 days`() {
        manager.appUpdateDeclined(versionCode, declinedAt)

        assertThat(manager.shouldPromptAppUpdate(versionCode, Date(at(2026, 9, 30)))).isFalse()
    }

    @Test
    fun `prompts for a declined version after 7 days`() {
        manager.appUpdateDeclined(versionCode, declinedAt)

        assertThat(manager.shouldPromptAppUpdate(versionCode, Date(at(2026, 10, 1)))).isTrue()
    }

    @Test
    fun `prompts for a newer version than the declined one straight away`() {
        manager.appUpdateDeclined(versionCode, declinedAt)

        assertThat(manager.shouldPromptAppUpdate(versionCode + 1, declinedAt)).isTrue()
    }
}
