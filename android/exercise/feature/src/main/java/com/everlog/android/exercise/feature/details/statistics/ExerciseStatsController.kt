package com.everlog.android.exercise.feature.details.statistics

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseHistory
import com.everlog.android.common.data.model.exercise.estimated1RM
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.kotlin.WeightUnit
import org.threeten.bp.DayOfWeek
import com.everlog.android.common.data.store.OnStoreItemsListener
import com.everlog.android.common.mvp.chart.BaseStatsController
import com.everlog.android.exercise.feature.ExerciseFeature
import com.github.mikephil.charting.data.Entry
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.max

/**
 * @param weightUnit The unit weights are totalled in
 * @param firstDayOfWeek Where "this week" starts
 */
class ExerciseStatsController(
    private val weightUnit: WeightUnit,
    private val firstDayOfWeek: DayOfWeek,
) : BaseStatsController() {

    fun loadStats(range: RangeType,
                  exercise: ELExercise,
                  listener: OnCompleteListener?) {
        ExerciseFeature.dependencies.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
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

    fun calculateStats(range: RangeType,
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
                                val inRange = workout.inRange(range, firstDayOfWeek)
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
                                    val workoutTotalWeight = routineExercise.getTotalWeight(weightUnit)
                                    val workoutTotalReps = routineExercise.getTotalReps()
                                    stats.totalWeight += workoutTotalWeight
                                    stats.totalReps += workoutTotalReps
                                    if (stats.heaviestSet == null || workoutHeaviestSet.isBetterThan(stats.heaviestSet, true)) {
                                        stats.heaviestSet = workoutHeaviestSet
                                    }
                                    // Add rep stats
                                    calculateAccumulativeValue(workout, stats.repCountsPerDay, workoutTotalReps, range)
                                    // Add weight stats
                                    calculateMaxValue(workout, stats.maxWeightsPerDay, workoutHeaviestSet.getWeight(weightUnit), range)
                                    val workout1RM = routineExercise.estimated1RM(weightUnit)
                                    if (workout1RM > 0) {
                                        // Add 1RM stats only if there's a set to estimate it from
                                        calculateMaxValue(workout, stats.max1RMsPerDay, workout1RM, range)
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