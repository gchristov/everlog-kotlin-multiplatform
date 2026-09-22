package com.everlog.config

import java.io.Serializable

data class HomeNotification (

        var title: String? = null,
        var description: String? = null,
        var imageUrl: String? = null,
        var actionId: String? = null,
        var actionUrl: String? = null,
        var minRequiredVersion: Int = 0,
        // Optional ISO-8601 instants (e.g. 2026-10-01T00:00:00Z) bounding when the banner may show.
        var startAt: String? = null,
        var endAt: String? = null

) : Serializable {

    enum class ActionType {
        NONE,
        MAINTENANCE,
        MUSCLE_GOALS,
        PLANS,
        SETTINGS,
        EXERCISES
    }

    fun getAction(): ActionType? {
        return if (actionId == null) {
            null
        } else try {
            ActionType.valueOf(actionId!!)
        } catch (e: Exception) {
            null
        }
    }
}
