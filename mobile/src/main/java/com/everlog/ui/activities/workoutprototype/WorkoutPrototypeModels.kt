package com.everlog.ui.activities.workoutprototype

import java.math.BigDecimal

// The workout screen prototype's own models: immutable, so the view-model's state can hold them, and
// in memory only. If the Spine and Dock design is built for real, they map onto ELWorkout and ELSet.

internal enum class SetMode {
    Reps,
    Time,
}

// What a set records: weight plus either reps or time, whichever [mode] says. Both are kept so
// switching the mode back and forth doesn't lose a value.
internal data class SetValues(
    val mode: SetMode,
    val reps: Int,
    val timeSeconds: Int,
    val weightKg: Float,
)

internal data class PrototypeSet(
    val values: SetValues,
    val done: Boolean = false,
    // What the same set was last time, if there is a last time
    val lastTime: SetValues? = null,
)

internal data class PrototypeExercise(
    val id: Long,
    val name: String,
    val imageUrl: String? = null,
)

// A round of a block: one set of each of its exercises, in order. For a single exercise it's just
// its set. The design calls a circuit's rounds sets too.
internal data class PrototypeRound(
    val id: Long,
    val sets: List<PrototypeSet>,
)

internal enum class BlockKind {
    Single,
    Superset,
    Circuit,
}

// One exercise, or exercises done together: two make a superset, more a circuit
internal data class PrototypeBlock(
    val id: Long,
    val exercises: List<PrototypeExercise>,
    val rounds: List<PrototypeRound>,
) {
    val kind: BlockKind
        get() = when (exercises.size) {
            1 -> BlockKind.Single
            2 -> BlockKind.Superset
            else -> BlockKind.Circuit
        }
}

// One exercise's set in one round of a block
internal data class SetRef(
    val blockId: Long,
    val roundId: Long,
    val index: Int,
)

internal data class RestTimer(
    val endsAt: Long,
    val totalSeconds: Int,
)

// A time set's countdown, [totalSeconds] long (for its progress)
internal data class SetTimer(
    val ref: SetRef,
    val endsAt: Long,
    val totalSeconds: Int,
) {
    fun remainingMillis(now: Long): Long = (endsAt - now).coerceAtLeast(0)
}

// A done set being changed in the dock, until Done (or Back)
internal data class EditingSet(
    val ref: SetRef,
    val values: SetValues,
)

internal enum class ValueField {
    Reps,
    Time,
    Weight,
}

internal sealed interface WorkoutDialog {
    data class Finish(val unfinishedSets: Int) : WorkoutDialog
    data class DeleteBlock(val blockId: Long) : WorkoutDialog
    data object Discard : WorkoutDialog
    data object RestTimer : WorkoutDialog
    data class EnterValue(val field: ValueField) : WorkoutDialog
}

// A swiped away set, kept until its Undo goes
internal data class DeletedRound(
    val blockId: Long,
    val position: Int,
    val round: PrototypeRound,
    val current: SetRef?,
)

internal data class WorkoutPrototypeState(
    val name: String,
    // Null until the workout has an exercise, so the empty workout's timer doesn't run
    val startedAt: Long?,
    val now: Long,
    val blocks: List<PrototypeBlock>,
    val current: SetRef?,
    // The one rest for the whole workout. Changing a rest by 10s changes this too.
    val restSeconds: Int = DefaultRestSeconds,
    val rest: RestTimer? = null,
    val setTimer: SetTimer? = null,
    val editing: EditingSet? = null,
    val editMode: Boolean = false,
    val dialog: WorkoutDialog? = null,
    val deletedRound: DeletedRound? = null,
    val message: String? = null,
    // Goes up each time a timer ends, for the screen to vibrate
    val alerts: Int = 0,
    // Goes up each time the current set's row is tapped, for the dock to nudge: that's where it's logged
    val dockNudges: Int = 0,
    val finished: Boolean = false,
) {
    val elapsedSeconds: Long
        get() = startedAt?.let { ((now - it) / 1000).coerceAtLeast(0) } ?: 0

    fun restRemainingSeconds(): Int = rest?.let { ceilSeconds(it.endsAt - now) } ?: 0

    fun block(id: Long): PrototypeBlock? = blocks.firstOrNull { it.id == id }

    fun unfinishedSets(): Int = blocks.sumOf { block -> block.rounds.sumOf { round -> round.sets.count { !it.done } } }

    companion object {
        const val DefaultRestSeconds = 60
    }
}

