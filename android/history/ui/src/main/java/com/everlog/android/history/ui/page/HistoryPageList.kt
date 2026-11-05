package com.everlog.android.history.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.format.workoutSummary
import java.util.Locale
import org.threeten.bp.format.TextStyle

/**
 * The days with workouts, oldest at the top, opening on the newest at the bottom. Scrolling up to the oldest day
 * shown asks for older ones, which go in above without moving the days in view.
 */
@Composable
internal fun HistoryList(
    state: HistoryPageState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onWorkout: (ELWorkout) -> Unit,
    onReachedOldest: () -> Unit,
    onScrolledToDay: () -> Unit,
) {
    val days = state.days
    val opened = remember(listState) { OpenedFlag() }
    LaunchedEffect(listState, days.isNotEmpty()) {
        if (days.isNotEmpty() && !opened.value) {
            listState.scrollToItem(days.lastIndex)
            opened.value = true
        }
    }
    LaunchedEffect(state.scrollTo) {
        val day = state.scrollTo ?: return@LaunchedEffect
        val index = days.indexOfFirst { it.date == day }
        if (index >= 0) listState.scrollToItem(index)
        opened.value = true
        onScrolledToDay()
    }
    val atOldest by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    // Asked again as each page lands, while the oldest day is still in view
    LaunchedEffect(atOldest, days.size, state.loadingMore) {
        if (atOldest && opened.value) onReachedOldest()
    }
    // Only days are items, so older days going in above keep the ones in view by their key
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(HistoryPageTags.List),
        state = listState,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(days, key = { _, day -> day.date.toEpochDay() }) { index, day ->
            HistoryDayRow(day = day, first = index == 0, last = index == days.lastIndex, state = state, onWorkout = onWorkout)
        }
    }
}

// The day on a timeline, beside a group of its workouts
@Composable
private fun HistoryDayRow(day: HistoryDay, first: Boolean, last: Boolean, state: HistoryPageState, onWorkout: (ELWorkout) -> Unit) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag(HistoryPageTags.day(day.date))) {
        Column(
            modifier = Modifier.widthIn(min = ListSizes.DateWidth).padding(top = ListSizes.DateTop, end = Theme.spacing.large),
            horizontalAlignment = Alignment.End,
        ) {
            AppText(text = day.date.dayOfMonth.toString(), style = Theme.typography.bodyBold, color = Theme.contentColors.secondary)
            AppText(
                text = day.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = Theme.typography.small,
                color = Theme.contentColors.secondary,
            )
            AppText(text = day.date.year.toString(), style = Theme.typography.small, color = Theme.contentColors.secondary)
        }
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = Theme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Line(visible = !first, modifier = Modifier.height(ListSizes.LineAboveDot))
            Box(
                modifier = Modifier.padding(vertical = Theme.spacing.extraSmall).size(ListSizes.Dot)
                    .background(Theme.backgrounds.separator, CircleShape),
            )
            Line(visible = !last, modifier = Modifier.weight(1f))
        }
        AppListGroup(modifier = Modifier.weight(1f).padding(bottom = Theme.spacing.large)) {
            day.workouts.forEach { workout ->
                row {
                    val unit = state.weightUnit
                    AppListItem(
                        title = workout.getName().orEmpty(),
                        subtitle = workoutSummary(context, workout.getDurationMillis(), workout.getTotalWeight(unit).takeIf { workout.hasWeight() }, unit),
                        onClick = { onWorkout(workout) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Line(visible: Boolean, modifier: Modifier) {
    if (visible) {
        Box(modifier = modifier.width(ListSizes.Line).background(Theme.backgrounds.separator))
    } else {
        Spacer(modifier = modifier.width(ListSizes.Line))
    }
}

// Whether the list has opened on the newest day, so reaching the top before that doesn't load older ones
private class OpenedFlag(var value: Boolean = false)

private object ListSizes {
    val DateWidth = 40.dp
    val DateTop = 6.dp
    val LineAboveDot = 27.dp
    val Dot = 8.dp
    val Line = 2.dp
}
