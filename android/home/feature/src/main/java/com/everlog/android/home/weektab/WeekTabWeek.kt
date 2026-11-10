package com.everlog.android.home.weektab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.R as KotlinR
import com.everlog.android.common.kotlin.dayOfWeekFormatted
import com.everlog.android.common.kotlin.format.StatsFormatUtils
import com.everlog.android.common.kotlin.format.weightUnitAbbreviation
import com.everlog.android.home.R
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.TextStyle
import java.util.Locale

@Composable
internal fun WeekDays(state: WeekTabState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppSurface(modifier = modifier.fillMaxWidth().clip(Theme.shapes.surface).clickable(onClick = onClick).testTag(WeekTabTags.Days)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.stats.days.forEach { day ->
                WeekDay(day = day, active = day in state.stats.workoutDays, today = day == state.today)
            }
        }
    }
}

@Composable
private fun WeekDay(day: LocalDate, active: Boolean, today: Boolean) {
    val color = when {
        active -> Theme.contentColors.onAction
        today -> Theme.contentColors.primary
        else -> Theme.contentColors.secondary
    }
    Column(
        modifier = Modifier
            .size(WeekTabSizes.DayWidth)
            .then(if (active) Modifier.background(Theme.contentColors.action, DayShape) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppText(text = day.dayOfMonth.toString(), style = Theme.typography.subtitle, color = color)
        AppText(text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = Theme.typography.small, color = color)
    }
}

@Composable
internal fun WeekStatCards(
    state: WeekTabState,
    onGoalClick: () -> Unit,
    onStatsClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    val unit = weightUnitAbbreviation(state.weightUnit)
    val hours = stringResource(KotlinR.string.hour)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        GoalCard(state = state, onClick = onGoalClick)
        StatCard(
            icon = painterResource(R.drawable.ic_check_circle),
            label = stringResource(DesignR.string.home_week_workouts_completed),
            value = StatsFormatUtils.formatNumberStatsLabel(state.stats.workouts),
            onClick = onStatsClick,
            modifier = Modifier.testTag(WeekTabTags.Workouts),
        )
        StatCard(
            icon = painterResource(DesignR.drawable.ic_weight),
            label = stringResource(DesignR.string.home_week_weight_lifted),
            value = StatsFormatUtils.formatWeightStatsLabel(state.stats.weightLifted),
            unit = unit,
            onClick = onStatsClick,
        )
        StatCard(
            icon = painterResource(DesignR.drawable.ic_time),
            label = stringResource(DesignR.string.home_week_total_time),
            value = StatsFormatUtils.formatTimeStatsLabel(state.stats.totalMillis),
            unit = hours,
            onClick = onStatsClick,
        )
        StatCard(
            icon = painterResource(R.drawable.ic_timelapse),
            label = stringResource(DesignR.string.home_week_average_time),
            value = StatsFormatUtils.formatTimeStatsLabel(state.stats.averageMillis),
            unit = hours,
            onClick = onStatsClick,
        )
    }
}

@Composable
private fun GoalCard(state: WeekTabState, onClick: () -> Unit) {
    val progress by animateFloatAsState(targetValue = state.goalProgress, animationSpec = tween(GoalAnimationMillis), label = "goal")
    val track = WeekTabColors.line
    val accent = Theme.contentColors.action
    StatSurface(onClick = onClick, modifier = Modifier.testTag(WeekTabTags.Goal)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = minOf(size.width, size.height) - WeekTabSizes.GoalProgressWidth.toPx()
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val arc = Size(diameter, diameter)
                drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(WeekTabSizes.GoalTrackWidth.toPx()))
                drawArc(accent, -90f, 360f * progress, false, topLeft, arc, style = Stroke(WeekTabSizes.GoalProgressWidth.toPx(), cap = StrokeCap.Round))
            }
            if (state.workoutsLeft > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText(text = state.workoutsLeft.toString(), style = Theme.typography.title)
                    AppText(text = stringResource(R.string.home_week_goal_left), style = Theme.typography.small, color = Theme.contentColors.secondary)
                }
            } else {
                AppText(text = stringResource(R.string.home_week_goal_achieved), style = Theme.typography.subtitle, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: Painter,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    StatSurface(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(WeekTabSizes.StatIcon),
                colorFilter = ColorFilter.tint(Theme.contentColors.secondary),
            )
            AppText(
                text = label,
                style = Theme.typography.body,
                color = Theme.contentColors.secondary,
                modifier = Modifier.padding(top = Theme.spacing.medium),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                AppText(text = value, style = Theme.typography.heading, maxLines = 1)
                unit?.let { AppText(text = it, style = Theme.typography.body, color = Theme.contentColors.secondary) }
            }
        }
    }
}

