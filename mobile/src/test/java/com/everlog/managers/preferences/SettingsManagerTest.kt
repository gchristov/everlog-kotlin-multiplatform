package com.everlog.managers.preferences

import com.everlog.testutil.InMemorySharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class SettingsManagerTest {

    private val manager = SettingsManager.manager

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `keeps the screen on by default`() {
        assertThat(manager.keepScreenOn()).isTrue()
    }

    @Test
    fun `saves keep screen on`() {
        manager.setKeepScreenOn(false)
        assertThat(manager.keepScreenOn()).isFalse()

        manager.setKeepScreenOn(true)
        assertThat(manager.keepScreenOn()).isTrue()
    }

    @Test
    fun `clearing user preferences restores the keep screen on default`() {
        manager.setKeepScreenOn(false)

        manager.clearUserPreferences()

        assertThat(manager.keepScreenOn()).isTrue()
    }
}
