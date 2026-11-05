package com.everlog.android.home.weektab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.ExerciseLibrary
import com.everlog.android.common.navigation.PlayStoreListing
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.WebLink
import com.everlog.android.home.HomeGraph
import com.everlog.android.home.HomeHost
import com.everlog.android.home.HomeScreenNames
import com.everlog.android.home.data.HomeNotificationTap
import com.everlog.android.home.data.model.HomeNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

// Home's Week tab in Compose (WeekTabScreen), inside Home's XML tab pager, while Remote Config's week_tab_v2 is on
class WeekTabFragment : BaseTabFragment() {

    private val graph by lazy { requireContext().appGraph<HomeGraph>() }
    private var mPresenter: PresenterWeekTab? = null

    private val viewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                WeekTabViewModel(
                    dispatcher = Dispatchers.Main,
                    workouts = graph.workoutRepository,
                    settings = graph.userSettings,
                    notifications = graph.homeNotifications,
                    config = graph.homeConfig,
                    analytics = graph.homeAnalytics,
                    today = { LocalDate.now() },
                )
            }
        }
        ViewModelProvider(this, factory)[WeekTabViewModel::class.java]
    }

    private val host get() = activity as? HomeHost

    override fun onFragmentCreated() {
        // Home hides its start workout button on an empty week, which has a button of its own
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.filterNot { it.loading }.map { it.empty }.distinctUntilChanged().collect { host?.setWeekEmpty(it) }
            }
        }
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
                    WeekTabScreen(
                        viewModel = viewModel,
                        actions = WeekTabActions(
                            showStatistics = { host?.showStatistics() },
                            showSettings = { host?.showSettings() },
                            showStartWorkout = { host?.showAddMenu() },
                            open = ::open,
                        ),
                    )
                }
            }
        }
    }

    override fun getAnalyticsScreenName(): String {
        return HomeScreenNames.WEEK
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        @Suppress("UNCHECKED_CAST")
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun setupPresenter() {
        mPresenter = PresenterWeekTab(refresh = { viewModel.refresh() })
    }

    private fun open(tap: HomeNotificationTap) {
        when (tap) {
            HomeNotificationTap.UpdateApp -> navigate(PlayStoreListing)
            is HomeNotificationTap.OpenUrl -> navigate(WebLink(tap.url))
            is HomeNotificationTap.Open -> when (tap.action) {
                HomeNotification.ActionType.MUSCLE_GOALS -> navigate(SetSuggestions)
                HomeNotification.ActionType.EXERCISES -> navigate(ExerciseLibrary)
                HomeNotification.ActionType.PLANS -> host?.showPlans()
                HomeNotification.ActionType.SETTINGS -> host?.showSettings()
                HomeNotification.ActionType.NONE, HomeNotification.ActionType.MAINTENANCE -> Unit
            }
            HomeNotificationTap.Nothing -> Unit
        }
    }

    private fun navigate(destination: Destination) {
        context?.let { graph.navigator.open(it, destination) }
    }
}
