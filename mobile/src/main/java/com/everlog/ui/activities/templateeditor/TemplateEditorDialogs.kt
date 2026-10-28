package com.everlog.ui.activities.templateeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.everlog.R
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.model.set.ELSetType
import com.everlog.ui.components.rest.RestStepper
import com.everlog.ui.design.elements.AppBottomSheet
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppTextField
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

@Composable
internal fun TemplateEditorDialogs(state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    when (val dialog = state.dialog) {
        null -> Unit
        TemplateEditorDialog.Name -> {
            var name by rememberSaveable { mutableStateOf(state.name) }
            val save = { viewModel.onNameSaved(name) }
            AppDialog(
                title = stringResource(R.string.template_editor_name),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(R.string.save), save),
                secondaryAction = AppDialogAction(stringResource(R.string.cancel), viewModel::onDialogDismissed),
            ) {
                AppTextField(
                    modifier = Modifier.testTag(TemplateEditorTags.NameField),
                    text = name,
                    onTextChange = { name = it },
                    autoFocus = true,
                    selectAllOnFocus = true,
                    onDone = save,
                )
            }
        }
        TemplateEditorDialog.Rest -> AppDialog(
            title = stringResource(R.string.template_editor_rest),
            body = stringResource(R.string.template_editor_rest_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(R.string.workout_log_done), viewModel::onDialogDismissed),
        ) {
            RestStepper(seconds = state.restSeconds, onStep = viewModel::onRestStep)
        }
        is TemplateEditorDialog.DeleteExercise -> {
            val name = state.blocks.block(dialog.blockId)?.exercises?.joinToString(" + ") { it.exercise?.name.orEmpty() }.orEmpty()
            AppDialog(
                title = stringResource(R.string.workout_log_delete_exercise_title, name),
                body = stringResource(R.string.workout_log_delete_exercise_body),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppDialogAction(stringResource(R.string.workout_log_delete)) { viewModel.onDeleteExerciseConfirmed(dialog.blockId) },
                secondaryAction = AppDialogAction(stringResource(R.string.cancel), viewModel::onDialogDismissed),
            )
        }
        TemplateEditorDialog.SetType -> SetTypeSheet(state.picked.size, viewModel)
        is TemplateEditorDialog.DeletePicked -> AppDialog(
            title = pluralStringResource(R.plurals.template_editor_delete_picked_title, dialog.count, dialog.count),
            body = pluralStringResource(R.plurals.template_editor_delete_picked_body, dialog.count),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(R.string.workout_log_delete), viewModel::onDeletePickedConfirmed),
            secondaryAction = AppDialogAction(stringResource(R.string.cancel), viewModel::onDialogDismissed),
        )
        TemplateEditorDialog.Leave -> AppDialog(
            title = stringResource(R.string.template_editor_leave_title),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(R.string.save), viewModel::onSave),
            secondaryAction = AppDialogAction(stringResource(R.string.discard), viewModel::onDiscard),
            tertiaryAction = AppDialogAction(stringResource(R.string.template_editor_keep_editing), viewModel::onDialogDismissed),
        )
    }
}

// Every set type, the ones for this number of exercises first; the others show but can't be picked
@Composable
private fun SetTypeSheet(count: Int, viewModel: TemplateEditorViewModel) {
    val context = LocalContext.current
    val (fits, others) = ELSetType.entries.partition { it.canBeSelected(count) }
    AppBottomSheet(
        modifier = Modifier.testTag(TemplateEditorTags.SetTypeSheet),
        onDismissRequest = viewModel::onDialogDismissed,
        title = pluralStringResource(R.plurals.template_editor_set_type_title, count, count),
    ) {
        val sheet = this
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
            AppListGroup {
                fits.forEach { type ->
                    row {
                        AppListItem(
                            title = type.getTitle(context).orEmpty(),
                            subtitle = type.getDescription(context),
                            onClick = { sheet.dismiss { viewModel.onSetTypePicked(type) } },
                        )
                    }
                }
            }
            if (others.isNotEmpty()) {
                AppListGroup(header = stringResource(R.string.template_editor_set_type_other)) {
                    others.forEach { type ->
                        row {
                            AppListItem(
                                title = type.getTitle(context).orEmpty(),
                                titleColor = Theme.contentColors.secondary,
                                subtitle = type.getDescription(context),
                            )
                        }
                    }
                }
            }
        }
    }
}

