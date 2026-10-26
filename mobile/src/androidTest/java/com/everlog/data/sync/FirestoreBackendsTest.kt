package com.everlog.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.BuildConfig
import com.everlog.managers.auth.LocalUserManager
import com.everlog.testutil.E2EAccount
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The Firestore backends against dev Firestore, as the E2E account. A second Firebase app signed in as the
 * same user, with its own cache, is another phone: what it changes reaches this one only through a sync.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreBackendsTest {

    private data class Entry(val id: String, val date: Long)

    private lateinit var collection: CollectionReference
    private lateinit var otherPhone: CollectionReference
    private lateinit var backend: FirestoreCollection<Entry>

    @Before
    fun setUp() = runBlocking<Unit> {
        E2EAccount.signIn()
        // A collection of its own per run, so nothing is cached for it yet
        val path = "users/${LocalUserManager.getUser()!!.id}/syncTest-${UUID.randomUUID()}"
        collection = FirebaseFirestore.getInstance().collection(path)
        otherPhone = otherPhoneFirestore().collection(path)
        backend = FirestoreCollection(
            documents = collection,
            order = { it.orderBy("date", Query.Direction.DESCENDING) },
            dateField = "date",
            dispatcher = Dispatchers.IO,
        ) { Entry(it.id, it.getLong("date")!!) }
    }

    @After
    fun tearDown() = runBlocking<Unit> {
        otherPhone.get().await().documents.forEach { it.reference.delete().await() }
    }

    @Test
    fun aCollectionNeverReadIsEmptyInTheCacheRatherThanWaiting() = runBlocking<Unit> {
        assertThat(backend.cached(since = null).within()).isEmpty()
    }

    @Test
    fun localWritesReachTheCacheStraightAway() = runBlocking<Unit> {
        collection.document("bench").set(mapOf("date" to 2L))

        assertThat(backend.cached(since = null).firstWith(listOf(Entry("bench", 2)))).isTrue()
    }

    @Test
    fun aSyncBringsAnotherPhonesChangesAndDeletions() = runBlocking<Unit> {
        otherPhone.document("bench").set(mapOf("date" to 2L)).await()
        otherPhone.document("squat").set(mapOf("date" to 1L)).await()
        assertThat(backend.fetchAll()).isEqualTo(2)
        assertThat(backend.cached(since = null).within()).containsExactly(Entry("bench", 2), Entry("squat", 1)).inOrder()

        otherPhone.document("squat").delete().await()
        otherPhone.document("bench").set(mapOf("date" to 3L)).await()
        backend.fetchAll()

        assertThat(backend.cached(since = null).within()).containsExactly(Entry("bench", 3))
    }

    @Test
    fun aRecentSyncBringsTheWindowAndDropsItsDeletions() = runBlocking<Unit> {
        otherPhone.document("old").set(mapOf("date" to 1L)).await()
        otherPhone.document("recent").set(mapOf("date" to 10L)).await()
        otherPhone.document("deleted").set(mapOf("date" to 11L)).await()
        backend.fetchAll()
        otherPhone.document("deleted").delete().await()
        otherPhone.document("new").set(mapOf("date" to 12L)).await()

        assertThat(backend.fetchSince(since = 5)).isEqualTo(2)

        assertThat(backend.cached(since = null).within()).containsExactly(Entry("new", 12), Entry("recent", 10), Entry("old", 1)).inOrder()
        assertThat(backend.cached(since = 5).within()).containsExactly(Entry("new", 12), Entry("recent", 10)).inOrder()
    }

    @Test
    fun chunksReadEveryDocumentByIdAndDropDeletions() = runBlocking<Unit> {
        listOf("a", "b", "c").forEach { otherPhone.document(it).set(mapOf("date" to 1L)).await() }
        // No date, so the screens' order leaves it out, but a full sync still reads it
        otherPhone.document("d").set(mapOf("name" to "no date")).await()
        backend.fetchAll()
        otherPhone.document("b").delete().await()

        val first = backend.fetchChunk(afterId = null, size = 2)
        val second = backend.fetchChunk(afterId = first.lastId, size = 2)

        assertThat(first).isEqualTo(Chunk(count = 2, lastId = "c"))
        assertThat(second).isEqualTo(Chunk(count = 1, lastId = "d"))
        assertThat(backend.cached(since = null).within().map { it.id }).containsExactly("a", "c")
        assertThat(backend.countInCache()).isEqualTo(3)
    }

    @Test
    fun aPageFollowsTheCachedOnesInTheScreensOrder() = runBlocking<Unit> {
        (1..5L).forEach { otherPhone.document("w$it").set(mapOf("date" to it)).await() }
        backend.fetchSince(since = 4)

        assertThat(backend.fetchPage(after = 2, size = 2)).isEqualTo(2)

        assertThat(backend.cached(since = null, limit = 3).within()).containsExactly(Entry("w5", 5), Entry("w4", 4), Entry("w3", 3)).inOrder()
        assertThat(backend.cached(since = null).within().map { it.id }).containsExactly("w5", "w4", "w3", "w2").inOrder()
    }

    @Test
    fun countsOnTheServerAndInTheCache() = runBlocking<Unit> {
        otherPhone.document("bench").set(mapOf("date" to 2L)).await()
        otherPhone.document("squat").set(mapOf("date" to 1L)).await()

        assertThat(backend.countOnServer()).isEqualTo(2)
        assertThat(backend.countInCache()).isEqualTo(0)

        backend.fetchAll()

        assertThat(backend.countInCache()).isEqualTo(2)
    }

    @Test
    fun aDocumentSyncSaysWhenItDoesntExist() = runBlocking<Unit> {
        val document = FirestoreDocument(collection.document("profile"), Dispatchers.IO) { Entry(it.id, it.getLong("date")!!) }
        assertThat(document.cached().within()).isNull()

        otherPhone.document("profile").set(mapOf("date" to 5L)).await()
        document.fetch()

        assertThat(document.cached().within()).isEqualTo(Entry("profile", 5))

        otherPhone.document("profile").delete().await()
        document.fetch()

        assertThat(document.cached().within()).isNull()
    }

    private fun otherPhoneFirestore(): FirebaseFirestore = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == OTHER_PHONE }
            ?: FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, OTHER_PHONE)
        val auth = FirebaseAuth.getInstance(app)
        if (auth.currentUser == null) auth.signInWithEmailAndPassword(BuildConfig.E2E_TEST_USER_EMAIL, BuildConfig.E2E_TEST_USER_PASSWORD).await()
        FirebaseFirestore.getInstance(app)
    }

    // The cache's current answer
    private suspend fun <T> Flow<T>.within(): T = withTimeout(TIMEOUT) { first() }

    private suspend fun <T> Flow<T>.firstWith(expected: T): Boolean = withTimeout(TIMEOUT) { first { it == expected } } == expected

    private companion object {
        const val OTHER_PHONE = "otherPhone"
        const val TIMEOUT = 10_000L
    }
}
