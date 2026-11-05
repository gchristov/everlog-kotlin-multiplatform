package com.everlog.android.history.ui.page

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.common.navigation.WorkoutDetails
import com.everlog.android.history.ui.HistoryGraph
import com.everlog.android.history.ui.HistoryScreenNames
import com.everlog.android.history.ui.R
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

// The Activity tab's history page in Compose (HistoryPageScreen), while Remote Config's history_v2 is on
class HistoryPageFragment : BaseTabFragment() {

    private val graph by lazy { requireContext().appGraph<HistoryGraph>() }
    private var mPresenter: PresenterHistoryPage? = null

    private val viewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                HistoryPageViewModel(
                    dispatcher = Dispatchers.Main,
                    workouts = graph.workoutRepository,
                    settings = graph.userSettings,
                    preferences = graph.historyPreferences,
                    exampleNames = ExampleNames.map(::getString),
                    today = { LocalDate.now() },
                )
            }
        }
        ViewModelProvider(this, factory)[HistoryPageViewModel::class.java]
    }

    // The tab holding the page was left for another: its pages can go
    fun onTabLeft() {
        viewModel.onLeft()
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
                    HistoryPageScreen(
                        viewModel = viewModel,
                        onWorkout = { workout ->
                            context?.let { graph.navigator.open(it, WorkoutDetails(workout, justFinished = false, fromPlan = false)) }
                        },
                    )
                }
            }
        }
    }

    override fun getAnalyticsScreenName(): String {
        return HistoryScreenNames.HISTORY
    }

    override fun getTitleResId(): Int {
        return DesignR.string.history_title
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        @Suppress("UNCHECKED_CAST")
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun setupPresenter() {
        mPresenter = PresenterHistoryPage(refresh = { viewModel.refresh() })
    }
}

private val ExampleNames = listOf(R.string.history_example_legs, R.string.history_example_push, R.string.history_example_pull)
