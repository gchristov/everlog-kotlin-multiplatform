package com.everlog.android.settings.data

import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class RealUserSettingsTest {

    private val settings = RealUserSettings()

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
        assertThat(settings.keepScreenOn()).isTrue()
    }

    @Test
    fun `saves keep screen on`() {
        settings.setKeepScreenOn(false)
        assertThat(settings.keepScreenOn()).isFalse()

        settings.setKeepScreenOn(true)
        assertThat(settings.keepScreenOn()).isTrue()
    }

    @Test
    fun `clearing user preferences restores the keep screen on default`() {
        settings.setKeepScreenOn(false)

        settings.clearUserPreferences()

        assertThat(settings.keepScreenOn()).isTrue()
    }
}
