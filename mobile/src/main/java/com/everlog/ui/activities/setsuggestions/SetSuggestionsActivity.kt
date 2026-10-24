package com.everlog.ui.activities.setsuggestions

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.config.FeedbackSource
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.navigator.feedbackFormLauncher
import kotlinx.coroutines.Dispatchers

/**
 * Where weight suggestions are set, while Remote Config's set_suggestions_v2 is on. The spec is on the Everlog
 * canvas (Set suggestions page).
 */
class SetSuggestionsActivity : CommonComposeActivity() {
    private val viewModel by viewModels<SetSuggestionsViewModel> {
        viewModelFactory {
            initializer {
                SetSuggestionsViewModel(
                    dispatcher = Dispatchers.Main,
                    settings = RealSetSuggestionsSettings(this@SetSuggestionsActivity),
                    analytics = RealSetSuggestionsAnalytics(),
                )
            }
        }
    }

    private val sendFeedback by lazy { feedbackFormLauncher(FeedbackSource.SET_SUGGESTIONS) }

    @Composable
    override fun Content() = SetSuggestionsScreen(viewModel = viewModel, onSendFeedback = sendFeedback)

    companion object {
        @JvmStatic
        fun launchIntent(context: Context): Intent = Intent(context, SetSuggestionsActivity::class.java)
    }
}
