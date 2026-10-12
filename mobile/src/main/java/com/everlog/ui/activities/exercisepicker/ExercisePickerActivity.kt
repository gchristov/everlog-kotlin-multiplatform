package com.everlog.ui.activities.exercisepicker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppCircularProgressIndicator
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.elements.list.items
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers

// Prototype of the new exercise picker (TAS-451), opened from Settings › Developer. It doesn't replace
// ExercisesActivity yet: picking only says what it would add. The spec is on the Everlog canvas.
class ExercisePickerActivity : CommonComposeActivity() {
    private val viewModel by viewModels<ExercisePickerViewModel> {
        createViewModelFactory { ExercisePickerViewModel(dispatcher = Dispatchers.Main, loadLibrary = ::loadPickerLibrary) }
    }

    @Composable
    override fun Content() = ExercisePickerScreen(viewModel = viewModel)
}

@Composable
internal fun ExercisePickerScreen(viewModel: ExercisePickerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = remember(state) { state.content() }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val closeMenu = {
        focusManager.clearFocus()
        viewModel.onCloseMenu()
    }

    BackHandler(enabled = state.menu != PickerMenu.Closed, onBack = closeMenu)
    PickerMessages(message = state.message, onShown = viewModel::onMessageShown)

    AppScreen(
        topBar = { AppBar(title = stringResource(R.string.exercise_picker_title), showBack = true) },
        footer = {
            val count = state.picked.size
            if (count > 0) {
                AppFooter(
                    actions = listOfNotNull(
                        AppFooterAction(
                            text = pluralStringResource(R.plurals.exercise_picker_add, count, count),
                            onClick = { viewModel.onAdd(superset = false) },
                        ),
                        AppFooterAction(
                            text = stringResource(R.string.exercise_picker_add_superset),
                            onClick = { viewModel.onAdd(superset = true) },
                            style = AppFooterAction.Style.Secondary,
                        ).takeIf { count >= 2 },
                    ),
                )
            }
        },
    ) { contentPadding ->
        when {
            state.loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }

            state.failed || content == null || state.library == null -> LoadFailed(onRetry = viewModel::onRetry)

            // The menu keeps above the keyboard
            else -> Column(modifier = Modifier.fillMaxSize().imePadding()) {
                val library = state.library!!
                PickerSearchBar(
                    modifier = Modifier.padding(horizontal = Theme.spacing.large),
                    tokens = state.tokens,
                    library = library,
                    text = state.text,
                    placeholder = if (state.tokens.isEmpty()) {
                        stringResource(R.string.exercise_picker_search, content.total)
                    } else {
                        stringResource(R.string.exercise_picker_search_more)
                    },
                    focusRequester = focusRequester,
                    onFocused = viewModel::onSearchFocused,
                    onTextChange = viewModel::onTextChange,
                    onSearchAction = closeMenu,
                    onEditToken = viewModel::onOpenKey,
                    onToggleOperator = viewModel::onToggleOperator,
                    onRemoveToken = viewModel::onRemoveToken,
                )
                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        PickerShortcuts(
                            modifier = Modifier.padding(top = Theme.spacing.medium),
                            keys = content.shortcuts,
                            showClearAll = state.tokens.isNotEmpty(),
                            onOpenKey = viewModel::onOpenKey,
                            onClearAll = viewModel::onClearAll,
                        )
                        PickerList(
                            listKey = state.tokens to state.text.trim(),
                            content = content,
                            hasText = state.text.isNotBlank(),
                            text = state.text.trim(),
                            hasTokens = state.tokens.isNotEmpty(),
                            contentPadding = contentPadding,
                            onToggleExercise = viewModel::onToggleExercise,
                            onToggleShowAll = viewModel::onToggleShowAll,
                            onClearAll = viewModel::onClearAll,
                        )
                    }
                    content.menu?.let { menu ->
                        PickerScrim(onClick = closeMenu)
                        PickerMenuCard(
                            modifier = Modifier.padding(start = Theme.spacing.large, end = Theme.spacing.large, top = Theme.spacing.small),
                            menu = menu,
                            library = library,
                            onRecent = { focusManager.clearFocus(); viewModel.onRecent(it) },
                            onOpenKey = viewModel::onOpenKey,
                            onToggleValue = viewModel::onToggleValue,
                            onSuggestion = { viewModel.onPickSuggestion(it.key, it.value.id) },
                            onDone = closeMenu,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerList(
    // A new search starts the list from the top
    listKey: Any,
    content: PickerContent,
    hasText: Boolean,
    text: String,
    hasTokens: Boolean,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onToggleExercise: (String) -> Unit,
    onToggleShowAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    val mostUsedTitle = stringResource(R.string.exercise_picker_most_used)
    val ownTitle = stringResource(R.string.exercise_picker_own)
    val listState = rememberLazyListState()
    LaunchedEffect(listKey) { listState.scrollToItem(0) }
    AppGroupedList(state = listState, contentPadding = contentPadding) {
        content.sections.forEach { section ->
            group(
                key = section.key,
                header = {
                    when (section) {
                        is PickerSection.MostUsed -> mostUsedTitle
                        is PickerSection.Family -> section.title
                        is PickerSection.Own -> ownTitle
                    }
                },
            ) {
                items(items = section.rows, key = { it.exercise.uuid }) { row ->
                    AppListItem(
                        title = row.exercise.name,
                        subtitle = if (section is PickerSection.MostUsed) {
                            pluralStringResource(R.plurals.exercise_picker_in_templates, row.exercise.templates, row.exercise.templates)
                        } else {
                            row.details.ifEmpty { null }
                        },
                        leading = { ExerciseThumbnail(imageUrl = row.exercise.imageUrl) },
                        trailing = { PickCheck(picked = row.picked) },
                        onClick = { onToggleExercise(row.exercise.uuid) },
                    )
                }
                if (section is PickerSection.MostUsed && (section.expanded || section.total > section.rows.size)) {
                    items(count = 1, key = { "show_all" }) {
                        AppListItem(
                            title = if (section.expanded) {
                                stringResource(R.string.exercise_picker_show_fewer)
                            } else {
                                stringResource(R.string.exercise_picker_show_all, section.total)
                            },
                            titleColor = Theme.contentColors.action,
                            trailing = { ShowAllChevron(expanded = section.expanded) },
                            onClick = onToggleShowAll,
                        )
                    }
                }
            }
        }
        if (content.sections.isEmpty()) {
            item(key = "empty") {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                        AppText(
                            text = if (hasTokens) {
                                stringResource(R.string.exercise_picker_empty_filters)
                            } else {
                                stringResource(R.string.exercise_picker_empty_text, text)
                            },
                            color = Theme.contentColors.secondary,
                        )
                        if (hasText) {
                            AppSecondaryButton(
                                onClick = { ELNavigator(context).openEditExercise(null) },
                                text = stringResource(R.string.exercise_picker_create, text),
                            )
                        }
                        if (hasTokens) {
                            AppTertiaryButton(onClick = onClearAll, text = stringResource(R.string.exercise_picker_clear_filters))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppText(text = stringResource(R.string.exercise_picker_load_failed), color = Theme.contentColors.secondary)
        AppButton(onClick = onRetry, text = stringResource(R.string.exercise_picker_retry))
    }
}

@Composable
private fun PickerMessages(message: PickerMessage?, onShown: () -> Unit) {
    val context = LocalContext.current
    val text = when (message) {
        null -> null
        is PickerMessage.TooMany -> stringResource(R.string.exercises_max_selection, message.max)
        // Prototype only: there's nothing to add them to
        is PickerMessage.Added -> "Would add ${message.count} " + (if (message.superset) "as a superset" else "exercises")
    }
    LaunchedEffect(message) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}
