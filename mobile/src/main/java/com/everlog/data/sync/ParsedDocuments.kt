package com.everlog.data.sync

/**
 * A listener's documents, parsed once each. Every snapshot of a query holds all its documents, so parsing
 * them all again would cost a long history on every local edit; only the changed ones are parsed.
 */
internal class ParsedDocuments<D, T : Any>(private val parse: (D) -> T?) {

    private val parsed = HashMap<String, T?>()

    // [document] null means removed
    class Change<D>(val id: String, val document: D?)

    // The documents in [order], which a snapshot gives in the query's order. Ones that don't parse are left out.
    fun update(changes: List<Change<D>>, order: List<String>): List<T> {
        changes.forEach { change ->
            if (change.document == null) parsed.remove(change.id) else parsed[change.id] = parse(change.document)
        }
        return order.mapNotNull { parsed[it] }
    }
}
