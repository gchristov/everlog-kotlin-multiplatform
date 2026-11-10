package com.everlog.android.home.workoutstab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppExpandableCard
import com.everlog.android.common.compose.elements.AppExpandableCardGroup
import com.everlog.android.common.compose.elements.AppExpandableCardRow
import com.everlog.android.common.compose.elements.AppHighlightAction
import com.everlog.android.common.compose.elements.AppHighlightCard
import com.everlog.android.common.compose.elements.AppThumbnailStack
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.home.R
import com.everlog.android.home.data.workouts.WorkoutsTabCard
import org.threeten.bp.LocalDate

// The card on top, which grows in when it's worked out (the history usually comes from the phone straight away)
@Composable
internal fun WorkoutsTabTopCard(
    card: WorkoutsTabCard?,
    onStart: () -> Unit,
    onDetails: (ELRoutine) -> Unit,
    onNewTemplate: () -> Unit,
    onStartEmpty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The last card, so it still shows while animating out
    var shown by remember { mutableStateOf(card) }
    if (card != null) shown = card
    AnimatedVisibility(visible = card != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        shown?.let { TopCard(it, onStart, onDetails, onNewTemplate, onStartEmpty, modifier.testTag(WorkoutsTabTags.Card)) }
    }
}

@Composable
private fun TopCard(
    card: WorkoutsTabCard,
    onStart: () -> Unit,
    onDetails: (ELRoutine) -> Unit,
    onNewTemplate: () -> Unit,
    onStartEmpty: () -> Unit,
    modifier: Modifier,
) {
    val start = AppHighlightAction(stringResource(R.string.workouts_tab_start), onStart)
    when (card) {
        is WorkoutsTabCard.PlanNext -> AppHighlightCard(
            modifier = modifier,
            label = stringResource(R.string.workouts_tab_plan_next),
            title = card.routine.name.orEmpty(),
            text = stringResource(R.string.workouts_tab_plan_next_detail, card.plan.name.orEmpty(), card.weekNumber, card.dayNumber),
            trailing = { Peek(card.routine) },
            primary = start,
            secondary = AppHighlightAction(stringResource(R.string.workouts_tab_details)) { onDetails(card.routine) },
        )
        is WorkoutsTabCard.Suggested -> AppHighlightCard(
            modifier = modifier,
            label = stringResource(R.string.workouts_tab_suggested),
            suggested = true,
            title = card.template.name.orEmpty(),
            text = stringResource(R.string.workouts_tab_suggested_detail, card.afterName, card.times, card.of),
            trailing = { Peek(card.template) },
            primary = start,
            secondary = AppHighlightAction(stringResource(R.string.workouts_tab_details)) { onDetails(card.template) },
        )
        is WorkoutsTabCard.StartHere -> AppHighlightCard(
            modifier = modifier,
            label = stringResource(R.string.workouts_tab_start_here),
            title = card.template.name.orEmpty(),
            text = stringResource(R.string.workouts_tab_start_here_detail),
            trailing = { Peek(card.template) },
            primary = start,
            secondary = AppHighlightAction(stringResource(R.string.workouts_tab_details)) { onDetails(card.template) },
        )
        WorkoutsTabCard.GetStarted -> AppHighlightCard(
            modifier = modifier,
            label = stringResource(R.string.workouts_tab_get_started),
            title = stringResource(R.string.workouts_tab_get_started_title),
            text = stringResource(R.string.workouts_tab_get_started_detail),
            primary = AppHighlightAction(stringResource(R.string.workouts_tab_new_template), onNewTemplate),
            secondary = AppHighlightAction(stringResource(R.string.workouts_tab_start_empty), onStartEmpty),
        )
    }
}

@Composable
private fun Peek(routine: ELRoutine) {
    AppThumbnailStack(imageUrls = routine.exerciseGroups.flatMap { it.exercises }.take(PeekCount).map { it.exercise?.imageUrl })
}

// Without templates: two made-up ones, faded, to show what saved templates look like
@Composable
internal fun WorkoutsTabExample(today: LocalDate, modifier: Modifier = Modifier) {
    val placeholders = AppExpandableCardGroup(List(PeekCount) { AppExpandableCardRow(title = "", imageUrl = null, trailing = "") })
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
        AppExampleLabel()
        AppExample {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                listOf(R.string.workouts_tab_example_push to ExampleDaysAgo, R.string.workouts_tab_example_pull to ExampleDaysAgo * 2)
                    .forEach { (name, daysAgo) ->
                        AppExpandableCard(
                            title = stringResource(name),
                            subtitle = doneAgoText(DoneAgo.Days(daysAgo)),
                            groups = listOf(placeholders),
                            open = false,
                            onToggle = {},
                        )
                    }
            }
        }
    }
}

private const val PeekCount = 3
private const val ExampleDaysAgo = 2
