package com.everlog.config

import com.everlog.BuildConfig
import java.io.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException

data class HomeNotification (

        // Optional explicit identifier for this banner, set in Firebase Remote Config. Changing it
        // re-shows the banner to users who already dismissed the previous one; leaving it unset
        // falls back to comparing the whole notification's content (see dismissalId()).
        var id: String? = null,
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

    fun canShow(): Boolean {
        return !title.isNullOrEmpty() && !description.isNullOrEmpty() && isWithinSchedule()
    }

    fun appUpdateRequired(): Boolean {
        // actionUrl takes priority over actionId: any current app version understands it, so it
        // alone doesn't require an update (though minRequiredVersion still can, independently).
        if (hasSupportedUrl()) {
            return BuildConfig.VERSION_CODE < minRequiredVersion
        }
        // Without a url, a valid actionId is required. A missing or unrecognised one means this
        // banner can't do anything on this version - most likely because it's a newer release's
        // banner (e.g. actionUrl-only) that this build doesn't have support for yet.
        val action = getAction() ?: return true
        return if (action == ActionType.MAINTENANCE) {
            false
        } else {
            BuildConfig.VERSION_CODE < minRequiredVersion
        }
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

    fun hasSupportedUrl(): Boolean {
        val url = actionUrl?.trim() ?: return false
        return url.startsWith("https://", true) || url.startsWith("http://", true)
    }

    /**
     * What counts as "the same" banner for dismissal/impression tracking: the explicit [id] when
     * set, or a hash of the whole content otherwise, so an id-less banner doesn't collide with a
     * different one that also forgot to set one.
     */
    fun dismissalId(): String {
        val explicitId = id?.trim()
        return if (!explicitId.isNullOrEmpty()) explicitId else "hash:${hashCode()}"
    }

    private fun isWithinSchedule(): Boolean {
        return try {
            val now = Instant.now()
            val start = startAt?.trim()?.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }
            val end = endAt?.trim()?.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }
            (start == null || !now.isBefore(start)) && (end == null || now.isBefore(end))
        } catch (e: DateTimeParseException) {
            // Unparseable schedule, so don't show rather than show forever.
            false
        }
    }
}
