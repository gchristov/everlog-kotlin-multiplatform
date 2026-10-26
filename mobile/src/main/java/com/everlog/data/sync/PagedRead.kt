package com.everlog.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * @param loadingMore A page is on its way from the server, for a footer spinner.
 * @param endReached Nothing older, here or on the server.
 */
data class Page<T>(val items: List<T>, val loadingMore: Boolean, val endReached: Boolean)

/**
 * One screen's pages of a collection (DataSync.pages). Its cache listener grows by a page at a time, so
 * a document added or deleted anywhere shows without rows skipping or repeating.
 */
class PagedRead<T> internal constructor(
    private val backend: CollectionBackend<T>,
    private val pageSize: Int,
    private val scope: CoroutineScope,
    status: Flow<SyncStatus>,
    sync: () -> Unit,
    private val fetchPage: suspend (after: Int) -> Int,
    endsWithSession: (Flow<Load<Page<T>>>) -> Flow<Load<Page<T>>>,
) {

    private val limit = MutableStateFlow(pageSize)
    private val paging = MutableStateFlow(Paging())

    // How many the cache showed for the current limit
    @Volatile
    private var shown = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    val loads: Flow<Load<Page<T>>> = endsWithSession(
        combine(limit.flatMapLatest { limit -> backend.cached(since = null, limit).map { Shown(it, limit) } }, status, paging) { shown, status, paging ->
            this.shown = shown.items.size
            fetchFromServerWhenShort(shown, status, paging)
            val short = shown.items.size < shown.limit
            val page = Page(shown.items, paging.loadingMore, endReached = short && (status.fullHistorySynced != null || paging.serverExhausted))
            loadOf(page, shown.items.isEmpty(), covered = status.lastSynced != null, needsFullHistory = false, status)
        }
            .catch { emit(Load.Unavailable(it)) }
            .distinctUntilChanged()
            .onStart { sync() }
    )

    // For a screen scrolled to the end. While the cache is still short of the last page, it retries that page instead.
    fun loadMore() {
        val current = limit.value
        if (shown < current) paging.update { it.copy(fetchedFor = 0) } else limit.value = current + pageSize
    }

    // Once the recent sync has landed, so its pages aren't read twice. Once per limit, so a failure doesn't retry in a loop.
    private fun fetchFromServerWhenShort(shown: Shown<T>, status: SyncStatus, paging: Paging) {
        val short = shown.items.size < shown.limit
        val historyLeft = status.fullHistorySynced == null && !paging.serverExhausted
        if (!short || !historyLeft || status.lastSynced == null || status.syncing || paging.loadingMore || paging.fetchedFor >= shown.limit) return
        if (!this.paging.compareAndSet(paging, paging.copy(loadingMore = true, fetchedFor = shown.limit))) return
        scope.launch {
            try {
                val fetched = fetchPage(shown.items.size)
                this@PagedRead.paging.update { it.copy(loadingMore = false, serverExhausted = fetched < pageSize) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w("Couldn't load an older page: %s", e.message)
                this@PagedRead.paging.update { it.copy(loadingMore = false) }
            }
        }
    }

    private class Shown<T>(val items: List<T>, val limit: Int)

    private data class Paging(
        val loadingMore: Boolean = false,
        val serverExhausted: Boolean = false,
        // The limit a server page was last fetched for
        val fetchedFor: Int = 0,
    )
}

private const val TAG = "PagedRead"
