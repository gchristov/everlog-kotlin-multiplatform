package com.everlog.ui.activities.exercisepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.R
import com.everlog.android.common.compose.CommonComposeActivity
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.config.AppConfig
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.exerciselibrary.RealLoadExerciseLibraryUseCase
import com.everlog.data.model.exercise.library.toELExercise
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.data.repositories.RealRoutineRepository
import com.everlog.data.repositories.RealWorkoutRepository
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import kotlinx.coroutines.Dispatchers

/**
 * The exercise picker, while Remote Config's exercise_picker_v2 is on. Browsing (Home › Exercises), a tap opens an
 * exercise; picking (the template and workout builders), it returns the picks as exercise groups in
 * [ELConstants.EXTRA_EXERCISE_GROUPS]. The spec is on the Everlog canvas (Exercise Library page).
 */
class ExercisePickerActivity : CommonComposeActivity() {
    private val viewModel by viewModels<ExercisePickerViewModel> {
        viewModelFactory {
            initializer {
                val io = Dispatchers.IO
                val exercises = RealExerciseRepository(io, AppDataSync.instance)
                ExercisePickerViewModel(
                    dispatcher = Dispatchers.Main,
                    mode = if (intent.getBooleanExtra(ExtraPick, false)) PickerMode.Pick else PickerMode.Browse,
                    savedState = createSavedStateHandle(),
                    loadLibrary = RealLoadExerciseLibraryUseCase(io, exercises, RealRoutineRepository(io, AppDataSync.instance), RealWorkoutRepository(io, AppDataSync.instance)),
                    recentSearches = RealExercisePickerRecentSearches(),
                    analytics = RealExercisePickerAnalytics(),
                    me = FilterValue(
                        id = CreatedByMe,
                        name = getString(R.string.exercise_picker_created_by_me),
                        aliases = resources.getStringArray(R.array.exercise_picker_created_by_me_aliases).toList(),
                    ),
                    maxPicks = AppConfig.configuration.maxExerciseSelection,
                )
            }
        }
    }

    private val sendFeedback by lazy { feedbackFormLauncher(FeedbackSource.EXERCISE_PICKER_MENU) }
    private val reportMissing by lazy { feedbackFormLauncher(FeedbackSource.EXERCISE_PICKER_NO_RESULTS) }

    @Composable
    override fun Content() = ExercisePickerScreen(
        viewModel = viewModel,
        onResult = ::finishWithPicks,
        onOpenExercise = { ELNavigator(this).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(it.toELExercise())) },
        onCreateExercise = { name -> ELNavigator(this).openCreateExercise(name) },
        onPickSetType = { count -> ELNavigator(this).openSetTypePicker(count) },
        onSendFeedback = sendFeedback,
        onReportMissing = reportMissing,
    )

    // A superset goes through the set type picker first
    @Deprecated("The navigator opens the set type picker with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_SET_TYPE && resultCode == RESULT_OK) {
            (data?.getSerializableExtra(ELConstants.EXTRA_SET_TYPE) as? ELSetType)?.let { viewModel.onAdd(it) }
        }
        // An exercise just created from here is picked
        if (requestCode == ELActivityRequestCodes.REQUEST_EDIT_EXERCISE && resultCode == RESULT_OK) {
            data?.getStringExtra(ELConstants.EXTRA_EXERCISE_UUID)?.let { viewModel.onExerciseCreated(it) }
        }
    }

    private fun finishWithPicks(result: PickerResult) {
        setResult(Activity.RESULT_OK, Intent().putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, ArrayList(result.toExerciseGroups())))
        finish()
    }

    companion object {
        private const val ExtraPick = "pick"

        @JvmStatic
        fun launchIntent(context: Context, pick: Boolean): Intent =
            Intent(context, ExercisePickerActivity::class.java).putExtra(ExtraPick, pick)
    }
}
