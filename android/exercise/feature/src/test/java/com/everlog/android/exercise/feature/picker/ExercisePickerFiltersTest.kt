package com.everlog.android.exercise.feature.picker

import com.everlog.android.exercise.testfixtures.LibraryFixtures
import com.everlog.android.exercise.testfixtures.LibraryFixtures.barbellBench
import com.everlog.android.exercise.testfixtures.LibraryFixtures.barbellRow
import com.everlog.android.exercise.testfixtures.LibraryFixtures.dumbbellBench
import com.everlog.android.exercise.testfixtures.LibraryFixtures.inclineDumbbellBench
import com.everlog.android.exercise.testfixtures.LibraryFixtures.own
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExercisePickerFiltersTest {

    private val me = FilterValue(id = CreatedByMe, name = "Me", aliases = listOf("mine"))
    private val index = PickerIndex(LibraryFixtures.library(), me)

    private fun search(text: String) = index.exercises.filter { index.searchScore(it, searchWords(text)) != null }

    private fun filter(vararg tokens: FilterToken) = index.exercises.filter { index.matches(it, tokens.toList()) }

    @Test
    fun `search words ignore case, accents and punctuation`() {
        assertThat(searchWords("Café-au-lait  DB!")).containsExactly("cafe", "au", "lait", "db").inOrder()
    }

    @Test
    fun `search matches words in any order, through names, aliases and filter values`() {
        assertThat(search("press bench db")).containsExactly(dumbbellBench, inclineDumbbellBench)
        assertThat(search("bent over")).containsExactly(barbellRow)
        assertThat(search("chest barbell")).containsExactly(barbellBench)
    }

    @Test
    fun `search forgives a typo in longer words only`() {
        assertThat(search("benhc")).containsExactly(barbellBench, dumbbellBench, inclineDumbbellBench)
        assertThat(search("dumbel row")).isEmpty()
        assertThat(search("rwo")).isEmpty()
    }

    @Test
    fun `search ranks the name's start first, then a name word, then other words, then typos`() {
        assertThat(index.searchScore(barbellBench, searchWords("barbell bench"))).isEqualTo(0)
        assertThat(index.searchScore(inclineDumbbellBench, searchWords("bench"))).isEqualTo(1)
        assertThat(index.searchScore(barbellBench, searchWords("chest"))).isEqualTo(2)
        assertThat(index.searchScore(barbellBench, searchWords("benhc"))).isEqualTo(3)
    }

    @Test
    fun `a token matches any of its values, and is not matches none of them`() {
        assertThat(filter(FilterToken(FilterKey.Equipment, listOf("barbell", "dumbbell"))))
            .containsExactly(barbellBench, dumbbellBench, inclineDumbbellBench, barbellRow)
        assertThat(filter(FilterToken(FilterKey.Equipment, listOf("barbell"), not = true)))
            .containsExactly(dumbbellBench, inclineDumbbellBench, own)
        assertThat(filter(FilterToken(FilterKey.Equipment, listOf("dumbbell")), FilterToken(FilterKey.SetUp, listOf("incline"))))
            .containsExactly(inclineDumbbellBench)
    }

    @Test
    fun `own exercises filter by muscle group through their category, and by created by me`() {
        assertThat(filter(FilterToken(FilterKey.Muscle, listOf("chest"))))
            .containsExactly(barbellBench, dumbbellBench, inclineDumbbellBench, own)
        assertThat(filter(FilterToken(FilterKey.CreatedBy, listOf(CreatedByMe)))).containsExactly(own)
    }

    @Test
    fun `filters offer only the values exercises use, movements A to Z, created by only with own exercises`() {
        assertThat(index.filterValues.getValue(FilterKey.Muscle).map { it.id }).containsExactly("chest", "back").inOrder()
        assertThat(index.filterValues.getValue(FilterKey.Movement).map { it.name }).containsExactly("Bench Press", "Row").inOrder()
        assertThat(index.filterValues.getValue(FilterKey.CreatedBy)).containsExactly(me)

        val withoutOwn = PickerIndex(LibraryFixtures.library(exercises = LibraryFixtures.global), me)
        assertThat(withoutOwn.filterValues.getValue(FilterKey.CreatedBy)).isEmpty()
    }

    @Test
    fun `tokens survive the saved state, and a malformed one is dropped`() {
        val token = FilterToken(FilterKey.Equipment, listOf("barbell", "dumbbell"), not = true)
        assertThat(decodeToken(encodeToken(token))).isEqualTo(token)
        assertThat(decodeToken("Unknown|is|barbell")).isNull()
        assertThat(decodeToken("Equipment|is|")).isNull()
    }
}
