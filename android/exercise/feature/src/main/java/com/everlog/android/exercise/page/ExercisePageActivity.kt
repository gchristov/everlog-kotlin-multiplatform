package com.everlog.android.exercise.page

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.mvp.activity.SignedInComposeActivity
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.exercise.ExerciseExtras
import com.everlog.android.exercise.ExerciseGraph
import com.everlog.android.exercise.R
import com.everlog.android.exercise.create.CreateExerciseActivity
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

// An exercise's page: its Info, Statistics and History tabs
class ExercisePageActivity : SignedInComposeActivity() {
    private val graph by lazy { appGraph<ExerciseGraph>() }

    override val requiredExtras = listOf(ExerciseExtras.EXERCISE)

    private val exercise by lazy { intent.getSerializableExtra(ExerciseExtras.EXERCISE) as ELExercise }

    private val viewModel by viewModels<ExercisePageViewModel> {
        viewModelFactory {
            initializer {
                ExercisePageViewModel(
                    dispatcher = Dispatchers.Main,
                    exercise = exercise,
                    userId = graph.userSession.user()?.id,
                    readHistory = graph.workoutRepository::exerciseHistory,
                    loadGuide = graph.loadExerciseGuide,
                    ownExerciseUpdates = graph.exerciseRepository::ownExerciseUpdates,
                    deleteExercise = graph.deleteExercise,
                    settings = graph.userSettings,
                    preferences = graph.exerciseStatsPreferences,
                    analytics = graph.exercisePageAnalytics,
                    feedback = { graph.navigator.canOpen(Feedback) },
                    // Named after the exercise's muscles and the splits it could be in, so the example looks like the user's
                    exampleNames = listOf(
                        categoryTitle(exercise.category) ?: getString(R.string.exercise_page_example_workout),
                        getString(R.string.exercise_page_example_upper_body),
                        getString(R.string.exercise_page_example_full_body),
                    ),
                    today = { LocalDate.now() },
                )
            }
        }
    }

    @Composable
    override fun Content() = ExercisePageScreen(
        viewModel = viewModel,
        tab = intent.getSerializableExtra(ExerciseExtras.DETAILS_TAB) as? ExercisePageTab ?: ExercisePageTab.Info,
        onEdit = { startActivity(CreateExerciseActivity.editIntent(this, it)) },
        onFeedback = { graph.navigator.open(this, Feedback) },
        onClose = ::finish,
    )

    companion object {
        private val Feedback = FeedbackForm(FeedbackSource.EXERCISE_STATISTICS)

        fun launchIntent(context: Context, exercise: ELExercise, tab: ExercisePageTab): Intent =
            Intent(context, ExercisePageActivity::class.java)
                .putExtra(ExerciseExtras.EXERCISE, exercise)
                .putExtra(ExerciseExtras.DETAILS_TAB, tab)
    }
}
