package com.everlog.config

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class HomeNotificationTest {

    // RemoteConfig parses the `notification_home` string with plain Gson, so test that exact path.
    private fun parse(json: String) = Gson().fromJson(json, HomeNotification::class.java)

    private fun notification(
            id: String? = null,
            title: String? = "Title",
            description: String? = "Description",
            actionId: String? = null,
            actionUrl: String? = null,
            minRequiredVersion: Int = 0,
            startAt: String? = null,
            endAt: String? = null
    ) = HomeNotification(
            id = id,
            title = title,
            description = description,
            actionId = actionId,
            actionUrl = actionUrl,
            minRequiredVersion = minRequiredVersion,
            startAt = startAt,
            endAt = endAt)

    // Parsing

    @Test
    fun `empty default parses to a notification with no title`() {
        assertThat(parse("{}").title).isNull()
    }

    @Test
    fun `parses the full remote config shape`() {
        val n = parse("""
            {
              "id": "survey-oct-2026",
              "title": "Help shape Everlog",
              "description": "3 quick questions",
              "imageUrl": "https://example.com/i.png",
              "actionUrl": "https://forms.gle/abc",
              "minRequiredVersion": 123,
              "startAt": "2026-10-01T00:00:00Z",
              "endAt": "2026-11-01T00:00:00Z"
            }
        """.trimIndent())
        assertThat(n.id).isEqualTo("survey-oct-2026")
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

    // canShow

    @Test
    fun `cannot show without a title or description`() {
        assertThat(notification(title = "").canShow()).isFalse()
        assertThat(notification(description = null).canShow()).isFalse()
    }

    @Test
    fun `cannot show before startAt or after endAt`() {
        assertThat(notification(startAt = "2999-01-01T00:00:00Z").canShow()).isFalse()
        assertThat(notification(endAt = "2000-01-01T00:00:00Z").canShow()).isFalse()
        assertThat(notification(startAt = "2000-01-01T00:00:00Z", endAt = "2999-01-01T00:00:00Z").canShow()).isTrue()
    }

    @Test
    fun `cannot show when a schedule date cannot be parsed`() {
        assertThat(notification(startAt = "tomorrow").canShow()).isFalse()
        assertThat(notification(endAt = "2999-13-45").canShow()).isFalse()
    }

    @Test
    fun `blank schedule values are ignored`() {
        assertThat(notification(startAt = " ", endAt = "").canShow()).isTrue()
    }

    // appUpdateRequired

    @Test
    fun `update required when there is no actionId and no actionUrl`() {
        // e.g. a user on a version before actionUrl existed, viewing a url-only banner: their
        // build can't do anything with it, so they should be nudged to update.
        assertThat(notification().appUpdateRequired()).isTrue()
    }

    @Test
    fun `update required for an action id this build does not know`() {
        assertThat(notification(actionId = "SOMETHING_NEW").appUpdateRequired()).isTrue()
    }

    @Test
    fun `update required when a valid action id is below minRequiredVersion`() {
        assertThat(notification(actionId = "PLANS", minRequiredVersion = Int.MAX_VALUE).appUpdateRequired()).isTrue()
    }

    @Test
    fun `not update required with a valid action id and satisfied minRequiredVersion`() {
        assertThat(notification(actionId = "PLANS", minRequiredVersion = 0).appUpdateRequired()).isFalse()
    }

    @Test
    fun `maintenance notices are never update required`() {
        assertThat(notification(actionId = "MAINTENANCE", minRequiredVersion = Int.MAX_VALUE).appUpdateRequired()).isFalse()
    }

    @Test
    fun `a url takes priority over an unknown action id`() {
        assertThat(notification(actionId = "SOMETHING_NEW", actionUrl = "https://example.com").appUpdateRequired()).isFalse()
    }

    @Test
    fun `a url still requires an update when below minRequiredVersion`() {
        assertThat(notification(actionUrl = "https://example.com", minRequiredVersion = Int.MAX_VALUE).appUpdateRequired()).isTrue()
    }

    // hasSupportedUrl

    @Test
    fun `only http and https urls are supported`() {
        assertThat(notification(actionUrl = "https://example.com").hasSupportedUrl()).isTrue()
        assertThat(notification(actionUrl = " http://example.com ").hasSupportedUrl()).isTrue()
        assertThat(notification(actionUrl = "javascript:alert(1)").hasSupportedUrl()).isFalse()
        assertThat(notification(actionUrl = null).hasSupportedUrl()).isFalse()
    }

    // Dismissal identity

    @Test
    fun `dismissal id uses the explicit id when set, ignoring the rest of the content`() {
        val a = notification(id = "banner-1", title = "A")
        val b = notification(id = "banner-1", title = "Completely different")
        assertThat(a.dismissalId()).isEqualTo(b.dismissalId())
    }

    @Test
    fun `dismissal id falls back to a content hash with no explicit id`() {
        val base = notification()
        assertThat(base.copy().dismissalId()).isEqualTo(base.dismissalId())
        assertThat(base.copy(description = "Different").dismissalId()).isNotEqualTo(base.dismissalId())
    }

    @Test
    fun `changing the id changes the dismissal id even with identical content`() {
        val a = notification(id = "banner-1")
        val b = notification(id = "banner-2")
        assertThat(a.dismissalId()).isNotEqualTo(b.dismissalId())
    }
}
