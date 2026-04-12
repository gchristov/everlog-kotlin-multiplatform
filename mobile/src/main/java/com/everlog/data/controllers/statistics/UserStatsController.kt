package com.everlog.data.controllers.statistics

import com.everlog.config.AppConfig
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.max

class UserStatsController : BaseStatsController() {

    fun loadStats(range: StatisticsHomeFragment.RangeType,
                  history: List<ELWorkout>,
                  listener: OnCompleteListener?) {
        Observable.just(calculateStats(range, history)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .subscribe({ data ->
                    data.overallWorkoutsCompleted = history.size
                    listener?.onComplete(data)
                }, { throwable -> listener?.onError(throwable) }))
    }

    private fun calculateStats(range: StatisticsHomeFragment.RangeType, history: List<ELWorkout>): Observable<StatsResult> {
        return Observable.fromCallable {
            val stats = StatsResult()
            val categoryCounts: MutableMap<String, PieEntry> = HashMap()
            val weightLiftedCounts: MutableMap<Long, BarEntry> = LinkedHashMap()
            val durationCounts: MutableMap<Long, Entry> = LinkedHashMap()
            val durationCountsPerMonth: MutableMap<Int, MutableList<Float>> = LinkedHashMap()

            history
                    .filter { it.inRange(range) } // Only use workouts in the specified range
                    .forEach {
                        val workoutTotalWeight = it.getTotalWeight()
                        stats.workoutsCompleted++
                        stats.totalWorkoutTimeMillis += it.getDurationMillis()
                        stats.totalWeightLifted += workoutTotalWeight
                        stats.longestSessionTimeMillis = max(stats.longestSessionTimeMillis, it.getDurationMillis())
                        stats.maxWeightLifted = max(stats.maxWeightLifted, it.getMaxWeight())
                        val day = it.getDay()
                        (stats.dayWorkoutMap as MutableMap)[day] = day
                        // Add category stats
                        calculateCategoryCounts(categoryCounts, it.getCategoryCounts())
                        // Add weight stats
                        calculateAccumulativeValue(it, weightLiftedCounts, workoutTotalWeight, range)
                        // Add session duration stats
                        calculateAverageValue(it, durationCounts, durationCountsPerMonth, TimeUnit.MILLISECONDS.toMinutes(it.getDurationMillis()).toFloat(), range)
            }
            if (stats.workoutsCompleted > 0) {
                stats.averageSessionTimeMillis = stats.totalWorkoutTimeMillis / stats.workoutsCompleted
            }
            // Sort categories
            for (category in categoryCounts.keys.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })) {
                stats.categoryCounts.add(categoryCounts[category]!!)
            }
            // Sort weights
            for (day in weightLiftedCounts.keys.reversed()) {
                stats.weightLiftedCounts.add(weightLiftedCounts[day]!!)
            }
            // Sort session durations
            for (day in durationCounts.keys.reversed()) {
                stats.durationCounts.add(durationCounts[day]!!)
            }
            stats
        }
    }

    data class StatsResult (

            var range: StatisticsHomeFragment.RangeType = AppConfig.configuration.defaultStatsRange,

            var dayWorkoutMap: Map<Int, Int> = HashMap(),

            var overallWorkoutsCompleted: Int = 0,

            var workoutsCompleted: Int = 0,
            var totalWorkoutTimeMillis: Long = 0,
            var totalWeightLifted: Float = 0F,
            var averageSessionTimeMillis: Long = 0,

            var longestSessionTimeMillis: Long = 0,
            var maxWeightLifted: Float = 0F,

            var categoryCounts: MutableList<PieEntry> = ArrayList(),
            var weightLiftedCounts: MutableList<BarEntry> = ArrayList(),
            var durationCounts: MutableList<Entry> = ArrayList()

    ) : Serializable

    interface OnCompleteListener {
        fun onComplete(result: StatsResult)
        fun onError(throwable: Throwable)
    }
}
