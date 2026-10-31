package com.everlog.android.template.feature.editor

import com.everlog.android.common.design.R as DesignR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.components.reorder.ReorderGrip
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.common.kotlin.format.setTypeTitle
import com.everlog.android.common.session.SetMode
import com.everlog.android.common.session.SetRef
import com.everlog.android.common.session.WorkoutBlock
import com.everlog.android.common.session.WorkoutSet
import com.everlog.android.common.sessionui.exercisetable.CellFocus
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.common.sessionui.exercisetable.GroupThumbnail
import com.everlog.android.common.sessionui.exercisetable.GroupThumbnailSizes
import com.everlog.android.common.sessionui.exercisetable.InsetDeleteButton
import com.everlog.android.common.sessionui.exercisetable.countField
import com.everlog.android.common.sessionui.exercisetable.typedSeconds
import com.everlog.android.template.feature.R

private object ExerciseSizes {
    val ThumbnailCorner = 12.dp
    val GroupRowGap = 6.dp
    // As the workout table's
    val RowHeight = 52.dp
    val RowPadding = 12.dp
    val SetColumn = 32.dp
    val Cell = 72.dp
    val ColumnGap = 8.dp
    val FocusOutline = 1.5.dp
}

// Between the grip and the card, which the card, its labels and the buttons under it sit past
private val GripGap = 8.dp

@Composable
internal fun cardIndent(): Dp = Theme.sizes.leading + GripGap

/**
 * One exercise, or a superset or circuit, as the workout screen shows it: its grip, image and name, then SET and REPS
 * (or TIME) over a card of its sets, each with its value and a bin.
 *
 * @param index Its place in the list, for test tags
 * @param collapsed Its name only, while an exercise is dragged
 * @param lifted The one being dragged
 * @param grip Drags it
 * @param keepClear How far above the list's bottom a set being typed into is kept, clear of the keypad, in pixels
 */
@Composable
internal fun TemplateExercise(
    index: Int,
    block: WorkoutBlock,
    state: TemplateEditorState,
    viewModel: TemplateEditorViewModel,
    collapsed: Boolean,
    lifted: Boolean,
    grip: Modifier,
    keepClear: Float,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ExerciseHeader(block, grip, collapsed, lifted, onOpenExercise)
        if (collapsed) return@Column
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        ColumnLabels(block, Modifier.padding(start = cardIndent()))
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
        Column(
            modifier = Modifier
                .padding(start = cardIndent())
                .fillMaxWidth()
                .clip(Theme.shapes.surface)
                .background(Theme.backgrounds.surface),
        ) {
            // A divider between rounds, none between a superset's exercises in one
            block.rounds.forEachIndexed { r, round ->
                val firstShown = round.sets.indexOfFirst { !it.removed }
                round.sets.forEachIndexed { i, set ->
                    if (set.removed) return@forEachIndexed
                    key(round.id, i) {
                        if (r > 0 && i == firstShown) HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                        SetRow(
                            place = SetPlace(index, r, i),
                            ref = SetRef(block.id, round.id, i),
                            number = if (round.sets.size > 1) "${r + 1}${'a' + i}" else "${r + 1}",
                            set = set,
                            state = state,
                            viewModel = viewModel,
                            keepClear = keepClear,
                        )
                    }
                }
            }
        }
    }
}

