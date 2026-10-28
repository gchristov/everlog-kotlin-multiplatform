package com.everlog.ui.activities.templateeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.ui.components.exercisetable.countField
import com.everlog.ui.components.exercisetable.typedSeconds
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.format.formatClock

// Prototype: one card per exercise (or group): its sets × reps on a line, typed with the keypad, and each set
// behind Each set. Hardcoded English, not for release.

private object SummarySizes {
    val Thumbnail = 40.dp
    val GroupThumbnail = 24.dp
    val Box = 64.dp
    val BoxHeight = 40.dp
    val Chip = 52.dp
    val ChipHeight = 36.dp
    val FocusOutline = 1.5.dp
}

@Composable
internal fun SetsAndRepsList(
    state: TemplateEditorState,
    viewModel: TemplateEditorViewModel,
    contentPadding: PaddingValues,
    header: (@Composable () -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Theme.spacing.large,
            end = Theme.spacing.large,
            top = Theme.spacing.medium,
            bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large,
        ),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        if (header != null) item(key = "header") { header() }
        items(state.blocks, key = { it.id }) { block -> SummaryCard(block, state, viewModel) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(block: WorkoutBlock, state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    val context = LocalContext.current
    val group = block.exercises.size > 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.surface)
            .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        if (group) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    modifier = Modifier.weight(1f),
                    text = block.setType?.getTitle(context)?.uppercase().orEmpty(),
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                )
                EachSetButton(block, state, viewModel)
            }
        }
        // A single exercise: its name, then sets × reps. A group: its rounds, then each exercise's reps.
        if (!group) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                AppThumbnail(imageUrl = block.exercises.first().exercise?.imageUrl, size = SummarySizes.Thumbnail, corner = 12.dp)
                AppText(
                    modifier = Modifier.weight(1f),
                    text = block.exercises.first().exercise?.name.orEmpty(),
                    style = Theme.typography.bodyBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                EachSetButton(block, state, viewModel)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            val sets = SummaryFocus.Sets(block.id)
            ValueBox(text = boxText(state, sets, block.rounds.size.toString()), focused = state.summary == sets) { viewModel.onSummaryTapped(sets) }
            AppText(text = if (group) "rounds" else if (block.rounds.size == 1) "set" else "sets", color = Theme.contentColors.secondary)
            if (!group) {
                AppText(text = "×", color = Theme.contentColors.secondary)
                EachValue(block, 0, state, viewModel)
            }
        }
        if (group) {
            block.exercises.forEachIndexed { i, exercise ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                    AppThumbnail(imageUrl = exercise.exercise?.imageUrl, size = SummarySizes.GroupThumbnail, corner = 8.dp)
                    AppText(
                        modifier = Modifier.weight(1f),
                        text = "${'a' + i} · ${exercise.exercise?.name.orEmpty()}",
                        style = Theme.typography.bodyBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    EachValue(block, i, state, viewModel)
                }
            }
        }
        if (block.id in state.expanded) {
            block.exercises.forEachIndexed { i, _ ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                    if (group) AppText(modifier = Modifier.align(Alignment.CenterVertically), text = "${'a' + i}", color = Theme.contentColors.secondary)
                    block.rounds.forEach { round ->
                        val set = round.sets[i]
                        if (set.removed) return@forEach
                        val cell = CellFocus(SetRef(block.id, round.id, i), countField(set))
                        SetChip(text = chipText(state, cell, set), focused = state.focus == cell) { viewModel.onCellTapped(cell) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EachSetButton(block: WorkoutBlock, state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    AppTertiaryButton(onClick = { viewModel.onToggleExpanded(block.id) }, text = if (block.id in state.expanded) "Hide sets" else "Each set")
}

// One value for all of an exercise's sets, or Mixed when they differ
@Composable
private fun EachValue(block: WorkoutBlock, index: Int, state: TemplateEditorState, viewModel: TemplateEditorViewModel) {
    val field = SummaryFocus.Each(block.id, index)
    val time = block.modeOf(index) == SetMode.Time
    val value = block.commonValue(index)?.let { if (time) formatClock(it.toLong()) else it.toString() } ?: "Mixed"
    ValueBox(text = boxText(state, field, value, time), focused = state.summary == field) { viewModel.onSummaryTapped(field) }
    AppText(text = if (time) "time" else "reps", color = Theme.contentColors.secondary)
}

private fun boxText(state: TemplateEditorState, field: SummaryFocus, value: String, time: Boolean = false): String {
    val typed = state.typed?.takeIf { state.summary == field } ?: return value
    return if (time) formatClock(typedSeconds(typed).toLong()) else typed
}

private fun chipText(state: TemplateEditorState, cell: CellFocus, set: WorkoutSet): String {
    val time = set.mode == SetMode.Time
    val typed = state.typed?.takeIf { state.focus == cell }
    val value = if (time) typed?.let { typedSeconds(it) } ?: set.timeSeconds else typed?.toIntOrNull() ?: set.reps
    return when {
        typed == "" -> ""
        value == null -> "–"
        time -> formatClock(value.toLong())
        else -> value.toString()
    }
}

@Composable
private fun ValueBox(text: String, focused: Boolean, onClick: () -> Unit) {
    val shape = Theme.shapes.button
    Box(
        modifier = Modifier
            .widthIn(min = SummarySizes.Box)
            .height(SummarySizes.BoxHeight)
            .clip(shape)
            .background(Theme.backgrounds.inset)
            .border(if (focused) SummarySizes.FocusOutline else 1.dp, if (focused) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.body.copy(fontFeatureSettings = "tnum"), maxLines = 1)
    }
}

@Composable
private fun SetChip(text: String, focused: Boolean, onClick: () -> Unit) {
    val shape = Theme.shapes.button
    Box(
        modifier = Modifier
            .size(SummarySizes.Chip, SummarySizes.ChipHeight)
            .clip(shape)
            .background(if (focused) Theme.backgrounds.selected else Theme.backgrounds.inset)
            .border(if (focused) SummarySizes.FocusOutline else 1.dp, if (focused) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.caption.copy(fontFeatureSettings = "tnum"), maxLines = 1)
    }
}
