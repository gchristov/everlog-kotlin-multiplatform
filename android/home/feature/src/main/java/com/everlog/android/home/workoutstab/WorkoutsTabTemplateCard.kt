package com.everlog.android.home.workoutstab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppExpandableCard
import com.everlog.android.common.compose.elements.AppExpandableCardGroup
import com.everlog.android.common.compose.elements.AppExpandableCardRow
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.format.setTypeTitle
import com.everlog.android.home.R
import org.threeten.bp.LocalDate

// A template: its name over when it was last done and its first exercises; open, every exercise, Details and Start
@Composable
internal fun WorkoutsTabTemplateCard(
    template: WorkoutsTabTemplate,
    lastDone: Map<String, Long>?,
    today: LocalDate,
    open: Boolean,
    onToggle: () -> Unit,
    onDetails: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts = listOfNotNull(
        lastDone?.let { doneAgoText(doneAgo(it[template.uuid], today)) },
        stringResource(R.string.workouts_tab_in_plan).takeIf { template.inPlan },
    )
    AppExpandableCard(
        modifier = modifier.testTag(WorkoutsTabTags.template(template.uuid)),
        title = template.routine.name.orEmpty(),
        subtitle = parts.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.workouts_tab_separator)),
        groups = exerciseGroups(template.routine),
        open = open,
        onToggle = onToggle,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                AppSecondaryButton(
                    modifier = Modifier.weight(1f).testTag(WorkoutsTabTags.templateDetails(template.uuid)),
                    onClick = onDetails,
                    text = stringResource(R.string.workouts_tab_details),
                )
                AppButton(
                    modifier = Modifier.weight(1f).testTag(WorkoutsTabTags.templateStart(template.uuid)),
                    onClick = onStart,
                    text = stringResource(R.string.workouts_tab_start),
                )
            }
        },
    )
}

// A superset or circuit is one group under its type; a single exercise is a group of its own
@Composable
internal fun exerciseGroups(routine: ELRoutine): List<AppExpandableCardGroup> {
    val context = LocalContext.current
    return routine.exerciseGroups.map { group ->
        AppExpandableCardGroup(
            label = group.type?.takeIf { group.exercises.size > 1 }?.let { setTypeTitle(context, it) },
            rows = group.exercises.map { exercise ->
                AppExpandableCardRow(
                    title = exercise.exercise?.name.orEmpty(),
                    imageUrl = exercise.exercise?.imageUrl,
                    trailing = setsTargetText(setsTarget(exercise.sets)),
                )
            },
        )
    }
}

@Composable
internal fun doneAgoText(doneAgo: DoneAgo): String = when (doneAgo) {
    DoneAgo.Today -> stringResource(R.string.workouts_tab_done_today)
    DoneAgo.Yesterday -> stringResource(R.string.workouts_tab_done_yesterday)
    is DoneAgo.Days -> pluralStringResource(R.plurals.workouts_tab_done_days, doneAgo.days, doneAgo.days)
    is DoneAgo.Weeks -> pluralStringResource(R.plurals.workouts_tab_done_weeks, doneAgo.weeks, doneAgo.weeks)
    DoneAgo.NotRecently -> stringResource(R.string.workouts_tab_not_done)
}

@Composable
private fun setsTargetText(target: SetsTarget): String = when (target) {
    is SetsTarget.Reps -> if (target.min == target.max) {
        stringResource(R.string.workouts_tab_reps, target.sets, target.min)
    } else {
        stringResource(R.string.workouts_tab_reps_range, target.sets, target.min, target.max)
    }
    is SetsTarget.Time -> if (target.minSeconds == target.maxSeconds) {
        stringResource(R.string.workouts_tab_time, target.sets, target.minSeconds)
    } else {
        stringResource(R.string.workouts_tab_time_range, target.sets, target.minSeconds, target.maxSeconds)
    }
    is SetsTarget.Sets -> pluralStringResource(R.plurals.workouts_tab_sets, target.sets, target.sets)
}
