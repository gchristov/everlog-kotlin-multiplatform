package com.everlog.android.settings.ui.tab

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import com.everlog.android.common.mvp.fragment.BaseFragmentPresenter
import com.everlog.android.common.mvp.fragment.BaseTabFragment
import com.everlog.android.common.navigation.DesignSystem
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.common.navigation.Login
import com.everlog.android.common.navigation.PlayStoreListing
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.ShareApp
import com.everlog.android.settings.ui.SettingsGraph
import com.everlog.android.settings.ui.SettingsScreenNames
import kotlinx.coroutines.Dispatchers

// Home's Settings tab in Compose (SettingsTabScreen), inside Home's XML tab pager, while Remote Config's settings_tab_v2 is on
class SettingsTabFragment : BaseTabFragment() {

    private val graph by lazy { requireContext().appGraph<SettingsGraph>() }
    private var mPresenter: PresenterSettingsTab? = null

    private val viewModel by lazy {
        val appContext = requireContext().applicationContext
        val factory = viewModelFactory {
            initializer {
                SettingsTabViewModel(
                    dispatcher = Dispatchers.Main,
                    settings = graph.userSettings,
                    session = graph.userSession,
                    config = graph.settingsConfig,
                    analytics = graph.settingsAnalytics,
                    canSendFeedback = { graph.navigator.canOpen(FeedbackForm(FeedbackSource.SETTINGS)) },
                    showDeveloper = appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
                    onSettingsChanged = {
                        LocalBroadcastManager.getInstance(appContext).sendBroadcast(Intent(MvpBroadcasts.PREFERENCES_CHANGED))
                    },
                )
            }
        }
        ViewModelProvider(this, factory)[SettingsTabViewModel::class.java]
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
                    SettingsTabScreen(
                        viewModel = viewModel,
                        actions = SettingsTabActions(
                            openSetSuggestions = { navigate(SetSuggestions) },
                            sendFeedback = { navigate(FeedbackForm(FeedbackSource.SETTINGS)) },
                            reviewOnPlay = { navigate(PlayStoreListing) },
                            share = { navigate(ShareApp) },
                            openDesignSystem = { navigate(DesignSystem) },
                            logOut = ::logOut,
                        ),
                        describeSetSuggestion = { goal -> graph.settingsConfig.setSuggestionText(requireContext(), goal) },
                    )
                }
            }
        }
    }

    override fun getAnalyticsScreenName(): String {
        return SettingsScreenNames.HOME_V2
    }

    override fun getTitleResId(): Int {
        return -1
    }

    override fun <T : BaseFragmentMvpView> getPresenter(): BaseFragmentPresenter<T>? {
        @Suppress("UNCHECKED_CAST")
        return mPresenter as? BaseFragmentPresenter<T>
    }

    override fun setupPresenter() {
        mPresenter = PresenterSettingsTab(refresh = { viewModel.refresh() })
    }

    private fun logOut() {
        graph.settingsAccount.logout {
            if (isAdded) navigate(Login)
        }
    }

    private fun navigate(destination: Destination) {
        context?.let { graph.navigator.open(it, destination) }
    }
}
