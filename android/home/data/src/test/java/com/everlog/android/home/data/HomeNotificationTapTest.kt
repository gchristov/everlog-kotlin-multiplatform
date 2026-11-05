package com.everlog.android.home.data

import com.everlog.android.home.data.model.HomeNotification
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeNotificationTapTest {

    private val appVersionCode = 2_240_000

    private fun notification(actionId: String? = null, actionUrl: String? = null, minRequiredVersion: Int = 0) =
        HomeNotification(title = "Title", description = "Description", actionId = actionId, actionUrl = actionUrl, minRequiredVersion = minRequiredVersion)

    @Test
    fun `a banner this version can't handle asks for an update`() {
        assertThat(notification(actionId = "PLANS", minRequiredVersion = Int.MAX_VALUE).tap(appVersionCode)).isEqualTo(HomeNotificationTap.UpdateApp)
        assertThat(notification(actionId = "SOMETHING_NEW").tap(appVersionCode)).isEqualTo(HomeNotificationTap.UpdateApp)
    }

    @Test
    fun `a link opens, trimmed, before any action`() {
        assertThat(notification(actionId = "PLANS", actionUrl = " https://example.com ").tap(appVersionCode))
            .isEqualTo(HomeNotificationTap.OpenUrl("https://example.com"))
    }

    @Test
    fun `an in-app action opens its place`() {
        listOf("MUSCLE_GOALS", "PLANS", "SETTINGS", "EXERCISES").forEach { id ->
            assertThat(notification(actionId = id).tap(appVersionCode))
                .isEqualTo(HomeNotificationTap.Open(HomeNotification.ActionType.valueOf(id)))
        }
    }

    @Test
    fun `a maintenance notice opens nothing`() {
        assertThat(notification(actionId = "MAINTENANCE").tap(appVersionCode)).isEqualTo(HomeNotificationTap.Nothing)
        assertThat(notification(actionId = "NONE").tap(appVersionCode)).isEqualTo(HomeNotificationTap.Nothing)
    }
}
