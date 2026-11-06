package com.everlog.android.exercise.page

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBar
import com.everlog.android.common.compose.elements.AppChipTabs
import com.everlog.android.common.compose.elements.AppDialog
import com.everlog.android.common.compose.elements.AppDialogAction
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppOverflowMenu
import com.everlog.android.common.compose.elements.AppScreen
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.everlog.android.exercise.R
import com.everlog.android.exercise.details.info.ExerciseInfoContent
import com.everlog.android.exercise.details.info.ExerciseInfoViewModel
import com.everlog.android.statistics.ui.R as StatisticsR
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange

/**
 * Renders the view-model's state and forwards what's tapped to it. Other screens open through the lambdas.
 *
 * @param tab The tab the page opens on
 * @param onEdit Opens the create screen on the user's own exercise
 */
@Composable
internal fun ExercisePageScreen(
    viewModel: ExercisePageViewModel,
    tab: ExercisePageTab,
    onEdit: (ELExercise) -> Unit,
    onFeedback: () -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.closed) {
        if (state.closed) onClose()
    }
    BackHandler(onBack = onClose)
    ExercisePageContent(
        state = state,
        tab = tab,
        actions = ExercisePageActions(
            onBack = onClose,
            onEdit = { onEdit(state.exercise) },
            onDelete = viewModel::onDeleteClicked,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteCancelled = viewModel::onDeleteCancelled,
            onDeleteFailedShown = viewModel::onDeleteFailedShown,
            onTabShown = viewModel::onTabShown,
            onRangePicked = viewModel::onRangePicked,
            onHistoryEnd = viewModel::onHistoryEnd,
            onTryAgain = viewModel::onTryAgain,
            onFeedback = onFeedback,
        ),
    )
}

internal class ExercisePageActions(
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onDeleteConfirmed: () -> Unit,
    val onDeleteCancelled: () -> Unit,
    val onDeleteFailedShown: () -> Unit,
    val onTabShown: (ExercisePageTab) -> Unit,
    val onRangePicked: (ExerciseStatsRange) -> Unit,
    val onHistoryEnd: () -> Unit,
    val onTryAgain: () -> Unit,
    val onFeedback: () -> Unit,
)

// The exercise's name and category in the bar, chip tabs under it, then the tabs' pages side by side, swiped or picked
@Composable
internal fun ExercisePageContent(state: ExercisePageState, tab: ExercisePageTab, actions: ExercisePageActions) {
    val pager = rememberPagerState(initialPage = tab.ordinal) { ExercisePageTab.entries.size }
    LaunchedEffect(pager.currentPage) {
        actions.onTabShown(ExercisePageTab.entries[pager.currentPage])
    }
    val snackbar = remember { SnackbarHostState() }
    Snackbars(state, snackbar, actions)
    AppScreen(
        topBar = {
            AppBar(
                title = state.exercise.name.orEmpty(),
                subtitle = categoryTitle(state.exercise.category),
                showBack = true,
                actions = if (state.own) {
                    {
                        AppOverflowMenu(
                            modifier = Modifier.testTag(ExercisePageTags.Menu),
                            items = listOf(
                                AppMenuItem(text = stringResource(R.string.exercise_page_edit), onClick = actions.onEdit),
                                AppMenuItem(text = stringResource(DesignR.string.delete), onClick = actions.onDelete),
                            ),
                        )
                    }
                } else {
                    null
                },
                // Stays put over the pages, as the bar has no header to scroll away
                header = {
                    AppChipTabs(
                        tabs = ExercisePageTab.entries.map { stringResource(it.title) },
                        pager = pager,
                        tabModifier = { Modifier.testTag(ExercisePageTags.tab(ExercisePageTab.entries[it])) },
                    )
                },
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().testTag(ExercisePageTags.Pager)) { page ->
                when (ExercisePageTab.entries[page]) {
                    ExercisePageTab.Info -> ExerciseInfoContent(
                        state = ExerciseInfoViewModel.State(
                            exercise = ExerciseInfoViewModel.Exercise(
                                uuid = state.exercise.uuid.orEmpty(),
                                name = state.exercise.name.orEmpty(),
                                imageUrl = state.exercise.imageUrl,
                                own = state.own,
                            ),
                            guide = state.guide,
                        ),
                    )
                    ExercisePageTab.Statistics -> ExercisePageStatistics(state, actions)
                    ExercisePageTab.History -> ExercisePageHistory(state, actions)
                }
            }
            AppSnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().testTag(ExercisePageTags.Snackbar),
            )
        }
    }
    if (state.deletePrompt) {
        AppDialog(
            title = stringResource(DesignR.string.delete_title),
            body = stringResource(R.string.create_exercise_delete_prompt),
            onDismissRequest = actions.onDeleteCancelled,
            primaryAction = AppDialogAction(text = stringResource(DesignR.string.delete), onClick = actions.onDeleteConfirmed),
            secondaryAction = AppDialogAction(text = stringResource(DesignR.string.cancel), onClick = actions.onDeleteCancelled),
        )
    }
}

// The last 12 months that couldn't load, with Try again, and a delete that failed
@Composable
private fun Snackbars(state: ExercisePageState, snackbar: SnackbarHostState, actions: ExercisePageActions) {
    val failedMessage = stringResource(StatisticsR.string.statistics_year_failed)
    val tryAgain = stringResource(StatisticsR.string.statistics_try_again)
    LaunchedEffect(state.failed) {
        if (state.failed) {
            val result = snackbar.showSnackbar(failedMessage, actionLabel = tryAgain, duration = SnackbarDuration.Indefinite)
            if (result == SnackbarResult.ActionPerformed) actions.onTryAgain()
        } else {
            snackbar.currentSnackbarData?.dismiss()
        }
    }
    val deleteFailed = stringResource(R.string.exercise_page_delete_failed)
    LaunchedEffect(state.deleteFailed) {
        if (state.deleteFailed) {
            actions.onDeleteFailedShown()
            snackbar.showSnackbar(deleteFailed)
        }
    }
}

private val ExercisePageTab.title: Int
    @StringRes get() = when (this) {
        ExercisePageTab.Info -> R.string.exercise_info_title
        ExercisePageTab.Statistics -> DesignR.string.statistics_title
        ExercisePageTab.History -> DesignR.string.history_title
    }

// The category's name, e.g. Chest
internal fun categoryTitle(category: String?): String? =
    category?.let { ArrayResourceTypeUtils.withExerciseCategories().getTitle(it, it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
