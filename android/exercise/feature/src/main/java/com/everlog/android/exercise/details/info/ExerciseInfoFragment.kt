package com.everlog.android.exercise.details.info

import com.everlog.android.exercise.ExerciseGraph
import com.everlog.android.common.kotlin.di.appGraph
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.exercise.ExerciseScreenNames
import com.everlog.android.exercise.R
import com.everlog.android.exercise.details.BaseExerciseTabFragment
import com.everlog.android.exercise.details.BasePresenterExerciseTab
import com.everlog.android.exercise.details.statistics.ExerciseStatsController
import kotlinx.coroutines.Dispatchers

// The Info tab, in Compose inside the XML tab pager (ExerciseInfoScreen), while exercise_details_v2 is on.
// The flag is meant to grow to the whole screen as it moves to Compose.
class ExerciseInfoFragment(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseInfo {

    private var mPresenter: PresenterExerciseInfo? = null
    private val graph by lazy { requireContext().appGraph<ExerciseGraph>() }

    private val viewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                val exercise = getExercise()
                ExerciseInfoViewModel(
                    dispatcher = Dispatchers.Main,
                    loadGuide = graph.loadExerciseGuide,
                    exercise = exercise.toInfoExercise(graph.userSession.user()?.id),
                )
            }
        }
        ViewModelProvider(this, factory)[ExerciseInfoViewModel::class.java]
    }

    override fun onFragmentCreated() {
        // No-op
    }

    override fun getLayoutResId(): Int {
        // Unused: getBindingView gives the view
        return 0
    }

    override fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): View {
        return ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Theme {
                    ExerciseInfoScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun getAnalyticsScreenName(): String {
        return ExerciseScreenNames.INFO_V2
    }

    override fun getTabPresenter(): BasePresenterExerciseTab<*>? {
        return mPresenter
    }

    override fun getTitleResId(): Int {
        return R.string.exercise_info_title
    }

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        // Reloads pass the exercise here (see BaseExerciseTabFragment.update): the view-model keeps
        // the guide, and the screen its scroll position
        if (isAdded) {
            viewModel.updateExercise(exercise.toInfoExercise(graph.userSession.user()?.id))
        }
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseInfo()
    }
}

private fun ELExercise.toInfoExercise(userId: String?) = ExerciseInfoViewModel.Exercise(
    uuid = uuid.orEmpty(),
    name = name.orEmpty(),
    imageUrl = imageUrl,
    own = isEditable(userId),
)
