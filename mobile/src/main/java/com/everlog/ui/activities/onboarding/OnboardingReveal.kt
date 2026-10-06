package com.everlog.ui.activities.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.everlog.R
import com.everlog.data.controllers.starterroutines.BuildStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.StarterProfile
import com.everlog.data.controllers.starterroutines.StarterRoutine
import com.everlog.data.controllers.starterroutines.StarterWeek
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppBarHeader
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.rememberAppBarScrollBehavior
import com.everlog.ui.design.theme.Theme

// The reveal from the Everlog Onboarding design: "Here's your week", the summary of the answers,
// then a card per routine. The first card starts open with its exercises, the others closed with a
// line about them, and any card opens or closes on a tap. Both buttons close the screen for now.
@Composable
internal fun OnboardingReveal(
    state: OnboardingViewModel.State,
    onRoutineToggle: (Int) -> Unit,
    onDone: () -> Unit,
) {
    val starter = state.starter ?: return
    val profile = OnboardingQuestions.starterProfile(state.answers)
    val experienced = profile.experience == StarterProfile.Experience.YEARS

    // The cards come in once, not again after e.g. rotating
    var revealShown by rememberSaveable { mutableStateOf(false) }
    val animateIn = remember { !revealShown }
    LaunchedEffect(Unit) { revealShown = true }

    val scrollState = rememberScrollState()
    // The header scrolls away with the cards, but only when they don't fit
    val appBarScrollBehavior = rememberAppBarScrollBehavior(canScroll = { scrollState.canScrollForward || scrollState.canScrollBackward })
    AppScreen(
        modifier = Modifier.nestedScroll(appBarScrollBehavior.nestedScrollConnection),
        topBar = {
            AppBar(
                header = {
                    AppBarHeader(
                        title = stringResource(R.string.onboarding_reveal_title),
                        body = onboardingSummary(state.answers, split = starter.week.split),
                    )
                },
                scrollBehavior = appBarScrollBehavior,
            )
        },
        footer = {
            AppFooter(
                header = if (experienced) stringResource(R.string.onboarding_reveal_experienced) else null,
                actions = listOf(
                    AppFooterAction(
                        text = stringResource(if (experienced) R.string.onboarding_reveal_add_own else R.string.onboarding_reveal_make_it_yours),
                        onClick = onDone,
                    ),
                    AppFooterAction(
                        text = stringResource(if (experienced) R.string.onboarding_reveal_use_this else R.string.onboarding_reveal_looks_good),
                        onClick = onDone,
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
            .padding(horizontal = Theme.spacing.large)
            .padding(bottom = bottomPadding + Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
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

// A routine's card. Open: its name and days over a row per exercise. Closed: its name over its
// days and size, with the first three exercises' images and a chevron.
@Composable
private fun RoutineCard(
    routine: StarterRoutine,
    imageUrls: List<String?>,
    // From the library, as the rest of the app shows them. The template's names are the fallback.
    names: List<String?>,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val days = routine.days.joinToString(stringResource(R.string.onboarding_building_separator)) { it.shortName() }
    val closedSummary = listOf(
        days,
        pluralStringResource(R.plurals.onboarding_reveal_exercises, routine.exercises.size, routine.exercises.size),
        routine.exercises.sumOf { it.sets }.let { pluralStringResource(R.plurals.onboarding_reveal_sets, it, it) },
    ).joinToString(stringResource(R.string.onboarding_building_separator))
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        contentPadding = PaddingValues(),
    ) {
        Column(modifier = Modifier.animateContentSize(tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard))) {
            RoutineCardHeader(
                name = routine.name,
                days = days,
                closedSummary = closedSummary,
                imageUrls = imageUrls.take(RevealSizes.StackCount),
                open = open,
                onToggle = onToggle,
            )
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(tween(OnboardingMotion.Expand, OnboardingMotion.ExpandDelay, OnboardingMotion.EmphasizedDecelerate)) +
                        expandVertically(tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard)),
                exit = fadeOut(tween(OnboardingMotion.FadeOut, easing = OnboardingMotion.EmphasizedAccelerate)) +
                        shrinkVertically(tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard)),
            ) {
                Column {
                    routine.exercises.forEachIndexed { index, exercise ->
                        // Between rows only, as the header leads straight into the first
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Theme.spacing.large),
                                thickness = 1.dp,
                                color = Theme.backgrounds.separator,
                            )
                        }
                        ExerciseRow(
                            name = names.getOrNull(index) ?: exercise.name,
                            imageUrl = imageUrls.getOrNull(index),
                            target = setsTarget(exercise),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCardHeader(
    name: String,
    days: String,
    closedSummary: String,
    imageUrls: List<String?>,
    open: Boolean,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard),
        label = "chevron",
    )
    val stateDescription = stringResource(if (open) R.string.onboarding_reveal_open else R.string.onboarding_reveal_closed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { this.stateDescription = stateDescription }
            .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = name, style = Theme.typography.heading)
            AnimatedVisibility(
                visible = !open,
                enter = fadeIn(tween(OnboardingMotion.SummaryFade, OnboardingMotion.SummaryDelay)) + expandVertically(tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard)),
                exit = fadeOut(tween(OnboardingMotion.FadeOut)) + shrinkVertically(tween(OnboardingMotion.Collapse, easing = OnboardingMotion.Standard)),
            ) {
                AppText(
                    text = closedSummary,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            }
        }
        // Open: the days, as the rows say the rest. Closed: a peek at the exercises.
        AnimatedContent(
            targetState = open,
            transitionSpec = {
                fadeIn(tween(OnboardingMotion.SummaryFade, easing = OnboardingMotion.Standard)) togetherWith
                        fadeOut(tween(OnboardingMotion.FadeOut, easing = OnboardingMotion.Standard))
            },
            contentAlignment = Alignment.CenterEnd,
            label = "headerEnd",
        ) { showDays ->
            if (showDays) {
                AppText(
                    text = days,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            } else {
                ThumbnailStack(imageUrls = imageUrls)
            }
        }
        AppIcon(
            modifier = Modifier.rotate(chevronRotation),
            imageVector = ImageVector.vectorResource(R.drawable.ic_keyboard_down),
            tint = Theme.contentColors.secondary,
        )
    }
}

// An exercise in an open card: its image, name, and sets × reps. Denser than an AppListItem, so a
// day's exercises fit with the other cards still in view.
@Composable
private fun ExerciseRow(
    name: String,
    imageUrl: String?,
    target: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .heightIn(min = RevealSizes.ExerciseRowHeight)
            .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        ExerciseThumbnail(imageUrl = imageUrl, size = RevealSizes.Thumbnail)
        AppText(
            modifier = Modifier.weight(1f),
            text = name,
        )
        AppText(
            text = target,
            style = Theme.typography.caption,
            color = Theme.contentColors.secondary,
        )
    }
}

// The first few exercises' images, overlapping, each ringed in the card's colour so they read apart
@Composable
private fun ThumbnailStack(imageUrls: List<String?>) {
    Row(horizontalArrangement = Arrangement.spacedBy(-RevealSizes.StackOverlap)) {
        imageUrls.forEach { imageUrl ->
            ExerciseThumbnail(
                modifier = Modifier.border(RevealSizes.StackRing, Theme.backgrounds.surface, CircleShape),
                imageUrl = imageUrl,
                size = RevealSizes.StackThumbnail,
            )
        }
    }
}

// The library's exercise images are a grey figure on white, so a missing one is a plain white disc
@Composable
private fun ExerciseThumbnail(
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Theme.contentColors.primary),
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
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

// Sizes from the design that the design system doesn't have
private object RevealSizes {
    val ExerciseRowHeight = 52.dp
    val Thumbnail = 36.dp
    val StackThumbnail = 30.dp
    val StackOverlap = 10.dp
    val StackRing = 2.dp
    const val StackCount = 3
}
