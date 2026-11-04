package com.everlog.android.home.data

import com.everlog.android.common.test.InMemorySharedPreferences
import com.everlog.android.common.test.install
import com.everlog.android.common.test.uninstall
import com.everlog.android.home.data.model.HomeNotification
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class RealHomeNotificationsTest {

    private val notifications = RealHomeNotifications()
    private val banner = HomeNotification(id = "survey", title = "Title", description = "Description")
    private lateinit var preferences: InMemorySharedPreferences

    @Before
    fun setUp() {
        preferences = InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `shows a banner until it's dismissed`() {
        assertThat(notifications.shouldShow(banner)).isTrue()

        notifications.dismissed(banner)

        assertThat(notifications.shouldShow(banner)).isFalse()
        assertThat(notifications.shouldShow(banner.copy(id = "next"))).isTrue()
    }

    @Test
    fun `doesn't show a missing banner or one that can't show`() {
        assertThat(notifications.shouldShow(null)).isFalse()
        assertThat(notifications.shouldShow(banner.copy(title = null))).isFalse()
    }

    @Test
    fun `a banner dismissed under the earlier versions' key stays dismissed`() {
        preferences.edit().putString("HOME_NOTIFICATION_LAST_DISMISSED_ID", "survey").apply()

        assertThat(notifications.shouldShow(banner)).isFalse()
    }

    @Test
    fun `clearing shows a dismissed banner again`() {
        notifications.dismissed(banner)

        notifications.clear()

        assertThat(notifications.shouldShow(banner)).isTrue()
    }
}
