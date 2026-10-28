package com.everlog.ui.activities.templateeditor

import com.everlog.config.AppConfig
import com.everlog.data.controllers.templatesession.DefaultReps
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.addRound
import com.everlog.data.controllers.workoutsession.block
import com.everlog.ui.components.exercisetable.secondsTyped

// Prototype: the sets × reps layout's fields, a block's number of sets and one value for all of an exercise's sets

internal sealed interface SummaryFocus {
    val blockId: Long

    data class Sets(override val blockId: Long) : SummaryFocus

    data class Each(override val blockId: Long, val index: Int) : SummaryFocus
}

internal fun WorkoutBlock.exerciseSets(index: Int) = rounds.map { it.sets[index] }.filterNot { it.removed }

internal fun WorkoutBlock.modeOf(index: Int): SetMode = exerciseSets(index).firstOrNull()?.mode ?: SetMode.Reps

// The value every set of the exercise has, or null when they differ
internal fun WorkoutBlock.commonValue(index: Int): Int? {
    val mode = modeOf(index)
    val values = exerciseSets(index).map { if (mode == SetMode.Time) it.timeSeconds else it.reps }.distinct()
    return values.singleOrNull()
}

internal fun SummaryFocus.field(blocks: List<WorkoutBlock>): ValueField = when (this) {
    is SummaryFocus.Sets -> ValueField.Reps
    is SummaryFocus.Each -> if (blocks.block(blockId)?.modeOf(index) == SetMode.Time) ValueField.Time else ValueField.Reps
}

// What backspace starts from
internal fun SummaryFocus.text(blocks: List<WorkoutBlock>): String {
    val block = blocks.block(blockId) ?: return ""
    return when (this) {
        is SummaryFocus.Sets -> block.rounds.size.toString()
        is SummaryFocus.Each -> block.commonValue(index)?.let { if (block.modeOf(index) == SetMode.Time) secondsTyped(it) else it.toString() }.orEmpty()
    }
}

internal fun List<WorkoutBlock>.withSummary(focus: SummaryFocus, value: Int?, ids: SessionIds): List<WorkoutBlock> {
    val block = block(focus.blockId) ?: return this
    return when (focus) {
        is SummaryFocus.Sets -> {
            val count = (value ?: return this).coerceIn(1, AppConfig.configuration.maxExerciseSets)
            if (count < block.rounds.size) {
                map { if (it.id == block.id) it.copy(rounds = it.rounds.take(count)) else it }
            } else {
                (block.rounds.size until count).fold(this) { blocks, _ -> blocks.addRound(block.id, ids) }
            }
        }
        is SummaryFocus.Each -> {
            val mode = block.modeOf(focus.index)
            map { b ->
                if (b.id != block.id) return@map b
                b.copy(rounds = b.rounds.map { round ->
                    round.copy(sets = round.sets.mapIndexed { i, set ->
                        when {
                            i != focus.index || set.removed -> set
                            mode == SetMode.Time -> set.copy(timeSeconds = value)
                            else -> set.copy(reps = value)
                        }
                    })
                })
            }
        }
    }
}

// The fields in order: each block's sets, then its exercises' values
internal fun List<WorkoutBlock>.summaryFields(): List<SummaryFocus> = flatMap { block ->
    listOf(SummaryFocus.Sets(block.id)) + block.exercises.indices.map { SummaryFocus.Each(block.id, it) }
}

internal fun TemplateEditorState.nextSummary(): SummaryFocus? {
    val fields = blocks.summaryFields()
    return fields.getOrNull(fields.indexOf(summary ?: return null) + 1)
}

// Steps of one set, or of the first set's value by a rep or the timer's step
internal fun WorkoutBlock.stepped(focus: SummaryFocus, direction: Int): Int = when (focus) {
    is SummaryFocus.Sets -> rounds.size + direction
    is SummaryFocus.Each -> {
        val first = exerciseSets(focus.index).firstOrNull()
        if (modeOf(focus.index) == SetMode.Time) {
            ((first?.timeSeconds ?: 0) + direction * AppConfig.configuration.defaultRestTimeOffsetSeconds).coerceAtLeast(0)
        } else {
            ((first?.reps ?: DefaultReps) + direction).coerceAtLeast(0)
        }
    }
}
