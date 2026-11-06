package com.everlog.android.statistics.ui.page

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBlurBox
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.R as KotlinR
import com.everlog.android.common.kotlin.format.StatsFormatUtils
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.everlog.android.common.kotlin.makeSingleLine
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.ui.R

@Composable
internal fun StatisticsPageScreen(viewModel: StatisticsPageViewModel, onFeedback: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatisticsPageContent(state = state, onPeriodPicked = viewModel::onPeriodPicked, onTryAgain = viewModel::onTryAgain, onFeedback = onFeedback)
}

@Composable
internal fun StatisticsPageContent(
    state: StatisticsPageState,
    onPeriodPicked: (StatsPeriod) -> Unit,
    onTryAgain: () -> Unit,
    onFeedback: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.statistics_year_failed)
    val tryAgain = stringResource(R.string.statistics_try_again)
    LaunchedEffect(state.failed) {
        if (state.failed) {
            val result = snackbar.showSnackbar(failedMessage, actionLabel = tryAgain, duration = SnackbarDuration.Indefinite)
            if (result == SnackbarResult.ActionPerformed) onTryAgain()
        } else {
            snackbar.currentSnackbarData?.dismiss()
        }
    }
    // The page scrolls behind the picker, which blurs it
    AppBlurBox(
        modifier = Modifier.fillMaxSize(),
        overlay = {
            if (state.example != null) {
                // Level with the picker
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(horizontal = Theme.spacing.large).height(PageSizes.PickerHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AppExampleLabel(modifier = Modifier.testTag(StatisticsPageTags.Example), floating = true)
                }
            }
            PeriodPicker(
                period = state.period,
                onPicked = onPeriodPicked,
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = Theme.spacing.large),
            )
            AppSnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = PageSizes.SnackbarBottom).testTag(StatisticsPageTags.Failed),
            )
        },
    ) {
        StatisticsPageBody(state = state, onFeedback = onFeedback)
    }
}