internal fun ceilSeconds(millis: Long): Int = ((millis.coerceAtLeast(0) + 999) / 1000).toInt()

// Every set in the order they're done: block by block, round by round, exercise by exercise
internal fun List<PrototypeBlock>.refs(): List<SetRef> = flatMap { block ->
    block.rounds.flatMap { round -> round.sets.indices.map { SetRef(block.id, round.id, it) } }
}

internal fun List<PrototypeBlock>.set(ref: SetRef): PrototypeSet? =
    firstOrNull { it.id == ref.blockId }?.rounds?.firstOrNull { it.id == ref.roundId }?.sets?.getOrNull(ref.index)

internal fun List<PrototypeBlock>.updateSet(ref: SetRef, transform: (PrototypeSet) -> PrototypeSet) = map { block ->
    if (block.id != ref.blockId) return@map block
    block.copy(rounds = block.rounds.map { round ->
        if (round.id != ref.roundId) round else round.copy(sets = round.sets.mapIndexed { i, set -> if (i == ref.index) transform(set) else set })
    })
}

// The first set not done after [ref], going round to the start if there's none after it
internal fun List<PrototypeBlock>.nextOpenAfter(ref: SetRef?): SetRef? {
    val refs = refs()
    val start = refs.indexOf(ref)
    val ordered = refs.drop(start + 1) + refs.take(start + 1)
    return ordered.firstOrNull { set(it)?.done == false }
}

internal fun formatWeight(kg: Float): String = BigDecimal(kg.toString()).stripTrailingZeros().toPlainString()

// 0:30, 1:05, 1:02:03
internal fun formatClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

// The sample workout the screen opens with, and the exercises Add exercise picks from. Mock data,
// not the library.
internal object WorkoutPrototypeSamples {
    private data class MockExercise(val name: String, val values: SetValues, val sets: Int)

    // The library's own images (prod Storage) for the mock exercises, which use the library's names
    private const val ImageBase = "https://storage.googleapis.com/everlog-prod.appspot.com/exercises/illustrations/"
    private val images = mapOf(
        "Dumbbell Bench Press" to "dumbbell%20chest%20press%20flat1.webp",
        "Incline Dumbbell Fly" to "dumbbell%20fly%20incline%20bench1.webp",
        "Dumbbell Lateral Raise" to "lateral%20raises%20dumbbell1.webp",
        "Cable Pushdown" to "Cable%20Pushdown1.webp",
        "Elbow Plank" to "plank%20on%20elbows.webp",
        "Barbell Squat" to "Barbell%20narrow%20stance%20squat1.webp",
        "Romanian Deadlift" to "Barbell%20romanian%20deadlift1.webp",
        "Pull Up" to "pull%20up%20normal%20grip%201.webp",
        "Seated Cable Row" to "Cable%20seated%20row1.webp",
        "Military Press" to "Barbell%20Standing%20Military%20Press1.webp",
        "Leg Press" to "leg%20press%20machine%20normal%20stance%201.webp",
        "Dumbbell Curl" to "dumbbell%20curls1.webp",
        "Cable Face Pull" to "Cable%20Face%20Pull%20With%20Rope1.webp",
        "Dumbbell Reverse Lunge" to "Dumbbell%20Rear%20Lunge1.webp",
        "Hanging Knee Raise" to "hanging%20knee%20raises1.webp",
        "Spider Plank" to "spider%20plank1.webp",
        "Side Plank" to "side%20plank1.webp",
    )

    private fun exercise(id: Long, name: String) = PrototypeExercise(id, name, images[name]?.let { ImageBase + it })

