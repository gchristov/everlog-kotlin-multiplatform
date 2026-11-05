package com.everlog.android.history.data

import com.everlog.android.common.persistence.PreferencesManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

enum class HistoryView { Calendar, List }

// The view the history page opens on: the one the user last picked, the list until they pick one
interface HistoryPreferences {
    fun view(): HistoryView

    fun setView(view: HistoryView)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<HistoryPreferences>())
@Inject
class RealHistoryPreferences : PreferencesManager(), HistoryPreferences {

    // A name it doesn't know, e.g. from a later version, opens the list
    override fun view(): HistoryView = HistoryView.entries.firstOrNull { it.name == getPreference(VIEW, null as String?) } ?: HistoryView.List

    override fun setView(view: HistoryView) = savePreference(view.name, VIEW)

    private companion object {
        const val VIEW = "HISTORY_VIEW"
    }
}
