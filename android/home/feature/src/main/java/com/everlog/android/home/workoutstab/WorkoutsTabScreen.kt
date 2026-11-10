package com.everlog.android.home.workoutstab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBarHeader
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppSectionHeader
import com.everlog.android.common.compose.elements.AppTertiaryButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.rememberShimmerBrush
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.home.R
import com.everlog.android.home.data.workouts.WorkoutsTabCard

// What the Workouts tab opens, which the fragment wires to the navigator
class WorkoutsTabActions(
    val showExercises: () -> Unit,
    val showTemplate: (ELRoutine) -> Unit,
    val start: (WorkoutsTabStart) -> Unit,
    val startEmpty: () -> Unit,
    val newTemplate: () -> Unit,
    val showPlan: (uuid: String) -> Unit,
    val newPlan: () -> Unit,
)

@Composable
internal fun WorkoutsTabScreen(viewModel: WorkoutsTabViewModel, actions: WorkoutsTabActions) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WorkoutsTabContent(
        state = state,
        actions = actions,
        onToggle = viewModel::onTemplateToggled,
        onCardStart = { viewModel.onCardStart()?.let(actions.start) },
        onTemplateStart = { actions.start(viewModel.onTemplateStart(it)) },
        onRetry = viewModel::onRetry,
    )
}

@Composable
internal fun WorkoutsTabContent(
    state: WorkoutsTabState,
    actions: WorkoutsTabActions,
    onToggle: (String) -> Unit,
    onCardStart: () -> Unit,
    onTemplateStart: (ELRoutine) -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AppBarHeader(title = stringResource(R.string.workouts_tab_title))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = WorkoutsTabSizes.BottomPadding),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            val margin = Modifier.padding(horizontal = Theme.spacing.large)
            ExercisesRow(onClick = actions.showExercises, modifier = margin)
            when {
                state.loading -> Loading(margin)
                state.unavailable -> Unavailable(onRetry = onRetry, modifier = margin)
                else -> Lists(state, actions, onToggle, onCardStart, onTemplateStart, margin)
            }
        }
    }
}

@Composable
private fun Lists(
    state: WorkoutsTabState,
    actions: WorkoutsTabActions,
    onToggle: (String) -> Unit,
    onCardStart: () -> Unit,
    onTemplateStart: (ELRoutine) -> Unit,
    margin: Modifier,
) {
    WorkoutsTabTopCard(
        card = state.card,
        onStart = onCardStart,
        onDetails = actions.showTemplate,
        onNewTemplate = actions.newTemplate,
        onStartEmpty = actions.startEmpty,
        modifier = margin,
    )
    if (state.card == WorkoutsTabCard.GetStarted) {
        WorkoutsTabExample(today = state.today, modifier = margin)
        return
    }
    Section(
        header = { SectionHeader(stringResource(R.string.workouts_tab_templates), actions.newTemplate, WorkoutsTabTags.NewTemplate, margin) },
    ) {
        state.templates.forEach { template ->
            WorkoutsTabTemplateCard(
                template = template,
                lastDone = state.lastDone,
                today = state.today,
                open = template.uuid in state.openTemplates,
                onToggle = { onToggle(template.uuid) },
                onDetails = { actions.showTemplate(template.routine) },
                onStart = { onTemplateStart(template.routine) },
                modifier = margin,
            )
        }
    }
    Section(
        header = { SectionHeader(stringResource(R.string.workouts_tab_plans), actions.newPlan, WorkoutsTabTags.NewPlan, margin) },
    ) {
        WorkoutsTabPlans(active = state.activePlan, plans = state.plans, onPlanClick = actions.showPlan, modifier = margin)
    }
}

// A header close over its cards, which keep the screen's gap between them
@Composable
private fun Section(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
        header()
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) { content() }
    }
}

@Composable
private fun ExercisesRow(onClick: () -> Unit, modifier: Modifier) {
    AppListGroup(modifier = modifier.testTag(WorkoutsTabTags.Exercises)) {
        row {
            AppListItem(
                title = stringResource(R.string.workouts_tab_exercises),
                trailing = {
                    AppText(
                        text = stringResource(R.string.workouts_tab_exercises_detail),
                        style = Theme.typography.caption,
                        color = Theme.contentColors.secondary,
                    )
                },
                onClick = onClick,
            )
        }
    }
}

// A section's header with a New button at its end, until Home gets a floating button for creating
@Composable
private fun SectionHeader(text: String, onNew: () -> Unit, newTag: String, modifier: Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppSectionHeader(text = text, modifier = Modifier.weight(1f))
        AppTertiaryButton(modifier = Modifier.testTag(newTag), onClick = onNew, text = stringResource(R.string.workouts_tab_new))
    }
}

@Composable
private fun Loading(margin: Modifier) {
    val brush = rememberShimmerBrush()
    repeat(WorkoutsTabSizes.LoadingCards) {
        Box(
            modifier = margin
                .fillMaxWidth()
                .height(WorkoutsTabSizes.LoadingCardHeight)
                .clip(Theme.shapes.surface)
                .background(brush),
        )
    }
}

@Composable
private fun Unavailable(onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = Theme.spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppText(text = stringResource(R.string.workouts_tab_unavailable), style = Theme.typography.subtitle, textAlign = TextAlign.Center)
        AppText(
            text = stringResource(R.string.workouts_tab_unavailable_detail),
            color = Theme.contentColors.secondary,
            textAlign = TextAlign.Center,
        )
        AppSecondaryButton(
            modifier = Modifier.padding(top = Theme.spacing.medium).testTag(WorkoutsTabTags.Retry),
            onClick = onRetry,
            text = stringResource(R.string.workouts_tab_retry),
        )
    }
}

internal object WorkoutsTabSizes {
    // Room for Home's start workout button over the end of the list
    val BottomPadding = 120.dp
    const val LoadingCards = 4
    val LoadingCardHeight = 72.dp
    val PlanCoverHeight = 150.dp
    val PlanProgressHeight = 6.dp
}
