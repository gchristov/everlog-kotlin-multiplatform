package com.everlog.android.settings.ui.tab

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.everlog.android.common.compose.components.broadcast.LocalBroadcastEffect
import com.everlog.android.common.compose.components.shown.ShownEffect
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.common.mvp.MvpBroadcasts
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

/**
 * Home's Settings tab with what it needs from the app's graph. It reads its values again when it shows ([shown]: its
 * tab is the current one), and when the user's settings or Remote Config change elsewhere (Set suggestions changes
 * the weekly goal on its own screen).
 */
@Composable
fun SettingsTabRoute(shown: Boolean, bottomPadding: Dp) {
    val context = LocalContext.current
    val graph = remember(context) { context.appGraph<SettingsGraph>() }
    val viewModel: SettingsTabViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
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
        },
    )
    ShownEffect(shown, onShown = {
        viewModel.refresh()
        CommonMvp.dependencies.screenViewed(SettingsScreenNames.HOME_V2)
    })
    LocalBroadcastEffect(MvpBroadcasts.PREFERENCES_CHANGED, MvpBroadcasts.REMOTE_CONFIG_REFRESHED) { viewModel.refresh() }
    val navigate = { destination: Destination -> graph.navigator.open(context, destination) }
    SettingsTabScreen(
        viewModel = viewModel,
        bottomPadding = bottomPadding,
        actions = SettingsTabActions(
            openSetSuggestions = { navigate(SetSuggestions) },
            sendFeedback = { navigate(FeedbackForm(FeedbackSource.SETTINGS)) },
            reviewOnPlay = { navigate(PlayStoreListing) },
            share = { navigate(ShareApp) },
            openDesignSystem = { navigate(DesignSystem) },
            logOut = { graph.settingsAccount.logout { navigate(Login) } },
        ),
        describeSetSuggestion = { goal -> graph.settingsConfig.setSuggestionText(context, goal) },
    )
}
