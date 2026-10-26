package com.everlog.data.sync

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

class PagedReadTest {

    private val now = 1_790_000_000_000L
    private val day = TimeUnit.DAYS.toMillis(1)
    private val records = InMemorySyncRecords()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val readers = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val dataSync = DataSync(
        records = records,
        settings = SyncSettings(maxAgeMillis = { TimeUnit.HOURS.toMillis(1) }, timeoutMillis = 100, chunkSize = 100),
        clock = { now },
        scope = syncScope,
        stopTimeoutMillis = 0,
    )

    // Every third day for most of a year, newest first: the last 3 months are the first 31
    private val workouts = (0 until 104).map { Doc("w%03d".format(it), now - it * 3 * day - day) }

    @After
    fun tearDown() {
        readers.cancel()
        syncScope.cancel()
    }

    @Test
    fun `the first page comes from the recent sync`() {
        val backend = FakeCollectionBackend(server = workouts)
        val pages = dataSync.pages(source(backend), pageSize = 10)

        val loads = read(pages.loads)

        assertThat(backend.calls).containsExactly("since")
        assertThat(loads.last()).isEqualTo(ready(workouts.take(10)))
    }

    @Test
    fun `the shimmer shows until the recent sync lands`() {
        val backend = FakeCollectionBackend(server = workouts).apply { gate = CompletableDeferred() }
        val loads = read(dataSync.pages(source(backend), pageSize = 10).loads)

        assertThat(loads).containsExactly(Load.Loading)

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(ready(workouts.take(10)))
    }

    @Test
    fun `more pages come from the cache while it has them`() {
        val backend = FakeCollectionBackend(server = workouts)
        val pages = dataSync.pages(source(backend), pageSize = 10)
        val loads = read(pages.loads)

        pages.loadMore()
        pages.loadMore()

        assertThat(backend.calls).containsExactly("since")
        assertThat(loads.last()).isEqualTo(ready(workouts.take(30)))
    }

    @Test
    fun `past what's cached, the next page comes from the server`() {
        val backend = FakeCollectionBackend(server = workouts)
        val pages = dataSync.pages(source(backend), pageSize = 20)
        val loads = read(pages.loads)
        backend.gate = CompletableDeferred()

        pages.loadMore()

        assertThat(backend.calls.last()).isEqualTo("page:31")
        assertThat(loads.last()).isEqualTo(ready(workouts.take(31), loadingMore = true))

        backend.gate!!.complete(Unit)

        assertThat(loads.last()).isEqualTo(ready(workouts.take(40)))
    }

    @Test
    fun `a short recent window fetches older workouts for the first page straight away`() {
        val sparse = listOf(Doc("recent", now - day)) + workouts.drop(40)
        val backend = FakeCollectionBackend(server = sparse)

        val loads = read(dataSync.pages(source(backend), pageSize = 10).loads)

        assertThat(backend.calls).containsExactly("since", "page:1").inOrder()
        assertThat(loads.last()).isEqualTo(ready(sparse.take(10)))
    }

    @Test
    fun `the end is reached when the server has nothing older`() {
        val backend = FakeCollectionBackend(server = workouts.take(30))
        val pages = dataSync.pages(source(backend), pageSize = 20)
        val loads = read(pages.loads)

        pages.loadMore()

        assertThat(loads.last()).isEqualTo(ready(workouts.take(30), endReached = true))

        pages.loadMore()

        assertThat(backend.calls).containsExactly("since", "page:30").inOrder()
    }

    @Test
    fun `after the full history, the end of the cache is the end`() {
        records.lastSynced["user/workouts"] = now
        records.fullHistorySynced["user/workouts"] = now
        val backend = FakeCollectionBackend(server = workouts.take(15), cached = workouts.take(15))
        val pages = dataSync.pages(source(backend), pageSize = 10)
        val loads = read(pages.loads)

        pages.loadMore()

        assertThat(backend.calls).isEmpty()
        assertThat(loads.last()).isEqualTo(ready(workouts.take(15), endReached = true))
    }

    @Test
    fun `a page that fails isn't retried on its own, and loading more retries it`() {
        val backend = FakeCollectionBackend(server = workouts)
        val pages = dataSync.pages(source(backend), pageSize = 20)
        val loads = read(pages.loads)
        backend.fail = { call -> if (call.startsWith("page")) IllegalStateException("offline") else null }

        pages.loadMore()

        assertThat(backend.calls).containsExactly("since", "page:31").inOrder()
        assertThat(loads.last()).isEqualTo(ready(workouts.take(31)))

        backend.fail = { null }
        pages.loadMore()

        assertThat(backend.calls).containsExactly("since", "page:31", "page:31").inOrder()
        assertThat(loads.last()).isEqualTo(ready(workouts.take(40)))
    }

    @Test
    fun `a new workout shows at the top without skipping or repeating rows`() {
        val backend = FakeCollectionBackend(server = workouts)
        val pages = dataSync.pages(source(backend), pageSize = 10)
        val loads = read(pages.loads)
        val today = Doc("today", now)

        backend.write(today)

        assertThat(loads.last()).isEqualTo(ready(listOf(today) + workouts.take(9)))

        pages.loadMore()

        assertThat(loads.last()).isEqualTo(ready(listOf(today) + workouts.take(19)))
    }

    private fun source(backend: FakeCollectionBackend) =
        CollectionSource("workouts", "user", SyncPlan.Recent(dateField = "date", months = 3), backend)

    private fun ready(items: List<Doc>, loadingMore: Boolean = false, endReached: Boolean = false) =
        Load.Ready(Page(items, loadingMore, endReached))

    private fun <T> read(flow: Flow<T>): List<T> {
        val loads = CopyOnWriteArrayList<T>()
        readers.launch { flow.collect { loads += it } }
        return loads
    }
}
