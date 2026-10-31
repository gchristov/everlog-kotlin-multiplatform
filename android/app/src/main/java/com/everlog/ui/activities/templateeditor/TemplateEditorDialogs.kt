package com.everlog.ui.activities.templateeditor

import com.everlog.android.common.design.R as DesignR
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.R
import com.everlog.android.common.compose.elements.AppDialog
import com.everlog.android.common.compose.elements.AppDialogAction
import com.everlog.android.common.compose.elements.AppTextField
import com.everlog.ui.components.rest.RestStepper

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
                primaryAction = AppDialogAction(stringResource(DesignR.string.save), save),
                secondaryAction = AppDialogAction(stringResource(DesignR.string.cancel), viewModel::onDialogDismissed),
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
        TemplateEditorDialog.Leave -> AppDialog(
            title = stringResource(R.string.template_editor_leave_title),
            onDismissRequest = viewModel::onDialogDismissed,
            primaryAction = AppDialogAction(stringResource(DesignR.string.save), viewModel::onSave),
            secondaryAction = AppDialogAction(stringResource(DesignR.string.discard), viewModel::onDiscard),
            tertiaryAction = AppDialogAction(stringResource(R.string.template_editor_keep_editing), viewModel::onDialogDismissed),
        )
    }
}
