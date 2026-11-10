package com.everlog.android.onboarding

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.everlog.android.common.compose.elements.AppBar
import com.everlog.android.common.compose.elements.AppBarHeader
import com.everlog.android.common.compose.elements.AppExpandableCard
import com.everlog.android.common.compose.elements.AppExpandableCardRow
import com.everlog.android.common.compose.elements.AppFooter
import com.everlog.android.common.compose.elements.AppFooterAction
import com.everlog.android.common.compose.elements.AppScreen
import com.everlog.android.common.compose.elements.rememberAppBarScrollBehavior
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.onboarding.R
import com.everlog.android.onboarding.data.BuildStarterRoutinesUseCase
import com.everlog.android.onboarding.data.StarterProfile
import com.everlog.android.onboarding.data.StarterRoutine
import com.everlog.android.onboarding.data.StarterWeek

// The reveal from the Everlog Onboarding design: "Here are your workout templates", the summary of
// the answers, then a card per routine. The first card starts open with its exercises, the others
// closed with a line about them, and any card opens or closes on a tap. The routines aren't tied to
// days of the week, so no days show. Looks good saves them, and Build my own template opens the
// routine builder; the screen closes once it saves a routine.
@Composable
internal fun OnboardingReveal(
    state: OnboardingState,
    onRoutineToggle: (Int) -> Unit,
    onLooksGood: () -> Unit,
    onBuildOwn: () -> Unit,
) {
    val starter = state.starter ?: return
    val profile = OnboardingQuestions.starterProfile(state.answers)
    val experienced = profile.experience == StarterProfile.Experience.YEARS

    // The cards come in once, not again after e.g. rotating
    var revealShown by rememberSaveable { mutableStateOf(false) }
    val animateIn = remember { !revealShown }
    LaunchedEffect(Unit) { revealShown = true }

    val scrollState = rememberScrollState()
    // The header scrolls away with the cards, but only when they don't fit, and its title stays in the bar
    val appBarScrollBehavior = rememberAppBarScrollBehavior(canScroll = { scrollState.canScrollForward || scrollState.canScrollBackward })
    val title = pluralStringResource(R.plurals.onboarding_reveal_title, starter.routines.size)
    AppScreen(
        modifier = Modifier.nestedScroll(appBarScrollBehavior.nestedScrollConnection),
        topBar = {
            AppBar(
                title = title,
                header = {
                    AppBarHeader(
                        title = title,
                        body = onboardingSummary(state.answers, split = starter.week.split),
                    )
                },
                scrollBehavior = appBarScrollBehavior,
            )
        },
        footer = {
            AppFooter(
                // Everyone hears the templates can be edited later. Experienced users also hear
                // they're only a starting point.
                header = pluralStringResource(
                    if (experienced) R.plurals.onboarding_reveal_experienced else R.plurals.onboarding_reveal_edit_later,
                    starter.routines.size,
                ),
                actions = listOf(
                    AppFooterAction(
                        text = stringResource(R.string.onboarding_reveal_looks_good),
                        onClick = onLooksGood,
                    ),
                    AppFooterAction(
                        text = stringResource(R.string.onboarding_reveal_build_own),
                        onClick = onBuildOwn,
                        style = AppFooterAction.Style.Secondary,
                    ),
                ),
            )
        },
    ) { contentPadding ->
        RevealRoutines(
            starter = starter,
            openRoutines = state.openRoutines,
            animateIn = animateIn,
            scrollState = scrollState,
            bottomPadding = contentPadding.calculateBottomPadding(),
            onRoutineToggle = onRoutineToggle,
        )
    }
}

@Composable
private fun RevealRoutines(
    starter: BuildStarterRoutinesUseCase.Result,
    openRoutines: Set<Int>,
    animateIn: Boolean,
    scrollState: ScrollState,
    bottomPadding: Dp,
    onRoutineToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            // The same padding and gaps as AppGroupedList: each routine is a group
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.large, bottom = bottomPadding + Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        starter.week.routines.forEachIndexed { index, routine ->
            // Each card a beat after the one above it
            EnterAnimation(animateIn = animateIn, delayMillis = RevealMotion.CardStagger * index) {
                RoutineCard(
                    routine = routine,
                    // The library's images for the exercises, in the same order
                    imageUrls = starter.routines.getOrNull(index)?.exerciseGroups.orEmpty().map { group ->
                        group.exercises.firstOrNull()?.exercise?.imageUrl
                    },
                    names = starter.routines.getOrNull(index)?.exerciseGroups.orEmpty().map { group ->
                        group.exercises.firstOrNull()?.exercise?.name
                    },
                    open = index in openRoutines,
                    onToggle = { onRoutineToggle(index) },
                )
            }
        }
    }
}

// A routine's card: its name over its size and the first exercises' images, opening to a row per exercise
@Composable
private fun RoutineCard(
    routine: StarterRoutine,
    imageUrls: List<String?>,
    // From the library, as the rest of the app shows them
    names: List<String?>,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val closedSummary = listOf(
        pluralStringResource(R.plurals.onboarding_reveal_exercises, routine.exercises.size, routine.exercises.size),
        routine.exercises.sumOf { it.sets }.let { pluralStringResource(R.plurals.onboarding_reveal_sets, it, it) },
    ).joinToString(stringResource(R.string.onboarding_building_separator))
    AppExpandableCard(
        title = routine.name,
        // Open, the rows say the rest
        subtitle = closedSummary.takeUnless { open },
        rows = routine.exercises.mapIndexed { index, exercise ->
            AppExpandableCardRow(
                title = names.getOrNull(index).orEmpty(),
                imageUrl = imageUrls.getOrNull(index),
                trailing = setsTarget(exercise),
            )
        },
        open = open,
        onToggle = onToggle,
    )
}

// e.g. 3 × 8–12, or 3 × 30 s for a hold
@Composable
private fun setsTarget(exercise: StarterRoutine.Exercise): String = when (val target = exercise.target) {
    is StarterRoutine.Target.Reps -> stringResource(R.string.onboarding_reveal_reps, exercise.sets, target.range.first, target.range.last)
    is StarterRoutine.Target.Time -> stringResource(R.string.onboarding_reveal_hold, exercise.sets, target.seconds)
}

@Composable
internal fun StarterWeek.Split.label(): String = stringResource(
    when (this) {
        StarterWeek.Split.FULL_BODY -> R.string.onboarding_reveal_split_full_body
        StarterWeek.Split.UPPER_LOWER -> R.string.onboarding_reveal_split_upper_lower
        StarterWeek.Split.PUSH_PULL_LEGS -> R.string.onboarding_reveal_split_push_pull_legs
    }
)

private object RevealMotion {
    // Each card enters this long after the one above it
    const val CardStagger = 60
}