    private val pool = listOf(
        MockExercise("Barbell Squat", SetValues(SetMode.Reps, reps = 5, timeSeconds = 30, weightKg = 80f), sets = 5),
        MockExercise("Romanian Deadlift", SetValues(SetMode.Reps, reps = 8, timeSeconds = 30, weightKg = 60f), sets = 3),
        MockExercise("Pull Up", SetValues(SetMode.Reps, reps = 8, timeSeconds = 30, weightKg = 0f), sets = 3),
        MockExercise("Seated Cable Row", SetValues(SetMode.Reps, reps = 12, timeSeconds = 30, weightKg = 45f), sets = 3),
        MockExercise("Military Press", SetValues(SetMode.Reps, reps = 8, timeSeconds = 30, weightKg = 40f), sets = 3),
        MockExercise("Leg Press", SetValues(SetMode.Reps, reps = 10, timeSeconds = 30, weightKg = 120f), sets = 3),
        MockExercise("Dumbbell Curl", SetValues(SetMode.Reps, reps = 12, timeSeconds = 30, weightKg = 12f), sets = 3),
        MockExercise("Cable Face Pull", SetValues(SetMode.Reps, reps = 15, timeSeconds = 30, weightKg = 20f), sets = 3),
        MockExercise("Dumbbell Reverse Lunge", SetValues(SetMode.Reps, reps = 12, timeSeconds = 30, weightKg = 16f), sets = 3),
        MockExercise("Hanging Knee Raise", SetValues(SetMode.Reps, reps = 10, timeSeconds = 30, weightKg = 0f), sets = 3),
        MockExercise("Spider Plank", SetValues(SetMode.Time, reps = 10, timeSeconds = 40, weightKg = 0f), sets = 3),
        MockExercise("Side Plank", SetValues(SetMode.Time, reps = 10, timeSeconds = 30, weightKg = 0f), sets = 2),
    )

    fun randomBlock(nextId: () -> Long, exclude: Set<String>): PrototypeBlock {
        val exercise = (pool.filterNot { it.name in exclude }.ifEmpty { pool }).random()
        return PrototypeBlock(
            id = nextId(),
            exercises = listOf(exercise(nextId(), exercise.name)),
            rounds = List(exercise.sets) { PrototypeRound(nextId(), listOf(PrototypeSet(exercise.values))) },
        )
    }

    // Push day, as on the design's boards: a single exercise, a superset, another single and a time
    // based one, each with last time's values
    fun pushDay(nextId: () -> Long): List<PrototypeBlock> {
        fun reps(reps: Int, kg: Float) = SetValues(SetMode.Reps, reps = reps, timeSeconds = 30, weightKg = kg)
        fun time(seconds: Int, kg: Float) = SetValues(SetMode.Time, reps = 10, timeSeconds = seconds, weightKg = kg)
        fun single(name: String, sets: List<Pair<SetValues, SetValues?>>) = PrototypeBlock(
            id = nextId(),
            exercises = listOf(exercise(nextId(), name)),
            rounds = sets.map { (target, last) -> PrototypeRound(nextId(), listOf(PrototypeSet(target, lastTime = last))) },
        )

        val superset = PrototypeBlock(
            id = nextId(),
            exercises = listOf(exercise(nextId(), "Incline Dumbbell Fly"), exercise(nextId(), "Dumbbell Lateral Raise")),
            rounds = listOf(
                reps(14, 10f) to reps(12, 6f),
                reps(14, 10f) to reps(12, 6f),
                reps(12, 8f) to reps(10, 6f),
            ).map { (flyLast, raiseLast) ->
                PrototypeRound(nextId(), listOf(PrototypeSet(reps(15, 10f), lastTime = flyLast), PrototypeSet(reps(12, 6f), lastTime = raiseLast)))
            },
        )

        return listOf(
            single("Dumbbell Bench Press", listOf(
                reps(15, 18f) to reps(15, 17f),
                reps(15, 18f) to reps(15, 16f),
                reps(15, 18f) to reps(14, 18f),
                reps(15, 18f) to reps(12, 20f),
            )),
            superset,
            single("Cable Pushdown", List(3) { reps(12, 25f) to reps(12, 22.5f) }),
            single("Elbow Plank", listOf(
                time(30, 10f) to time(25, 12f),
                time(30, 10f) to time(25, 10f),
                time(30, 10f) to time(20, 10f),
            )),
        )
    }
}
