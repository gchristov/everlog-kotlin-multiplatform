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
    private val day = TimeUnit.DAYS.toMillis(1)
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
    fun `only a read from before a recent collection's window reaches back`() {
        val windowFrom = windowStart(now, months = 3)

        assertThat(reachesBack(SyncPlan.Whole, from = null, now)).isNull()
        assertThat(reachesBack(recent, from = null, now)).isNull()
        assertThat(reachesBack(recent, from = windowFrom, now)).isNull()
        assertThat(reachesBack(recent, from = windowFrom - 1, now)).isEqualTo(windowFrom - 1)
    }

    @Test
    fun `the work a read needs`() {
        val yearAgo = now - 365 * day
        val never = SyncStatus()
        val fresh = SyncStatus(lastSynced = now)
        val stale = SyncStatus(lastSynced = now - hour)
        val freshBack = SyncStatus(lastSynced = now, syncedBackTo = yearAgo, syncedBackAt = now - 10 * hour)
        val staleBack = SyncStatus(lastSynced = now - hour, syncedBackTo = yearAgo, syncedBackAt = now - 10 * hour)

        assertThat(syncWork(SyncPlan.Whole, null, never, now, hour, week)).isEqualTo(SyncWork.Whole)
        assertThat(syncWork(SyncPlan.Whole, null, fresh, now, hour, week)).isNull()
        assertThat(syncWork(recent, null, never, now, hour, week)).isEqualTo(SyncWork.Recent())
        assertThat(syncWork(recent, null, fresh, now, hour, week)).isNull()
        assertThat(syncWork(recent, null, stale, now, hour, week)).isEqualTo(SyncWork.Recent())
        assertThat(syncWork(recent, yearAgo, fresh, now, hour, week)).isEqualTo(SyncWork.Recent(yearAgo))
        assertThat(syncWork(recent, yearAgo, freshBack, now, hour, week)).isNull()
        assertThat(syncWork(recent, yearAgo + day, freshBack, now, hour, week)).isNull()
        assertThat(syncWork(recent, yearAgo - day, freshBack, now, hour, week)).isEqualTo(SyncWork.Recent(yearAgo - day))
        assertThat(syncWork(recent, yearAgo, staleBack, now, hour, week)).isEqualTo(SyncWork.Recent())
    }

    @Test
    fun `the part older than the window syncs again once it's a week old, only as far back as the read goes`() {
        val yearAgo = now - 365 * day
        val fresh = SyncStatus(lastSynced = now, syncedBackTo = yearAgo - 30 * day, syncedBackAt = now - week + 1)
        val stale = SyncStatus(lastSynced = now, syncedBackTo = yearAgo - 30 * day, syncedBackAt = now - week)

        assertThat(syncWork(recent, yearAgo, fresh, now, hour, week)).isNull()
        assertThat(syncWork(recent, yearAgo, stale, now, hour, week)).isEqualTo(SyncWork.Recent(yearAgo))
        assertThat(syncWork(recent, null, stale, now, hour, week)).isNull()
    }

    @Test
    fun `a running sync covers one that goes no further back`() {
        assertThat(SyncWork.Recent(backTo = 5).covers(SyncWork.Recent())).isTrue()
        assertThat(SyncWork.Recent(backTo = 5).covers(SyncWork.Recent(backTo = 8))).isTrue()
        assertThat(SyncWork.Recent(backTo = 8).covers(SyncWork.Recent(backTo = 5))).isFalse()
        assertThat(SyncWork.Recent().covers(SyncWork.Recent(backTo = 5))).isFalse()
        assertThat(SyncWork.Whole.covers(SyncWork.Whole)).isTrue()
    }

    @Test
    fun `a read is covered once synced as far back as it goes`() {
        val recentOnly = SyncStatus(lastSynced = now)
        val back = recentOnly.copy(syncedBackTo = 100, syncedBackAt = now)

        assertThat(covers(SyncStatus(), backTo = null)).isFalse()
        assertThat(covers(recentOnly, backTo = null)).isTrue()
        assertThat(covers(recentOnly, backTo = 100)).isFalse()
        assertThat(covers(back, backTo = 100)).isTrue()
        assertThat(covers(back, backTo = 150)).isTrue()
        assertThat(covers(back, backTo = 50)).isFalse()
    }

    @Test
    fun `what a screen shows`() {
        val syncing = SyncStatus(syncing = true)
        val failed = SyncStatus(lastFailure = IllegalStateException("offline"))
        val cached = listOf("bench")

        assertThat(loadOf(emptyList<String>(), isEmpty = true, covered = true, needsComplete = false, SyncStatus())).isEqualTo(Load.Ready(emptyList<String>()))
        assertThat(loadOf(cached, isEmpty = false, covered = false, needsComplete = false, syncing)).isEqualTo(Load.Ready(cached))
        assertThat(loadOf(cached, isEmpty = false, covered = false, needsComplete = true, syncing)).isEqualTo(Load.Loading)
        assertThat(loadOf(emptyList<String>(), isEmpty = true, covered = false, needsComplete = false, syncing)).isEqualTo(Load.Loading)
        assertThat(loadOf(emptyList<String>(), isEmpty = true, covered = false, needsComplete = false, failed)).isEqualTo(Load.Unavailable(failed.lastFailure!!))
        assertThat(loadOf(cached, isEmpty = false, covered = false, needsComplete = true, failed)).isEqualTo(Load.Unavailable(failed.lastFailure!!))
    }

    @Test
    fun `the cache has caught up once it shows what the last sync brought`() {
        val synced = SyncStatus(lastSynced = 1, syncs = 1, lastFetched = 3)

        assertThat(caughtUp(cachedDocuments = 1, synced, cacheBehind = true)).isFalse()
        assertThat(caughtUp(cachedDocuments = 3, synced, cacheBehind = true)).isTrue()
        assertThat(caughtUp(cachedDocuments = 1, synced, cacheBehind = false)).isTrue()
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
    fun `the older part's max age is the default when Remote Config has none or a bad one, debug builds too`() {
        assertThat(olderMaxAgeMillis(remoteDays = 30, defaultDays = 7)).isEqualTo(TimeUnit.DAYS.toMillis(30))
        assertThat(olderMaxAgeMillis(remoteDays = null, defaultDays = 7)).isEqualTo(week)
        assertThat(olderMaxAgeMillis(remoteDays = 0, defaultDays = 7)).isEqualTo(week)
        assertThat(olderMaxAgeMillis(remoteDays = -1, defaultDays = 7)).isEqualTo(week)
    }

    private fun utc(year: Int, month: Int, day: Int): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        clear()
        set(year, month, day)
        timeInMillis
    }
}
