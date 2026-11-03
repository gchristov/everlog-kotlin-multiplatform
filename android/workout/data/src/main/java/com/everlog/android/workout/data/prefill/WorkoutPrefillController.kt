package com.everlog.android.workout.data.prefill

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseHistory
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.exercise.estimated1RM
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.workout.data.prefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.android.common.kotlin.WeightUnit
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WorkoutPrefillController {

    companion object {

        private const val TAG = "PrefillController"

        // How far back a 1RM still counts. After a longer break, the last session's weights are a
        // safer place to restart than a percentage of an old 1RM.
        internal val ORM_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(90)

        // How far back a last session still counts. Older than that, entering the weights again is easier than
        // working from a session that may no longer reflect what the user lifts, and the history read stays small.
        internal val LAST_SESSION_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(365)

        /**
         * Fills the empty values of the given exercises' sets.
         *
         * @return what each exercise was prefilled from, by exercise uuid, so sets added later can use it
         */
        internal fun prefill(exercises: List<ELRoutineExercise>, history: List<ELWorkout>, now: Long, settings: PrefillSettings): Map<String, PrefillSource> {
            val sources = buildPrefillSources(exercises.mapNotNull { it.exercise }, history, now, settings.weightUnit)
            applyPrefill(exercises, sources, settings)
            return sources
        }

        /**
         * @return what each exercise would be prefilled from, by exercise uuid
         */
        internal fun buildPrefillSources(exercises: List<ELExercise>, history: List<ELWorkout>, now: Long, weightUnit: WeightUnit): Map<String, PrefillSource> {
            // The store orders by created date, which can differ from when the workout was completed
            val newestFirst = history.sortedByDescending { it.completedDate }
            val sources = HashMap<String, PrefillSource>()
            exercises.forEach { exercise ->
                exercise.uuid?.let { sources[it] = buildPrefillSource(exercise, newestFirst, now, weightUnit) }
            }
            return sources
        }

        internal fun applyPrefill(exercises: List<ELRoutineExercise>, sources: Map<String, PrefillSource>, settings: PrefillSettings) {
            Timber.tag(TAG).d("Starting set prefill")
            exercises.forEach { routineExercise ->
                val source = sources[routineExercise.exercise?.uuid] ?: return@forEach
                routineExercise.sets.forEachIndexed { setIndex, setToPrefill ->
                    prefillSet(source, setToPrefill, setIndex, settings)
                }
            }
            Timber.tag(TAG).d("Finished set prefill")
        }

        /**
         * Fills a set just added during the workout with the same set of the last session, replacing
         * the values it copied from the previous set. Does nothing if the last session didn't log that set.
         *
         * @return true if the set was prefilled
         */
        fun prefillAddedSet(source: PrefillSource, addedSet: ELSet, setIndex: Int, settings: PrefillSettings): Boolean {
            val historicSet = source.lastSession?.exercise?.sets?.getOrNull(setIndex)
            if (historicSet == null || historicSet.isWithoutData()) {
                return false
            }
            addedSet.clearPerformedStats()
            // Copied from the previous set, it isn't a template target for this one
            addedSet.clearRequiredReps()
            addedSet.clearRequiredTime()
            addedSet.remainingTimeSeconds = null
            prefillSet(source, addedSet, setIndex, settings)
            return true
        }

        // Fills a set's empty values from its exercise's source: the 1RM target for the muscle goal, then its last
        // session's set at the same index. Also used by the Compose workout screen, set by set.
        internal fun prefillSet(source: PrefillSource, setToPrefill: ELSet, setIndex: Int, settings: PrefillSettings) {
            // 1. Prefill using 1RM
            WorkoutPrefill1RMController().prefill(source, setToPrefill, setIndex, settings)
            // 2. Prefill from history
            WorkoutPrefillHistoryController().prefill(source, setToPrefill, setIndex, settings)
        }

        /**
         * Looks at the last year of the user's history rather than a calendar range, so prefilling
         * doesn't reset when a new month starts.
         *
         * @param newestFirst the user's workouts, most recently completed first
         */
        internal fun buildPrefillSource(exercise: ELExercise,
                                        newestFirst: List<ELWorkout>,
                                        now: Long,
                                        weightUnit: WeightUnit): PrefillSource {
            // Sessions where the exercise was actually logged, newest first
            val sessions = newestFirst
                    .filter { now - it.completedDate <= LAST_SESSION_WINDOW_MILLIS }
                    .flatMap { workout -> workout.sessionsOf(exercise).map { ELExerciseHistory(it, workout) } }
            val orm = best1RM(sessions.filter { now - it.workout!!.completedDate <= ORM_WINDOW_MILLIS }, weightUnit)
            return PrefillSource(sessions.firstOrNull(), orm)
        }

        private fun best1RM(sessions: List<ELExerciseHistory>, weightUnit: WeightUnit): Float {
            return sessions.maxOfOrNull { it.exercise!!.estimated1RM(weightUnit) } ?: 0f
        }
    }
}

// Where the workout logged the exercise, leaving out the times it was in the workout but never done
internal fun ELWorkout.sessionsOf(exercise: ELExercise): List<ELRoutineExercise> =
        findExercise(exercise).orEmpty().filter { it.getSetsWithData().isNotEmpty() }
