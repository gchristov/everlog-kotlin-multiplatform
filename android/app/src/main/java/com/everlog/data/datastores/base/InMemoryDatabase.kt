package com.everlog.data.datastores.base

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Items saved by the in-memory stores, by collection and ID. Lives for as long as the app process.
 */
object InMemoryDatabase {

    fun interface Listener {
        fun onChanged()
    }

    private val collections = ConcurrentHashMap<String, MutableMap<String, Any>>()
    private val listeners = ConcurrentHashMap<String, CopyOnWriteArrayList<Listener>>()

    @JvmStatic
    fun put(collection: String, id: String?, item: Any) {
        if (id == null) {
            return
        }
        collection(collection)[id] = copy(item)
        notifyChanged(collection)
    }

    @JvmStatic
    fun delete(collection: String, id: String?) {
        if (id == null) {
            return
        }
        if (collection(collection).remove(id) != null) {
            notifyChanged(collection)
        }
    }

    @JvmStatic
    fun get(collection: String, id: String?): Any? {
        return id?.let { collections[collection]?.get(it) }?.let { copy(it) }
    }

    @JvmStatic
    fun <T> items(collection: String, order: Comparator<in T>?): List<T> {
        @Suppress("UNCHECKED_CAST")
        val items = collections[collection]?.values?.map { copy(it) as T }?.toMutableList() ?: mutableListOf()
        if (order != null) {
            items.sortWith(order)
        }
        return items
    }

    @JvmStatic
    fun addListener(collection: String, listener: Listener) {
        listeners.getOrPut(collection) { CopyOnWriteArrayList() }.addIfAbsent(listener)
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

    private fun collection(collection: String): MutableMap<String, Any> {
        return collections.getOrPut(collection) { ConcurrentHashMap() }
    }

    private fun notifyChanged(collection: String) {
        listeners[collection]?.forEach { it.onChanged() }
    }

    // Screens edit the objects they load, so keep separate copies that only change on a save
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
