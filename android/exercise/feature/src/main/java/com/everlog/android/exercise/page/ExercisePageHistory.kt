package com.everlog.android.exercise.page

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.everlog.android.common.compose.elements.AppBlurBox
import com.everlog.android.common.compose.elements.AppCircularProgressIndicator
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppTimelineItem
import com.everlog.android.common.compose.elements.TimelineSizes
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.set.summaryValues
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.exerciseSummary
import com.everlog.android.common.kotlin.format.setSummary
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import kotlinx.coroutines.delay
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId

/**
 * Each time the exercise was done, on a timeline as the Activity tab's history shows workouts: oldest at the top,
 * opening on the newest at the bottom. Scrolling up to the oldest asks for the year before, which goes in above
 * without moving what's in view. A session saved while the newest is in view brings the list down to it.
 */
@Composable
internal fun ExercisePageHistory(state: ExercisePageState, actions: ExercisePageActions) {
    val example = state.historyExample
    val bottom = HistorySizes.BottomPadding
    // Room at the top for the example's label, otherwise the screen margin. The spinner for an older year floats over
    // the oldest session, which it blurs.
    val top = if (example != null) HistorySizes.ExampleTopPadding else Theme.spacing.large
    val padding = PaddingValues(start = Theme.spacing.large, top = top, end = Theme.spacing.large, bottom = bottom)
    // Only once a year has taken a moment: one the cache has comes back at once, and would flash it
    val showLoadingMore by produceState(false, state.loadingMore) {
        if (state.loadingMore) delay(HistorySizes.LoadingMoreDelayMillis)
        value = state.loadingMore
    }
    // The sessions scroll behind the example's label and the spinner, which blur them, as on the Activity tab
    AppBlurBox(
        modifier = Modifier.fillMaxSize(),
        overlay = {
            if (example != null) {
                AppExampleLabel(modifier = Modifier.align(Alignment.TopStart).padding(horizontal = Theme.spacing.large), floating = true)
            }
            if (showLoadingMore && state.sessions.isNotEmpty()) {
                AppSurface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = Theme.spacing.small).testTag(ExercisePageTags.LoadingMore),
                    contentPadding = PaddingValues(Theme.spacing.small),
                ) {
                    AppCircularProgressIndicator()
                }
            }
        },
    ) {
        when {
            // Nothing to show yet: the last 12 months had no sessions and older years are being read
            state.loading || (state.sessions.isEmpty() && example == null && !state.olderFailed) -> HistoryLoading(padding)
            // Opening on the newest, as the history does, under the label
            example != null -> AppExample(modifier = Modifier.fillMaxSize().testTag(ExercisePageTags.HistoryExample)) {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = example.lastIndex)
                Sessions(example.reversed(), state.weightUnit, padding, listState)
            }
            else -> HistoryList(state, padding, actions.onHistoryEnd)
        }
    }
}

@Composable
private fun HistoryList(state: ExercisePageState, padding: PaddingValues, onReachedOldest: () -> Unit) {
    val oldestFirst = remember(state.sessions) { state.sessions.reversed() }
    val listState = rememberLazyListState()
    // State, so a list short enough to fit asks for older sessions once it has opened, without being scrolled
    var opened by remember(listState) { mutableStateOf(false) }
    LaunchedEffect(oldestFirst.isNotEmpty()) {
        if (oldestFirst.isNotEmpty() && !opened) {
            listState.scrollToEnd(oldestFirst)
            opened = true
        }
    }
    // A session saved while the newest is in view brings the list down to it, like a chat
    val newest = oldestFirst.lastOrNull()?.workoutUuid
    val followed = remember(listState) { FollowedNewest() }
    LaunchedEffect(newest) {
        val previous = followed.value
        followed.value = newest
        if (previous == null || newest == null || !opened) return@LaunchedEffect
        val previousIndex = oldestFirst.indexOfLast { it.workoutUuid == previous }
        if (previousIndex >= 0 && listState.layoutInfo.visibleItemsInfo.any { it.index >= previousIndex }) listState.scrollToEnd(oldestFirst)
    }
    val atOldest by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    // Asked again as each year lands, while the oldest is still in view. An empty history reads back by itself
    // (ExercisePageViewModel), showing the shimmer meanwhile.
    LaunchedEffect(atOldest, oldestFirst.size, state.loadingMore, opened) {
        if (atOldest && opened) onReachedOldest()
    }
    Sessions(oldestFirst, state.weightUnit, padding, listState)
}

@Composable
private fun Sessions(
    oldestFirst: List<ExerciseSession>,
    unit: WeightUnit,
    padding: PaddingValues,
    listState: LazyListState,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(ExercisePageTags.History), state = listState, contentPadding = padding) {
        // Only sessions are items, so older ones going in above keep the ones in view by their key
        val keys = sessionKeys(oldestFirst)
        itemsIndexed(oldestFirst, key = { index, _ -> keys[index] }) { index, session ->
            SessionRow(session, first = index == 0, last = index == oldestFirst.lastIndex, unit = unit, key = keys[index])
        }
    }
}

