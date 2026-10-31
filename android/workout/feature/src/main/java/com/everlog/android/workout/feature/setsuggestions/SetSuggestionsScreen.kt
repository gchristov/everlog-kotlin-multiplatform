package com.everlog.android.workout.feature.setsuggestions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBar
import com.everlog.android.common.compose.elements.AppCheck
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppScreen
import com.everlog.android.common.compose.elements.list.AppGroupedList
import com.everlog.android.common.compose.elements.list.AppGroupedListScope
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.list.items
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.workout.feature.R
import com.everlog.android.common.design.R as DesignR

/**
 * Off or last time on top, the 1RM goals below them with a sheet on how their weights are worked out.
 */
@Composable
internal fun SetSuggestionsScreen(viewModel: SetSuggestionsViewModel, onSendFeedback: (() -> Unit)? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    SetSuggestionsContent(
        state = state,
        onPick = viewModel::onPick,
        onShowHow = viewModel::onShowHow,
        onSendFeedback = onSendFeedback,
    )
    if (state.showingHow) {
        HowGoalWeightsSheet(example = state.example, unit = state.unit, onDismiss = viewModel::onHideHow)
    }
}

@Composable
private fun SetSuggestionsContent(
    state: SetSuggestionsState,
    onPick: (SetSuggestion) -> Unit,
    onShowHow: () -> Unit,
    onSendFeedback: (() -> Unit)?,
) {
    AppScreen(topBar = { AppBar(title = stringResource(DesignR.string.set_suggestions_title), showBack = true) }) { contentPadding ->
        AppGroupedList(contentPadding = contentPadding) {
            options(key = "simple", header = null, options = SetSuggestion.simple, selected = state.selected, onPick = onPick)
            options(key = "goals", header = R.string.set_suggestions_goals, options = SetSuggestion.goals, selected = state.selected, onPick = onPick) {
                AppListItem(
                    modifier = Modifier.testTag(SetSuggestionsTags.How),
                    title = stringResource(R.string.set_suggestions_how),
                    leading = { AppIcon(imageVector = ImageVector.vectorResource(DesignR.drawable.ic_info), tint = Theme.contentColors.action) },
                    onClick = onShowHow,
                )
            }
            if (onSendFeedback != null) {
                group(key = "feedback") {
                    items(count = 1, key = { "send_feedback" }) {
                        AppListItem(
                            modifier = Modifier.testTag(SetSuggestionsTags.SendFeedback),
                            title = stringResource(DesignR.string.feedback_send),
                            subtitle = stringResource(R.string.feedback_set_suggestions_subtitle),
                            leading = { AppIcon(imageVector = ImageVector.vectorResource(DesignR.drawable.ic_feedback), tint = Theme.contentColors.action) },
                            onClick = onSendFeedback,
                        )
                    }
                }
            }
        }
    }
}

private fun AppGroupedListScope.options(
    key: String,
    header: Int?,
    options: List<SetSuggestion>,
    selected: SetSuggestion?,
    onPick: (SetSuggestion) -> Unit,
    after: (@Composable () -> Unit)? = null,
) {
    group(key = key, header = header?.let { { stringResource(it) } }) {
        items(options, key = { it.name }) { option ->
            AppListItem(
                modifier = Modifier.testTag(SetSuggestionsTags.option(option)),
                title = stringResource(option.title),
                subtitle = option.subtitle(LocalContext.current),
                trailing = { AppCheck(checked = option == selected) },
                onClick = { onPick(option) },
            )
        }
        if (after != null) items(count = 1, key = { "$key-after" }) { after() }
    }
}

object SetSuggestionsTags {
    const val How = "set_suggestions_how"
    const val HowDone = "set_suggestions_how_done"
    const val SendFeedback = "set_suggestions_send_feedback"

    fun option(option: SetSuggestion) = "set_suggestions_${option.name}"
}

@Preview
@Composable
private fun SetSuggestionsPreview() {
    Theme {
        SetSuggestionsContent(
            state = SetSuggestionsState(selected = SetSuggestion.LastTime, unit = WeightUnit.KILOGRAM),
            onPick = {},
            onShowHow = {},
            onSendFeedback = {},
        )
    }
}
