package com.everlog.android.home.workoutstab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.ExerciseLibrary
import com.everlog.android.common.navigation.NewPlan
import com.everlog.android.common.navigation.PlanDetails
import com.everlog.android.common.navigation.StartEmptyWorkout
import com.everlog.android.common.navigation.StartWorkout
import com.everlog.android.common.navigation.TemplateDetails
import com.everlog.android.common.navigation.TemplateEditor
import com.everlog.android.home.HomeGraph
import com.everlog.android.home.HomeScreenNames
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

// Home's Workouts tab in Compose (WorkoutsTabScreen), inside Home's XML tab pager, while Remote Config's workouts_tab_v2 is on
class WorkoutsTabFragment : BaseTabFragment() {

    private val graph by lazy { requireContext().appGraph<HomeGraph>() }
    private var mPresenter: PresenterWorkoutsTab? = null

    private val viewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                WorkoutsTabViewModel(
                    dispatcher = Dispatchers.Main,
                    lists = graph.loadWorkoutsTabLists,
                    history = graph.loadTemplateHistory,
                    planProgress = graph.planProgress,
                    analytics = graph.homeAnalytics,
                    today = { LocalDate.now() },
                )
            }
        }
        ViewModelProvider(this, factory)[WorkoutsTabViewModel::class.java]
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
                    WorkoutsTabScreen(
                        viewModel = viewModel,
                        actions = WorkoutsTabActions(
                            showExercises = { navigate(ExerciseLibrary) },
                            showTemplate = { navigate(TemplateDetails(it, viewOnly = false)) },
                            start = { navigate(StartWorkout(it.routine, it.fromPlan)) },
                            startEmpty = { navigate(StartEmptyWorkout) },
                            newTemplate = { navigate(TemplateEditor()) },
                            showPlan = { navigate(PlanDetails(it)) },
                            newPlan = { navigate(NewPlan) },
                        ),
                    )
                }
            }
        }
    }

    override fun getAnalyticsScreenName(): String {
        return HomeScreenNames.WORKOUTS_V2
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        @Suppress("UNCHECKED_CAST")
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun setupPresenter() {
        mPresenter = PresenterWorkoutsTab(onShown = viewModel::onShown, onHidden = viewModel::onHidden)
    }

    private fun navigate(destination: Destination) {
        context?.let { graph.navigator.open(it, destination) }
    }
}
