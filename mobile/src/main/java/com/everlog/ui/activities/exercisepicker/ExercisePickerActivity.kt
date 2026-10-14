package com.everlog.ui.activities.exercisepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
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
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.R
import com.everlog.config.AppConfig
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.exerciselibrary.RealLoadExerciseLibraryUseCase
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.set.ELSetType
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.data.repositories.RealRoutineRepository
import com.everlog.data.repositories.RealWorkoutRepository
import com.everlog.managers.analytics.AnalyticsConstants
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.home.exercise.details.ExerciseDetailsActivity
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppCheck
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.elements.list.items
import com.everlog.ui.design.elements.search.AppSearchBar
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.navigator.ELNavigator
import kotlinx.coroutines.Dispatchers

/**
 * The exercise picker (TAS-451), in place of ExercisesActivity when Remote Config's
 * exercise_picker_v2 is on. Browsing (Home › Exercises), a tap opens an exercise; picking (the
 * template and workout builders), it returns the picks the way ExercisesActivity does, as
 * [ELConstants.EXTRA_EXERCISE_GROUPS]. The spec is on the Everlog canvas (Exercise Library page).
 */
class ExercisePickerActivity : CommonComposeActivity() {
    private val viewModel by viewModels<ExercisePickerViewModel> {
        viewModelFactory {
            initializer {
                val io = Dispatchers.IO
                val exercises = RealExerciseRepository(io)
                ExercisePickerViewModel(
                    dispatcher = Dispatchers.Main,
                    mode = if (intent.getBooleanExtra(ExtraPick, false)) PickerMode.Pick else PickerMode.Browse,
                    savedState = createSavedStateHandle(),
                    loadLibrary = RealLoadExerciseLibraryUseCase(io, exercises, RealRoutineRepository(io), RealWorkoutRepository(io)),
                    ownExerciseUpdates = exercises::ownExerciseUpdates,
                    recentSearches = RealExercisePickerRecentSearches(),
                    analytics = RealExercisePickerAnalytics(),
                    me = FilterValue(
                        id = CreatedByMe,
                        name = getString(R.string.exercise_picker_created_by_me),
                        aliases = resources.getStringArray(R.array.exercise_picker_created_by_me_aliases).toList(),
                    ),
                    maxPicks = AppConfig.configuration.maxExerciseSelection,
                )
            }
        }
    }

    @Composable
    override fun Content() = ExercisePickerScreen(viewModel = viewModel, onResult = ::finishWithPicks)

    // A superset goes through the set type picker first, like ExercisesActivity
    @Deprecated("The navigator opens the set type picker with startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_SET_TYPE && resultCode == RESULT_OK) {
            (data?.getSerializableExtra(ELConstants.EXTRA_SET_TYPE) as? ELSetType)?.let { viewModel.onAdd(it) }
        }
    }

    // As ExercisesActivity's PresenterExercises.finishSelection: one group per exercise, or one
    // group for all of them when linking them as a superset or another multi-exercise set
    private fun finishWithPicks(result: PickerResult) {
        val link = result.setType != ELSetType.SINGLE
        val groups = ArrayList<ELExerciseGroup>()
        var group = ELExerciseGroup.buildDefault(result.setType)
        result.exercises.forEach { exercise ->
            group.exercises.add(ELRoutineExercise.buildRoutineExercise(exercise.toELExercise()))
            if (!link) {
                groups.add(group)
                group = ELExerciseGroup.buildDefault(result.setType)
            }
        }
        if (link) groups.add(group)
        setResult(Activity.RESULT_OK, Intent().putExtra(ELConstants.EXTRA_EXERCISE_GROUPS, groups))
        finish()
    }

    companion object {
        private const val ExtraPick = "pick"

        @JvmStatic
        fun launchIntent(context: Context, pick: Boolean): Intent =
            Intent(context, ExercisePickerActivity::class.java).putExtra(ExtraPick, pick)
    }
}

// The exercise as the rest of the app knows it
internal fun LibraryExercise.toELExercise() = ELExercise(
    uuid = uuid,
    name = name,
    category = category,
    splitSides = splitSides,
    createdDate = createdDate,
    imageUrl = imageUrl,
    createdByUserId = createdByUserId,
    privateToUser = own,
)

@Composable
internal fun ExercisePickerScreen(viewModel: ExercisePickerViewModel, onResult: (PickerResult) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = state.content
    val index = state.index
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val pick = state.mode == PickerMode.Pick
    val closeMenu = {
        focusManager.clearFocus()
        viewModel.onCloseMenu()
    }
    val openDetails: (LibraryExercise) -> Unit = { exercise ->
        ELNavigator(context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise.toELExercise()))
    }

    // On resume, before any of the screen's events, so they're credited to it
    LifecycleResumeEffect(state.mode) {
        AnalyticsManager.manager.screenName(
            if (pick) AnalyticsConstants.SCREEN_EXERCISE_PICKER_V2 else AnalyticsConstants.SCREEN_EXERCISES_V2
        )
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
                // Opens the create screen; the new exercise then shows up in the list by itself
                actions = {
                    AppIconButton(
                        icon = ImageVector.vectorResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.exercise_picker_create_exercise),
                        onClick = {
                            viewModel.onCreateTapped("plus")
                            ELNavigator(context).openEditExercise(null)
                        },
                    )
                },
            )
        },
        footer = {
            val count = state.picked.size
            // The last count stays while the footer slides away, so its buttons don't read "Add 0"
            var shownCount by remember { mutableIntStateOf(count) }
            if (count > 0) shownCount = count
            // From the first pick, like the current picker; a superset needs two
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
                            // Picks the kind of set first (superset, giant set…), like the current picker
                            onClick = { ELNavigator(context).openSetTypePicker(shownCount) },
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
                                if (!pick) openDetails(row.exercise)
                            },
                            onLongPress = { row -> if (pick) openDetails(row.exercise) },
                            onToggleShowAll = viewModel::onToggleShowAll,
                            onCreate = {
                                viewModel.onCreateTapped("search")
                                ELNavigator(context).openEditExercise(null)
                            },
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
