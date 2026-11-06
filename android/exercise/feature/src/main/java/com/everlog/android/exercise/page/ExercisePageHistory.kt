package com.everlog.android.exercise.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppCircularProgressIndicator
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.set.summaryValues
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.exerciseSummary
import com.everlog.android.common.kotlin.format.setSummary
import com.everlog.android.exercise.R
import com.everlog.android.statistics.ui.R as StatisticsR
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import kotlinx.coroutines.delay
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.TextStyle
import java.util.Locale

/**
 * Each time the exercise was done, on a timeline as the Activity tab's history shows workouts: oldest at the top,
 * opening on the newest at the bottom. Scrolling up to the oldest asks for the year before, which goes in above
 * without moving what's in view. A session saved while the newest is in view brings the list down to it.
 */
@Composable
internal fun ExercisePageHistory(state: ExercisePageState, actions: ExercisePageActions) {
    val example = state.historyExample
    val bottom = HistorySizes.BottomPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val padding = PaddingValues(start = Theme.spacing.large, end = Theme.spacing.large, bottom = bottom)
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> HistoryLoading(padding)
            // Opening on the newest, as the history does, under the label
            example != null -> AppExample(modifier = Modifier.fillMaxSize().testTag(ExercisePageTags.HistoryExample)) {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = example.lastIndex)
                Sessions(example.reversed(), state.weightUnit, PaddingValues(start = Theme.spacing.large, top = HistorySizes.ExampleTop, end = Theme.spacing.large, bottom = bottom), listState)
            }
            else -> HistoryList(state, padding, actions.onHistoryEnd)
        }
        if (example != null) {
            AppExampleLabel(modifier = Modifier.align(Alignment.TopStart).padding(horizontal = Theme.spacing.large), floating = true)
        }
        // Only once a year has taken a moment: one the cache has comes back at once, and would flash it
        val showLoadingMore by produceState(false, state.loadingMore) {
            if (state.loadingMore) delay(HistorySizes.LoadingMoreDelayMillis)
            value = state.loadingMore
        }
        if (showLoadingMore) {
            AppSurface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = Theme.spacing.small).testTag(ExercisePageTags.LoadingMore),
                contentPadding = PaddingValues(Theme.spacing.small),
            ) {
                AppCircularProgressIndicator()
            }
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
    // Asked again as each year lands, while the oldest is still in view. With nothing in the last 12 months, the
    // empty list is at its oldest straight away.
    LaunchedEffect(atOldest, oldestFirst.size, state.loadingMore, opened) {
        if (atOldest && (opened || oldestFirst.isEmpty())) onReachedOldest()
    }
    Sessions(oldestFirst, state.weightUnit, padding, listState, olderFailed = state.olderFailed, onRetryOlder = onReachedOldest)
}

@Composable
private fun Sessions(
    oldestFirst: List<ExerciseSession>,
    unit: WeightUnit,
    padding: PaddingValues,
    listState: LazyListState,
    olderFailed: Boolean = false,
    onRetryOlder: () -> Unit = {},
) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(ExercisePageTags.History), state = listState, contentPadding = padding) {
        if (olderFailed) {
            item(key = "olderFailed") {
                AppListGroup(modifier = Modifier.padding(bottom = Theme.spacing.large)) {
                    row {
                        AppListItem(
                            modifier = Modifier.testTag(ExercisePageTags.OlderFailed),
                            title = stringResource(R.string.exercise_page_older_failed),
                            subtitle = stringResource(StatisticsR.string.statistics_try_again),
                            onClick = onRetryOlder,
                        )
                    }
                }
            }
        }
        // Only sessions are items, so older ones going in above keep the ones in view by their key
        val keys = sessionKeys(oldestFirst)
        itemsIndexed(oldestFirst, key = { index, _ -> keys[index] }) { index, session ->
            SessionRow(session, first = index == 0, last = index == oldestFirst.lastIndex, unit = unit, key = keys[index])
        }
    }
}

// The day on a timeline, beside the workout's name and what the exercise added up to, then each set
@Composable
private fun SessionRow(session: ExerciseSession, first: Boolean, last: Boolean, unit: WeightUnit, key: String) {
    val context = LocalContext.current
    val day = Instant.ofEpochMilli(session.completedDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val exercise = session.exercise
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag(ExercisePageTags.session(key))) {
        DateColumn(day)
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = Theme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Line(visible = !first, modifier = Modifier.height(HistorySizes.LineAboveDot))
            Box(modifier = Modifier.padding(vertical = Theme.spacing.extraSmall).size(HistorySizes.Dot).background(Theme.backgrounds.separator, CircleShape))
            Line(visible = !last, modifier = Modifier.weight(1f))
        }
        AppListGroup(modifier = Modifier.weight(1f).padding(bottom = Theme.spacing.large)) {
            row {
                AppListItem(
                    title = session.workoutName.orEmpty(),
                    subtitle = exerciseSummary(context, exercise.getTotalWeight(unit), exercise.getTotalReps(), exercise.getTotalTimeSeconds(), unit),
                )
            }
            exercise.getSetsWithData().forEachIndexed { setIndex, set ->
                setSummary(context, set.summaryValues(useTemplate = false), unit)?.let { summary ->
                    row {
                        AppListItem(
                            title = summary,
                            leading = { AppText(text = "${setIndex + 1}", style = Theme.typography.caption, color = Theme.contentColors.secondary) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateColumn(day: LocalDate) {
    Column(
        modifier = Modifier.widthIn(min = HistorySizes.DateWidth).padding(top = HistorySizes.DateTop, end = Theme.spacing.large),
        horizontalAlignment = Alignment.End,
    ) {
        AppText(text = day.dayOfMonth.toString(), style = Theme.typography.bodyBold, color = Theme.contentColors.secondary)
        AppText(text = day.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(), style = Theme.typography.small, color = Theme.contentColors.secondary)
        AppText(text = day.year.toString(), style = Theme.typography.small, color = Theme.contentColors.secondary)
    }
}

@Composable
private fun Line(visible: Boolean, modifier: Modifier) {
    if (visible) {
        Box(modifier = modifier.width(HistorySizes.Line).background(Theme.backgrounds.separator))
    } else {
        Spacer(modifier = modifier.width(HistorySizes.Line))
    }
}

// A few sessions' rows on the timeline, as the list will show them
@Composable
private fun HistoryLoading(padding: PaddingValues) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).testTag(ExercisePageTags.HistoryLoading),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        repeat(HistorySizes.LoadingRows) {
            Row {
                Spacer(modifier = Modifier.width(HistorySizes.DateWidth + Theme.spacing.large * 2 + HistorySizes.Line))
                AppSurface(modifier = Modifier.weight(1f).height(HistorySizes.LoadingRowHeight)) {
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(HistorySizes.LoadingTitleHeight).background(brush, RoundedCornerShape(HistorySizes.LoadingLineCorner)))
                }
            }
        }
    }
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
    val DateWidth = 40.dp
    val DateTop = 6.dp
    val LineAboveDot = 27.dp
    val Dot = 8.dp
    val Line = 2.dp
    val BottomPadding = 32.dp
    // Room for the example's label above the sessions
    val ExampleTop = 44.dp
    val LoadingRowHeight = 120.dp
    val LoadingTitleHeight = 14.dp
    val LoadingLineCorner = 6.dp
    const val LoadingRows = 4
    const val LoadingMoreDelayMillis = 300L
}
