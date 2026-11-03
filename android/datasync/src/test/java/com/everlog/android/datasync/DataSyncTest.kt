package com.everlog.android.datasync

import com.everlog.android.common.kotlin.Load
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class DataSyncTest {

    private var now = 1_790_000_000_000L
    private var maxAge = TimeUnit.HOURS.toMillis(1)
    private val olderMaxAge = TimeUnit.DAYS.toMillis(7)
    private val records = InMemorySyncRecords()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val readers = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val dataSync = DataSync(
        records = records,
        settings = SyncSettings(maxAgeMillis = { maxAge }, olderMaxAgeMillis = { olderMaxAge }, timeoutMillis = 100, chunkSize = 2),
        clock = { now },
        scope = syncScope,
        stopTimeoutMillis = 0,
    )

    private val day = TimeUnit.DAYS.toMillis(1)
    private val recentPlan = SyncPlan.Recent(dateField = "date", months = 3)
    private val yearBack = windowStart(now, months = 12)
    private val bench = Doc("bench", now - 2 * day)
    private val squat = Doc("squat", now - 10 * day)
    private val deadlift = Doc("deadlift", now - 200 * day)
    private val press = Doc("press", now - 400 * day)
    private val curl = Doc("curl", now - 150 * day)
    private val dip = Doc("dip", now - 250 * day)
    private val row = Doc("row", now - 500 * day)

    @After
    fun tearDown() {
        readers.cancel()
        syncScope.cancel()
    }

    // Main journeys

    @Test
    fun `a first read shows the shimmer, then what the server has`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat)).holdingServer()
        val loads = read(dataSync.observe(whole(backend)))

        assertThat(loads).containsExactly(Load.Loading)

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
        assertThat(records.lastSynced["user/routines"]).isEqualTo(now)
    }

    @Test
    fun `what's cached shows straight away, and the sync's result once it lands`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench)).holdingServer()
        val loads = read(dataSync.observe(whole(backend)))

        assertThat(loads).containsExactly(Load.Ready(listOf(bench)))

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
    }

    @Test
    fun `a complete-only collection waits for its first sync rather than showing the part that's cached`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench)).holdingServer()
        val loads = read(dataSync.observe(completeOnly(backend)))

        assertThat(loads).containsExactly(Load.Loading)

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
    }

    @Test
    fun `a complete-only collection shows its cache straight away once it has synced`() {
        synced("library", at = now - 2 * maxAge)
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench, squat)).holdingServer()
        val loads = read(dataSync.observe(completeOnly(backend)))

        assertThat(loads).containsExactly(Load.Ready(listOf(bench, squat)))
    }

    @Test
    fun `an empty collection is ready once the server says so`() {
        val loads = read(dataSync.observe(whole(FakeCollectionBackend())))

        assertThat(loads.last()).isEqualTo(Load.Ready(emptyList<Doc>()))
    }

    @Test
    fun `a read within the max age stays on the cache`() {
        records.lastSynced["user/routines"] = now - TimeUnit.MINUTES.toMillis(59)
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench))

        val loads = read(dataSync.observe(whole(backend)))

        assertThat(backend.calls).isEmpty()
        assertThat(loads).containsExactly(Load.Ready(listOf(bench)))
    }

    @Test
    fun `a read once the max age has passed syncs again`() {
        records.lastSynced["user/routines"] = now - TimeUnit.MINUTES.toMillis(60)
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench))

        val loads = read(dataSync.observe(whole(backend)))

        assertThat(backend.calls).containsExactly("all")
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
    }

    @Test
    fun `local writes reach every reader without a sync`() {
        val backend = FakeCollectionBackend(server = listOf(bench))
        val first = read(dataSync.observe(whole(backend)))
        val second = read(dataSync.observe(whole(backend)))
        val renamed = bench.copy(name = "Bench press")

        backend.write(renamed)

        assertThat(first.last()).isEqualTo(Load.Ready(listOf(renamed)))
        assertThat(second.last()).isEqualTo(Load.Ready(listOf(renamed)))
        assertThat(backend.calls).containsExactly("all")
    }

    @Test
    fun `a reader after the listener stopped sees writes made meanwhile, not the last reader's copy`() {
        val backend = FakeCollectionBackend(server = listOf(bench))
        val firstReader = readers.launch { dataSync.observe(whole(backend)).collect {} }
        firstReader.cancel()
        val renamed = bench.copy(name = "Bench press")

        backend.write(renamed)
        val loads = read(dataSync.observe(whole(backend)))

        assertThat(loads.first()).isEqualTo(Load.Ready(listOf(renamed)))
    }

    @Test
    fun `readers at the same time share one sync`() {
        val backend = FakeCollectionBackend(server = listOf(bench)).holdingServer()
        val first = read(dataSync.observe(whole(backend)))
        val second = read(dataSync.observe(whole(backend)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("all")
        assertThat(first.last()).isEqualTo(Load.Ready(listOf(bench)))
        assertThat(second.last()).isEqualTo(Load.Ready(listOf(bench)))
    }

    @Test
    fun `a sync that finishes before the cache shows what it brought doesn't show empty`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat)).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(whole(backend)))
        backend.gate!!.complete(Unit)

        assertThat(loads).containsExactly(Load.Loading)

        backend.deliverCache()

        assertThat(loads).containsExactly(Load.Loading, Load.Ready(listOf(bench, squat))).inOrder()
    }

    @Test
    fun `a complete-only collection's first sync waits for the cache to show what it brought`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench)).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(completeOnly(backend)))
        backend.gate!!.complete(Unit)

        assertThat(loads).containsExactly(Load.Loading)

        backend.deliverCache()

        assertThat(loads).containsExactly(Load.Loading, Load.Ready(listOf(bench, squat))).inOrder()
    }

    @Test
    fun `a complete-only collection shows a delete made right after its first sync`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench))
        val loads = read(dataSync.observe(completeOnly(backend)))

        backend.cache.value = listOf(bench)

        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench)))
    }

    @Test
    fun `a complete-only collection stays on its cache while a later sync lands`() {
        synced("library", at = now - 2 * maxAge)
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench, squat)).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(completeOnly(backend)))
        backend.gate!!.complete(Unit)
        backend.deliverCache()

        assertThat(loads).containsExactly(Load.Ready(listOf(bench, squat)))
    }

    @Test
    fun `an empty collection is ready as soon as its sync finishes, without waiting for the cache`() {
        val backend = FakeCollectionBackend().holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(whole(backend)))

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(Load.Ready(emptyList<Doc>()))
    }

    @Test
    fun `a new reader isn't held back by syncs its cache already shows`() {
        val backend = FakeCollectionBackend(server = listOf(bench))
        read(dataSync.observe(whole(backend)))
        backend.server.clear()
        backend.cache.value = emptyList()
        now += TimeUnit.MINUTES.toMillis(1)

        val loads = read(dataSync.observe(whole(backend)))

        assertThat(loads.last()).isEqualTo(Load.Ready(emptyList<Doc>()))
    }

    // Workouts: the recent window, and reaching further back

    @Test
    fun `a recent read syncs only the window and shows what's cached meanwhile`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat, deadlift, press), cached = listOf(squat)).holdingServer()
        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(loads).containsExactly(Load.Ready(listOf(squat)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since")
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
        assertThat(records.syncedBack).isEmpty()
    }

    @Test
    fun `a read further back than the window waits for that part, in pages, and no further`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat, curl, deadlift, dip, press, row), cached = listOf(bench)).holdingServer()
        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(loads).containsExactly(Load.Loading)

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since", "between:$yearBack:null", "between:$yearBack:deadlift").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat, curl, deadlift, dip)))
        assertThat(records.syncedBack["user/workouts"]).isEqualTo(SyncedBack(to = yearBack, at = now))
        assertThat(records.lastSynced["user/workouts"]).isEqualTo(now)
    }

    @Test
    fun `a read within the part synced back reads the cache without syncing`() {
        syncedBack(to = yearBack - day, at = now - TimeUnit.HOURS.toMillis(2), lastSynced = now)
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift), cached = listOf(bench, deadlift))

        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(backend.calls).isEmpty()
        assertThat(loads).containsExactly(Load.Ready(listOf(bench, deadlift)))
    }

    @Test
    fun `a read further back stays on its cache while a due recent sync lands`() {
        syncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2))
        val cached = listOf(bench, squat, deadlift)
        val backend = FakeCollectionBackend(server = cached, cached = cached).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 12)))
        backend.gate!!.complete(Unit)
        backend.deliverCache()

        assertThat(loads).containsExactly(Load.Ready(cached))
    }

    @Test
    fun `a complete read of the window waits for its first sync, while a list of it shows the cache`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(squat)).holdingServer()
        val calculation = read(dataSync.observe(recent(backend), Range.Last(months = 3), complete = true))
        val list = read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(calculation).containsExactly(Load.Loading)
        assertThat(list).containsExactly(Load.Ready(listOf(squat)))

        backend.gate!!.complete(Unit)

        assertThat(calculation.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
    }

    @Test
    fun `a complete read of the window that can't sync before its first is unavailable`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(squat))
        backend.fail = { IllegalStateException("offline") }

        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 3), complete = true))

        assertThat(loads.last()).isInstanceOf(Load.Unavailable::class.java)
    }

    @Test
    fun `readers reaching back at once share one sync`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, press)).holdingServer()
        val statistics = read(dataSync.observe(recent(backend), Range.Last(months = 12)))
        val read = readers.async { dataSync.read(recent(backend), yearBack, null) }

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since", "between:$yearBack:null").inOrder()
        assertThat(statistics.last()).isEqualTo(Load.Ready(listOf(bench, deadlift)))
        assertThat(read.docs()).containsExactly(bench, deadlift)
    }

    @Test
    fun `a read reaching back during a recent sync runs after it`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift)).holdingServer()
        read(dataSync.observe(recent(backend), Range.Last(months = 3)))
        val statistics = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since", "since", "between:$yearBack:null").inOrder()
        assertThat(statistics.last()).isEqualTo(Load.Ready(listOf(bench, deadlift)))
    }

    @Test
    fun `after syncing back, due syncs read the window and check the count of what was synced back`() {
        syncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift), cached = listOf(bench, deadlift))

        read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(backend.calls).containsExactly("since", "count").inOrder()
        assertThat(backend.cacheCounts.first().first).isEqualTo(yearBack)
        assertThat(records.syncedBack["user/workouts"]).isEqualTo(SyncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2)))
    }

    @Test
    fun `the cache is counted a year at a time`() {
        val longAgo = now - 800 * day
        syncedBack(to = longAgo, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench), cached = listOf(bench))

        read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(backend.cacheCounts).containsExactly(
            longAgo to longAgo + 365 * day,
            longAgo + 365 * day to longAgo + 730 * day,
            longAgo + 730 * day to null,
        ).inOrder()
    }

    @Test
    fun `a week after syncing back, a read reaching back syncs that part again, showing the cache meanwhile`() {
        syncedBack(to = yearBack, at = now - TimeUnit.DAYS.toMillis(7), lastSynced = now)
        // Edited on another phone months ago: same count, outside the window
        val edited = deadlift.copy(name = "Romanian deadlift")
        val backend = FakeCollectionBackend(server = listOf(bench, edited), cached = listOf(bench, deadlift)).holdingServer()
        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(loads).containsExactly(Load.Ready(listOf(bench, deadlift)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since", "between:$yearBack:null").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, edited)))
        assertThat(records.syncedBack["user/workouts"]).isEqualTo(SyncedBack(to = yearBack, at = now))
    }

    @Test
    fun `a week after syncing back further, a read syncs again only as far back as it goes`() {
        syncedBack(to = now - 700 * day, at = now - TimeUnit.DAYS.toMillis(7), lastSynced = now)
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift), cached = listOf(bench, deadlift))

        read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(backend.calls).containsExactly("since", "between:$yearBack:null").inOrder()
        assertThat(records.syncedBack["user/workouts"]).isEqualTo(SyncedBack(to = yearBack, at = now))
    }

    @Test
    fun `within a week of syncing back, a due sync reads only the window`() {
        syncedBack(to = yearBack, at = now - TimeUnit.DAYS.toMillis(6))
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift), cached = listOf(bench, deadlift))

        read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(backend.calls).containsExactly("since", "count").inOrder()
    }

    @Test
    fun `a count that differs from the cache's syncs the part synced back again`() {
        syncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2))
        // Added on another phone months ago, so the window doesn't bring it
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, dip), cached = listOf(bench, dip))

        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(backend.calls).containsExactly("since", "count", "between:$yearBack:null", "between:$yearBack:deadlift").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, deadlift, dip)))
        assertThat(records.syncedBack["user/workouts"]).isEqualTo(SyncedBack(to = yearBack, at = now))
    }

    @Test
    fun `edits waiting to upload don't trigger a sync of the part synced back`() {
        syncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench), cached = listOf(bench))
        backend.write(squat, uploaded = false)

        read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(backend.calls).containsExactly("since", "count").inOrder()
    }

    @Test
    fun `a count that fails still counts the recent sync`() {
        syncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench), cached = listOf(bench))
        backend.fail = { call -> if (call == "count") IllegalStateException("offline") else null }

        read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(records.lastSynced["user/workouts"]).isEqualTo(now)
        assertThat(dataSync.status.value["user/workouts"]?.lastFailure).isNull()
    }

    // Reading a part of the history once

    @Test
    fun `a read within the window waits for its first sync, then reads that part of the cache once`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat, deadlift), cached = listOf(bench)).holdingServer()
        val read = readers.async { dataSync.read(recent(backend), now - 5 * day, null) }

        assertThat(read.isCompleted).isFalse()

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since")
        assertThat(read.docs()).containsExactly(bench)
        assertThat(backend.cacheReads).containsExactly(now - 5 * day to null)
    }

    @Test
    fun `a read further back than the window waits for that part the first time`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat, deadlift, press, row), cached = listOf(bench)).holdingServer()
        val read = readers.async { dataSync.read(recent(backend), yearBack, now - 100 * day) }

        assertThat(read.isCompleted).isFalse()

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since", "between:$yearBack:null").inOrder()
        assertThat(read.docs()).containsExactly(deadlift)
        assertThat(records.syncedBack["user/workouts"]).isEqualTo(SyncedBack(to = yearBack, at = now))
    }

    @Test
    fun `a read of a part synced back reads the cache without waiting`() {
        syncedBack(to = yearBack, at = now - TimeUnit.HOURS.toMillis(2), lastSynced = now)
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift), cached = listOf(bench, deadlift)).holdingServer()

        val read = readers.async { dataSync.read(recent(backend), yearBack, now - 100 * day) }

        assertThat(read.docs()).containsExactly(deadlift)
    }

    @Test
    fun `a read further back than what was synced back waits for the rest`() {
        syncedBack(to = now - 200 * day, at = now - TimeUnit.HOURS.toMillis(2), lastSynced = now)
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, dip), cached = listOf(bench, deadlift)).holdingServer()

        val read = readers.async { dataSync.read(recent(backend), yearBack, null) }

        assertThat(read.isCompleted).isFalse()
        backend.gate!!.complete(Unit)
        assertThat(read.docs()).containsExactly(bench, deadlift, dip)
    }

    @Test
    fun `a read that can't sync the part it needs is unavailable`() {
        val backend = FakeCollectionBackend(server = listOf(bench, press), cached = listOf(bench)).apply { fail = { IllegalStateException("offline") } }

        val read = readers.async { dataSync.read(recent(backend), yearBack, null) }

        assertThat((read.completed() as Load.Unavailable).error).hasMessageThat().isEqualTo("offline")
        assertThat(backend.cacheReads).isEmpty()
    }

    @Test
    fun `a read after a failed sync tries again rather than failing straight away`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift)).apply { fail = { IllegalStateException("offline") } }
        readers.async { dataSync.read(recent(backend), yearBack, null) }
        backend.fail = { null }

        val read = readers.async { dataSync.read(recent(backend), yearBack, null) }

        assertThat(read.docs()).containsExactly(bench, deadlift)
    }

    // Fetching documents by id

    @Test
    fun `documents fetched by id reach readers, and the ones the server doesn't have are left out`() {
        synced("user/routines", at = now)
        val backend = FakeCollectionBackend(server = listOf(bench, squat, deadlift), cached = listOf(bench))
        val loads = read(dataSync.observe(whole(backend)))

        val fetched = runBlocking { dataSync.fetchIds(whole(backend), setOf("squat", "missing")) }

        assertThat(fetched).isEqualTo(Load.Ready(setOf("squat")))
        assertThat(backend.calls).containsExactly("ids:squat,missing")
        assertThat((loads.last() as Load.Ready).value).containsExactly(bench, squat)
    }

    @Test
    fun `ids are fetched at most thirty at a time`() {
        val backend = FakeCollectionBackend()

        runBlocking { dataSync.fetchIds(whole(backend), (1..65).map { "id$it" }.toSet()) }

        assertThat(backend.calls).hasSize(3)
    }

    @Test
    fun `no ids fetch nothing`() {
        val backend = FakeCollectionBackend()

        val fetched = runBlocking { dataSync.fetchIds(whole(backend), emptySet()) }

        assertThat(fetched).isEqualTo(Load.Ready(emptySet<String>()))
        assertThat(backend.calls).isEmpty()
    }

    @Test
    fun `fetching ids that fails is unavailable, leaving the cache as it was`() {
        val backend = FakeCollectionBackend(server = listOf(squat), cached = listOf(bench)).apply { fail = { IllegalStateException("offline") } }

        val fetched = runBlocking { dataSync.fetchIds(whole(backend), setOf("squat")) }

        assertThat((fetched as Load.Unavailable).error).hasMessageThat().isEqualTo("offline")
        assertThat(backend.cache.value).containsExactly(bench)
    }

    @Test
    fun `fetching ids from a server that doesn't answer times out`() {
        val backend = FakeCollectionBackend(server = listOf(squat)).apply { hang = true }

        val fetched = runBlocking { dataSync.fetchIds(whole(backend), setOf("squat")) }

        assertThat((fetched as Load.Unavailable).error).isInstanceOf(SyncTimeoutException::class.java)
    }

    // Failures

    @Test
    fun `a first read that can't reach the server is unavailable, and reading again retries`() {
        val backend = FakeCollectionBackend(server = listOf(bench))
        backend.fail = { IllegalStateException("offline") }
        val failed = read(dataSync.observe(whole(backend)))

        assertThat(failed.last()).isInstanceOf(Load.Unavailable::class.java)
        assertThat(records.lastSynced).isEmpty()

        backend.fail = { null }
        readers.coroutineContext[kotlinx.coroutines.Job]!!.children.forEach { it.cancel() }
        val retried = read(dataSync.observe(whole(backend)))

        assertThat(retried.last()).isEqualTo(Load.Ready(listOf(bench)))
    }

    @Test
    fun `a complete-only collection that can't sync before its first is unavailable, whatever is cached`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench))
        backend.fail = { IllegalStateException("offline") }

        val loads = read(dataSync.observe(completeOnly(backend)))

        assertThat(loads.last()).isInstanceOf(Load.Unavailable::class.java)
    }

    @Test
    fun `a sync that fails with data cached stays on the cache`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat), cached = listOf(bench))
        backend.fail = { IllegalStateException("offline") }

        val loads = read(dataSync.observe(whole(backend)))

        assertThat(loads).containsExactly(Load.Ready(listOf(bench)))
        assertThat(dataSync.status.value["user/routines"]?.lastFailure).hasMessageThat().isEqualTo("offline")
    }

    @Test
    fun `a server that doesn't answer times out`() {
        val backend = FakeCollectionBackend(server = listOf(bench))
        backend.hang = true

        val loads = read(dataSync.observe(whole(backend)))

        awaitUntil { loads.lastOrNull() is Load.Unavailable }
        assertThat((loads.last() as Load.Unavailable).error).isInstanceOf(SyncTimeoutException::class.java)
    }

    @Test
    fun `a sync back that fails part way isn't recorded, and starts over next time`() {
        val backend = FakeCollectionBackend(server = listOf(bench, curl, deadlift, dip))
        backend.fail = { call -> if (call == "between:$yearBack:deadlift") IllegalStateException("offline") else null }
        val failed = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(failed.last()).isInstanceOf(Load.Unavailable::class.java)
        assertThat(records.syncedBack).isEmpty()

        backend.fail = { null }
        backend.calls.clear()
        read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(backend.calls).containsExactly("since", "between:$yearBack:null", "between:$yearBack:deadlift").inOrder()
    }

    @Test
    fun `a sync time in the future counts as due`() {
        records.lastSynced["user/routines"] = now + day
        val backend = FakeCollectionBackend(server = listOf(bench))

        read(dataSync.observe(whole(backend)))

        assertThat(backend.calls).containsExactly("all")
    }

    // Documents

    @Test
    fun `a missing document shows the shimmer until the server says it doesn't exist`() {
        val backend = FakeDocumentBackend(server = null).holdingServer()
        val loads = read(dataSync.observe(document(backend)))

        assertThat(loads).containsExactly(Load.Loading)

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(Load.Ready(null))
    }

    @Test
    fun `a document whose sync finishes before the cache shows it isn't reported missing`() {
        val backend = FakeDocumentBackend(server = bench).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(document(backend)))
        backend.gate!!.complete(Unit)

        assertThat(loads).containsExactly(Load.Loading)

        backend.deliverCache()

        assertThat(loads.last()).isEqualTo(Load.Ready(bench))
    }

    @Test
    fun `a cached document shows straight away, then the server's copy`() {
        val renamed = bench.copy(name = "Bench press")
        val backend = FakeDocumentBackend(server = renamed, cached = bench).holdingServer()
        val loads = read(dataSync.observe(document(backend)))

        assertThat(loads).containsExactly(Load.Ready(bench))

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(Load.Ready(renamed))
    }

    // Status and signing out

    @Test
    fun `the status shows a sync running, then when it synced`() {
        val backend = FakeCollectionBackend(server = listOf(bench)).holdingServer()
        read(dataSync.observe(whole(backend)))

        assertThat(dataSync.status.value["user/routines"]).isEqualTo(SyncStatus(syncing = true))

        backend.gate!!.complete(Unit)

        assertThat(dataSync.status.value["user/routines"]).isEqualTo(SyncStatus(lastSynced = now, syncs = 1, lastFetched = 1))
    }

    @Test
    fun `signing out ends reads, stops syncs and forgets them`() {
        synced("user/plans", at = now)
        val backend = FakeCollectionBackend(server = listOf(bench)).holdingServer()
        var ended = false
        readers.launch {
            dataSync.observe(whole(backend)).collect {}
            ended = true
        }

        dataSync.signOut()
        backend.gate!!.complete(Unit)

        assertThat(ended).isTrue()
        assertThat(records.lastSynced).isEmpty()
        assertThat(dataSync.status.value).isEmpty()
    }

    // Helpers

    private fun whole(backend: FakeCollectionBackend) = CollectionSource("routines", "user", SyncPlan.Whole, backend)

    private fun completeOnly(backend: FakeCollectionBackend) = CollectionSource("library", null, SyncPlan.Whole, backend, completeOnly = true)

    private fun recent(backend: FakeCollectionBackend) = CollectionSource("workouts", "user", recentPlan, backend)

    private fun document(backend: FakeDocumentBackend) = DocumentSource("profile", "user", backend)

    private fun synced(key: String, at: Long) {
        records.lastSynced[key] = at
    }

    private fun syncedBack(to: Long, at: Long, lastSynced: Long = at) {
        records.lastSynced["user/workouts"] = lastSynced
        records.syncedBack["user/workouts"] = SyncedBack(to, at)
    }

    private fun <B : FakeServer> B.holdingServer(): B = apply { gate = CompletableDeferred() }

    // Everything a reader is shown, as it arrives
    private fun <T> read(flow: Flow<T>): List<T> {
        val loads = CopyOnWriteArrayList<T>()
        readers.launch { flow.collect { loads += it } }
        return loads
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> Deferred<T>.completed(): T = getCompleted()

    // What a one-off read found, in no particular order
    private fun Deferred<Load<List<Doc>>>.docs(): List<Doc> = (completed() as Load.Ready).value

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertThat(condition()).isTrue()
    }
}