// The day on a timeline, beside the workout's name and what the exercise added up to, then each set numbered in a
// circle: the old screen's layout, kept local until the history is redesigned
@Composable
private fun SessionRow(session: ExerciseSession, first: Boolean, last: Boolean, unit: WeightUnit, key: String) {
    val context = LocalContext.current
    val day = Instant.ofEpochMilli(session.completedDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val exercise = session.exercise
    AppTimelineItem(date = day, first = first, last = last, modifier = Modifier.testTag(ExercisePageTags.session(key))) {
        Column(modifier = Modifier.padding(top = Theme.spacing.extraSmall, bottom = SessionSizes.ExtraBottom)) {
            AppText(text = session.workoutName.orEmpty(), style = Theme.typography.heading)
            AppText(
                text = exerciseSummary(context, exercise.getTotalWeight(unit), exercise.getTotalReps(), exercise.getTotalTimeSeconds(), unit),
                style = SessionSizes.Summary,
                color = Theme.contentColors.secondary,
                modifier = Modifier.padding(top = Theme.spacing.extraSmall),
            )
            Column(
                modifier = Modifier.padding(top = Theme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                exercise.getSetsWithData().forEachIndexed { setIndex, set ->
                    setSummary(context, set.summaryValues(useTemplate = false), unit)?.let { summary -> SetRow(number = setIndex + 1, summary = summary) }
                }
            }
        }
    }
}

@Composable
private fun SetRow(number: Int, summary: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(SessionSizes.SetNumberInset)
                .size(SessionSizes.SetNumber)
                .border(SessionSizes.SetNumberBorder, Theme.contentColors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = number.toString(), style = SessionSizes.SetNumberText)
        }
        AppText(text = summary, style = SessionSizes.Set, modifier = Modifier.padding(start = SessionSizes.SetGap))
    }
}

// The old screen's sizes (row_history_exercise, row_set_summary)
private object SessionSizes {
    val Summary = TextStyle(fontSize = 15.sp)
    val Set = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)
    val SetNumberText = TextStyle(fontSize = 15.sp)
    val SetNumber = 31.dp
    val SetNumberInset = 2.dp
    val SetNumberBorder = 1.dp
    val SetGap = 10.dp
    // On top of the timeline's spacing, as the old rows were 20dp apart
    val ExtraBottom = 4.dp
}

// As the Activity tab's history list loads: a date beside a group with a name over its summary
@Composable
private fun HistoryLoading(padding: PaddingValues) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).testTag(ExercisePageTags.HistoryLoading),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        repeat(HistorySizes.LoadingRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                Box(modifier = Modifier.width(TimelineSizes.DateWidth).height(HistorySizes.LoadingDateHeight).background(brush, Theme.shapes.button))
                AppSurface(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(HistorySizes.LoadingRowContentHeight),
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small, Alignment.CenterVertically),
                    ) {
                        LoadingLine(brush, fraction = 0.5f, height = HistorySizes.LoadingTitleHeight)
                        LoadingLine(brush, fraction = 0.75f, height = HistorySizes.LoadingSubtitleHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingLine(brush: Brush, fraction: Float, height: Dp) {
    Box(modifier = Modifier.fillMaxWidth(fraction).height(height).background(brush, RoundedCornerShape(HistorySizes.LoadingLineCorner)))
}

// A session's workout and which of the exercise's sessions in it this is, as a workout with the exercise twice has two
internal fun sessionKeys(sessions: List<ExerciseSession>): List<String> {
    val seen = HashMap<String, Int>()
    return sessions.map { session ->
        val nth = seen.merge(session.workoutUuid, 1, Int::plus)!!
        "${session.workoutUuid}#$nth"
    }
}

// The newest session as the list last saw it, to tell one saved after it
private class FollowedNewest(var value: String? = null)

// Down to the end of the last session, past the bottom padding: scrolling to it alone puts its top at the top, which
// a tall session overflows. The offset is cut back to how far the list goes.
private suspend fun LazyListState.scrollToEnd(sessions: List<ExerciseSession>) = scrollToItem(sessions.lastIndex, Int.MAX_VALUE)

private object HistorySizes {
    val BottomPadding = 32.dp
    // The example's label above the sessions, a group's spacing apart
    val ExampleTopPadding = 52.dp
    // The Activity tab's history shimmer (HistoryPageScreen)
    val LoadingDateHeight = 24.dp
    val LoadingRowContentHeight = 48.dp
    val LoadingTitleHeight = 14.dp
    val LoadingSubtitleHeight = 10.dp
    val LoadingLineCorner = 6.dp
    const val LoadingRows = 4
    // As the Activity tab's history
    const val LoadingMoreDelayMillis = 400L
}
