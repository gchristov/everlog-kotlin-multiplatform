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
        internal val ORM_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(90)

        fun prefillWorkout(workout: ELWorkout, listener: OnExercisePrefillListener) {
            // Fetch user history
            ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
                override fun onItemsLoaded(history: MutableList<ELWorkout>, fromCache: Boolean) {
                    Observable.fromCallable { prefill(workout, history, System.currentTimeMillis()) }
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
            ongoingWorkout.getExerciseGroups().forEach { group ->
                group.exercises.forEach { routineExercise ->
                    val source = buildPrefillSource(routineExercise.exercise!!, history, now)
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
         */
        internal fun buildPrefillSource(exercise: ELExercise,
                                        history: List<ELWorkout>,
                                        now: Long): BaseWorkoutPrefillController.PrefillSource {
            // Sessions where the exercise was actually logged, newest first. The store orders by
            // created date, which can differ from when the workout was completed.
            val sessions = history
                    .sortedByDescending { it.completedDate }
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
                val heaviestSet = session.exercise!!.getBestSet(true)
                // Only sets with weight count towards 1RM
                if (heaviestSet.isWeightEntered()) BaseStatsController.calculate1RM(heaviestSet) else 0f
            } ?: 0f
        }
    }

    interface OnExercisePrefillListener {
        fun onSuccess()
        fun onError(throwable: Throwable)
    }
}
