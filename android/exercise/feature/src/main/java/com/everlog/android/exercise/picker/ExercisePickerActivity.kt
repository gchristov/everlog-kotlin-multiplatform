package com.everlog.android.exercise.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.CommonComposeActivity
import com.everlog.android.exercise.data.model.library.toELExercise
import com.everlog.android.exercise.ExerciseFeature
import com.everlog.android.exercise.ExerciseFeedbackForm
import com.everlog.android.exercise.R
import kotlinx.coroutines.Dispatchers

/**
 * The exercise picker. Browsing (Home › Exercises), a tap opens an exercise; picking (the template and workout
 * builders), it returns the picks as exercise groups in [EXTRA_EXERCISE_GROUPS]. The spec is on the Everlog canvas (Exercise Library page).
 */
class ExercisePickerActivity : CommonComposeActivity() {
    private val dependencies = ExerciseFeature.dependencies

    private val viewModel by viewModels<ExercisePickerViewModel> {
        viewModelFactory {
            initializer {
                ExercisePickerViewModel(
                    dispatcher = Dispatchers.Main,
                    mode = if (intent.getBooleanExtra(ExtraPick, false)) PickerMode.Pick else PickerMode.Browse,
                    savedState = createSavedStateHandle(),
                    loadLibrary = dependencies.loadExerciseLibrary(),
                    recentSearches = RealExercisePickerRecentSearches(),
                    analytics = dependencies.pickerAnalytics(),
                    me = FilterValue(
                        id = CreatedByMe,
                        name = getString(R.string.exercise_picker_created_by_me),
                        aliases = resources.getStringArray(R.array.exercise_picker_created_by_me_aliases).toList(),
                    ),
                    maxPicks = MAX_PICKS,
                )
            }
        }
    }

    private val sendFeedback by lazy { dependencies.feedbackForm(this, ExerciseFeedbackForm.PickerMenu) }
    private val reportMissing by lazy { dependencies.feedbackForm(this, ExerciseFeedbackForm.PickerNoResults) }

    @Composable
    override fun Content() = ExercisePickerScreen(
        viewModel = viewModel,
        onResult = ::finishWithPicks,
        onOpenExercise = { dependencies.openExerciseDetails(this, it.toELExercise()) },
        onCreateExercise = { name -> dependencies.openCreateExercise(this, name) },
        onPickSetType = { count -> dependencies.openSetTypePicker(this, count) },
        onSendFeedback = sendFeedback,
        onReportMissing = reportMissing,
    )

    // A superset goes through the set type picker first, and an exercise just created from here is picked
    @Deprecated("The navigator opens the set type picker and the create screen with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        dependencies.pickedSetType(requestCode, resultCode, data)?.let { viewModel.onAdd(it) }
        dependencies.createdExerciseUuid(requestCode, resultCode, data)?.let { viewModel.onExerciseCreated(it) }
    }

    private fun finishWithPicks(result: PickerResult) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_EXERCISE_GROUPS, ArrayList(result.toExerciseGroups())))
        finish()
    }

    companion object {
        // The picks, as a list of exercise groups
        const val EXTRA_EXERCISE_GROUPS = "EXTRA_EXERCISE_GROUPS"
        const val MAX_PICKS = 10

        private const val ExtraPick = "pick"

        @JvmStatic
        fun launchIntent(context: Context, pick: Boolean): Intent =
            Intent(context, ExercisePickerActivity::class.java).putExtra(ExtraPick, pick)
    }
}
