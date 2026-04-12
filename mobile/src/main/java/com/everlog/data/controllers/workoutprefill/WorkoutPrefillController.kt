package com.everlog.data.controllers.workoutprefill

import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemsListener
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber

class WorkoutPrefillController {

    companion object {

        private const val TAG = "PrefillController"

        fun prefillWorkout(workout: ELWorkout, listener: OnExercisePrefillListener) {
            // Fetch user history
            ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
                override fun onItemsLoaded(history: MutableList<ELWorkout>, fromCache: Boolean) {
                    Observable.just(prefill(workout, history)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.computation())
                            .subscribe({ listener.onSuccess() })
                            { throwable: Throwable -> listener.onError(throwable) })
                }

                override fun onItemsLoadingError(throwable: Throwable) {
                    listener.onError(throwable)
                }
            })
        }

        private fun prefill(ongoingWorkout: ELWorkout, history: List<ELWorkout>): Observable<Void?> {
            return Observable.fromCallable {
                Timber.tag(TAG).d("Starting weight prefill")
                // Keep track of exercises which have not been prefilled
                val unprefilledExercises = ArrayList<BaseWorkoutPrefillController.UnprefilledExercise>()
                ongoingWorkout.getExerciseGroups().forEach { group ->
                    group.exercises.forEach { routineExercise ->
                        val unprefilled = BaseWorkoutPrefillController.UnprefilledExercise()
                        unprefilled.routineExercise = routineExercise
                        unprefilled.exercise = routineExercise.exercise
                        unprefilled.group = group
                        unprefilledExercises.add(unprefilled)
                    }
                }
                val allStats = ExerciseStatsController().calculateStats(
                        StatisticsHomeFragment.RangeType.MONTH,
                        unprefilledExercises.map { it.routineExercise!!.exercise!! },
                        history,
                        false).toBlocking().first()
                unprefilledExercises.forEach { unprefilled ->
                    val exerciseStats = allStats[unprefilled.exercise!!.uuid]!!
                    unprefilled.routineExercise?.sets?.forEachIndexed { setIndex, setToPrefill ->
                        // 1. Prefill using 1RM
                        WorkoutPrefill1RMController().prefill(exerciseStats, setToPrefill, setIndex)
                        // 2. Prefill from history
                        WorkoutPrefillHistoryController().prefill(exerciseStats, setToPrefill, setIndex)
                    }
                }
                Timber.tag(TAG).d("Finished weight prefill")
                null
            }
        }
    }

    interface OnExercisePrefillListener {
        fun onSuccess()
        fun onError(throwable: Throwable)
    }
}