package com.everlog.android.history.ui.page

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBlurBox
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppCircularProgressIndicator
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.history.data.HistoryView
import com.everlog.android.history.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

@Composable
internal fun HistoryPageScreen(viewModel: HistoryPageViewModel, onWorkout: (ELWorkout) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryPageContent(
        state = state,
        onViewPicked = viewModel::onViewPicked,
        onDayPicked = viewModel::onDayPicked,
        onReachedOldest = viewModel::onReachedOldest,
        onScrolledToDay = viewModel::onScrolledToDay,
        onEmptyDayShown = viewModel::onEmptyDayShown,
        onTryAgain = viewModel::onTryAgain,
        onTryOlderAgain = viewModel::onTryOlderAgain,
        onWorkout = onWorkout,
    )
}

@Composable
internal fun HistoryPageContent(
    state: HistoryPageState,
    onViewPicked: (HistoryView) -> Unit,
    onDayPicked: (LocalDate) -> Unit,
    onReachedOldest: () -> Unit,
    onScrolledToDay: () -> Unit,
    onEmptyDayShown: () -> Unit,
    onTryAgain: () -> Unit,
    onTryOlderAgain: () -> Unit,
    onWorkout: (ELWorkout) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val emptyDay = stringResource(R.string.history_empty_day_prompt)
    val failedMessage = stringResource(R.string.history_failed)
    val tryAgain = stringResource(R.string.history_try_again)
    val failed by rememberUpdatedState(state.failed)
    val scope = rememberCoroutineScope()
    suspend fun showFailed() {
        if (snackbar.showSnackbar(failedMessage, actionLabel = tryAgain, duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) {
            onTryAgain()
        }
    }
    LaunchedEffect(state.failed) {
        if (state.failed) showFailed() else snackbar.currentSnackbarData?.takeIf { it.visuals.actionLabel != null }?.dismiss()
    }
    val olderFailedMessage = stringResource(R.string.history_older_failed)
    LaunchedEffect(state.olderFailed) {
        if (state.olderFailed) {
            if (snackbar.showSnackbar(olderFailedMessage, actionLabel = tryAgain, duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) {
                onTryOlderAgain()
            }
        } else if (!state.failed) {
            snackbar.currentSnackbarData?.takeIf { it.visuals.actionLabel != null }?.dismiss()
        }
    }
    LaunchedEffect(state.emptyDayPicked) {
        if (state.emptyDayPicked) {
            // Outside this effect, which clearing the flag ends
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(emptyDay)
                // Back to the way to try again, which the message replaced
                if (failed) showFailed()
            }
            onEmptyDayShown()
        }
    }
    // Only once a page has taken a moment: one the cache has comes back at once, and would flash it
    val showLoadingMore by produceState(false, state.loadingMore) {
        if (state.loadingMore) delay(PageSizes.LoadingMoreDelayMillis)
        value = state.loadingMore
    }
    // Kept while switching between the calendar and the list, and across reads (see HistoryList)
    val listState = remember { LazyListState() }
    val padding = PaddingValues(start = Theme.spacing.large, top = PageSizes.TopPadding, end = Theme.spacing.large, bottom = PageSizes.BottomPadding)
    // The days scroll behind the picker and the spinner, which blur them
    AppBlurBox(
        modifier = Modifier.fillMaxSize(),
        overlay = {
            if (state.example != null) {
                // Level with the picker
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(horizontal = Theme.spacing.large).height(PageSizes.PickerHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AppExampleLabel(modifier = Modifier.testTag(HistoryPageTags.Example), floating = true)
                }
            }
            ViewPicker(
                view = state.view,
                onPicked = onViewPicked,
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = Theme.spacing.large),
            )
            // Not over the example: with no workouts, the check for older ones on the server isn't one the user asked for
            if (showLoadingMore && state.example == null) {
                AppSurface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = PageSizes.TopPadding).testTag(HistoryPageTags.LoadingMore),
                    contentPadding = PaddingValues(Theme.spacing.small),
                ) {
                    AppCircularProgressIndicator()
                }
            }
            AppSnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = PageSizes.SnackbarBottom)
                    .testTag(HistoryPageTags.Snackbar),
            )
        },
    ) {
        when {
            state.loading -> HistoryLoading(state.view, padding)
            state.example != null -> HistoryExample(state.copy(days = state.example), padding)
            state.view == HistoryView.List -> HistoryList(
                state = state,
                listState = listState,
                contentPadding = padding,
                onWorkout = onWorkout,
                onReachedOldest = onReachedOldest,
                onScrolledToDay = onScrolledToDay,
            )
            else -> key(state.read) {
                HistoryCalendar(state = state, contentPadding = padding, onDayPicked = onDayPicked, onReachedOldest = onReachedOldest)
            }
        }
    }
}

