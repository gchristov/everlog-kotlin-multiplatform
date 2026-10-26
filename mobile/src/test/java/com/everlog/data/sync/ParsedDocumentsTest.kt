package com.everlog.data.sync

import com.everlog.data.sync.ParsedDocuments.Change
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParsedDocumentsTest {

    private val parses = mutableListOf<String>()
    // Documents are their raw text here; "broken" doesn't parse
    private val parsed = ParsedDocuments<String, String> { document ->
        parses += document
        document.uppercase().takeUnless { document == "broken" }
    }

    @Test
    fun `only changed documents are parsed again`() {
        parsed.update(listOf(Change("a", "bench"), Change("b", "squat")), order = listOf("a", "b"))
        parses.clear()

        val documents = parsed.update(listOf(Change("b", "front squat")), order = listOf("a", "b"))

        assertThat(parses).containsExactly("front squat")
        assertThat(documents).containsExactly("BENCH", "FRONT SQUAT").inOrder()
    }

    @Test
    fun `documents come in the snapshot's order, without removed ones`() {
        parsed.update(listOf(Change("a", "bench"), Change("b", "squat"), Change("c", "row")), order = listOf("a", "b", "c"))

        val documents = parsed.update(listOf(Change<String>("b", null)), order = listOf("c", "a"))

        assertThat(documents).containsExactly("ROW", "BENCH").inOrder()
    }

    @Test
    fun `a document that doesn't parse is left out`() {
        val documents = parsed.update(listOf(Change("a", "bench"), Change("b", "broken")), order = listOf("a", "b"))

        assertThat(documents).containsExactly("BENCH")
    }
}
