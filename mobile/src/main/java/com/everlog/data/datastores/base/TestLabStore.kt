package com.everlog.data.datastores.base

import com.everlog.data.model.ELFirestoreModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Keeps what the app writes during a Firebase Test Lab run in memory instead of Firestore, so
 * robots don't create data in the Test Lab account. The stores read it back on top of what's in
 * Firestore, keyed by collection path and document ID. Lives for as long as the app process.
 */
object TestLabStore {

    fun interface Listener {
        fun onChanged()
    }

    sealed class Entry {
        class Present(val item: Any) : Entry()
        object Deleted : Entry()
    }

    private val collections = ConcurrentHashMap<String, MutableMap<String, Entry>>()
    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<Listener>>()

    @JvmStatic
    fun put(collectionPath: String, id: String?, item: Any) {
        if (id == null) {
            return
        }
        collection(collectionPath)[id] = Entry.Present(copy(item))
        notifyChanged(collectionPath)
    }

    @JvmStatic
    fun delete(collectionPath: String, id: String?) {
        if (id == null) {
            return
        }
        collection(collectionPath)[id] = Entry.Deleted
        notifyChanged(collectionPath)
    }

    /** The in-memory state of a document, or null if it was never written during this run. */
    @JvmStatic
    fun get(collectionPath: String, id: String?): Entry? {
        if (id == null) {
            return null
        }
        return when (val entry = collections[collectionPath]?.get(id)) {
            is Entry.Present -> Entry.Present(copy(entry.item))
            else -> entry
        }
    }

    /**
     * Combines [items] loaded from Firestore with this collection's in-memory writes: written
     * documents replace or add to the loaded ones, deleted ones are dropped. Sorted with [order] if
     * given, to match the Firestore query's ordering.
     */
    @JvmStatic
    fun <T> merge(collectionPath: String, items: List<T>, order: Comparator<in T>?): List<T> {
        val entries = collections[collectionPath] ?: return items
        val merged = ArrayList<T>()
        items.filterTo(merged) { item -> (item as? ELFirestoreModel)?.documentId()?.let { entries[it] } == null }
        entries.values.forEach { entry ->
            if (entry is Entry.Present) {
                @Suppress("UNCHECKED_CAST")
                merged.add(copy(entry.item) as T)
            }
        }
        if (order != null) {
            merged.sortWith(order)
        }
        return merged
    }

    @JvmStatic
    fun addListener(collectionPath: String, listener: Listener) {
        listeners.getOrPut(collectionPath) { CopyOnWriteArrayList() }.addIfAbsent(listener)
    }

    @JvmStatic
    fun removeListener(listener: Listener) {
        listeners.values.forEach { it.remove(listener) }
    }

    @JvmStatic
    fun clear() {
        collections.clear()
        listeners.clear()
    }

    private fun collection(collectionPath: String): MutableMap<String, Entry> {
        return collections.getOrPut(collectionPath) { ConcurrentHashMap() }
    }

    private fun notifyChanged(collectionPath: String) {
        listeners[collectionPath]?.forEach { it.onChanged() }
    }

    // Screens edit the objects they load, so keep separate copies that only change on a write
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> copy(item: T): T {
        if (item !is Serializable) {
            return item
        }
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(item) }
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() as T }
    }
}