@Composable
private fun StatisticsPageBody(state: StatisticsPageState, onFeedback: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(top = PageSizes.TopPadding, bottom = PageSizes.BottomPadding),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        val example = state.example
        when {
            state.loading -> StatisticsLoading()
            example != null -> AppExample {
                val exampleState = state.copy(stats = example)
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    SummaryCards(exampleState)
                    StatisticsCharts(exampleState, touchable = false)
                }
            }
            else -> {
                SummaryCards(state)
                StatisticsCharts(state)
            }
        }
        if (state.feedback && example == null) {
            AppListGroup {
                row {
                    AppListItem(
                        modifier = Modifier.testTag(StatisticsPageTags.Feedback),
                        title = stringResource(DesignR.string.feedback_send),
                        subtitle = stringResource(R.string.statistics_feedback_subtitle),
                        leading = { AppIcon(imageVector = ImageVector.vectorResource(DesignR.drawable.ic_feedback), tint = Theme.contentColors.action) },
                        onClick = onFeedback,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodPicker(period: StatsPeriod, onPicked: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppChip(
            text = stringResource(period.title),
            onClick = { open = true },
            style = AppChipStyle.Action,
            dropdown = true,
            floating = true,
            modifier = Modifier.testTag(StatisticsPageTags.Period),
        )
        AppMenu(
            expanded = open,
            onDismissRequest = { open = false },
            items = StatsPeriod.entries.map { option ->
                AppMenuItem(
                    text = stringResource(option.title),
                    selected = option == period,
                    onClick = {
                        open = false
                        onPicked(option)
                    },
                )
            },
        )
    }
}

// The XML page's six numbers, two to a row
@Composable
private fun SummaryCards(state: StatisticsPageState) {
    val stats = state.stats
    val weight = weightUnitAbbreviation(state.weightUnit)
    val hours = stringResource(KotlinR.string.hour)
    val cards = listOf(
        Summary(StatsFormatUtils.formatNumberStatsLabel(stats.workouts), null, label(DesignR.string.home_week_workouts_completed)),
        Summary(StatsFormatUtils.formatWeightStatsLabel(stats.totalWeight), weight, label(DesignR.string.home_week_weight_lifted)),
        Summary(StatsFormatUtils.formatTimeStatsLabel(stats.totalMillis), hours, label(DesignR.string.home_week_total_time)),
        Summary(StatsFormatUtils.formatTimeStatsLabel(stats.averageMillis), hours, label(DesignR.string.home_week_average_time)),
        Summary(StatsFormatUtils.formatTimeStatsLabel(stats.longestMillis), hours, label(R.string.statistics_longest_session)),
        Summary(StatsFormatUtils.formatWeightStatsLabel(stats.maxWeight), weight, label(DesignR.string.workout_details_max_weight)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large), modifier = Modifier.testTag(StatisticsPageTags.Workouts)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                row.forEach { SummaryCard(it, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private data class Summary(val value: String, val unit: String?, val label: String)

@Composable
private fun label(@StringRes id: Int) = stringResource(id).lowercase().makeSingleLine()

@Composable
private fun SummaryCard(summary: Summary, modifier: Modifier = Modifier) {
    AppSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                AppText(text = summary.value, style = Theme.typography.heading)
                summary.unit?.let {
                    AppText(text = it, style = Theme.typography.caption, color = Theme.contentColors.secondary, modifier = Modifier.padding(start = Theme.spacing.extraSmall))
                }
            }
            AppText(text = summary.label, style = Theme.typography.caption, color = Theme.contentColors.secondary, textAlign = TextAlign.Center)
        }
    }
}

// The summary cards, each with its value over its label, and a chart's card with its title, as the page will show them
@Composable
private fun StatisticsLoading() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.testTag(StatisticsPageTags.Loading), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        repeat(SummaryRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                repeat(2) {
                    AppSurface(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(PageSizes.LoadingCardContentHeight),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small, Alignment.CenterVertically),
                        ) {
                            LoadingLine(brush, fraction = 0.35f, height = PageSizes.LoadingValueHeight)
                            LoadingLine(brush, fraction = 0.65f, height = PageSizes.LoadingLabelHeight)
                        }
                    }
                }
            }
        }
        AppSurface(modifier = Modifier.fillMaxWidth().height(PageSizes.LoadingChartHeight), contentPadding = PaddingValues()) {
            LoadingLine(brush, fraction = 0.4f, height = PageSizes.LoadingTitleHeight, modifier = Modifier.padding(Theme.spacing.large))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Theme.backgrounds.separator))
        }
    }
}

@Composable
private fun LoadingLine(brush: Brush, fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(fraction).height(height).background(brush, RoundedCornerShape(PageSizes.LoadingLineCorner)))
}

private val StatsPeriod.title: Int
    @StringRes get() = when (this) {
        StatsPeriod.Week -> DesignR.string.range_week
        StatsPeriod.Month -> DesignR.string.range_month
        StatsPeriod.Last12Months -> R.string.statistics_period_last_12_months
    }

private const val SummaryRows = 3

private object PageSizes {
    // Room for the period picker above the page, and a group's spacing below it
    val TopPadding = 52.dp
    // The picker's chip, for the example label beside it
    val PickerHeight = 36.dp
    // Room for Home's start workout button over the end of the page
    val BottomPadding = 180.dp
    // Above Home's tab bar and its start workout button, which the page runs under
    val SnackbarBottom = 128.dp
    // A summary card's value and label, as tall as their text
    val LoadingCardContentHeight = 40.dp
    val LoadingValueHeight = 18.dp
    val LoadingLabelHeight = 10.dp
    // A chart's title, at the chart card's own padding
    val LoadingTitleHeight = 14.dp
    val LoadingLineCorner = 6.dp
    val LoadingChartHeight = 350.dp
}
