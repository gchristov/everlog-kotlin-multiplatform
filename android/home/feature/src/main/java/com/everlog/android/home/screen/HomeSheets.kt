package com.everlog.android.home.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.everlog.android.common.compose.elements.AppBottomSheet
import com.everlog.android.common.compose.elements.AppBottomSheetScope
import com.everlog.android.common.compose.elements.AppConfirmSheet
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppSheetAction
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.home.R
import com.everlog.android.common.design.R as DesignR

// What the start and new menus and the prompt open
class HomeSheetActions(
    val startFromTemplate: () -> Unit,
    val startEmpty: () -> Unit,
    val newTemplate: () -> Unit,
    val newPlan: () -> Unit,
    val resume: (ELWorkout) -> Unit,
)

@Composable
internal fun HomeSheets(state: HomeState, viewModel: HomeViewModel, actions: HomeSheetActions) {
    when (state.sheet) {
        HomeSheet.Start -> AppBottomSheet(onDismissRequest = viewModel::onSheetClosed, title = stringResource(DesignR.string.home_add_title)) {
            Menu(
                Option(stringResource(R.string.home_start_from_template), null, DesignR.drawable.ic_routines, HomeTags.StartFromTemplate, actions.startFromTemplate),
                Option(stringResource(R.string.home_start_empty), null, DesignR.drawable.ic_barbell, HomeTags.StartEmpty, actions.startEmpty),
            )
        }
        HomeSheet.New -> AppBottomSheet(onDismissRequest = viewModel::onSheetClosed, title = stringResource(R.string.home_new_title)) {
            Menu(
                Option(
                    stringResource(R.string.home_new_template),
                    stringResource(R.string.home_new_template_detail),
                    DesignR.drawable.ic_routines,
                    HomeTags.NewTemplate,
                    actions.newTemplate,
                ),
                Option(stringResource(R.string.home_new_plan), stringResource(R.string.home_new_plan_detail), DesignR.drawable.ic_calendar, HomeTags.NewPlan, actions.newPlan),
            )
        }
        null -> Unit
    }
    when (val prompt = state.prompt) {
        is HomePrompt.OngoingWorkout -> {
            val sets = prompt.workout.getCompletedSetsCount()
            AppConfirmSheet(
                title = stringResource(R.string.home_ongoing_title),
                body = if (sets > 0) {
                    pluralStringResource(R.plurals.home_ongoing_body_sets, sets, sets)
                } else {
                    stringResource(R.string.home_ongoing_body)
                },
                onDismissRequest = viewModel::onPromptDismissed,
                primaryAction = AppSheetAction(stringResource(R.string.home_ongoing_resume), { viewModel.onResumeWorkout()?.let(actions.resume) }, HomeTags.Resume),
                secondaryAction = AppSheetAction(stringResource(DesignR.string.discard), viewModel::onDiscardWorkout, HomeTags.Discard),
            )
        }
        null -> Unit
    }
}

private class Option(val title: String, val detail: String?, @param:DrawableRes val icon: Int, val tag: String, val onClick: () -> Unit)

// The options as rows in a group; picking one closes the sheet, then opens it
@Composable
private fun AppBottomSheetScope.Menu(vararg options: Option) {
    val sheet = this
    AppListGroup(modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            row {
                AppListItem(
                    modifier = Modifier.testTag(option.tag),
                    title = option.title,
                    subtitle = option.detail,
                    leading = { AppIcon(imageVector = ImageVector.vectorResource(option.icon), tint = Theme.contentColors.action) },
                    onClick = { sheet.dismiss(then = option.onClick) },
                )
            }
        }
    }
}
