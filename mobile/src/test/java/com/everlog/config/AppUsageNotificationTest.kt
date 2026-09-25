package com.everlog.config

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class AppUsageNotificationTest {

    // Matches the production remote config: every 4 days at 10:00
    private val notification = AppUsageNotification(
            title = "You haven't recorded workouts recently",
            description = "Tap to start your training",
            scheduleIntervalDays = 4,
            scheduleHourOfDay = 10)

    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis
    }

    private fun days(value: Long) = TimeUnit.DAYS.toMillis(value)

    // First trigger

    @Test
    fun `first trigger is the configured hour, interval days after the given time`() {
        val lastActive = at(2026, 9, 25, 23, 40)

        assertThat(notification.getFirstTriggerAtMillis(lastActive)).isEqualTo(at(2026, 9, 29, 10))
    }

    // Should show

    @Test
    fun `shows once the user has been away for the full interval`() {
        val lastActive = at(2026, 9, 25, 23, 40)

        assertThat(notification.shouldShow(lastActive, 0, at(2026, 9, 29, 10))).isTrue()
    }

    @Test
    fun `skips a stale alarm when the user was active since it was scheduled`() {
        // Alarm scheduled on the 21st by an older app version, but the user trained on the 25th
        val lastActive = at(2026, 9, 25, 18)

        assertThat(notification.shouldShow(lastActive, 0, at(2026, 9, 26, 10))).isFalse()
    }

    @Test
    fun `skips right up until the first trigger`() {
        val lastActive = at(2026, 9, 25, 23, 40)

        assertThat(notification.shouldShow(lastActive, 0, at(2026, 9, 29, 9, 59))).isFalse()
    }

    @Test
    fun `shows when activity was never recorded`() {
        assertThat(notification.shouldShow(0, 0, at(2026, 9, 29, 10))).isTrue()
    }

    @Test
    fun `skips a backlog of stale alarms firing soon after one was shown`() {
        val lastActive = at(2026, 9, 25, 23, 40)
        val shown = at(2026, 9, 29, 10)

        assertThat(notification.shouldShow(lastActive, shown, shown + TimeUnit.HOURS.toMillis(5))).isFalse()
    }

    @Test
    fun `shows the next repeat when the user stays away`() {
        val lastActive = at(2026, 9, 25, 23, 40)
        val shown = at(2026, 9, 29, 10)

        assertThat(notification.shouldShow(lastActive, shown, shown + days(4))).isTrue()
    }

    @Test
    fun `shows after a new absence even if the previous reminder was recent`() {
        // Reminder shown, user opened the app shortly after, then left for the full interval
        val shown = at(2026, 9, 29, 10)
        val lastActive = at(2026, 9, 29, 12)

        assertThat(notification.shouldShow(lastActive, shown, at(2026, 10, 3, 10))).isTrue()
    }
}
