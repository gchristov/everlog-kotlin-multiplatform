package com.everlog.config

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class HomeNotificationTest {

    // RemoteConfig parses the `notification_home` string with plain Gson, so test that exact path.
    private fun parse(json: String) = Gson().fromJson(json, HomeNotification::class.java)

    @Test
    fun `empty default parses to a notification with no title`() {
        assertThat(parse("{}").title).isNull()
    }

    @Test
    fun `parses the full remote config shape`() {
        val n = parse("""
            {
              "title": "Help shape Everlog",
              "description": "3 quick questions",
              "imageUrl": "https://example.com/i.png",
              "actionUrl": "https://forms.gle/abc",
              "minRequiredVersion": 123,
              "startAt": "2026-10-01T00:00:00Z",
              "endAt": "2026-11-01T00:00:00Z"
            }
        """.trimIndent())
        assertThat(n.title).isEqualTo("Help shape Everlog")
        assertThat(n.description).isEqualTo("3 quick questions")
        assertThat(n.actionUrl).isEqualTo("https://forms.gle/abc")
        assertThat(n.minRequiredVersion).isEqualTo(123)
        assertThat(n.startAt).isEqualTo("2026-10-01T00:00:00Z")
        assertThat(n.endAt).isEqualTo("2026-11-01T00:00:00Z")
    }

    @Test
    fun `unknown fields are ignored so older configs and newer keys stay compatible`() {
        val n = parse("""{"title":"a","description":"b","somethingNew":true}""")
        assertThat(n.title).isEqualTo("a")
        assertThat(n.description).isEqualTo("b")
    }

    @Test
    fun `getAction parses known ids and returns null otherwise`() {
        assertThat(HomeNotification(actionId = "PLANS").getAction()).isEqualTo(HomeNotification.ActionType.PLANS)
        assertThat(HomeNotification(actionId = "NOPE").getAction()).isNull()
        assertThat(HomeNotification(actionId = null).getAction()).isNull()
    }

    // Dismissal stores hashCode(), so changing any field must re-show, and equal content must not.
    @Test
    fun `hash changes when any field changes`() {
        val base = HomeNotification(title = "a", description = "b")
        assertThat(base.copy().hashCode()).isEqualTo(base.hashCode())
        assertThat(base.copy(actionUrl = "https://x.com").hashCode()).isNotEqualTo(base.hashCode())
        assertThat(base.copy(endAt = "2026-11-01T00:00:00Z").hashCode()).isNotEqualTo(base.hashCode())
        assertThat(base.copy(description = "c").hashCode()).isNotEqualTo(base.hashCode())
    }
}
