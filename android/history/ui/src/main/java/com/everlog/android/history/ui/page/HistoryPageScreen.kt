package com.everlog.android.history.ui.page

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBlurBox
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppCircularProgressIndicator
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.history.ui.R
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
    // Kept while switching between the calendar and the list, and started again with each read
    val listState = remember(state.read) { LazyListState() }
    val padding = PaddingValues(start = Theme.spacing.large, top = PageSizes.TopPadding, end = Theme.spacing.large, bottom = PageSizes.BottomPadding)
    // The days scroll behind the picker and the spinner, which blur them
    AppBlurBox(
        modifier = Modifier.fillMaxSize(),
        overlay = {
            ViewPicker(
                view = state.view,
                onPicked = onViewPicked,
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = Theme.spacing.large),
            )
            if (state.loadingMore) {
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
            state.view == HistoryView.List && state.days.isEmpty() -> HistoryEmpty(padding)
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

@Composable
private fun HistoryEmpty(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding).padding(top = PageSizes.EmptyTop), contentAlignment = Alignment.TopCenter) {
        AppText(
            text = stringResource(DesignR.string.history_empty_prompt),
            style = Theme.typography.subtitle,
            color = Theme.contentColors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(PageSizes.EmptyWidth).testTag(HistoryPageTags.Empty),
        )
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
                    Box(modifier = Modifier.weight(1f).height(PageSizes.LoadingRowHeight).background(brush, Theme.shapes.surface))
                }
            }
            HistoryView.Calendar -> {
                Box(modifier = Modifier.width(PageSizes.LoadingMonthWidth).height(PageSizes.LoadingDateHeight).background(brush, Theme.shapes.button))
                repeat(PageSizes.LoadingWeeks) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                        repeat(DaysInWeek) { Box(modifier = Modifier.weight(1f).aspectRatio(1f).background(brush)) }
                    }
                }
            }
        }
    }
}

private val HistoryView.title: Int
    @StringRes get() = when (this) {
        HistoryView.Calendar -> R.string.view_type_calendar
        HistoryView.List -> R.string.view_type_list
    }

private const val DaysInWeek = 7

private object PageSizes {
    // Room for the view picker above the content
    val TopPadding = 64.dp
    // Room for Home's start-workout button below the content
    val BottomPadding = 180.dp
    val SnackbarBottom = 128.dp
    val EmptyTop = 48.dp
    val EmptyWidth = 250.dp
    const val LoadingRows = 4
    const val LoadingWeeks = 5
    val LoadingRowHeight = 80.dp
    val LoadingDateWidth = 40.dp
    val LoadingDateHeight = 24.dp
    val LoadingMonthWidth = 160.dp
}
