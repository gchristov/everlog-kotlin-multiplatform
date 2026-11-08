package com.everlog.android.common.compose.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppWheelTest {

    @Test
    fun `the first item shown is centred until it has scrolled more than halfway out`() {
        assertThat(centredIndex(firstVisible = 3, scrollOffsetPx = 0, itemPx = 100f, count = 7)).isEqualTo(3)
        assertThat(centredIndex(firstVisible = 3, scrollOffsetPx = 50, itemPx = 100f, count = 7)).isEqualTo(3)
        assertThat(centredIndex(firstVisible = 3, scrollOffsetPx = 51, itemPx = 100f, count = 7)).isEqualTo(4)
    }

    @Test
    fun `the centred item stays within the list`() {
        assertThat(centredIndex(firstVisible = 6, scrollOffsetPx = 90, itemPx = 100f, count = 7)).isEqualTo(6)
    }

    @Test
    fun `before laying out the centred item is the one it was asked to show`() {
        assertThat(centredIndex(firstVisible = 2, scrollOffsetPx = 0, itemPx = 100f, count = 0)).isEqualTo(2)
    }
}