@Composable
private fun ViewPicker(view: HistoryView, onPicked: (HistoryView) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppChip(
            text = stringResource(view.title),
            onClick = { open = true },
            style = AppChipStyle.Action,
            dropdown = true,
            floating = true,
            modifier = Modifier.testTag(HistoryPageTags.View),
        )
        AppMenu(
            expanded = open,
            onDismissRequest = { open = false },
            items = HistoryView.entries.map { option ->
                AppMenuItem(
                    text = stringResource(option.title),
                    selected = option == view,
                    onClick = {
                        open = false
                        onPicked(option)
                    },
                )
            },
        )
    }
}

// The sample days in the view picked, faded and still, opening on the newest like the real ones
@Composable
private fun HistoryExample(example: HistoryPageState, padding: PaddingValues) {
    AppExample(modifier = Modifier.fillMaxSize()) {
        when (example.view) {
            HistoryView.List -> HistoryList(
                state = example,
                listState = remember { LazyListState() },
                contentPadding = padding,
                onWorkout = {},
                onReachedOldest = {},
                onScrolledToDay = {},
            )
            HistoryView.Calendar -> HistoryCalendar(state = example, contentPadding = padding, onDayPicked = {}, onReachedOldest = {})
        }
    }
}

// The view as it will show: days on a timeline, or a month of squares
@Composable
private fun HistoryLoading(view: HistoryView, padding: PaddingValues) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).testTag(HistoryPageTags.Loading),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        when (view) {
            HistoryView.List -> repeat(PageSizes.LoadingRows) {
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    Box(modifier = Modifier.width(PageSizes.LoadingDateWidth).height(PageSizes.LoadingDateHeight).background(brush, Theme.shapes.button))
                    // A workout's name over its summary, in its group
                    AppSurface(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(PageSizes.LoadingRowContentHeight),
                            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small, Alignment.CenterVertically),
                        ) {
                            LoadingLine(brush, fraction = 0.5f, height = PageSizes.LoadingTitleHeight)
                            LoadingLine(brush, fraction = 0.75f, height = PageSizes.LoadingSubtitleHeight)
                        }
                    }
                }
            }
            // The month's name over its line, the days of the week, then squares with their numbers
            HistoryView.Calendar -> Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(PageSizes.LoadingMonthHeaderHeight).padding(horizontal = Theme.spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
                ) {
                    Box(modifier = Modifier.width(PageSizes.LoadingMonthWidth).height(PageSizes.LoadingTitleHeight).background(brush, LoadingLineShape))
                    Box(modifier = Modifier.weight(1f).height(PageSizes.LoadingCellLine).background(Theme.backgrounds.separator))
                }
                Row(modifier = Modifier.padding(bottom = Theme.spacing.small)) {
                    repeat(DaysInWeek) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(PageSizes.LoadingSubtitleHeight).background(brush, LoadingLineShape))
                        }
                    }
                }
                repeat(PageSizes.LoadingWeeks) {
                    Row {
                        repeat(DaysInWeek) {
                            Box(
                                modifier = Modifier.weight(1f).aspectRatio(1f).padding(PageSizes.LoadingCellGap)
                                    .border(PageSizes.LoadingCellLine, Theme.backgrounds.separator),
                                contentAlignment = Alignment.BottomEnd,
                            ) {
                                Box(
                                    modifier = Modifier.padding(PageSizes.LoadingCellNumberMargin)
                                        .size(PageSizes.LoadingCellNumberWidth, PageSizes.LoadingTitleHeight).background(brush, LoadingLineShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingLine(brush: Brush, fraction: Float, height: Dp) {
    Box(modifier = Modifier.fillMaxWidth(fraction).height(height).background(brush, LoadingLineShape))
}

private val LoadingLineShape = RoundedCornerShape(PageSizes.LoadingLineCorner)

private val HistoryView.title: Int
    @StringRes get() = when (this) {
        HistoryView.Calendar -> R.string.view_type_calendar
        HistoryView.List -> R.string.view_type_list
    }

private const val DaysInWeek = 7

private object PageSizes {
    // Room for the view picker above the content, and a group's spacing below it
    val TopPadding = 52.dp
    // Room for Home's start-workout button below the content
    val BottomPadding = 180.dp
    val SnackbarBottom = 128.dp
    // The picker's chip, for the example label beside it
    val PickerHeight = 36.dp
    const val LoadingRows = 4
    const val LoadingWeeks = 5
    // A row's name and summary, as tall as their text
    val LoadingRowContentHeight = 48.dp
    val LoadingTitleHeight = 14.dp
    val LoadingSubtitleHeight = 10.dp
    val LoadingLineCorner = 6.dp
    // As the calendar's own
    val LoadingMonthHeaderHeight = 80.dp
    val LoadingCellGap = 3.dp
    val LoadingCellLine = 1.dp
    val LoadingCellNumberMargin = 8.dp
    val LoadingCellNumberWidth = 16.dp
    val LoadingDateWidth = 40.dp
    val LoadingDateHeight = 24.dp
    val LoadingMonthWidth = 160.dp
    const val LoadingMoreDelayMillis = 400L
}
