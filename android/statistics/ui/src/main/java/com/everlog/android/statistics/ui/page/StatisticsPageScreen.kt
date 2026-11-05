package com.everlog.android.statistics.ui.page

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppTertiaryButton
import com.everlog.android.common.compose.elements.AppText
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
    Box(modifier = Modifier.fillMaxSize()) {
        StatisticsPageBody(state = state, onPeriodPicked = onPeriodPicked, onFeedback = onFeedback)
        AppSnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = PageSizes.SnackbarBottom).testTag(StatisticsPageTags.Failed),
        )
    }
}

@Composable
private fun StatisticsPageBody(state: StatisticsPageState, onPeriodPicked: (StatsPeriod) -> Unit, onFeedback: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.large, bottom = PageSizes.BottomPadding),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        PeriodPicker(period = state.period, onPicked = onPeriodPicked, modifier = Modifier.align(Alignment.End))
        if (state.loading) {
            StatisticsLoading()
        } else {
            SummaryCards(state)
            StatisticsCharts(state)
        }
        if (state.feedback) {
            Column(modifier = Modifier.fillMaxWidth().testTag(StatisticsPageTags.Feedback), horizontalAlignment = Alignment.CenterHorizontally) {
                AppText(text = stringResource(DesignR.string.statistics_footer_text), style = Theme.typography.body, color = Theme.contentColors.secondary)
                AppTertiaryButton(onClick = onFeedback, text = stringResource(DesignR.string.exercises_footer_link))
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

// The summary cards and a chart, as the page will show them
@Composable
private fun StatisticsLoading() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.testTag(StatisticsPageTags.Loading), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        repeat(SummaryRows) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                repeat(2) { Box(modifier = Modifier.weight(1f).height(PageSizes.LoadingCardHeight).background(brush, Theme.shapes.surface)) }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(PageSizes.LoadingChartHeight).background(brush, Theme.shapes.surface))
    }
}

private val StatsPeriod.title: Int
    @StringRes get() = when (this) {
        StatsPeriod.Week -> DesignR.string.range_week
        StatsPeriod.Month -> DesignR.string.range_month
        StatsPeriod.Last12Months -> R.string.statistics_period_last_12_months
    }

private const val SummaryRows = 3

private object PageSizes {
    // Room for Home's start workout button over the end of the page
    val BottomPadding = 180.dp
    // Above Home's tab bar and its start workout button, which the page runs under
    val SnackbarBottom = 128.dp
    val LoadingCardHeight = 72.dp
    val LoadingChartHeight = 350.dp
}
