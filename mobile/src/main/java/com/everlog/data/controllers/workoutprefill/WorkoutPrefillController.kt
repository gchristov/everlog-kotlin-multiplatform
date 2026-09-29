package com.everlog.data.controllers.workoutprefill

import com.everlog.data.controllers.statistics.BaseStatsController
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemsListener
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseHistory
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WorkoutPrefillController {

    companion object {

        private const val TAG = "PrefillController"

        // How far back a 1RM still counts. After a longer break, the last session's weights are a
        // safer place to restart than a percentage of an old 1RM.
        private val ORM_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(90)

        /**
         * Loads the user's history, then fills the empty values of the given exercises' sets on the main thread.
         *
         * @param fill false to only load what the exercises would be prefilled from, e.g. when resuming a
         * workout whose sets were already prefilled and may have been cleared by the user since
         */
        fun prefillExercises(exercises: List<ELRoutineExercise>, fill: Boolean, listener: OnExercisePrefillListener) {
            // Read before leaving the main thread, the workout screen keeps changing the exercises
            val toLoad = exercises.mapNotNull { it.exercise }
            // Fetch user history
            ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
                override fun onItemsLoaded(history: MutableList<ELWorkout>, fromCache: Boolean) {
                    // Copy before leaving this thread, as the store keeps updating its list
                    val snapshot = ArrayList(history)
                    Observable.fromCallable { buildPrefillSources(toLoad, snapshot, System.currentTimeMillis()) }
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribe({ sources ->
                                // Sets are only changed on the main thread, where the user edits them too
                                if (fill) {
                                    applyPrefill(exercises, sources)
                                }
                                listener.onSuccess(sources)
                            })
                            { throwable: Throwable -> listener.onError(throwable) }
                }

                override fun onItemsLoadingError(throwable: Throwable) {
                    listener.onError(throwable)
                }
            })
        }

        /**
         * Fills the empty values of the given exercises' sets.
         *
         * @return what each exercise was prefilled from, by exercise uuid, so sets added later can use it
         */
        internal fun prefill(exercises: List<ELRoutineExercise>, history: List<ELWorkout>, now: Long): Map<String, PrefillSource> {
            val sources = buildPrefillSources(exercises.mapNotNull { it.exercise }, history, now)
            applyPrefill(exercises, sources)
            return sources
        }

        /**
         * @return what each exercise would be prefilled from, by exercise uuid
         */
        internal fun buildPrefillSources(exercises: List<ELExercise>, history: List<ELWorkout>, now: Long): Map<String, PrefillSource> {
            // The store orders by created date, which can differ from when the workout was completed
            val newestFirst = history.sortedByDescending { it.completedDate }
            val sources = HashMap<String, PrefillSource>()
            exercises.forEach { exercise ->
                exercise.uuid?.let { sources[it] = buildPrefillSource(exercise, newestFirst, now) }
            }
            return sources
        }

        internal fun applyPrefill(exercises: List<ELRoutineExercise>, sources: Map<String, PrefillSource>) {
            Timber.tag(TAG).d("Starting set prefill")
            exercises.forEach { routineExercise ->
                val source = sources[routineExercise.exercise?.uuid] ?: return@forEach
                routineExercise.sets.forEachIndexed { setIndex, setToPrefill ->
                    prefillSet(source, setToPrefill, setIndex)
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
        fun prefillAddedSet(source: PrefillSource, addedSet: ELSet, setIndex: Int): Boolean {
            val historicSet = source.lastSession?.exercise?.sets?.getOrNull(setIndex)
            if (historicSet == null || historicSet.isWithoutData()) {
                return false
            }
            addedSet.clearPerformedStats()
            // Copied from the previous set, it isn't a template target for this one
            addedSet.clearRequiredReps()
            addedSet.clearRequiredTime()
            addedSet.remainingTimeSeconds = null
            prefillSet(source, addedSet, setIndex)
            return true
        }

        private fun prefillSet(source: PrefillSource, setToPrefill: ELSet, setIndex: Int) {
            // 1. Prefill using 1RM
            WorkoutPrefill1RMController().prefill(source, setToPrefill, setIndex)
            // 2. Prefill from history
            WorkoutPrefillHistoryController().prefill(source, setToPrefill, setIndex)
        }

        /**
         * Looks at all of the user's history rather than a calendar range, so prefilling
         * doesn't reset when a new month starts.
         *
         * @param newestFirst the user's workouts, most recently completed first
         */
        internal fun buildPrefillSource(exercise: ELExercise,
                                        newestFirst: List<ELWorkout>,
                                        now: Long): PrefillSource {
            // Sessions where the exercise was actually logged, newest first
            val sessions = newestFirst
                    .flatMap { workout ->
                        workout.findExercise(exercise).orEmpty()
                                .filter { it.getSetsWithData().isNotEmpty() }
                                .map { ELExerciseHistory(it, workout) }
                    }
            val orm = best1RM(sessions.filter { now - it.workout!!.completedDate <= ORM_WINDOW_MILLIS })
            return PrefillSource(sessions.firstOrNull(), orm)
        }

        private fun best1RM(sessions: List<ELExerciseHistory>): Float {
            return sessions.maxOfOrNull { BaseStatsController.calculate1RM(it.exercise!!) } ?: 0f
        }
    }

    interface OnExercisePrefillListener {
        fun onSuccess(sources: Map<String, PrefillSource>)
        fun onError(throwable: Throwable)
    }
}
