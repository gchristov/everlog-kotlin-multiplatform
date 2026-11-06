package com.everlog.android.exercise.page

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppStatTile
import com.everlog.android.common.compose.elements.AppStatTilePlaceholder
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.kotlin.format.StatsFormatUtils
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.everlog.android.common.kotlin.makeSingleLine
import com.everlog.android.exercise.R
import com.everlog.android.statistics.ui.R as StatisticsR
import com.everlog.android.statistics.ui.chart.ChartLine
import com.everlog.android.statistics.ui.chart.ChartPoint
import com.everlog.android.statistics.ui.chart.StatisticsChartCard
import com.everlog.android.statistics.ui.chart.StatisticsChartSizes
import com.everlog.android.statistics.ui.chart.StatisticsLineChart
import com.everlog.android.workout.data.exercisehistory.ExerciseStats
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import org.threeten.bp.LocalDate

// The range's numbers and charts, as the Activity tab's statistics page shows them, with a faded example while the
// range has no sessions
@Composable
internal fun ExercisePageStatistics(state: ExercisePageState, actions: ExercisePageActions) {
    val example = state.statsExample
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(bottom = Theme.spacing.large)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (example != null) AppExampleLabel(modifier = Modifier.testTag(ExercisePageTags.StatisticsExample))
            Box(modifier = Modifier.weight(1f))
            RangePicker(range = state.range, onPicked = actions.onRangePicked)
        }
        when {
            state.loading -> StatisticsLoading()
            example != null -> AppExample {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    StatTiles(example, state.weightUnit)
                    Charts(example, state.weightUnit, state.today, touchable = false)
                }
            }
            else -> {
                StatTiles(state.stats, state.weightUnit)
                Charts(state.stats, state.weightUnit, state.today)
            }
        }
        if (state.feedback && example == null) {
            AppListGroup {
                row {
                    AppListItem(
                        modifier = Modifier.testTag(ExercisePageTags.Feedback),
                        title = stringResource(DesignR.string.feedback_send),
                        subtitle = stringResource(StatisticsR.string.statistics_feedback_subtitle),
                        leading = { AppIcon(imageVector = ImageVector.vectorResource(DesignR.drawable.ic_feedback), tint = Theme.contentColors.action) },
                        onClick = actions.onFeedback,
                    )
                }
            }
        }
    }
}

@Composable
private fun RangePicker(range: ExerciseStatsRange, onPicked: (ExerciseStatsRange) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AppChip(
            text = stringResource(range.title),
            onClick = { open = true },
            style = AppChipStyle.Action,
            dropdown = true,
            modifier = Modifier.testTag(ExercisePageTags.Range),
        )
        AppMenu(
            expanded = open,
            onDismissRequest = { open = false },
            items = ExerciseStatsRange.entries.map { option ->
                AppMenuItem(
                    text = stringResource(option.title),
                    selected = option == range,
                    onClick = {
                        open = false
                        onPicked(option)
                    },
                )
            },
        )
    }
}

// Times performed, the weight and reps it added up to, and the heaviest set, two to a row
@Composable
private fun StatTiles(stats: ExerciseStats, unit: WeightUnit) {
    val weight = weightUnitAbbreviation(unit)
    val tiles = listOf(
        Tile(StatsFormatUtils.formatNumberStatsLabel(stats.timesPerformed), null, label(R.string.exercise_statistics_times_performed)),
        Tile(StatsFormatUtils.formatWeightStatsLabel(stats.totalWeight), weight, label(DesignR.string.home_week_weight_lifted)),
        Tile(StatsFormatUtils.formatNumberStatsLabel(stats.totalReps), null, label(R.string.exercise_statistics_total_reps)),
        Tile(StatsFormatUtils.formatWeightStatsLabel(stats.heaviestSet?.getWeight(unit) ?: 0f), weight, label(DesignR.string.workout_details_max_weight)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large), modifier = Modifier.testTag(ExercisePageTags.Tiles)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                row.forEach { AppStatTile(value = it.value, unit = it.unit, label = it.label, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private data class Tile(val value: String, val unit: String?, val label: String)

@Composable
private fun label(@StringRes id: Int) = stringResource(id).lowercase().makeSingleLine()

// Reps against the heaviest weight, and the best estimated 1RM, each a point per day, or per month for 12 months
@Composable
private fun Charts(stats: ExerciseStats, unit: WeightUnit, today: LocalDate, touchable: Boolean = true) {
    val weightTitle = "${stringResource(DesignR.string.weight)} (${weightUnitAbbreviation(unit)})"
    val reps = stringResource(DesignR.string.reps)
    val weight = stringResource(DesignR.string.weight)
    val xLabel: (Float) -> String = { stats.range.chartLabel(it, today) }
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large), modifier = Modifier.testTag(ExercisePageTags.Charts)) {
        StatisticsChartCard(title = stringResource(R.string.exercise_statistics_reps_weight)) {
            StatisticsLineChart(
                lines = listOf(
                    ChartLine(stats.reps.map { ChartPoint(stats.range.chartX(it.start), it.value) }, name = reps),
                    ChartLine(stats.maxWeight.map { ChartPoint(stats.range.chartX(it.start), it.value) }, name = weight, color = DesignR.color.pie_chart_4, rightAxis = true),
                ),
                leftAxisTitle = reps,
                rightAxisTitle = weightTitle,
                xLabel = xLabel,
                touchable = touchable,
            )
        }
        StatisticsChartCard(title = stringResource(R.string.exercise_statistics_1rm)) {
            StatisticsLineChart(
                lines = listOf(ChartLine(stats.oneRepMax.map { ChartPoint(stats.range.chartX(it.start), it.value) })),
                leftAxisTitle = weightTitle,
                xLabel = xLabel,
                touchable = touchable,
            )
        }
    }
}

// The tiles, each with its value over its label, and a chart's card with its title, as the tab will show them
@Composable
private fun StatisticsLoading() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.testTag(ExercisePageTags.StatisticsLoading), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                repeat(2) { AppStatTilePlaceholder(brush, modifier = Modifier.weight(1f)) }
            }
        }
        AppSurface(modifier = Modifier.fillMaxWidth().height(StatisticsChartSizes.Height)) {
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(StatisticsSizes.LoadingTitleHeight).background(brush, RoundedCornerShape(StatisticsSizes.LoadingLineCorner)))
        }
    }
}

private val ExerciseStatsRange.title: Int
    @StringRes get() = when (this) {
        ExerciseStatsRange.Today -> DesignR.string.range_today
        ExerciseStatsRange.Week -> DesignR.string.range_week
        ExerciseStatsRange.Month -> DesignR.string.range_month
        ExerciseStatsRange.Last12Months -> StatisticsR.string.statistics_period_last_12_months
    }

private object StatisticsSizes {
    val LoadingTitleHeight = 14.dp
    val LoadingLineCorner = 6.dp
}
