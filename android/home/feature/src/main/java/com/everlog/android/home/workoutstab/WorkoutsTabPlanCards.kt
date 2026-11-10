package com.everlog.android.home.workoutstab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppImage
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.home.R

// The plan being followed as a cover card, the others as rows with a small cover, or a line saying there are none
@Composable
internal fun WorkoutsTabPlans(
    active: WorkoutsTabActivePlan?,
    plans: List<WorkoutsTabPlan>,
    onPlanClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    active?.let { ActivePlanCard(it, onClick = { onPlanClick(it.plan.uuid) }, modifier = modifier) }
    if (plans.isEmpty() && active == null) {
        AppListGroup(modifier = modifier) {
            row {
                AppListItem(
                    title = stringResource(R.string.workouts_tab_no_plans),
                    subtitle = stringResource(R.string.workouts_tab_no_plans_detail),
                )
            }
        }
    }
    if (plans.isNotEmpty()) {
        AppListGroup(modifier = modifier) {
            plans.forEach { plan ->
                row {
                    AppListItem(
                        modifier = Modifier.testTag(WorkoutsTabTags.plan(plan.uuid)),
                        title = plan.name,
                        subtitle = planLength(plan),
                        leading = { AppThumbnail(imageUrl = plan.imageUrl, size = Theme.sizes.leading) },
                        onClick = { onPlanClick(plan.uuid) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePlanCard(active: WorkoutsTabActivePlan, onClick: () -> Unit, modifier: Modifier) {
    AppSurface(
        modifier = modifier.fillMaxWidth().clip(Theme.shapes.surface).clickable(onClick = onClick).testTag(WorkoutsTabTags.ActivePlan),
        elevation = 0.dp,
        contentPadding = PaddingValues(),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(WorkoutsTabSizes.PlanCoverHeight)) {
            active.plan.imageUrl?.let {
                AppImage(modifier = Modifier.fillMaxSize(), imageUrl = it, contentScale = ContentScale.Crop)
            }
            // Darkens the bottom of the photo so the name reads on it
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Theme.backgrounds.surface))),
            )
            AppText(
                modifier = Modifier
                    .padding(Theme.spacing.medium)
                    .clip(Theme.shapes.button)
                    .background(Theme.contentColors.action)
                    .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
                text = stringResource(R.string.workouts_tab_plan_active),
                style = Theme.typography.small,
                color = Theme.contentColors.onAction,
            )
            AppText(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.medium),
                text = active.plan.name,
                style = Theme.typography.heading,
            )
        }
        Column(
            modifier = Modifier.padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                AppText(
                    modifier = Modifier.weight(1f),
                    text = listOf(
                        stringResource(R.string.workouts_tab_plan_week, active.weekNumber, active.plan.weeks),
                        stringResource(R.string.workouts_tab_plan_this_week, active.doneThisWeek, active.workoutsThisWeek),
                    ).joinToString(stringResource(R.string.workouts_tab_separator)),
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
                active.next?.let {
                    AppText(text = stringResource(R.string.workouts_tab_plan_next_up, it), style = Theme.typography.caption, color = Theme.contentColors.secondary)
                }
            }
            LinearProgressIndicator(
                progress = { active.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(WorkoutsTabSizes.PlanProgressHeight),
                color = Theme.contentColors.action,
                trackColor = Theme.backgrounds.primary,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun planLength(plan: WorkoutsTabPlan): String = listOf(
    pluralStringResource(R.plurals.workouts_tab_plan_weeks, plan.weeks, plan.weeks),
    pluralStringResource(R.plurals.workouts_tab_plan_days, plan.daysPerWeek, plan.daysPerWeek),
).joinToString(stringResource(R.string.workouts_tab_separator))
