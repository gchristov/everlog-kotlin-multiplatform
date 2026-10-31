package com.everlog.android.workout.setsuggestions

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.CommonComposeActivity
import com.everlog.android.workout.WorkoutFeature
import kotlinx.coroutines.Dispatchers

/**
 * Where weight suggestions are set, while Remote Config's set_suggestions_v2 is on. The spec is on the Everlog
 * canvas (Set suggestions page).
 */
class SetSuggestionsActivity : CommonComposeActivity() {
    private val dependencies = WorkoutFeature.dependencies

    private val viewModel by viewModels<SetSuggestionsViewModel> {
        viewModelFactory {
            initializer {
                SetSuggestionsViewModel(
                    dispatcher = Dispatchers.Main,
                    settings = dependencies.setSuggestionsSettings(this@SetSuggestionsActivity),
                    analytics = dependencies.setSuggestionsAnalytics(),
                )
            }
        }
    }

    private val sendFeedback by lazy { dependencies.setSuggestionsFeedback(this) }

    @Composable
    override fun Content() = SetSuggestionsScreen(viewModel = viewModel, onSendFeedback = sendFeedback)

    companion object {
        @JvmStatic
        fun launchIntent(context: Context): Intent = Intent(context, SetSuggestionsActivity::class.java)
    }
}
