package com.everlog.ui.activities.exercisepicker

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.elements.list.items
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers

// Prototype of the new exercise picker (TAS-451), opened from Settings › Developer in both its modes.
// It doesn't replace ExercisesActivity yet: picking only says what it would add. The spec is on the
// Everlog canvas.
class ExercisePickerActivity : CommonComposeActivity() {
    private val viewModel by viewModels<ExercisePickerViewModel> {
        createViewModelFactory {
            ExercisePickerViewModel(
                dispatcher = Dispatchers.Main,
                mode = if (intent.getBooleanExtra(ExtraPick, false)) PickerMode.Pick else PickerMode.Browse,
                loadLibrary = ::loadPickerLibrary,
            )
        }
    }

    @Composable
    override fun Content() = ExercisePickerScreen(viewModel = viewModel)

    companion object {
        private const val ExtraPick = "pick"

        @JvmStatic
        fun launchIntent(context: Context, pick: Boolean): Intent =
            Intent(context, ExercisePickerActivity::class.java).putExtra(ExtraPick, pick)
    }
}

@Composable
internal fun ExercisePickerScreen(viewModel: ExercisePickerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = remember(state) { state.content() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val pick = state.mode == PickerMode.Pick
    val closeMenu = {
        focusManager.clearFocus()
        viewModel.onCloseMenu()
    }
    val openDetails: (String) -> Unit = { uuid ->
        state.library?.details?.get(uuid)?.let { exercise ->
            ELNavigator(context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise))
        }
    }

    BackHandler(enabled = state.menu != PickerMenu.Closed, onBack = closeMenu)
    PickerMessages(message = state.message, onShown = viewModel::onMessageShown)

    AppScreen(
        topBar = {
            AppBar(
                title = stringResource(if (pick) R.string.exercise_picker_title else R.string.exercise_picker_title_browse),
                showBack = true,
            )
        },
        footer = {
            val count = state.picked.size
            // From the first pick, like the current picker; a superset needs two
            if (pick && count > 0) {
                AppFooter(
                    actions = listOf(
                        AppFooterAction(
                            text = pluralStringResource(R.plurals.exercise_picker_add, count, count),
                            onClick = { viewModel.onAdd(superset = false) },
                        ),
                        AppFooterAction(
                            text = stringResource(R.string.exercise_picker_add_superset),
                            onClick = { viewModel.onAdd(superset = true) },
                            enabled = count >= 2,
                            style = AppFooterAction.Style.Secondary,
                        ),
                    ),
                )
            }
        },
    ) { contentPadding ->
        // The menu keeps above the keyboard
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            PickerSearchBar(
                modifier = Modifier.padding(horizontal = Theme.spacing.large),
                tokens = state.tokens,
                library = state.library,
                text = state.text,
                placeholder = when {
                    content == null -> stringResource(R.string.exercise_picker_search_loading)
                    state.tokens.isEmpty() -> stringResource(R.string.exercise_picker_search, content.total)
                    else -> stringResource(R.string.exercise_picker_search_more)
                },
                enabled = content != null,
                focusRequester = focusRequester,
                onFocused = viewModel::onSearchFocused,
                onTextChange = viewModel::onTextChange,
                onSearchAction = closeMenu,
                onEditToken = viewModel::onOpenKey,
                onToggleOperator = viewModel::onToggleOperator,
                onRemoveToken = viewModel::onRemoveToken,
            )
            when {
                state.loading -> PickerSkeleton()
                state.failed || content == null || state.library == null -> LoadFailed(onRetry = viewModel::onRetry)
                else -> Box(modifier = Modifier.weight(1f)) {
                    Column {
                        PickerShortcuts(
                            modifier = Modifier.padding(top = Theme.spacing.medium),
                            keys = content.shortcuts,
                            showClearAll = state.tokens.isNotEmpty(),
                            onOpenKey = viewModel::onOpenKey,
                            onClearAll = viewModel::onClearAll,
                        )
                        // Keeps the list clear of the chips as it scrolls under them
                        Spacer(modifier = Modifier.height(Theme.spacing.small))
                        PickerList(
                            listKey = state.tokens to state.text.trim(),
                            pick = pick,
                            content = content,
                            text = state.text.trim(),
                            hasTokens = state.tokens.isNotEmpty(),
                            contentPadding = contentPadding,
                            onTap = { uuid -> if (pick) viewModel.onToggleExercise(uuid) else openDetails(uuid) },
                            onLongPress = { uuid -> if (pick) openDetails(uuid) },
                            onToggleShowAll = viewModel::onToggleShowAll,
                        )
                    }
                    content.menu?.let { menu ->
                        PickerScrim(onClick = closeMenu)
                        PickerMenuCard(
                            modifier = Modifier.padding(start = Theme.spacing.large, end = Theme.spacing.large, top = Theme.spacing.small),
                            menu = menu,
                            library = state.library!!,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickerList(
    // A new search starts the list from the top
    listKey: Any,
    pick: Boolean,
    content: PickerContent,
    text: String,
    hasTokens: Boolean,
    contentPadding: PaddingValues,
    onTap: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleShowAll: () -> Unit,
) {
    val context = LocalContext.current
    val mostUsedTitle = stringResource(R.string.exercise_picker_most_used)
    val recentTitle = stringResource(R.string.exercise_picker_recently_done)
    val ownTitle = stringResource(R.string.exercise_picker_own)
    val listState = rememberLazyListState()
    LaunchedEffect(listKey) { listState.scrollToItem(0) }
    AppGroupedList(state = listState, contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
        content.sections.forEach { section ->
            group(
                key = section.key,
                header = {
                    when (section) {
                        is PickerSection.Top -> if (section.kind == PickerMode.Pick) mostUsedTitle else recentTitle
                        is PickerSection.Family -> section.title
                        is PickerSection.Own -> ownTitle
                    }
                },
            ) {
                items(items = section.rows, key = { it.exercise.uuid }) { row ->
                    val uuid = row.exercise.uuid
                    AppListItem(
                        modifier = Modifier.combinedClickable(onClick = { onTap(uuid) }, onLongClick = { onLongPress(uuid) }),
                        title = row.exercise.name,
                        subtitle = when {
                            section is PickerSection.Top && section.kind == PickerMode.Pick ->
                                pluralStringResource(R.plurals.exercise_picker_in_templates, row.exercise.templates, row.exercise.templates)

                            section is PickerSection.Top -> row.exercise.lastDone?.let {
                                DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
                            }

                            else -> row.details.ifEmpty { null }
                        },
                        leading = { ExerciseThumbnail(imageUrl = row.exercise.imageUrl) },
                        trailing = {
                            if (pick) {
                                PickCheck(picked = row.picked)
                            } else {
                                AppIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_right), tint = Theme.contentColors.secondary)
                            }
                        },
                    )
                }
                if (section is PickerSection.Top && (section.expanded || section.total > section.rows.size)) {
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
                        if (text.isNotEmpty()) {
                            AppSecondaryButton(
                                onClick = { ELNavigator(context).openEditExercise(null) },
                                text = stringResource(R.string.exercise_picker_create, text),
                            )
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
