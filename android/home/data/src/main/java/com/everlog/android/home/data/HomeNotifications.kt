package com.everlog.android.home.data

import com.everlog.android.common.persistence.PreferencesManager
import com.everlog.android.home.data.model.HomeNotification
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

// Whether Home shows its banner, remembering the one the user last dismissed
interface HomeNotifications {
    fun shouldShow(notification: HomeNotification?): Boolean

    fun dismissed(notification: HomeNotification)

    // On sign-out, so the next user sees the banner again
    fun clear()
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<HomeNotifications>())
@Inject
class RealHomeNotifications : PreferencesManager(), HomeNotifications {

    override fun shouldShow(notification: HomeNotification?): Boolean =
        notification != null && notification.canShow() && notification.dismissalId() != getPreference(LAST_DISMISSED_ID, null as String?)

    override fun dismissed(notification: HomeNotification) = savePreference(notification.dismissalId(), LAST_DISMISSED_ID)

    override fun clear() = removePreference(LAST_DISMISSED_ID)

    private companion object {
        // The key earlier versions used, so a banner dismissed in one stays dismissed after an update
        const val LAST_DISMISSED_ID = "HOME_NOTIFICATION_LAST_DISMISSED_ID"
    }
}
