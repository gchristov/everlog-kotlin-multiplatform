package com.everlog.android.template.editor

import com.everlog.android.common.design.R as DesignR
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppConfirmSheet
import com.everlog.android.common.compose.elements.AppSheetAction
import com.everlog.android.common.compose.elements.AppTextField
import com.everlog.android.template.R
import com.everlog.android.common.compose.components.rest.RestWheel
import com.everlog.android.workoutbuilder.data.WorkoutBuilderDefaults

// The template editor's prompts, each a bottom sheet
@Composable
internal fun TemplateEditorDialogs(state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    when (val dialog = state.dialog) {
        null -> Unit
        TemplateEditorDialog.Name -> {
            var name by rememberSaveable { mutableStateOf(state.name) }
            val save = { viewModel.onNameSaved(name) }
            AppConfirmSheet(
                title = stringResource(R.string.template_editor_name),
                onDismissRequest = viewModel::onDialogDismissed,
                primaryAction = AppSheetAction(stringResource(DesignR.string.save), save),
                secondaryAction = AppSheetAction(stringResource(DesignR.string.cancel), viewModel::onDialogDismissed),
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
        TemplateEditorDialog.Rest -> AppConfirmSheet(
            title = stringResource(R.string.template_editor_rest),
            body = stringResource(R.string.template_editor_rest_body),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppSheetAction(stringResource(DesignR.string.workout_log_done), viewModel::onDialogDismissed),
        ) {
            RestWheel(seconds = state.restSeconds, stepSeconds = WorkoutBuilderDefaults.TIME_STEP_SECONDS, onPick = viewModel::onRestPicked)
        }
        TemplateEditorDialog.Leave -> AppConfirmSheet(
            title = stringResource(R.string.template_editor_leave_title),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppSheetAction(stringResource(DesignR.string.save), viewModel::onSave),
            secondaryAction = AppSheetAction(stringResource(DesignR.string.discard), viewModel::onDiscard),
            tertiaryAction = AppSheetAction(stringResource(R.string.template_editor_keep_editing), viewModel::onDialogDismissed),
        )
    }
}
