package com.everlog.data.datastores.base

import com.everlog.data.model.ELRoutine
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class InMemoryDatabaseTest {

    private val routines = "routines"
    private val byName: Comparator<ELRoutine> = compareBy(nullsFirst()) { it.name }

    private fun routine(uuid: String, name: String) = ELRoutine(uuid = uuid, name = name)

    @After
    fun tearDown() {
        InMemoryDatabase.clear()
    }

    @Test
    fun `an empty collection has no items`() {
        assertThat(InMemoryDatabase.items<ELRoutine>(routines, byName)).isEmpty()
        assertThat(InMemoryDatabase.get(routines, "1")).isNull()
    }

    @Test
    fun `items are listed in the given order`() {
        InMemoryDatabase.put(routines, "2", routine("2", "Chest"))
        InMemoryDatabase.put(routines, "1", routine("1", "Arms"))
        InMemoryDatabase.put(routines, "3", routine("3", "Back"))

        val items = InMemoryDatabase.items<ELRoutine>(routines, byName)

        assertThat(items.map { it.name }).containsExactly("Arms", "Back", "Chest").inOrder()
    }

    @Test
    fun `saving an item again replaces it`() {
        InMemoryDatabase.put(routines, "1", routine("1", "Arms"))
        InMemoryDatabase.put(routines, "1", routine("1", "Arms day"))

        assertThat(InMemoryDatabase.items<ELRoutine>(routines, byName).map { it.name }).containsExactly("Arms day")
        assertThat((InMemoryDatabase.get(routines, "1") as ELRoutine).name).isEqualTo("Arms day")
    }

    @Test
    fun `deleted items are gone`() {
        InMemoryDatabase.put(routines, "1", routine("1", "Arms"))
        InMemoryDatabase.put(routines, "2", routine("2", "Back"))

        InMemoryDatabase.delete(routines, "1")

        assertThat(InMemoryDatabase.items<ELRoutine>(routines, byName).map { it.uuid }).containsExactly("2")
        assertThat(InMemoryDatabase.get(routines, "1")).isNull()
    }

    @Test
    fun `collections are separate`() {
        InMemoryDatabase.put("plans", "1", routine("1", "Arms"))

        assertThat(InMemoryDatabase.items<ELRoutine>(routines, byName)).isEmpty()
    }

    @Test
    fun `items without an ID are ignored`() {
        InMemoryDatabase.put(routines, null, routine("1", "Arms"))

        assertThat(InMemoryDatabase.items<ELRoutine>(routines, byName)).isEmpty()
        assertThat(InMemoryDatabase.get(routines, null)).isNull()
    }

    @Test
    fun `changing an item after saving or reading it doesn't change what's stored`() {
        val saved = routine("1", "Arms")
        InMemoryDatabase.put(routines, "1", saved)
        saved.name = "Edited without saving"

        (InMemoryDatabase.get(routines, "1") as ELRoutine).name = "Edited after reading"
        InMemoryDatabase.items<ELRoutine>(routines, byName).first().name = "Edited in a list"

        assertThat((InMemoryDatabase.get(routines, "1") as ELRoutine).name).isEqualTo("Arms")
    }

    @Test
    fun `listeners hear about changes to their collection only`() {
        var changes = 0
        val listener = InMemoryDatabase.Listener { changes++ }
        InMemoryDatabase.addListener(routines, listener)

        InMemoryDatabase.put(routines, "1", routine("1", "Arms"))
        InMemoryDatabase.delete(routines, "1")
        InMemoryDatabase.delete(routines, "1")
        InMemoryDatabase.put("plans", "1", routine("1", "Arms"))
        InMemoryDatabase.removeListener(listener)
        InMemoryDatabase.put(routines, "2", routine("2", "Back"))

        // The second delete changes nothing, so isn't reported
        assertThat(changes).isEqualTo(2)
    }
}
