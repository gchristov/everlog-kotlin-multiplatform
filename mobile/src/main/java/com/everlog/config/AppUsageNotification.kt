package com.everlog.config

import java.io.Serializable
import java.util.*

/**
 * Remote Config for the reminder shown when the user hasn't been in the app for a while.
 *
 * Reminder 1 is due [intervalDays] (0) after the user was last active, reminder 2 [intervalDays] (1)
 * after reminder 1 was shown, and so on, always at [scheduleHourOfDay] local time. Any visit starts
 * the sequence again from reminder 1. [title], [description] and [scheduleIntervalDays] are the
 * original fields, still read by older app versions, and are the fallback for the newer ones.
 */
data class AppUsageNotification (

        var title: String? = null,
        var description: String? = null,
        var scheduleIntervalDays: Int = 0,
        var scheduleHourOfDay: Int = 0,
        // Gap before each reminder, the last one repeats. Missing means every scheduleIntervalDays.
        // Nullable entries as Gson reads a stray null in the console's JSON into the list.
        var backoffIntervalsDays: List<Int?>? = null,
        // Stop after this many reminders without a visit. 0 means never stop.
        var maxReminders: Int = 0,
        // Text for each reminder, the last one repeats. Missing fields fall back to title/description.
        var messages: List<Message?>? = null

) : Serializable {

    data class Message (

            var title: String? = null,
            var description: String? = null

    ) : Serializable

    data class Reminder (

            // 1 for the first reminder since the user was last active
            val attempt: Int,
            val dueAtMillis: Long,
            val title: String,
            val description: String

    ) {

        fun isDue(now: Long): Boolean {
            return now >= dueAtMillis
        }

        /**
         * When to set the alarm for this reminder. An overdue reminder (the phone was off, or the app
         * was just updated) waits for the next [hourOfDay] rather than showing straight away, which
         * could be the middle of the night.
         */
        fun alarmAtMillis(now: Long, hourOfDay: Int): Long {
            if (dueAtMillis >= now) {
                return dueAtMillis
            }
            val next = atHourOfDay(now, 0, hourOfDay)
            return if (next >= now) next else atHourOfDay(now, 1, hourOfDay)
        }
    }

    /**
     * The next reminder to show, or null if there's none (the user has had [maxReminders] reminders
     * since they were last active, or we don't know when that was).
     *
     * [shownCount] is the number of reminders shown in a row, the last one at [lastShownAt]. It only
     * counts if that was after [lastActiveAt], otherwise the user has been back since. Times later than
     * [now] mean the device clock was wrong, then corrected: [lastActiveAt] is treated as [now], and
     * [lastShownAt] as never, starting the sequence again. Otherwise they'd push reminders back until
     * real time catches up.
     */
    fun nextReminder(lastActiveAt: Long, lastShownAt: Long, shownCount: Int, now: Long): Reminder? {
        if (lastActiveAt <= 0) {
            return null
        }
        val active = minOf(lastActiveAt, now)
        val shown = if (lastShownAt > now) 0 else lastShownAt
        val index = if (shown > active) maxOf(shownCount, 0) else 0
        if (maxReminders > 0 && index >= maxReminders) {
            return null
        }
        val anchor = if (index == 0) active else shown
        val message = messages?.filterNotNull()?.takeIf { it.isNotEmpty() }?.let { it[minOf(index, it.size - 1)] }
        return Reminder(
                attempt = index + 1,
                dueAtMillis = atHourOfDay(anchor, intervalDays(index), scheduleHourOfDay),
                title = message?.title?.takeUnless { it.isBlank() } ?: title!!,
                description = message?.description?.takeUnless { it.isBlank() } ?: description!!)
    }

    fun intervalDays(index: Int): Int {
        val intervals = backoffIntervalsDays?.filterNotNull()?.filter { it > 0 }
        if (intervals.isNullOrEmpty()) {
            return scheduleIntervalDays
        }
        return intervals[minOf(index, intervals.size - 1)]
    }

    fun isValid(): Boolean {
        return (!title.isNullOrEmpty()
                && !description.isNullOrEmpty()
                && scheduleIntervalDays > 0
                && scheduleHourOfDay >= 0
                && scheduleHourOfDay <= 24)
    }

    companion object {

        // Calendar days rather than 24h blocks so the hour stays put across daylight saving changes
        private fun atHourOfDay(fromMillis: Long, plusDays: Int, hourOfDay: Int): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = fromMillis
            cal[Calendar.HOUR_OF_DAY] = hourOfDay
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            cal[Calendar.MILLISECOND] = 0
            cal.add(Calendar.DAY_OF_MONTH, plusDays)
            return cal.timeInMillis
        }
    }
}
