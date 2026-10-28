package com.everlog.ui.activities.templateeditor

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.R
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.templatesession.RealLoadTemplateUseCase
import com.everlog.data.controllers.templatesession.RealSaveTemplateUseCase
import com.everlog.data.controllers.workoutprefill.RealLoadPrefillSourcesUseCase
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.data.repositories.RealOngoingPlanRepository
import com.everlog.data.repositories.RealRoutineRepository
import com.everlog.data.repositories.RealWorkoutRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.RemoteConfigManager
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.components.exercisetable.ExerciseTab
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers

/**
 * The template editor in Compose, while Remote Config's template_editor_v2 is on ([editIntent]). It reads the
 * template by its uuid rather than taking it in the intent, which a big template could outgrow.
 */
class TemplateEditorActivity : CommonComposeActivity() {
    private val viewModel by viewModels<TemplateEditorViewModel> {
        viewModelFactory {
            initializer {
                val routines = RealRoutineRepository(Dispatchers.IO, AppDataSync.instance)
                val exercises = RealExerciseRepository(Dispatchers.IO, AppDataSync.instance)
                TemplateEditorViewModel(
                    dispatcher = Dispatchers.Main,
                    uuid = intent.getStringExtra(ExtraRoutineUuid),
                    defaultName = getString(R.string.template_editor_default_name),
                    loadTemplate = RealLoadTemplateUseCase(Dispatchers.Default, routines, exercises),
                    saveTemplate = RealSaveTemplateUseCase(Dispatchers.IO, routines, RealOngoingPlanRepository(applicationContext)),
                    analytics = RealTemplateEditorAnalytics(),
                    ownExerciseUpdates = exercises::ownExerciseUpdates,
                    prototype = prototype,
                    loadPrefillSources = RealLoadPrefillSourcesUseCase(Dispatchers.Default, RealWorkoutRepository(Dispatchers.IO, AppDataSync.instance)),
                )
            }
        }
    }

    override val requiresUser = true

    private val prototype by lazy { TemplateEditorPrototype.read(this) }

    @Composable
    override fun Content() = TemplateEditorScreen(
        viewModel = viewModel,
        onAddExercise = { ELNavigator(this).openExercisePicker() },
        onOpenExercise = { exercise, tab ->
            val type = when (tab) {
                ExerciseTab.Info -> ExerciseDetailsActivity.Companion.Type.INFO
                ExerciseTab.History -> ExerciseDetailsActivity.Companion.Type.HISTORY
            }
            ELNavigator(this).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise).type(type))
        },
        onExit = ::exit,
        prototype = prototype,
    )

    @Deprecated("The navigator opens the exercise picker with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_EXERCISES && resultCode == RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            (data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>)?.let(viewModel::onExercisesPicked)
        }
    }

    // The routine goes back as the old editor's did: the routine picker starts a workout from a new one
    private fun exit(exit: TemplateEditorExit) {
        if (exit is TemplateEditorExit.Saved) {
            setResult(RESULT_OK, Intent().putExtra(ELConstants.EXTRA_ROUTINE, exit.routine))
            if (exit.isNew && intent.getBooleanExtra(ELConstants.EXTRA_SHOW_DETAILS_ON_SUCCESS, true)) {
                ELNavigator(this).openRoutineDetails(exit.routine, false)
            }
        }
        finish()
    }

    companion object {
        private const val ExtraRoutineUuid = "EXTRA_ROUTINE_UUID"

        /**
         * The template editor for every entry point: this screen while template_editor_v2 is on, the old one
         * otherwise. A template to edit is passed by its uuid.
         */
        @JvmStatic
        fun editIntent(context: Context, properties: CreateRoutineActivity.Companion.Properties): Intent =
            if (RemoteConfigManager.manager.templateEditorV2()) {
                launchIntent(context, properties.routine?.uuid, properties.showDetailsOnSuccess)
            } else {
                CreateRoutineActivity.launchIntent(context, properties)
            }

        /**
         * @param uuid The template to edit, or null for a new one
         * @param showDetailsOnSuccess Opens a new template's details once it's saved
         */
        internal fun launchIntent(context: Context, uuid: String?, showDetailsOnSuccess: Boolean): Intent =
            Intent(context, TemplateEditorActivity::class.java)
                .putExtra(ExtraRoutineUuid, uuid)
                .putExtra(ELConstants.EXTRA_SHOW_DETAILS_ON_SUCCESS, showDetailsOnSuccess)
    }
}
