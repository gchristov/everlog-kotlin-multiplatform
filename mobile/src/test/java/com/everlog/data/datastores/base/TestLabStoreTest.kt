package com.everlog.data.datastores.base

import com.everlog.data.model.ELRoutine
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test

class TestLabStoreTest {

    private val path = "users/test/routines"
    private val byName: Comparator<ELRoutine> = compareBy(nullsFirst()) { it.name }

    private fun routine(uuid: String, name: String) = ELRoutine(uuid = uuid, name = name)

    @After
    fun tearDown() {
        TestLabStore.clear()
    }

    @Test
    fun `nothing written leaves loaded items as they are`() {
        val loaded = listOf(routine("1", "Arms"))

        assertThat(TestLabStore.merge(path, loaded, byName)).containsExactlyElementsIn(loaded)
        assertThat(TestLabStore.get(path, "1")).isNull()
    }

    @Test
    fun `written items are added in query order`() {
        TestLabStore.put(path, "2", routine("2", "Back"))

        val merged = TestLabStore.merge(path, listOf(routine("1", "Arms"), routine("3", "Chest")), byName)

        assertThat(merged.map { it.name }).containsExactly("Arms", "Back", "Chest").inOrder()
    }

    @Test
    fun `written items replace loaded ones with the same ID`() {
        TestLabStore.put(path, "1", routine("1", "Arms day"))

        val merged = TestLabStore.merge(path, listOf(routine("1", "Arms")), byName)

        assertThat(merged.map { it.name }).containsExactly("Arms day")
    }

    @Test
    fun `deleted items are dropped`() {
        TestLabStore.delete(path, "1")

        val merged = TestLabStore.merge(path, listOf(routine("1", "Arms"), routine("2", "Back")), byName)

        assertThat(merged.map { it.uuid }).containsExactly("2")
        assertThat(TestLabStore.get(path, "1")).isInstanceOf(TestLabStore.Entry.Deleted::class.java)
    }

    @Test
    fun `other collections aren't affected`() {
        TestLabStore.put("users/test/plans", "1", routine("1", "Arms day"))

        val merged = TestLabStore.merge(path, listOf(routine("1", "Arms")), byName)

        assertThat(merged.map { it.name }).containsExactly("Arms")
    }

    @Test
    fun `changing an item after writing it doesn't change what's stored`() {
        val written = routine("1", "Arms")
        TestLabStore.put(path, "1", written)
        written.name = "Edited without saving"

        val stored = (TestLabStore.get(path, "1") as TestLabStore.Entry.Present).item as ELRoutine
        stored.name = "Edited after reading"

        val reread = (TestLabStore.get(path, "1") as TestLabStore.Entry.Present).item as ELRoutine
        assertThat(reread.name).isEqualTo("Arms")
    }

    @Test
    fun `listeners hear about writes to their collection only`() {
        var changes = 0
        val listener = TestLabStore.Listener { changes++ }
        TestLabStore.addListener(path, listener)

        TestLabStore.put(path, "1", routine("1", "Arms"))
        TestLabStore.delete(path, "1")
        TestLabStore.put("users/test/plans", "1", routine("1", "Arms"))
        TestLabStore.removeListener(listener)
        TestLabStore.put(path, "2", routine("2", "Back"))

        assertThat(changes).isEqualTo(2)
    }
}
