package com.everlog.ui.fragments.home.exercise.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.R
import com.everlog.data.controllers.exerciseguide.RealLoadExerciseGuideUseCase
import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.fragments.home.exercise.BaseExerciseTabFragment
import com.everlog.ui.fragments.home.exercise.BasePresenterExerciseTab
import kotlinx.coroutines.Dispatchers

// The Info tab, in Compose inside the XML tab pager (ExerciseInfoScreen), while exercise_details_v2 is on.
// The flag is meant to grow to the whole screen as it moves to Compose.
class ExerciseInfoFragment(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) : BaseExerciseTabFragment(exercise, stats), MvpViewExerciseInfo {

    private var mPresenter: PresenterExerciseInfo? = null

    private val viewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                val exercise = getExercise()
                val io = Dispatchers.IO
                ExerciseInfoViewModel(
                    dispatcher = Dispatchers.Main,
                    loadGuide = RealLoadExerciseGuideUseCase(io, RealExerciseRepository(io)),
                    exercise = ExerciseInfoViewModel.Exercise(
                        uuid = exercise.uuid.orEmpty(),
                        name = exercise.name.orEmpty(),
                        imageUrl = exercise.imageUrl,
                        own = exercise.isEditable(),
                    ),
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
        return AnalyticsConstants.SCREEN_EXERCISE_INFO
    }

    override fun getTabPresenter(): BasePresenterExerciseTab<*>? {
        return mPresenter
    }

    override fun getTitleResId(): Int {
        return R.string.exercise_info_title
    }

    override fun loadExerciseDetails(exercise: ELExercise, stats: ExerciseStatsController.StatsResult?) {
        // No-op: the screen opens new tabs when the exercise changes, so the arguments' copy is current
    }

    // Setup

    override fun setupPresenter() {
        mPresenter = PresenterExerciseInfo()
    }
}
