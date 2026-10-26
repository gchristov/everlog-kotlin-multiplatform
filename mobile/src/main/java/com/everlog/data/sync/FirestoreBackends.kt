package com.everlog.data.sync

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

    override fun cached(since: Long?): Flow<List<T>> {
        val query = order(if (since == null) documents else documents.whereGreaterThanOrEqualTo(requireDateField(), since))
        return flow {
            // Per listener: a new one starts with every document added
            val parsed = ParsedDocuments<DocumentSnapshot, T> { it.parseOrNull(parse) }
            emitAll(query.cacheSnapshots().map { snapshot ->
                val changes = snapshot.documentChanges.map { change ->
                    ParsedDocuments.Change<DocumentSnapshot>(change.document.id, change.document.takeUnless { change.type == DocumentChange.Type.REMOVED })
                }
                parsed.update(changes, snapshot.documents.map { it.id })
            })
        }.flowOn(dispatcher)
    }

    override suspend fun fetchAll(): Int = documents.get(Source.SERVER).await().size()

    override suspend fun fetchSince(since: Long): Int =
        documents.whereGreaterThanOrEqualTo(requireDateField(), since).get(Source.SERVER).await().size()

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

    override fun cached(): Flow<T?> = callbackFlow {
        val registration = document.addSnapshotListener(cacheOnly) { snapshot, error ->
            if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
        }
        awaitClose { registration.remove() }
    }
        .map { snapshot -> if (snapshot.exists()) snapshot.parseOrNull(parse) else null }
        .flowOn(dispatcher)

    override suspend fun fetch() {
        document.get(Source.SERVER).await()
    }
}

// Firestore puts local writes in the cache straight away, so this follows the user's own edits for free
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
