package com.everlog.android.statistics.ui.page

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.statistics.ui.StatisticsGraph
import com.everlog.android.statistics.ui.StatisticsScreenNames
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

// The Activity tab's statistics page in Compose (StatisticsPageScreen), while Remote Config's statistics_v2 is on
class StatisticsPageFragment : BaseTabFragment() {

    private val graph by lazy { requireContext().appGraph<StatisticsGraph>() }
    private var mPresenter: PresenterStatisticsPage? = null

    private val viewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                StatisticsPageViewModel(
                    dispatcher = Dispatchers.Main,
                    workouts = graph.workoutRepository,
                    settings = graph.userSettings,
                    analytics = graph.statisticsAnalytics,
                    preferences = graph.statisticsPreferences,
                    feedback = { graph.navigator.canOpen(Feedback) },
                    today = { LocalDate.now() },
                )
            }
        }
        ViewModelProvider(this, factory)[StatisticsPageViewModel::class.java]
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
                    StatisticsPageScreen(
                        viewModel = viewModel,
                        onFeedback = { context?.let { graph.navigator.open(it, Feedback) } },
                        // The page runs under Home's tab bar, as the old Home's tabs do
                        bottomPadding = dimensionResource(DesignR.dimen.footer_bottom_padding_double),
                    )
                }
            }
        }
    }

    override fun getAnalyticsScreenName(): String {
        return StatisticsScreenNames.STATISTICS_V2
    }

    override fun getTitleResId(): Int {
        return DesignR.string.statistics_title
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        @Suppress("UNCHECKED_CAST")
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun setupPresenter() {
        mPresenter = PresenterStatisticsPage(refresh = { viewModel.refresh() })
    }

    private companion object {
        val Feedback = FeedbackForm(FeedbackSource.ACTIVITY_STATISTICS)
    }
}
