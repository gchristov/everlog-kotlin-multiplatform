package com.everlog.data.model

interface ELFirestoreModel {

    fun documentId(): String?
    fun asMap(): Map<String, Any?>

    companion object {
        fun <T : ELFirestoreModel> asMappedList(items: List<T>): List<Map<String, Any?>> {
            return items.map { it.asMap() }
        }
    }
}