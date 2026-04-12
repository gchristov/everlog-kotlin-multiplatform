package com.everlog.data.controllers.statistics

import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemsListener
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseHistory
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.github.mikephil.charting.data.Entry
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

class ExerciseStatsController : BaseStatsController() {

    fun loadStats(range: StatisticsHomeFragment.RangeType,
                  exercise: ELExercise,
                  listener: OnCompleteListener?) {
        ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
            override fun onItemsLoaded(items: List<ELWorkout>, fromCache: Boolean) {
                Observable.just(calculateStats(range, listOf(exercise), items, true)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.computation())
                        .subscribe({ data ->
                            if (data.containsKey(exercise.uuid)) {
                                listener?.onComplete(data.getValue(exercise.uuid!!))
                            } else {
                                listener?.onError(Exception("Could not calculate exercise history"))
                            }
                        })
                        { throwable -> listener?.onError(throwable) })
            }

            override fun onItemsLoadingError(throwable: Throwable) {
                listener?.onError(throwable)
            }
        })
    }

    fun calculateStats(range: StatisticsHomeFragment.RangeType,
                       exercises: List<ELExercise>,
                       history: List<ELWorkout>,
                       allowHistoryOutOfRange: Boolean): Observable<Map<String, StatsResult>> {
        return Observable.fromCallable {
            // Build stats map
            val exercisesMap = HashMap<String, StatsResult>()
            exercises.forEach {
                val stats = StatsResult()
                exercisesMap[it.uuid!!] = stats
            }
            // Go through history and build stats for all given exercises
            history.forEach { workout ->
                exercises.forEach { exercise ->
                    val stats = exercisesMap[exercise.uuid!!]!!
                    val matches = workout.findExercise(exercise)
                    var workoutUsedForCalculations = false
                    matches
                            ?.filter { it.getSetsWithData().isNotEmpty() }
                            ?.forEach { routineExercise ->
                                val inRange = workout.inRange(range)
                                if (inRange || allowHistoryOutOfRange) {
                                    // Add workout to exercise history
                                    val historicItem = ELExerciseHistory()
                                    historicItem.workout = workout
                                    historicItem.exercise = routineExercise
                                    (stats.history as MutableList).add(historicItem)
                                }
                                // Calculate stats for relevant range
                                if (inRange) {
                                    workoutUsedForCalculations = true
                                    val workoutHeaviestSet = routineExercise.getBestSet(true)
                                    val workoutTotalWeight = routineExercise.getTotalWeight()
                                    val workoutTotalReps = routineExercise.getTotalReps()
                                    stats.totalWeight += workoutTotalWeight
                                    stats.totalReps += workoutTotalReps
                                    if (stats.heaviestSet == null || workoutHeaviestSet.isBetterThan(stats.heaviestSet, true)) {
                                        stats.heaviestSet = workoutHeaviestSet
                                    }
                                    // Add rep stats
                                    calculateAccumulativeValue(workout, stats.repCountsPerDay, workoutTotalReps, range)
                                    // Add weight stats
                                    calculateMaxValue(workout, stats.maxWeightsPerDay, workoutHeaviestSet.getWeight(), range)
                                    if (workoutHeaviestSet.isWeightEntered()) {
                                        // Add 1RM stats only if set has weight
                                        calculateMaxValue(workout, stats.max1RMsPerDay, calculate1RM(workoutHeaviestSet), range)
                                    }
                                }
                            }
                    if (workoutUsedForCalculations) {
                        stats.timesPerformed++
                    }
                }
            }
            exercisesMap.values.forEach {
                it.convertStats()
            }
            exercisesMap
        }
    }

    data class StatsResult (

            var timesPerformed: Int = 0,
            var heaviestSet: ELSet? = null,
            var totalReps: Int = 0,
            var totalWeight: Float = 0F,
            var orm: Float = 0F,

            val history: List<ELExerciseHistory> = ArrayList(),

            val repCounts: MutableList<Entry> = ArrayList(),
            val weightCounts: MutableList<Entry> = ArrayList(),
            val ormCounts: MutableList<Entry> = ArrayList(),

            // Aux

            internal val repCountsPerDay: MutableMap<Long, Entry> = LinkedHashMap(),
            internal val maxWeightsPerDay: MutableMap<Long, Entry> = LinkedHashMap(),
            internal val max1RMsPerDay: MutableMap<Long, Entry> = LinkedHashMap()

    ) {

        internal fun convertStats() {
            // Sort rep counts
            repCountsPerDay.keys.reversed().forEach {
                repCounts.add(repCountsPerDay[it]!!)
            }
            // Sort weight counts
            maxWeightsPerDay.keys.reversed().forEach {
                weightCounts.add(maxWeightsPerDay[it]!!)
            }
            // Sort 1RM counts
            max1RMsPerDay.keys.reversed().forEach {
                val entry = max1RMsPerDay[it]!!
                ormCounts.add(entry)
                orm = max(orm, entry.y)
            }
        }
    }

    interface OnCompleteListener {
        fun onComplete(result: StatsResult)
        fun onError(throwable: Throwable)
    }
}