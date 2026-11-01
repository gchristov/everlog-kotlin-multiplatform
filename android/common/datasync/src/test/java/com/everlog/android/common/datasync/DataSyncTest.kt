package com.everlog.android.common.datasync

import com.everlog.android.common.kotlin.Load
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class DataSyncTest {

    private var now = 1_790_000_000_000L
    private var maxAge = TimeUnit.HOURS.toMillis(1)
    private val fullHistoryMaxAge = TimeUnit.DAYS.toMillis(7)
    private val records = InMemorySyncRecords()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val readers = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val dataSync = DataSync(
        records = records,
        settings = SyncSettings(maxAgeMillis = { maxAge }, fullHistoryMaxAgeMillis = { fullHistoryMaxAge }, timeoutMillis = 100, chunkSize = 2),
        clock = { now },
        scope = syncScope,
        stopTimeoutMillis = 0,
    )

    private val day = TimeUnit.DAYS.toMillis(1)
    private val recentPlan = SyncPlan.Recent(dateField = "date", months = 3)
    private val bench = Doc("bench", now - 2 * day)
    private val squat = Doc("squat", now - 10 * day)
    private val deadlift = Doc("deadlift", now - 200 * day)
    private val press = Doc("press", now - 400 * day)
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
        synced("library", fullHistory = false, at = now - 2 * maxAge)
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
        synced("library", fullHistory = false, at = now - 2 * maxAge)
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

    // Workouts: recent syncs, full history and ranges

    @Test
    fun `a recent read syncs only the window and shows what's cached meanwhile`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat, deadlift, press), cached = listOf(squat)).holdingServer()
        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 3)))

        assertThat(loads).containsExactly(Load.Ready(listOf(squat)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since")
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat)))
        assertThat(records.fullHistorySynced).isEmpty()
    }

    @Test
    fun `an all-time read waits for the full history, in chunks, even with some of it cached`() {
        val backend = FakeCollectionBackend(server = listOf(bench, squat, deadlift, press, row), cached = listOf(bench)).holdingServer()
        val loads = read(dataSync.observe(recent(backend), Range.All))

        assertThat(loads).containsExactly(Load.Loading)

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("chunk:null", "chunk:deadlift", "chunk:row").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, squat, deadlift, press, row)))
        assertThat(records.fullHistorySynced["user/workouts"]).isEqualTo(now)
        assertThat(records.lastSynced["user/workouts"]).isEqualTo(now)
    }

    @Test
    fun `an all-time read waits for the cache to show the full history once it has synced`() {
        val all = listOf(bench, deadlift)
        val backend = FakeCollectionBackend(server = all, cached = listOf(bench)).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(recent(backend)))
        backend.gate!!.complete(Unit)

        assertThat(loads).containsExactly(Load.Loading)

        backend.deliverCache()

        assertThat(loads).containsExactly(Load.Loading, Load.Ready(all)).inOrder()
    }

    @Test
    fun `an all-time read stays on its cache while a due recent sync lands`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.HOURS.toMillis(2))
        val all = listOf(bench, squat, deadlift, press, row)
        val backend = FakeCollectionBackend(server = all, cached = all).holdingServer().apply { cacheLate = true }
        val loads = read(dataSync.observe(recent(backend)))
        backend.gate!!.complete(Unit)
        backend.deliverCache()

        assertThat(loads).containsExactly(Load.Ready(all))
    }

    @Test
    fun `a read longer than the window needs the full history`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, press))

        val loads = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        assertThat(backend.calls).containsExactly("chunk:null", "chunk:deadlift").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, deadlift)))
    }

    @Test
    fun `readers asking for the full history at once share one full sync`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, press)).holdingServer()
        val overview = read(dataSync.observe(recent(backend), Range.All))
        val statistics = read(dataSync.observe(recent(backend), Range.Last(months = 12)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("chunk:null", "chunk:deadlift").inOrder()
        assertThat(overview.last()).isEqualTo(Load.Ready(listOf(bench, deadlift, press)))
        assertThat(statistics.last()).isEqualTo(Load.Ready(listOf(bench, deadlift)))
    }

    @Test
    fun `a full history asked for during a recent sync runs after it`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift)).holdingServer()
        read(dataSync.observe(recent(backend), Range.Last(months = 3)))
        val overview = read(dataSync.observe(recent(backend), Range.All))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("since", "chunk:null", "chunk:deadlift").inOrder()
        assertThat(overview.last()).isEqualTo(Load.Ready(listOf(bench, deadlift)))
    }

    @Test
    fun `after the full history, due syncs read the window and check the count`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench, press), cached = listOf(bench, press))

        read(dataSync.observe(recent(backend), Range.All))

        assertThat(backend.calls).containsExactly("since", "count").inOrder()
        assertThat(records.fullHistorySynced["user/workouts"]).isEqualTo(now - TimeUnit.HOURS.toMillis(2))
    }

    @Test
    fun `a week after the full history, an all-time read syncs it again, showing the cache meanwhile`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.DAYS.toMillis(7))
        // Edited on another phone months ago: same count, outside the window
        val edited = press.copy(name = "Overhead press")
        val backend = FakeCollectionBackend(server = listOf(bench, edited), cached = listOf(bench, press)).holdingServer()
        val loads = read(dataSync.observe(recent(backend), Range.All))

        assertThat(loads).containsExactly(Load.Ready(listOf(bench, press)))

        backend.gate!!.complete(Unit)

        assertThat(backend.calls).containsExactly("chunk:null", "chunk:press").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, edited)))
        assertThat(records.fullHistorySynced["user/workouts"]).isEqualTo(now)
    }

    @Test
    fun `within a week of the full history, a due sync reads only the window`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.DAYS.toMillis(6))
        val backend = FakeCollectionBackend(server = listOf(bench, press), cached = listOf(bench, press))

        read(dataSync.observe(recent(backend), Range.All))

        assertThat(backend.calls).containsExactly("since", "count").inOrder()
    }

    @Test
    fun `a count that differs from the cache's syncs the full history again`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.HOURS.toMillis(2))
        // Added on another phone months ago, so the window doesn't bring it
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, press), cached = listOf(bench, press))

        val loads = read(dataSync.observe(recent(backend), Range.All))

        assertThat(backend.calls).containsExactly("since", "count", "chunk:null", "chunk:deadlift").inOrder()
        assertThat(loads.last()).isEqualTo(Load.Ready(listOf(bench, deadlift, press)))
        assertThat(records.fullHistorySynced["user/workouts"]).isEqualTo(now)
    }

    @Test
    fun `edits waiting to upload don't trigger a full sync`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench), cached = listOf(bench))
        backend.write(squat, uploaded = false)

        read(dataSync.observe(recent(backend), Range.All))

        assertThat(backend.calls).containsExactly("since", "count").inOrder()
    }

    @Test
    fun `a count that fails still counts the recent sync`() {
        synced("user/workouts", fullHistory = true, at = now - TimeUnit.HOURS.toMillis(2))
        val backend = FakeCollectionBackend(server = listOf(bench), cached = listOf(bench))
        backend.fail = { call -> if (call == "count") IllegalStateException("offline") else null }

        read(dataSync.observe(recent(backend), Range.All))

        assertThat(records.lastSynced["user/workouts"]).isEqualTo(now)
        assertThat(dataSync.status.value["user/workouts"]?.lastFailure).isNull()
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
    fun `a full sync that fails part way isn't recorded, and starts over next time`() {
        val backend = FakeCollectionBackend(server = listOf(bench, deadlift, press))
        backend.fail = { call -> if (call == "chunk:deadlift") IllegalStateException("offline") else null }
        val failed = read(dataSync.observe(recent(backend), Range.All))

        assertThat(failed.last()).isInstanceOf(Load.Unavailable::class.java)
        assertThat(records.fullHistorySynced).isEmpty()

        backend.fail = { null }
        backend.calls.clear()
        read(dataSync.observe(recent(backend), Range.All))

        assertThat(backend.calls).containsExactly("chunk:null", "chunk:deadlift").inOrder()
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
        synced("user/plans", fullHistory = false, at = now)
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

    private fun synced(key: String, fullHistory: Boolean, at: Long) {
        records.lastSynced[key] = at
        if (fullHistory) records.fullHistorySynced[key] = at
    }

    private fun <B : FakeServer> B.holdingServer(): B = apply { gate = CompletableDeferred() }

    // Everything a reader is shown, as it arrives
    private fun <T> read(flow: Flow<T>): List<T> {
        val loads = CopyOnWriteArrayList<T>()
        readers.launch { flow.collect { loads += it } }
        return loads
    }

    private fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertThat(condition()).isTrue()
    }
}