@Composable
private fun StatSurface(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AppSurface(
        modifier = modifier
            .size(width = WeekTabSizes.StatCardWidth, height = WeekTabSizes.StatCardHeight)
            .clip(Theme.shapes.surface)
            .clickable(onClick = onClick),
        content = content,
    )
}

// The section header and stat cards the week will show, at their sizes, running off the screen like the real row:
// the goal's ring, then each card's icon, label and value
@Composable
internal fun WeekStatsLoading(contentPadding: PaddingValues) {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.testTag(WeekTabTags.Loading), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .width(WeekTabSizes.LoadingHeaderWidth)
                .height(WeekTabSizes.LoadingHeaderHeight)
                .background(brush, DayShape),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState(), enabled = false).padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            LoadingCard {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(WeekTabSizes.GoalProgressWidth / 2)
                            .border(WeekTabSizes.GoalProgressWidth, brush, CircleShape),
                    )
                }
            }
            repeat(LoadingCards - 1) {
                LoadingCard {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.size(WeekTabSizes.StatIcon).background(brush, CircleShape))
                        LoadingLine(brush, fraction = 0.8f, modifier = Modifier.padding(top = Theme.spacing.medium))
                        LoadingLine(brush, fraction = 0.5f, modifier = Modifier.padding(top = Theme.spacing.small))
                        Spacer(modifier = Modifier.weight(1f))
                        LoadingLine(brush, fraction = 0.4f, height = WeekTabSizes.LoadingValueHeight)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(content: @Composable () -> Unit) {
    AppSurface(modifier = Modifier.size(width = WeekTabSizes.StatCardWidth, height = WeekTabSizes.StatCardHeight), content = content)
}

@Composable
private fun LoadingLine(brush: Brush, fraction: Float, modifier: Modifier = Modifier, height: Dp = WeekTabSizes.LoadingLineHeight) {
    Box(modifier = modifier.fillMaxWidth(fraction).height(height).background(brush, DayShape))
}

@Composable
internal fun EmptyWeekCard(today: LocalDate, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val onAccent = Theme.contentColors.onAction
    Surface(
        modifier = modifier.fillMaxWidth().testTag(WeekTabTags.Empty),
        color = Theme.contentColors.action,
        contentColor = onAccent,
        shape = Theme.shapes.surface,
        shadowElevation = WeekTabSizes.CardElevation,
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.large)) {
            AppText(text = today.formatted().uppercase(), style = Theme.typography.small, color = onAccent)
            AppText(
                text = stringResource(R.string.home_week_empty_title),
                style = Theme.typography.heading,
                color = onAccent,
                modifier = Modifier.padding(top = Theme.spacing.small),
            )
            AppText(
                text = stringResource(R.string.home_week_empty_subtitle),
                style = Theme.typography.body,
                color = onAccent,
                modifier = Modifier.padding(top = Theme.spacing.extraSmall),
            )
            Surface(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(top = Theme.spacing.large),
                color = Theme.backgrounds.surface,
                shape = Theme.shapes.button,
            ) {
                Row(
                    modifier = Modifier.heightIn(min = Theme.sizes.button).padding(horizontal = Theme.spacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayIcon()
                    AppText(
                        text = stringResource(DesignR.string.home_add_title),
                        style = Theme.typography.button,
                        modifier = Modifier.padding(start = Theme.spacing.small),
                    )
                }
            }
        }
    }
}

// An accent circle with a play triangle, like the XML tab's layer-list, which painterResource can't load
@Composable
private fun PlayIcon() {
    Box(
        modifier = Modifier.size(WeekTabSizes.StartIcon).background(Theme.contentColors.action, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_play_triangle),
            contentDescription = null,
            modifier = Modifier.size(WeekTabSizes.StartIcon / 2),
            colorFilter = ColorFilter.tint(Theme.contentColors.onAction),
        )
    }
}

private fun LocalDate.formatted(): String = DateTimeUtils.toDate(atStartOfDay(ZoneId.systemDefault()).toInstant()).dayOfWeekFormatted()

private val DayShape = RoundedCornerShape(WeekTabSizes.DayCorner)

private const val GoalAnimationMillis = 2000

// One more than fit on a phone, so the row reads as running on like the cards
private const val LoadingCards = 3
