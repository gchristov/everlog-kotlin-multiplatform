package com.everlog.data.controllers.workoutprefill

import com.everlog.data.controllers.statistics.BaseStatsController
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemsListener
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseHistory
import com.everlog.data.model.workout.ELWorkout
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WorkoutPrefillController {

    companion object {

        private const val TAG = "PrefillController"

        // How far back to look for a recent 1RM before falling back to the all-time best
        private val ORM_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(90)
        // 1RM formulas are only reliable up to ~10 reps, beyond that they wildly overestimate
        private const val ORM_MAX_REPS = 10

        fun prefillWorkout(workout: ELWorkout, listener: OnExercisePrefillListener) {
            // Fetch user history
            ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
                override fun onItemsLoaded(history: MutableList<ELWorkout>, fromCache: Boolean) {
                    // Copy before leaving this thread, as the store keeps updating its list
                    val snapshot = ArrayList(history)
                    Observable.fromCallable { prefill(workout, snapshot, System.currentTimeMillis()) }
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribe({ listener.onSuccess() })
                            { throwable: Throwable -> listener.onError(throwable) }
                }

                override fun onItemsLoadingError(throwable: Throwable) {
                    listener.onError(throwable)
                }
            })
        }

        internal fun prefill(ongoingWorkout: ELWorkout, history: List<ELWorkout>, now: Long) {
            Timber.tag(TAG).d("Starting weight prefill")
            // The store orders by created date, which can differ from when the workout was completed
            val newestFirst = history.sortedByDescending { it.completedDate }
            ongoingWorkout.getExerciseGroups().forEach { group ->
                group.exercises.forEach { routineExercise ->
                    val source = buildPrefillSource(routineExercise.exercise!!, newestFirst, now)
                    routineExercise.sets.forEachIndexed { setIndex, setToPrefill ->
                        // 1. Prefill using 1RM
                        WorkoutPrefill1RMController().prefill(source, setToPrefill, setIndex)
                        // 2. Prefill from history
                        WorkoutPrefillHistoryController().prefill(source, setToPrefill, setIndex)
                    }
                }
            }
            Timber.tag(TAG).d("Finished weight prefill")
        }

        /**
         * Looks at all of the user's history rather than a calendar range, so prefilling
         * doesn't reset when a new month starts.
         *
         * @param newestFirst the user's workouts, most recently completed first
         */
        internal fun buildPrefillSource(exercise: ELExercise,
                                        newestFirst: List<ELWorkout>,
                                        now: Long): BaseWorkoutPrefillController.PrefillSource {
            // Sessions where the exercise was actually logged, newest first
            val sessions = newestFirst
                    .flatMap { workout ->
                        workout.findExercise(exercise).orEmpty()
                                .filter { it.getSetsWithData().isNotEmpty() }
                                .map { ELExerciseHistory(it, workout) }
                    }
            val recentOrm = best1RM(sessions.filter { now - it.workout!!.completedDate <= ORM_WINDOW_MILLIS })
            val orm = if (recentOrm > 0) recentOrm else best1RM(sessions)
            return BaseWorkoutPrefillController.PrefillSource(sessions.firstOrNull(), orm)
        }

        private fun best1RM(sessions: List<ELExerciseHistory>): Float {
            return sessions.maxOfOrNull { session ->
                // Use each session's heaviest set that has weight and few enough reps for an accurate 1RM
                val heaviestSet = session.exercise!!.sets
                        .filter { it.isWeightEntered() && it.getReps() in 1..ORM_MAX_REPS }
                        .maxByOrNull { it.getWeight() }
                if (heaviestSet != null) BaseStatsController.calculate1RM(heaviestSet) else 0f
            } ?: 0f
        }
    }

    interface OnExercisePrefillListener {
        fun onSuccess()
        fun onError(throwable: Throwable)
    }
}
