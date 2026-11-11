package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.everlog.android.common.composetest.FeatureTest
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.After
import org.junit.Test
import org.threeten.bp.LocalDate

class AppTimelineItemTest : FeatureTest() {

    private val locale = Locale.getDefault()

    @After
    fun tearDown() = Locale.setDefault(locale)

    @Test
    fun everyMonthsContentStartsInTheSamePlace() {
        // British English shortens September to SEPT, wider than the year below it
        Locale.setDefault(Locale.UK)
        val days = listOf(LocalDate.of(2020, 9, 17), LocalDate.of(2020, 11, 13), LocalDate.of(2021, 5, 1))
        setContent {
            Column {
                days.forEachIndexed { index, day ->
                    AppTimelineItem(date = day, first = index == 0, last = index == days.lastIndex) {
                        Box(modifier = Modifier.fillMaxWidth().height(48.dp).testTag("content-$index"))
                    }
                }
            }
        }

        val starts = days.indices.map { compose.onNodeWithTag("content-$it").getUnclippedBoundsInRoot().left }
        assertThat(starts.distinct()).hasSize(1)
    }
}
