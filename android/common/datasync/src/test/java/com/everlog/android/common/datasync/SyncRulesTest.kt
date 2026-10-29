package com.everlog.android.common.datasync

import com.everlog.android.common.kotlin.Load
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SyncRulesTest {

    private val now = 1_790_000_000_000L
    private val hour = TimeUnit.HOURS.toMillis(1)
    private val week = TimeUnit.DAYS.toMillis(7)
    private val recent = SyncPlan.Recent(dateField = "date", months = 3)

    @Test
    fun `due when never synced, once the max age has passed, or when the clock moved back`() {
        assertThat(isDue(lastSynced = null, now, hour)).isTrue()
        assertThat(isDue(lastSynced = now - hour + 1, now, hour)).isFalse()
        assertThat(isDue(lastSynced = now - hour, now, hour)).isTrue()
        assertThat(isDue(lastSynced = now + 1, now, hour)).isTrue()
    }

    @Test
    fun `a max age of zero is always due`() {
        assertThat(isDue(lastSynced = now, now, maxAgeMillis = 0)).isTrue()
    }

    @Test
    fun `only reads further back than the window need the full history`() {
        assertThat(needsFullHistory(SyncPlan.Whole, Range.All)).isFalse()
        assertThat(needsFullHistory(recent, Range.All)).isTrue()
        assertThat(needsFullHistory(recent, Range.Last(months = 3))).isFalse()
        assertThat(needsFullHistory(recent, Range.Last(months = 4))).isTrue()
    }

    @Test
    fun `the work a read needs`() {
        val never = SyncStatus()
        val fresh = SyncStatus(lastSynced = now)
        val stale = SyncStatus(lastSynced = now - hour)
        val freshFull = SyncStatus(lastSynced = now, fullHistorySynced = now - 10 * hour)
        val staleFull = SyncStatus(lastSynced = now - hour, fullHistorySynced = now - 10 * hour)

        assertThat(syncWork(SyncPlan.Whole, Range.All, never, now, hour, week)).isEqualTo(SyncWork.Whole)
        assertThat(syncWork(SyncPlan.Whole, Range.All, fresh, now, hour, week)).isNull()
        assertThat(syncWork(recent, Range.Last(3), never, now, hour, week)).isEqualTo(SyncWork.Recent)
        assertThat(syncWork(recent, Range.Last(3), fresh, now, hour, week)).isNull()
        assertThat(syncWork(recent, Range.Last(3), stale, now, hour, week)).isEqualTo(SyncWork.Recent)
        assertThat(syncWork(recent, Range.All, fresh, now, hour, week)).isEqualTo(SyncWork.FullHistory)
        assertThat(syncWork(recent, Range.All, freshFull, now, hour, week)).isNull()
        assertThat(syncWork(recent, Range.All, staleFull, now, hour, week)).isEqualTo(SyncWork.Recent)
    }

    @Test
    fun `an all-time read syncs the full history again once it's a week old`() {
        val fresh = SyncStatus(lastSynced = now, fullHistorySynced = now - week + 1)
        val stale = SyncStatus(lastSynced = now, fullHistorySynced = now - week)

        assertThat(syncWork(recent, Range.All, fresh, now, hour, week)).isNull()
        assertThat(syncWork(recent, Range.All, stale, now, hour, week)).isEqualTo(SyncWork.FullHistory)
        assertThat(syncWork(recent, Range.Last(3), stale, now, hour, week)).isNull()
    }

    @Test
    fun `a running full history covers a recent sync, not the other way round`() {
        assertThat(SyncWork.FullHistory.covers(SyncWork.Recent)).isTrue()
        assertThat(SyncWork.Recent.covers(SyncWork.FullHistory)).isFalse()
        assertThat(SyncWork.Whole.covers(SyncWork.Whole)).isTrue()
    }

    @Test
    fun `a read is covered once synced as far back as it goes`() {
        val recentOnly = SyncStatus(lastSynced = now)

        assertThat(covers(SyncStatus(), SyncPlan.Whole, Range.All)).isFalse()
        assertThat(covers(recentOnly, SyncPlan.Whole, Range.All)).isTrue()
        assertThat(covers(recentOnly, recent, Range.Last(3))).isTrue()
        assertThat(covers(recentOnly, recent, Range.All)).isFalse()
        assertThat(covers(recentOnly.copy(fullHistorySynced = now), recent, Range.All)).isTrue()
    }

    @Test
    fun `what a screen shows`() {
        val syncing = SyncStatus(syncing = true)
        val failed = SyncStatus(lastFailure = IllegalStateException("offline"))
        val cached = listOf("bench")

        assertThat(loadOf(emptyList<String>(), isEmpty = true, covered = true, needsFullHistory = false, SyncStatus())).isEqualTo(Load.Ready(emptyList<String>()))
        assertThat(loadOf(cached, isEmpty = false, covered = false, needsFullHistory = false, syncing)).isEqualTo(Load.Ready(cached))
        assertThat(loadOf(cached, isEmpty = false, covered = false, needsFullHistory = true, syncing)).isEqualTo(Load.Loading)
        assertThat(loadOf(emptyList<String>(), isEmpty = true, covered = false, needsFullHistory = false, syncing)).isEqualTo(Load.Loading)
        assertThat(loadOf(emptyList<String>(), isEmpty = true, covered = false, needsFullHistory = false, failed)).isEqualTo(Load.Unavailable(failed.lastFailure!!))
        assertThat(loadOf(cached, isEmpty = false, covered = false, needsFullHistory = true, failed)).isEqualTo(Load.Unavailable(failed.lastFailure!!))
    }

    @Test
    fun `the window goes back calendar months`() {
        val zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val may31 = utc(2026, Calendar.MAY, 31)

            assertThat(windowStart(may31, months = 3)).isEqualTo(utc(2026, Calendar.FEBRUARY, 28))
        } finally {
            TimeZone.setDefault(zone)
        }
    }

    @Test
    fun `the max age is zero in debug builds, the default when Remote Config has none or a bad one`() {
        assertThat(maxAgeMillis(debug = true, remoteMinutes = 60, defaultMinutes = 60)).isEqualTo(0)
        assertThat(maxAgeMillis(debug = false, remoteMinutes = 15, defaultMinutes = 60)).isEqualTo(TimeUnit.MINUTES.toMillis(15))
        assertThat(maxAgeMillis(debug = false, remoteMinutes = null, defaultMinutes = 60)).isEqualTo(hour)
        assertThat(maxAgeMillis(debug = false, remoteMinutes = -5, defaultMinutes = 60)).isEqualTo(hour)
    }

    @Test
    fun `the full history's max age is the default when Remote Config has none or a bad one, debug builds too`() {
        assertThat(fullHistoryMaxAgeMillis(remoteDays = 30, defaultDays = 7)).isEqualTo(TimeUnit.DAYS.toMillis(30))
        assertThat(fullHistoryMaxAgeMillis(remoteDays = null, defaultDays = 7)).isEqualTo(week)
        assertThat(fullHistoryMaxAgeMillis(remoteDays = 0, defaultDays = 7)).isEqualTo(week)
        assertThat(fullHistoryMaxAgeMillis(remoteDays = -1, defaultDays = 7)).isEqualTo(week)
    }

    private fun utc(year: Int, month: Int, day: Int): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        clear()
        set(year, month, day)
        timeInMillis
    }
}
