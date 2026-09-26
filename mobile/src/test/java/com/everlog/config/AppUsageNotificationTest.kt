package com.everlog.config

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class AppUsageNotificationTest {

    // The original config, before backoff: every 4 days at 10:00
    private val legacy = AppUsageNotification(
            title = "You haven't recorded workouts recently",
            description = "Tap to start your training",
            scheduleIntervalDays = 4,
            scheduleHourOfDay = 10)

    private val backoff = legacy.copy(
            scheduleIntervalDays = 7,
            scheduleHourOfDay = 13,
            backoffIntervalsDays = listOf(7, 14, 30),
            maxReminders = 6)

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

    private fun hours(value: Long) = TimeUnit.HOURS.toMillis(value)

    /**
     * Replays a user who stays away after [lastActive]: fires the alarm where it's set, shows due
     * reminders, and returns when each one showed.
     */
    private fun remindersWhileAway(notification: AppUsageNotification, lastActive: Long, until: Long): List<Long> {
        val shownAt = mutableListOf<Long>()
        var now = lastActive
        while (true) {
            val reminder = notification.nextReminder(lastActive, shownAt.lastOrNull() ?: 0, shownAt.size, now) ?: break
            now = reminder.alarmAtMillis(now, notification.scheduleHourOfDay)
            if (now > until) break
            assertThat(reminder.isDue(now)).isTrue()
            shownAt += now
        }
        return shownAt
    }

    // First reminder

    @Test
    fun `first reminder is at the configured hour, interval days after the user was last active`() {
        val lastActive = at(2026, 9, 25, 23, 40)

        val reminder = legacy.nextReminder(lastActive, 0, 0, lastActive)!!

        assertThat(reminder.attempt).isEqualTo(1)
        assertThat(reminder.dueAtMillis).isEqualTo(at(2026, 9, 29, 10))
    }

    @Test
    fun `reminder is not due right up until its time`() {
        val lastActive = at(2026, 9, 25, 23, 40)
        val reminder = legacy.nextReminder(lastActive, 0, 0, lastActive)!!

        assertThat(reminder.isDue(at(2026, 9, 29, 9, 59))).isFalse()
        assertThat(reminder.isDue(at(2026, 9, 29, 10))).isTrue()
    }

    @Test
    fun `no reminder when the user was never active`() {
        assertThat(legacy.nextReminder(-1, 0, 0, at(2026, 9, 29, 10))).isNull()
    }

    // Backoff

    @Test
    fun `backs off to monthly, then stops after the max reminders`() {
        val lastActive = at(2026, 9, 1, 18)

        val shown = remindersWhileAway(backoff, lastActive, until = at(2027, 12, 31, 0))

        // Day 7, 21, 51, then every 30 days
        assertThat(shown).containsExactly(
                at(2026, 9, 8, 13),
                at(2026, 9, 22, 13),
                at(2026, 10, 22, 13),
                at(2026, 11, 21, 13),
                at(2026, 12, 21, 13),
                at(2027, 1, 20, 13)).inOrder()
    }

    @Test
    fun `no more reminders once the max is reached`() {
        val lastActive = at(2026, 9, 1, 18)

        assertThat(backoff.nextReminder(lastActive, at(2027, 1, 20, 13), 6, at(2027, 3, 1, 13))).isNull()
    }

    @Test
    fun `without backoff reminds every interval forever`() {
        val lastActive = at(2026, 9, 25, 18)

        val shown = remindersWhileAway(legacy, lastActive, until = at(2026, 12, 31, 0))

        assertThat(shown).hasSize(24)
        assertThat(shown.zipWithNext { a, b -> TimeUnit.MILLISECONDS.toDays(b - a + hours(1)) }.distinct()).containsExactly(4L)
    }

    @Test
    fun `a visit starts the sequence again`() {
        // Two reminders shown, then the user came back and left again
        val lastShown = at(2026, 9, 22, 13)
        val lastActive = at(2026, 9, 23, 20)

        val reminder = backoff.nextReminder(lastActive, lastShown, 2, lastActive)!!

        assertThat(reminder.attempt).isEqualTo(1)
        assertThat(reminder.dueAtMillis).isEqualTo(at(2026, 9, 30, 13))
    }

    @Test
    fun `a visit after the max reminders starts the sequence again`() {
        val lastActive = at(2027, 2, 1, 20)

        assertThat(backoff.nextReminder(lastActive, at(2027, 1, 20, 13), 6, lastActive)?.attempt).isEqualTo(1)
    }

    @Test
    fun `a single reminder when the max is 1`() {
        val lastActive = at(2026, 9, 1, 18)

        val shown = remindersWhileAway(backoff.copy(maxReminders = 1), lastActive, until = at(2027, 12, 31, 0))

        assertThat(shown).containsExactly(at(2026, 9, 8, 13))
    }

    @Test
    fun `an invalid backoff interval falls back to the default interval for that reminder only`() {
        val notification = backoff.copy(scheduleIntervalDays = 5, backoffIntervalsDays = listOf(7, null, 0, -3, 30))

        assertThat(notification.intervalDays(0)).isEqualTo(7)
        assertThat(notification.intervalDays(1)).isEqualTo(5)
        assertThat(notification.intervalDays(2)).isEqualTo(5)
        assertThat(notification.intervalDays(3)).isEqualTo(5)
        assertThat(notification.intervalDays(4)).isEqualTo(30)
        assertThat(notification.intervalDays(9)).isEqualTo(30)
    }

    @Test
    fun `an empty backoff list reminds every interval`() {
        assertThat(backoff.copy(backoffIntervalsDays = emptyList()).intervalDays(5)).isEqualTo(7)
    }

    // Late alarms

    @Test
    fun `an overdue reminder waits for the configured hour instead of showing at night`() {
        // Phone was off, turned back on at 02:00
        val lastActive = at(2026, 9, 1, 18)
        val now = at(2026, 9, 20, 2)
        val reminder = backoff.nextReminder(lastActive, 0, 0, now)!!

        assertThat(reminder.isDue(now)).isTrue()
        assertThat(reminder.alarmAtMillis(now, 13)).isEqualTo(at(2026, 9, 20, 13))
    }

    @Test
    fun `an overdue reminder after the configured hour waits until the next day`() {
        val lastActive = at(2026, 9, 1, 18)
        val now = at(2026, 9, 20, 15)

        assertThat(backoff.nextReminder(lastActive, 0, 0, now)!!.alarmAtMillis(now, 13)).isEqualTo(at(2026, 9, 21, 13))
    }

    @Test
    fun `a late reminder pushes the next one back rather than bursting to catch up`() {
        // Reminder 1 was due on the 8th but only showed on the 25th
        val lastActive = at(2026, 9, 1, 18)
        val shown = at(2026, 9, 25, 13)

        val reminder = backoff.nextReminder(lastActive, shown, 1, shown)!!

        assertThat(reminder.attempt).isEqualTo(2)
        assertThat(reminder.dueAtMillis).isEqualTo(at(2026, 10, 9, 13))
    }

    @Test
    fun `an old alarm firing the day after a reminder does not show another`() {
        val lastActive = at(2026, 9, 1, 18)
        val shown = at(2026, 9, 8, 13)
        val reminder = backoff.nextReminder(lastActive, shown, 1, at(2026, 9, 9, 10))!!

        assertThat(reminder.isDue(at(2026, 9, 9, 10))).isFalse()
    }

    @Test
    fun `an old alarm firing before the reminder is due does not show it`() {
        // Alarms set by older app versions keep firing at 10:00 every few days
        val lastActive = at(2026, 9, 25, 18)
        val reminder = backoff.nextReminder(lastActive, 0, 0, at(2026, 9, 28, 10))!!

        assertThat(reminder.isDue(at(2026, 9, 28, 10))).isFalse()
    }

    // Clock changes

    @Test
    fun `keeps the configured hour across a daylight saving change`() {
        // Clocks go back on 25 Oct 2026
        val lastActive = at(2026, 10, 20, 18)

        assertThat(backoff.nextReminder(lastActive, 0, 0, lastActive)!!.dueAtMillis).isEqualTo(at(2026, 10, 27, 13))
    }

    @Test
    fun `treats a last active time in the future as now after the clock was corrected`() {
        val now = at(2026, 9, 29, 10)

        assertThat(backoff.nextReminder(at(2026, 12, 7, 12), 0, 0, now)!!.dueAtMillis).isEqualTo(at(2026, 10, 6, 13))
    }

    @Test
    fun `starts the sequence again when the last shown time is in the future after the clock was corrected`() {
        // Reminders shown with the clock ahead, then the clock was corrected and the user visited
        val lastActive = at(2026, 9, 26, 9)
        val now = at(2026, 9, 26, 10)

        val reminder = backoff.nextReminder(lastActive, at(2026, 11, 17, 14), 3, now)!!

        assertThat(reminder.attempt).isEqualTo(1)
        assertThat(reminder.dueAtMillis).isEqualTo(at(2026, 10, 3, 13))
    }

    // Messages

    // Reminder [attempt] (1 for the first) for a user away since 1 Sep, with the previous ones shown
    private fun reminder(notification: AppUsageNotification, attempt: Int): AppUsageNotification.Reminder {
        val lastActive = at(2026, 9, 1, 18)
        val lastShown = if (attempt > 1) at(2026, 9, 8, 13) else 0
        return notification.nextReminder(lastActive, lastShown, attempt - 1, at(2026, 9, 8, 13))!!
    }

    @Test
    fun `uses the message for each reminder, the last one repeating`() {
        val notification = backoff.copy(messages = listOf(
                AppUsageNotification.Message("First", "One"),
                AppUsageNotification.Message("Second", "Two")))
        val lastActive = at(2026, 9, 1, 18)

        fun reminder(count: Int) = notification.nextReminder(lastActive, at(2026, 9, 8, 13), count, at(2026, 9, 8, 13))!!

        assertThat(notification.nextReminder(lastActive, 0, 0, lastActive)!!.title).isEqualTo("First")
        assertThat(reminder(1).title).isEqualTo("Second")
        assertThat(reminder(1).description).isEqualTo("Two")
        assertThat(reminder(4).title).isEqualTo("Second")
    }

    @Test
    fun `falls back to the default text for missing message fields`() {
        val notification = backoff.copy(messages = listOf(AppUsageNotification.Message(title = "Custom", description = " ")))
        val lastActive = at(2026, 9, 1, 18)

        val reminder = notification.nextReminder(lastActive, 0, 0, lastActive)!!

        assertThat(reminder.title).isEqualTo("Custom")
        assertThat(reminder.description).isEqualTo(legacy.description)
    }

    @Test
    fun `uses the default text without messages`() {
        val lastActive = at(2026, 9, 1, 18)

        assertThat(backoff.nextReminder(lastActive, 0, 0, lastActive)!!.title).isEqualTo(legacy.title)
        assertThat(backoff.copy(messages = emptyList()).nextReminder(lastActive, 0, 0, lastActive)!!.title).isEqualTo(legacy.title)
    }

    @Test
    fun `a null message falls back to the default text for that reminder only`() {
        val notification = backoff.copy(messages = listOf(null, AppUsageNotification.Message("Second", "Two")))

        assertThat(reminder(notification, 1).title).isEqualTo(legacy.title)
        assertThat(reminder(notification, 1).description).isEqualTo(legacy.description)
        assertThat(reminder(notification, 2).title).isEqualTo("Second")
    }

    @Test
    fun `a blank message in the middle falls back without shifting the later ones`() {
        val notification = backoff.copy(messages = listOf(
                AppUsageNotification.Message("First", "One"),
                AppUsageNotification.Message(" ", ""),
                AppUsageNotification.Message("Third", "Three")))

        assertThat(reminder(notification, 1).title).isEqualTo("First")
        assertThat(reminder(notification, 2).title).isEqualTo(legacy.title)
        assertThat(reminder(notification, 2).description).isEqualTo(legacy.description)
        assertThat(reminder(notification, 3).title).isEqualTo("Third")
        assertThat(reminder(notification, 6).title).isEqualTo("Third")
    }

    @Test
    fun `a null last message repeats as the default text`() {
        val notification = backoff.copy(messages = listOf(AppUsageNotification.Message("First", "One"), null))

        assertThat(reminder(notification, 5).title).isEqualTo(legacy.title)
    }

    // Validation

    @Test
    fun `backoff config is valid`() {
        assertThat(backoff.isValid()).isTrue()
    }

    @Test
    fun `invalid without the default text, which older app versions and fallbacks need`() {
        assertThat(backoff.copy(title = null).isValid()).isFalse()
        assertThat(backoff.copy(description = "").isValid()).isFalse()
    }

    @Test
    fun `invalid without an interval or with an hour out of range`() {
        assertThat(backoff.copy(scheduleIntervalDays = 0).isValid()).isFalse()
        assertThat(backoff.copy(scheduleHourOfDay = 25).isValid()).isFalse()
    }

    // Remote Config

    @Test
    fun `parses the original config`() {
        val json = """{"title":"T","description":"D","scheduleIntervalDays":4,"scheduleHourOfDay":10}"""

        val notification = RemoteConfig.parse(json, AppUsageNotification::class.java)!!

        assertThat(notification.isValid()).isTrue()
        assertThat(notification.backoffIntervalsDays).isNull()
        assertThat(notification.maxReminders).isEqualTo(0)
        assertThat(notification.messages).isNull()
        assertThat(notification.intervalDays(3)).isEqualTo(4)
    }

    @Test
    fun `parses the backoff config`() {
        val json = """{"title":"T","description":"D","scheduleIntervalDays":7,"scheduleHourOfDay":13,
            "backoffIntervalsDays":[7,14,30],"maxReminders":6,
            "messages":[{"title":"A","description":"B"},{"description":"C"}]}"""

        val notification = RemoteConfig.parse(json, AppUsageNotification::class.java)!!

        assertThat(notification.backoffIntervalsDays).containsExactly(7, 14, 30).inOrder()
        assertThat(notification.maxReminders).isEqualTo(6)
        assertThat(notification.messages).containsExactly(
                AppUsageNotification.Message("A", "B"),
                AppUsageNotification.Message(null, "C")).inOrder()
    }

    @Test
    fun `parses a stray null in the lists without crashing`() {
        val json = """{"title":"T","description":"D","scheduleIntervalDays":7,"scheduleHourOfDay":13,
            "backoffIntervalsDays":[7,null,30],"messages":[null,{"title":"A"}]}"""

        val notification = RemoteConfig.parse(json, AppUsageNotification::class.java)!!

        assertThat(notification.intervalDays(1)).isEqualTo(7)
        assertThat(notification.intervalDays(2)).isEqualTo(30)
        assertThat(reminder(notification, 1).title).isEqualTo("T")
        assertThat(reminder(notification, 2).title).isEqualTo("A")
        assertThat(reminder(notification, 2).description).isEqualTo("D")
    }

    @Test
    fun `drops a malformed config instead of crashing`() {
        val json = """{"title":"T","description":"D","scheduleIntervalDays":7,"maxReminders":"six"}"""

        assertThat(RemoteConfig.parse(json, AppUsageNotification::class.java)).isNull()
    }
}
