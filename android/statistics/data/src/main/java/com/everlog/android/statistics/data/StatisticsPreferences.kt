package com.everlog.android.statistics.data

import com.everlog.android.common.persistence.PreferencesManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

// The period the statistics page opens on: the one the user last picked, this week until they pick one
interface StatisticsPreferences {
    fun period(): StatsPeriod

    fun setPeriod(period: StatsPeriod)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<StatisticsPreferences>())
@Inject
class RealStatisticsPreferences : PreferencesManager(), StatisticsPreferences {

    // A name it doesn't know, e.g. from a later version, opens this week
    override fun period(): StatsPeriod = StatsPeriod.entries.firstOrNull { it.name == getPreference(PERIOD, null as String?) } ?: StatsPeriod.Week

    override fun setPeriod(period: StatsPeriod) = savePreference(period.name, PERIOD)

    private companion object {
        const val PERIOD = "STATISTICS_PERIOD"
    }
}
