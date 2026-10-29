package com.everlog.ui.components.reorder

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReorderRulesTest {

    @Test
    fun `an item between its neighbours' middles stays`() {
        assertThat(swapStep(middle = 150f, previousMiddle = 50f, nextMiddle = 250f)).isEqualTo(0)
    }

    @Test
    fun `past the next item's middle it moves down`() {
        assertThat(swapStep(middle = 251f, previousMiddle = 50f, nextMiddle = 250f)).isEqualTo(1)
    }

    @Test
    fun `past the previous item's middle it moves up`() {
        assertThat(swapStep(middle = 49f, previousMiddle = 50f, nextMiddle = 250f)).isEqualTo(-1)
    }

    @Test
    fun `at either end, or beside an item not laid out yet, it stays`() {
        assertThat(swapStep(middle = 900f, previousMiddle = 50f, nextMiddle = null)).isEqualTo(0)
        assertThat(swapStep(middle = -900f, previousMiddle = null, nextMiddle = 250f)).isEqualTo(0)
    }

    @Test
    fun `away from the edges the list doesn't scroll`() {
        assertThat(autoScrollStep(top = 300f, bottom = 400f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)).isEqualTo(0f)
    }

    @Test
    fun `near the bottom it scrolls down, faster the closer`() {
        val near = autoScrollStep(top = 800f, bottom = 925f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)
        val nearer = autoScrollStep(top = 850f, bottom = 975f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)
        assertThat(near).isWithin(0.01f).of(5f)
        assertThat(nearer).isWithin(0.01f).of(15f)
    }

    @Test
    fun `near the top it scrolls up`() {
        assertThat(autoScrollStep(top = 50f, bottom = 150f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)).isWithin(0.01f).of(-10f)
    }

    @Test
    fun `past an edge it scrolls at its fastest, no faster`() {
        assertThat(autoScrollStep(top = 950f, bottom = 1200f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)).isEqualTo(20f)
        assertThat(autoScrollStep(top = -300f, bottom = -200f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)).isEqualTo(-20f)
    }

    @Test
    fun `an item taller than the view scrolls up`() {
        assertThat(autoScrollStep(top = 50f, bottom = 990f, viewTop = 0f, viewBottom = 1000f, edge = 100f, maxStep = 20f)).isLessThan(0f)
    }
}
