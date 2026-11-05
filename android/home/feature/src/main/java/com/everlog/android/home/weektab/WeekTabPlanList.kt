package com.everlog.android.home.weektab

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.home.R
import com.everlog.android.plan.ui.R as PlanUiR

// The plan the user is following: its progress, then the current week's days, the next one with Skip and Start
@Composable
internal fun WeekTabPlanList(
    plan: WeekTabPlan,
    onPlanClick: () -> Unit,
    onDayClick: (WeekTabPlanDay) -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        PlanProgressCard(plan = plan, onClick = onPlanClick)
        AppText(
            text = stringResource(R.string.home_week_plan_week, plan.weekNumber, plan.weekCount),
            style = Theme.typography.subtitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        plan.days.forEach { day ->
            PlanDayRow(day = day, onClick = { onDayClick(day) }, onSkip = onSkip, onStart = onStart)
        }
        if (plan.done) {
            AppButton(
                onClick = onStart,
                text = stringResource(R.string.home_week_plan_finish),
                modifier = Modifier.fillMaxWidth().testTag(WeekTabTags.FinishPlan),
            )
        }
    }
}

@Composable
private fun PlanProgressCard(plan: WeekTabPlan, onClick: () -> Unit) {
    AppSurface(modifier = Modifier.fillMaxWidth().clip(Theme.shapes.surface).clickable(onClick = onClick).testTag(WeekTabTags.Plan)) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = plan.name, style = Theme.typography.subtitle, modifier = Modifier.weight(1f))
                AppText(text = stringResource(R.string.home_week_plan_progress, plan.progress), style = Theme.typography.body)
            }
            LinearProgressIndicator(
                progress = { plan.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(WeekTabSizes.PlanProgressHeight),
                color = Theme.contentColors.action,
                trackColor = Theme.backgrounds.primary,
                strokeCap = StrokeCap.Round,
                gapSize = WeekTabSizes.None,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun PlanDayRow(day: WeekTabPlanDay, onClick: () -> Unit, onSkip: () -> Unit, onStart: () -> Unit) {
    val label = stringResource(R.string.home_week_plan_day, day.number)
    val routine = day.routine?.name ?: stringResource(DesignR.string.plan_details_rest_title)
    Row(modifier = Modifier.testTag(WeekTabTags.planDay(day.number)), horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        DayMarker(complete = day.complete, modifier = if (day.complete) Modifier.align(Alignment.CenterVertically) else Modifier.padding(top = WeekTabSizes.MarkerTop))
        if (day.complete) {
            // A done day has no card, just a line before its text
            Row(
                modifier = Modifier.weight(1f).clip(Theme.shapes.surface).clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
            ) {
                Box(modifier = Modifier.width(WeekTabSizes.DoneLineWidth).height(WeekTabSizes.DoneLineHeight).background(WeekTabColors.line))
                DayText(label = label, routine = routine)
            }
        } else {
            AppSurface(modifier = Modifier.weight(1f).clip(Theme.shapes.surface).clickable(onClick = onClick)) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    DayText(label = label, routine = routine)
                    if (day.next) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                            AppSecondaryButton(
                                onClick = onSkip,
                                text = stringResource(DesignR.string.home_week_plan_skip),
                                modifier = Modifier.weight(1f).testTag(WeekTabTags.SkipDay),
                            )
                            AppButton(
                                onClick = onStart,
                                text = stringResource(if (day.routine == null) DesignR.string.complete else PlanUiR.string.home_week_plan_start),
                                modifier = Modifier.weight(1f).testTag(WeekTabTags.StartDay),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayText(label: String, routine: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
        AppText(text = label, style = Theme.typography.heading)
        AppText(text = routine, style = Theme.typography.body, color = Theme.contentColors.secondary)
    }
}

// A filled accent circle with a tick for a done day, an accent ring for the others
@Composable
private fun DayMarker(complete: Boolean, modifier: Modifier = Modifier) {
    val accent = Theme.contentColors.action
    Box(
        modifier = modifier
            .size(WeekTabSizes.Marker)
            .then(if (complete) Modifier.background(accent, CircleShape) else Modifier.border(WeekTabSizes.MarkerBorder, accent, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (complete) {
            Image(
                painter = painterResource(DesignR.drawable.ic_check_green),
                contentDescription = null,
                modifier = Modifier.size(WeekTabSizes.MarkerTick),
                colorFilter = ColorFilter.tint(Theme.contentColors.onAction),
            )
        }
    }
}
