package com.everlog.ui.activities.exercisepicker

import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.testutil.LibraryFixtures
import com.everlog.testutil.LibraryFixtures.barbellBench
import com.everlog.testutil.LibraryFixtures.barbellRow
import com.everlog.testutil.LibraryFixtures.dumbbellBench
import com.everlog.testutil.LibraryFixtures.inclineDumbbellBench
import com.everlog.testutil.LibraryFixtures.own
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExercisePickerContentTest {

    private val me = FilterValue(id = CreatedByMe, name = "Me")

    private fun PickerContent.top() = sections.filterIsInstance<PickerSection.Top>().singleOrNull()

    private fun PickerContent.uuids(section: PickerSection?) = section?.rows?.map { it.exercise.uuid }

    @Test
    fun `picking, the most used in templates come first, five until Show all, the rest stay in their movement`() {
        val rows = (1..6).map { LibraryExercise(uuid = "row$it", name = "Row $it", family = "row", equipment = "barbell", primaryMuscles = listOf("lats")) }
        val index = PickerIndex(LibraryFixtures.library(exercises = rows, templateCounts = rows.associate { it.uuid to it.uuid.last().digitToInt() }), me)

        val content = ExercisePickerState(mode = PickerMode.Pick).buildContent(index)
        assertThat(content.uuids(content.top())).containsExactly("row6", "row5", "row4", "row3", "row2").inOrder()
        assertThat(content.top()?.let { (it as PickerSection.Top).total }).isEqualTo(6)
        assertThat(content.uuids(content.sections.last())).containsExactly("row1")

        val expanded = ExercisePickerState(mode = PickerMode.Pick, showAllTop = true).buildContent(index)
        assertThat(expanded.uuids(expanded.top())).hasSize(6)
        assertThat(expanded.sections).hasSize(1)
    }

    @Test
    fun `browsing, the recently done come first, newest first`() {
        val index = PickerIndex(LibraryFixtures.library(lastDone = mapOf(dumbbellBench.uuid to 200L, barbellRow.uuid to 300L)), me)

        val content = ExercisePickerState(mode = PickerMode.Browse).buildContent(index)
        assertThat(content.uuids(content.top())).containsExactly(barbellRow.uuid, dumbbellBench.uuid).inOrder()
        assertThat(content.sections.flatMap { it.rows }.count { it.exercise == dumbbellBench }).isEqualTo(1)
    }

    @Test
    fun `movements follow the muscle groups, the default variation first, own exercises last`() {
        val content = ExercisePickerState(mode = PickerMode.Pick).buildContent(PickerIndex(LibraryFixtures.library(), me))

        assertThat(content.top()).isNull()
        assertThat(content.sections.map { it.key }).containsExactly("family_bench_press", "family_row", "own").inOrder()
        assertThat(content.uuids(content.sections.first()))
            .containsExactly(barbellBench.uuid, dumbbellBench.uuid, inclineDumbbellBench.uuid).inOrder()
        assertThat(content.uuids(content.sections.last())).containsExactly(own.uuid)
    }

    @Test
    fun `typing a word suggests filter values, counted with the rest of the search`() {
        val index = PickerIndex(LibraryFixtures.library(), me)

        val menu = ExercisePickerState(mode = PickerMode.Pick, text = "incline db", menu = PickerMenu.Start).buildContent(index).menu
        assertThat(menu).isInstanceOf(MenuContent.Typing::class.java)
        val suggestion = (menu as MenuContent.Typing).suggestions.single()
        assertThat(suggestion.key).isEqualTo(FilterKey.Equipment)
        assertThat(suggestion.value.id).isEqualTo("dumbbell")
        assertThat(suggestion.count).isEqualTo(1)
        assertThat(menu.resultCount).isEqualTo(1)
    }
}
