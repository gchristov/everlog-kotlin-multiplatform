package com.everlog.ui.activities.exercisepicker

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.android.common.compose.elements.AppBar
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppCheck
import com.everlog.android.common.compose.elements.AppFooter
import com.everlog.android.common.compose.elements.AppFooterAction
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppOverflowMenu
import com.everlog.android.common.compose.elements.AppScreen
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppTertiaryButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.elements.list.AppGroupedList
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.elements.list.items
import com.everlog.android.common.compose.elements.search.AppSearchBar
import com.everlog.android.common.compose.theme.Theme
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.set.ELSetType
import com.everlog.android.common.design.R as DesignR

@Composable
internal fun ExercisePickerScreen(
    viewModel: ExercisePickerViewModel,
    onResult: (PickerResult) -> Unit,
    onOpenExercise: (LibraryExercise) -> Unit,
    // Named what was searched for, or nothing
    onCreateExercise: (String?) -> Unit,
    // Superset, giant set and so on, for this many exercises
    onPickSetType: (Int) -> Unit,
    // Null when there's no feedback form, which leaves out the menu item and the no-results link
    onSendFeedback: (() -> Unit)? = null,
    onReportMissing: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = state.content
    val index = state.index
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val pick = state.mode == PickerMode.Pick
    val closeMenu = {
        focusManager.clearFocus()
        viewModel.onCloseMenu()
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    LaunchedEffect(state.result) { state.result?.let(onResult) }
    BackHandler(enabled = state.menu != PickerMenu.Closed, onBack = closeMenu)
    PickerMessages(message = state.message, onShown = viewModel::onMessageShown)

    AppScreen(
        topBar = {
            AppBar(
                title = stringResource(if (pick) R.string.exercise_picker_title else R.string.exercise_picker_title_browse),
                showBack = true,
                // Create opens the create screen; the new exercise then shows up in the list by itself.
                actions = {
                    AppOverflowMenu(
                        items = listOfNotNull(
                            AppMenuItem(
                                text = stringResource(R.string.exercise_picker_create_exercise),
                                icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
                                onClick = {
                                    viewModel.onCreateTapped("menu")
                                    onCreateExercise(null)
                                },
                            ),
                            onSendFeedback?.let {
                                AppMenuItem(
                                    text = stringResource(R.string.feedback_send),
                                    icon = ImageVector.vectorResource(R.drawable.ic_feedback),
                                    onClick = it,
                                )
                            },
                        ),
                    )
                },
            )
        },
        footer = {
            val count = state.picked.size
            // The last count stays while the footer slides away, so its buttons don't read "Add 0"
            var shownCount by remember { mutableIntStateOf(count) }
            if (count > 0) shownCount = count
            // From the first pick; a superset needs two
            AnimatedVisibility(
                visible = pick && count > 0,
                enter = slideInVertically(tween(FooterMotion.In)) { it } + fadeIn(tween(FooterMotion.In)),
                exit = slideOutVertically(tween(FooterMotion.Out)) { it } + fadeOut(tween(FooterMotion.Out)),
            ) {
                AppFooter(
                    header = stringResource(R.string.exercise_picker_details_hint),
                    actions = listOf(
                        AppFooterAction(
                            text = pluralStringResource(R.plurals.exercise_picker_add, shownCount, shownCount),
                            onClick = { viewModel.onAdd(ELSetType.SINGLE) },
                        ),
                        AppFooterAction(
                            text = stringResource(R.string.exercise_picker_add_superset),
                            onClick = { onPickSetType(shownCount) },
                            enabled = shownCount >= 2,
                            style = AppFooterAction.Style.Secondary,
                        ),
                    ),
                )
            }
        },
    ) { contentPadding ->
        // The menu keeps above the keyboard. AppScreen counts the navigation bar as handled, so
        // imePadding() would leave out its height, but this content runs behind it: pad by the
        // keyboard's whole height instead.
        val keyboard = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
        Column(modifier = Modifier.fillMaxSize().padding(bottom = keyboard)) {
            AppSearchBar(
                modifier = Modifier.padding(horizontal = Theme.spacing.large),
                text = state.text,
                onTextChange = viewModel::onTextChange,
                placeholder = when {
                    content == null -> stringResource(R.string.exercise_picker_search_loading)
                    state.tokens.isEmpty() -> stringResource(R.string.exercise_picker_search, content.total)
                    else -> stringResource(R.string.exercise_picker_search_more)
                },
                enabled = content != null,
                focusRequester = focusRequester,
                onFocused = viewModel::onSearchFocused,
                onSearch = closeMenu,
            ) {
                if (index != null) {
                    state.tokens.forEach { token ->
                        FilterTokenView(
                            token = token,
                            index = index,
                            onEdit = { viewModel.onOpenKey(token.key, source = "token") },
                            onToggleOperator = { viewModel.onToggleOperator(token.key) },
                            onRemove = { viewModel.onRemoveToken(token.key) },
                        )
                    }
                }
            }
            when {
                state.failed -> LoadFailed(onRetry = viewModel::onRetry)
                // Also while the first list is worked out
                content == null || index == null -> PickerSkeleton()
                else -> Box(modifier = Modifier.weight(1f)) {
                    Column {
                        PickerShortcuts(
                            modifier = Modifier.padding(top = Theme.spacing.medium),
                            keys = content.shortcuts,
                            showClearAll = state.tokens.isNotEmpty(),
                            onOpenKey = { viewModel.onOpenKey(it, source = "chip") },
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
                            onTap = { row, section, position ->
                                viewModel.onExerciseTapped(row.exercise.uuid, section.analyticsName, position)
                                if (!pick) onOpenExercise(row.exercise)
                            },
                            onLongPress = { row -> if (pick) onOpenExercise(row.exercise) },
                            onToggleShowAll = viewModel::onToggleShowAll,
                            onCreate = {
                                viewModel.onCreateTapped("search")
                                onCreateExercise(state.text.trim())
                            },
                            onReportMissing = onReportMissing,
                        )
                    }
                    content.menu?.let { menu ->
                        PickerScrim(onClick = closeMenu)
                        PickerMenuCard(
                            // Kept clear of the keyboard when the menu fills the space above it
                            modifier = Modifier.padding(
                                start = Theme.spacing.large,
                                end = Theme.spacing.large,
                                top = Theme.spacing.small,
                                bottom = Theme.spacing.large,
                            ),
                            menu = menu,
                            index = index,
                            onRecent = {
                                focusManager.clearFocus()
                                viewModel.onRecent(it)
                            },
                            onOpenKey = { viewModel.onOpenKey(it, source = "menu") },
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
    onTap: (PickerRow, PickerSection, Int) -> Unit,
    onLongPress: (PickerRow) -> Unit,
    onToggleShowAll: () -> Unit,
    onCreate: () -> Unit,
    onReportMissing: (() -> Unit)?,
) {
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
                items(count = section.rows.size, key = { section.rows[it].exercise.uuid }) { position ->
                    val row = section.rows[position]
                    AppListItem(
                        modifier = Modifier.combinedClickable(onClick = { onTap(row, section, position) }, onLongClick = { onLongPress(row) }),
                        title = row.exercise.name,
                        subtitle = when {
                            section is PickerSection.Top && section.kind == PickerMode.Pick ->
                                pluralStringResource(R.plurals.exercise_picker_in_templates, row.templates, row.templates)

                            section is PickerSection.Top -> row.lastDone?.let {
                                DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
                            }

                            else -> row.details.ifEmpty { null }
                        },
                        leading = { AppThumbnail(imageUrl = row.exercise.imageUrl) },
                        // Browsing, rows end in nothing: a chevron is an iOS pattern
                        trailing = if (pick) {
                            { AppCheck(checked = row.picked) }
                        } else {
                            null
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
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onCreate,
                                text = stringResource(R.string.exercise_picker_create, text),
                            )
                        }
                        if (onReportMissing != null) {
                            AppTertiaryButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onReportMissing,
                                text = stringResource(R.string.exercise_picker_missing),
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
    }
    LaunchedEffect(message) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}

private object FooterMotion {
    const val In = 250
    const val Out = 200
}