// The grip on the left, beside the last exercise as the workout's exercise menu is, then the image and name, or a
// group's type over its exercises. While dragged, a card of the names alone.
@Composable
private fun ExerciseHeader(
    block: WorkoutBlock,
    grip: Modifier,
    collapsed: Boolean,
    lifted: Boolean,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
) {
    val context = LocalContext.current
    val group = block.exercises.size > 1
    val shape = Theme.shapes.surface
    Row(verticalAlignment = Alignment.CenterVertically) {
        ReorderGrip(modifier = grip.alignBy { it.measuredHeight / 2 })
        Spacer(modifier = Modifier.width(Theme.spacing.large))
        Column(
            modifier = Modifier
                .weight(1f)
                .alignBy(LastExerciseCenter)
                .then(
                    when {
                        lifted -> Modifier
                            .clip(shape)
                            .background(Theme.backgrounds.surfaceRaised)
                            .border(1.dp, Theme.contentColors.action, shape)
                        collapsed -> Modifier
                            .clip(shape)
                            .background(Theme.backgrounds.surface)
                        else -> Modifier
                    },
                )
                // A single exercise's card is the grip's height, so the two line up
                .heightIn(min = if (collapsed) Theme.sizes.leading else 0.dp)
                .padding(
                    start = if (collapsed) ExerciseSizes.RowPadding else 0.dp,
                    end = ExerciseSizes.RowPadding,
                    top = if (collapsed && group) Theme.spacing.small else 0.dp,
                    bottom = if (collapsed && group) Theme.spacing.small else 0.dp,
                ),
            verticalArrangement = if (group) Arrangement.spacedBy(ExerciseSizes.GroupRowGap) else Arrangement.Center,
        ) {
            if (group) {
                AppText(
                    // Lined up with the names, past the images
                    modifier = Modifier.padding(start = if (collapsed) 0.dp else GroupThumbnailSizes.Size + Theme.spacing.medium),
                    text = block.setType?.let { setTypeTitle(context, it.name) }?.uppercase().orEmpty(),
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                )
            }
            block.exercises.forEachIndexed { i, exercise ->
                val corner = if (group) GroupThumbnailSizes.Corner else ExerciseSizes.ThumbnailCorner
                Row(
                    modifier = Modifier
                        .then(if (i == block.exercises.lastIndex) Modifier.providingCenter(LastExerciseCenter) else Modifier)
                        // The press shows with the image's corners
                        .clip(RoundedCornerShape(corner))
                        .clickable(enabled = exercise.exercise != null && !collapsed) {
                            exercise.exercise?.let { onOpenExercise(it, ExerciseTab.Info) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                ) {
                    when {
                        collapsed -> Unit
                        group -> GroupThumbnail(exercise.exercise?.imageUrl, 'a' + i)
                        else -> AppThumbnail(imageUrl = exercise.exercise?.imageUrl, size = Theme.sizes.leading, corner = corner)
                    }
                    AppText(
                        text = exercise.exercise?.name.orEmpty(),
                        style = Theme.typography.bodyBold,
                        maxLines = if (collapsed || group) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnLabels(block: WorkoutBlock, modifier: Modifier) {
    val modes = block.rounds.flatMap { round -> round.sets.filterNot { it.removed }.map { it.mode } }.toSet()
    val count = when {
        modes == setOf(SetMode.Time) -> stringResource(DesignR.string.workout_log_column_time)
        SetMode.Time in modes -> stringResource(DesignR.string.workout_log_column_reps_time)
        else -> stringResource(DesignR.string.workout_log_column_reps)
    }
    Row(
        modifier = modifier.padding(horizontal = ExerciseSizes.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(ExerciseSizes.ColumnGap),
    ) {
        ColumnLabel(stringResource(DesignR.string.workout_log_column_set), Modifier.width(ExerciseSizes.SetColumn))
        ColumnLabel(count, Modifier.width(ExerciseSizes.Cell))
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier) {
    AppText(modifier = modifier, text = text, style = Theme.typography.small, color = Theme.contentColors.secondary, textAlign = TextAlign.Center, maxLines = 1)
}

// Where a set's row is, for its test tags: its exercise in the list, its round, and its exercise in the round
private data class SetPlace(val exercise: Int, val round: Int, val inRound: Int)

// The set's number, its reps or time to tap and type, and its bin. The row being typed into is tinted.
@Composable
private fun SetRow(
    place: SetPlace,
    ref: SetRef,
    number: String,
    set: WorkoutSet,
    state: TemplateEditorState,
    viewModel: TemplateEditorViewModel,
    keepClear: Float,
) {
    val cell = CellFocus(ref, countField(set))
    val focused = state.focus == cell
    val selected = state.focus?.ref == ref
    val requester = remember { BringIntoViewRequester() }
    var height by remember { mutableIntStateOf(0) }
    LaunchedEffect(focused, state.reveal) {
        // With the space under it the keypad covers
        if (focused) requester.bringIntoView(Rect(0f, 0f, 1f, height + keepClear))
    }
    Row(
        modifier = Modifier
            .testTag(TemplateEditorTags.set(place.exercise, place.round, place.inRound))
            .fillMaxWidth()
            .height(ExerciseSizes.RowHeight)
            .onSizeChanged { height = it.height }
            .bringIntoViewRequester(requester)
            .background(if (selected) Theme.backgrounds.selected else Theme.backgrounds.surface)
            .padding(start = ExerciseSizes.RowPadding, end = Theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ExerciseSizes.ColumnGap),
    ) {
        AppText(
            modifier = Modifier.width(ExerciseSizes.SetColumn),
            text = number,
            color = Theme.contentColors.secondary,
            textAlign = TextAlign.Center,
        )
        ValueCell(
            modifier = Modifier.testTag(TemplateEditorTags.value(place.exercise, place.round, place.inRound)),
            text = valueText(state, cell, set),
            selected = selected,
            focused = focused,
            onClick = { viewModel.onCellTapped(cell) },
        )
        Spacer(modifier = Modifier.weight(1f))
        InsetDeleteButton(
            modifier = Modifier.testTag(TemplateEditorTags.deleteSet(place.exercise, place.round, place.inRound)),
            onClick = { viewModel.onDeleteSet(ref) },
            contentDescription = stringResource(R.string.template_editor_delete_set),
        )
    }
}

// What's being typed, or else the reps or time
private fun valueText(state: TemplateEditorState, cell: CellFocus, set: WorkoutSet): String {
    val typed = state.typed?.takeIf { state.focus == cell }
    if (typed == "") return ""
    val time = set.mode == SetMode.Time
    val value = if (time) typed?.let { typedSeconds(it) } ?: set.timeSeconds else typed?.toIntOrNull() ?: set.reps
    return when {
        value == null -> "–"
        time -> formatClock(value.toLong())
        else -> value.toString()
    }
}

// As the workout table's cell: the value, in a box on the selected row, outlined while it's typed into
@Composable
private fun ValueCell(modifier: Modifier, text: String, selected: Boolean, focused: Boolean, onClick: () -> Unit) {
    val shape = Theme.shapes.button
    Box(
        modifier = modifier
            .size(ExerciseSizes.Cell, Theme.sizes.rowControl)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(Theme.backgrounds.inset)
                        .border(
                            if (focused) ExerciseSizes.FocusOutline else 1.dp,
                            if (focused) Theme.contentColors.action else Theme.backgrounds.separator,
                            shape,
                        )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.body.copy(fontFeatureSettings = "tnum"), maxLines = 1)
    }
}

// The middle of the header's last exercise, which the grip lines up with
private val LastExerciseCenter = HorizontalAlignmentLine(merger = { _, last -> last })

private fun Modifier.providingCenter(line: HorizontalAlignmentLine) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, mapOf(line to placeable.height / 2)) { placeable.place(0, 0) }
}
