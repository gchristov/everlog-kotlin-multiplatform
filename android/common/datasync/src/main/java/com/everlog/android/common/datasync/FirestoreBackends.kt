package com.everlog.android.common.datasync

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenSource
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SnapshotListenOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * A collection in Firestore.
 *
 * @param documents Every document of the source, e.g. a user's workouts, or the plans they created. Syncs
 * and counts read it as it is, so documents missing the order's field still sync.
 * @param order How screens see them, e.g. newest first.
 * @param dateField The plan's date field, for SyncPlan.Recent collections.
 */
class FirestoreCollection<T : Any>(
    private val documents: Query,
    private val order: (Query) -> Query = { it },
    private val dateField: String? = null,
    private val dispatcher: CoroutineDispatcher,
    private val parse: (DocumentSnapshot) -> T?,
) : CollectionBackend<T> {

    override fun cachedDocuments(since: Long?, limit: Int?): Flow<CachedDocuments<T>> {
        val ordered = order(if (since == null) documents else documents.whereGreaterThanOrEqualTo(requireDateField(), since))
        // Never the same query as a server read (see cacheOnly): every listener has a limit no read uses,
        // and a page one more than it shows. Cache reads are free and need no index.
        val query = ordered.limit(limit?.let { it + 1L } ?: Int.MAX_VALUE.toLong())
        return flow {
            // Per listener: a new one starts with every document added
            val parsed = ParsedDocuments<DocumentSnapshot, T> { it.parseOrNull(parse) }
            emitAll(query.cacheSnapshots().map { snapshot ->
                val changes = snapshot.documentChanges.map { change ->
                    ParsedDocuments.Change<DocumentSnapshot>(change.document.id, change.document.takeUnless { change.type == DocumentChange.Type.REMOVED })
                }
                val shown = parsed.update(changes, snapshot.documents.map { it.id })
                CachedDocuments(if (limit == null) shown else shown.take(limit), documents = snapshot.size())
            })
        }.flowOn(dispatcher)
    }

    override suspend fun fetchAll(): Int = documents.get(Source.SERVER).await().size()

    override suspend fun fetchSince(since: Long): Int =
        documents.whereGreaterThanOrEqualTo(requireDateField(), since).get(Source.SERVER).await().size()

    override suspend fun fetchPage(after: Int, size: Int): Int {
        val ordered = order(documents)
        val last = if (after == 0) null else ordered.limit(after.toLong()).get(Source.CACHE).await().documents.lastOrNull()
        return (if (last == null) ordered else ordered.startAfter(last)).limit(size.toLong()).get(Source.SERVER).await().size()
    }

    override suspend fun fetchChunk(afterId: String?, size: Int): Chunk {
        val byId = documents.orderBy(FieldPath.documentId())
        val snapshot = (if (afterId == null) byId else byId.startAfter(afterId)).limit(size.toLong()).get(Source.SERVER).await()
        return Chunk(snapshot.size(), snapshot.documents.lastOrNull()?.id)
    }

    override suspend fun countOnServer(): Long = documents.count().get(AggregateSource.SERVER).await().count

    override suspend fun countInCache(): Long =
        documents.get(Source.CACHE).await().documents.count { !it.metadata.hasPendingWrites() }.toLong()

    private fun requireDateField() = requireNotNull(dateField) { "Reading by date needs the collection's dateField" }
}

class FirestoreDocument<T : Any>(
    private val document: DocumentReference,
    private val dispatcher: CoroutineDispatcher,
    private val parse: (DocumentSnapshot) -> T?,
) : DocumentBackend<T> {

    // Through a query, so it's never the same target as fetch()'s read of the document (see cacheOnly)
    override fun cached(): Flow<T?> = document.parent.whereEqualTo(FieldPath.documentId(), document.id).cacheSnapshots()
        .map { snapshot -> snapshot.documents.firstOrNull()?.parseOrNull(parse) }
        .flowOn(dispatcher)

    override suspend fun fetch(): Boolean = document.get(Source.SERVER).await().exists()
}

/**
 * Firestore puts local writes in the cache straight away, so this follows the user's own edits for free.
 *
 * A server read of the very query a cache-only listener follows is answered from that listener's view, with
 * the cache's data marked as the server's, once the listener has seen one server read: a sync that
 * reads nothing. So listeners never use the same query as a server read (FirestoreBackendsTest).
 */
private val cacheOnly = SnapshotListenOptions.Builder().setSource(ListenSource.CACHE).build()

// Unbuffered snapshots would be dropped when parsing falls behind, losing their changes
private fun Query.cacheSnapshots(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener(cacheOnly) { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}.buffer(Channel.UNLIMITED)

// One document that doesn't parse is left out, rather than failing the whole read
private fun <T> DocumentSnapshot.parseOrNull(parse: (DocumentSnapshot) -> T?): T? =
    runCatching { parse(this) }
        .onFailure { Timber.tag("FirestoreBackends").w(it, "Skipped %s, which doesn't parse", reference.path) }
        .getOrNull()
