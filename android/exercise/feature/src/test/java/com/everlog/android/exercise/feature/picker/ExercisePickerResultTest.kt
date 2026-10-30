package com.everlog.android.exercise.feature.picker

import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.exercise.testfixtures.LibraryFixtures.barbellBench
import com.everlog.android.exercise.testfixtures.LibraryFixtures.barbellRow
import com.everlog.android.exercise.testfixtures.LibraryFixtures.own
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExercisePickerResultTest {

    @Test
    fun `each exercise added on its own gets its own group, in the order picked`() {
        val groups = PickerResult(listOf(barbellRow, barbellBench), ELSetType.SINGLE).toExerciseGroups()

        assertThat(groups.map { group -> group.exercises.map { it.exercise?.uuid } })
            .containsExactly(listOf(barbellRow.uuid), listOf(barbellBench.uuid)).inOrder()
        assertThat(groups.map { it.type }).containsExactly(ELSetType.SINGLE.name, ELSetType.SINGLE.name)
    }

    @Test
    fun `a superset links every exercise in one group of its type`() {
        val groups = PickerResult(listOf(barbellBench, barbellRow), ELSetType.SUPER).toExerciseGroups()

        assertThat(groups).hasSize(1)
        assertThat(groups.single().type).isEqualTo(ELSetType.SUPER.name)
        assertThat(groups.single().exercises.map { it.exercise?.uuid }).containsExactly(barbellBench.uuid, barbellRow.uuid).inOrder()
    }

    @Test
    fun `a picked exercise keeps what the rest of the app needs, an own one stays private`() {
        val exercise = PickerResult(listOf(own), ELSetType.SINGLE).toExerciseGroups().single().exercises.single().exercise!!

        assertThat(exercise.name).isEqualTo(own.name)
        assertThat(exercise.category).isEqualTo(own.category)
        assertThat(exercise.privateToUser).isTrue()
    }
}
