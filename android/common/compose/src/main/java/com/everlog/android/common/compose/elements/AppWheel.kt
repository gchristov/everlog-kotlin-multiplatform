package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import com.everlog.android.common.compose.theme.Theme

/**
 * One column of an [AppWheel]: its values as shown, the one picked, and what to call it for accessibility.
 */
data class AppWheelColumn(
    val items: List<String>,
    val selectedIndex: Int,
    val onPick: (Int) -> Unit,
    val contentDescription: String,
)

/**
 * Values picked by scrolling them under a highlighted centre row, e.g. in a bottom sheet: one column, or several side
 * by side (whole kilograms and quarters), with an optional [suffix] after them (the unit). Each column snaps to a
 * value and calls its [AppWheelColumn.onPick] once it settles; tapping a value scrolls it to the centre. A column whose
 * [AppWheelColumn.selectedIndex] changes from outside scrolls to it.
 */
@Composable
fun AppWheel(
    columns: List<AppWheelColumn>,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.surface, Theme.shapes.surface)
            .padding(vertical = Theme.spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.large)
                .height(WheelSizes.Item + WheelSizes.BandOutset * 2)
                .background(Theme.backgrounds.selected, WheelSizes.BandShape)
                .border(1.dp, Theme.contentColors.action, WheelSizes.BandShape),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            columns.forEach { WheelColumn(it) }
            suffix?.let {
                AppText(
                    modifier = Modifier.padding(start = Theme.spacing.small),
                    text = it,
                    style = Theme.typography.heading,
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(column: AppWheelColumn) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = column.selectedIndex)
    val scope = rememberCoroutineScope()
    val itemPx = with(LocalDensity.current) { WheelSizes.Item.toPx() }
    val centred by remember { derivedStateOf { centredIndex(state, itemPx) } }
    val onPick by rememberUpdatedState(column.onPick)
    val selected by rememberUpdatedState(column.selectedIndex)

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect { if (centred != selected) onPick(centred) }
    }
    LaunchedEffect(column.selectedIndex) {
        if (!state.isScrollInProgress && centred != column.selectedIndex) state.animateScrollToItem(column.selectedIndex)
    }

    val last = column.items.lastIndex
    val centredStyle = Theme.typography.heading.copy(fontFeatureSettings = "tnum")
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // As wide as its longest value in the centre row's style, so no value wraps, and wide enough to grab easily
    val width = remember(column.items, centredStyle) {
        val widest = column.items.maxOfOrNull { measurer.measure(it, centredStyle, maxLines = 1).size.width } ?: 0
        maxOf(with(density) { widest.toDp() } + WheelSizes.ColumnPadding * 2, WheelSizes.MinColumn)
    }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = WheelSizes.Item * (WheelSizes.Visible / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .height(WheelSizes.Item * WheelSizes.Visible)
            .semantics {
                contentDescription = column.contentDescription
                stateDescription = column.items.getOrElse(centred) { "" }
                customActions = listOfNotNull(
                    CustomAccessibilityAction(column.items.getOrElse(centred - 1) { "" }) {
                        scope.launch { state.animateScrollToItem(centred - 1) }
                        true
                    }.takeIf { centred > 0 },
                    CustomAccessibilityAction(column.items.getOrElse(centred + 1) { "" }) {
                        scope.launch { state.animateScrollToItem(centred + 1) }
                        true
                    }.takeIf { centred < last },
                )
            },
    ) {
        itemsIndexed(column.items) { index, item ->
            val distance = kotlin.math.abs(index - centred)
            Box(
                modifier = Modifier
                    .height(WheelSizes.Item)
                    .fillMaxWidth()
                    .clickable { scope.launch { state.animateScrollToItem(index) } },
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    modifier = Modifier.alpha(if (distance >= 2) WheelSizes.FarAlpha else 1f),
                    text = item,
                    maxLines = 1,
                    style = if (distance == 0) {
                        centredStyle
                    } else {
                        Theme.typography.body.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum")
                    },
                    color = if (distance == 0) Theme.contentColors.primary else Theme.contentColors.secondary,
                )
            }
        }
    }
}

// The item under the centre row: the first one shown, or the next once it has scrolled more than halfway out
private fun centredIndex(state: LazyListState, itemPx: Float): Int =
    centredIndex(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, itemPx, state.layoutInfo.totalItemsCount)

internal fun centredIndex(firstVisible: Int, scrollOffsetPx: Int, itemPx: Float, count: Int): Int {
    if (count == 0) return 0
    val index = if (scrollOffsetPx > itemPx / 2) firstVisible + 1 else firstVisible
    return index.coerceIn(0, count - 1)
}

private object WheelSizes {
    val Item = 44.dp
    const val Visible = 5
    val ColumnPadding = 24.dp
    val MinColumn = 96.dp
    val BandOutset = 2.dp
    val BandShape = RoundedCornerShape(12.dp)
    const val FarAlpha = 0.5f
}

@Preview
@Composable
private fun AppWheelPreview() {
    Theme {
        AppWheel(
            columns = listOf(
                AppWheelColumn((1..20).map { "$it" }, selectedIndex = 0, onPick = {}, contentDescription = "Whole kilograms"),
                AppWheelColumn(listOf(".00", ".25", ".50", ".75"), selectedIndex = 1, onPick = {}, contentDescription = "Quarters"),
            ),
            suffix = "kg",
        )
    }
}
