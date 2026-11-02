package com.everlog.android.template.editor

import com.everlog.android.template.TemplateGraph
import com.everlog.android.common.kotlin.di.appGraph
import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.mvp.activity.SignedInComposeActivity
import com.everlog.android.template.R
import com.everlog.android.template.TemplateExtras
import com.everlog.android.template.details.RoutineDetailsActivity
import kotlinx.coroutines.Dispatchers

/**
 * The template editor in Compose. It reads the template by its uuid rather than taking it in the intent, which a big
 * template could outgrow.
 */
class TemplateEditorActivity : SignedInComposeActivity() {
    private val graph by lazy { appGraph<TemplateGraph>() }
    private val navigation by lazy { graph.templateNavigation }

    private val viewModel by viewModels<TemplateEditorViewModel> {
        viewModelFactory {
            initializer {
                TemplateEditorViewModel(
                    dispatcher = Dispatchers.Main,
                    uuid = intent.getStringExtra(ExtraRoutineUuid),
                    defaultName = getString(R.string.template_editor_default_name),
                    loadTemplate = graph.loadTemplate,
                    saveTemplate = graph.saveTemplate,
                    analytics = graph.templateEditorAnalytics,
                    ownExerciseUpdates = graph.exerciseRepository::ownExerciseUpdates,
                )
            }
        }
    }

    @Composable
    override fun Content() = TemplateEditorScreen(
        viewModel = viewModel,
        onAddExercise = { navigation.openExercisePicker(this) },
        onOpenExercise = { exercise, tab -> navigation.openExerciseDetails(this, exercise, tab) },
        onExit = ::exit,
    )

    @Deprecated("The exercise picker opens with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        navigation.pickedExerciseGroups(requestCode, resultCode, data)?.let(viewModel::onExercisesPicked)
    }

    // The routine goes back as the old editor's did: the routine picker starts a workout from a new one
    private fun exit(exit: TemplateEditorExit) {
        if (exit is TemplateEditorExit.Saved) {
            setResult(RESULT_OK, Intent().putExtra(TemplateExtras.ROUTINE, exit.routine))
            if (exit.isNew && intent.getBooleanExtra(TemplateExtras.SHOW_DETAILS_ON_SUCCESS, true)) {
                startActivity(RoutineDetailsActivity.launchIntent(this, exit.routine, viewOnly = false))
            }
        }
        finish()
    }

    companion object {
        private const val ExtraRoutineUuid = "EXTRA_ROUTINE_UUID"

        /**
         * @param uuid The template to edit, or null for a new one
         * @param showDetailsOnSuccess Opens a new template's details once it's saved
         */
        fun launchIntent(context: Context, uuid: String?, showDetailsOnSuccess: Boolean): Intent =
            Intent(context, TemplateEditorActivity::class.java)
                .putExtra(ExtraRoutineUuid, uuid)
                .putExtra(TemplateExtras.SHOW_DETAILS_ON_SUCCESS, showDetailsOnSuccess)
    }
}
