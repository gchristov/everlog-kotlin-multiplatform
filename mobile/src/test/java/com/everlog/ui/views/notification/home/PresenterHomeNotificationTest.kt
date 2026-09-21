package com.everlog.ui.views.notification.home

import com.everlog.config.HomeNotification
import com.everlog.managers.analytics.Analytic
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.ui.navigator.Navigator
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class PresenterHomeNotificationTest {

    private val calls = mutableListOf<String>()
    private var dismissedHash: HomeNotification? = null
    private var alreadyDismissed = false

    private val appLaunchManager = object : AppLaunchManager() {
        override fun shouldShowHomeNotification(notification: HomeNotification?) = !alreadyDismissed

        override fun homeNotificationDismissed(notification: HomeNotification?) {
            dismissedHash = notification
        }
    }

    private lateinit var presenter: TestPresenter

    // Records every call made on the interface, in the form "method(arg1, arg2)".
    private inline fun <reified T> recorder(prefix: String): T {
        val handler = InvocationHandler { _, method, args ->
            calls.add("$prefix.${method.name}(${args?.joinToString(", ") { it.toString() } ?: ""})")
            null
        }
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java), handler) as T
    }

    // Analytics isn't under test here (the real manager needs Firebase), so swallow the calls.
    private inline fun <reified T> silent(): T =
            Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java), InvocationHandler { _, _, _ -> null }) as T

    private inner class TestPresenter : PresenterHomeNotification(appLaunchManager, silent<Analytic>()) {
        val view: MvpViewHomeNotification = recorder("view")

        init {
            navigator = recorder<Navigator>("navigator")
        }

        override fun getMvpView(): MvpViewHomeNotification = view
    }

    @Before
    fun setUp() {
        presenter = TestPresenter()
    }

    private fun notification(
            title: String? = "Title",
            description: String? = "Description",
            actionId: String? = null,
            actionUrl: String? = null,
            minRequiredVersion: Int = 0,
            startAt: String? = null,
            endAt: String? = null
    ) = HomeNotification(
            title = title,
            description = description,
            actionId = actionId,
            actionUrl = actionUrl,
            minRequiredVersion = minRequiredVersion,
            startAt = startAt,
            endAt = endAt)

    private fun shownWith(n: HomeNotification, updateRequired: Boolean) = "view.showNotification($n, $updateRequired)"

    // Visibility

    @Test
    fun `shows a valid notification`() {
        val n = notification()
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    @Test
    fun `hides when there is no notification`() {
        presenter.onNotificationChanged(null)
        assertThat(calls).containsExactly("view.hideNotification()")
    }

    @Test
    fun `hides the empty remote config default`() {
        presenter.onNotificationChanged(HomeNotification())
        assertThat(calls).containsExactly("view.hideNotification()")
    }

    @Test
    fun `hides when title or description is missing`() {
        presenter.onNotificationChanged(notification(title = ""))
        presenter.onNotificationChanged(notification(description = null))
        assertThat(calls).containsExactly("view.hideNotification()", "view.hideNotification()").inOrder()
    }

    @Test
    fun `hides when already dismissed`() {
        alreadyDismissed = true
        presenter.onNotificationChanged(notification())
        assertThat(calls).containsExactly("view.hideNotification()")
    }

    // Schedule

    @Test
    fun `hides before startAt`() {
        presenter.onNotificationChanged(notification(startAt = "2999-01-01T00:00:00Z"))
        assertThat(calls).containsExactly("view.hideNotification()")
    }

    @Test
    fun `shows after startAt`() {
        val n = notification(startAt = "2000-01-01T00:00:00Z")
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    @Test
    fun `hides after endAt`() {
        presenter.onNotificationChanged(notification(endAt = "2000-01-01T00:00:00Z"))
        assertThat(calls).containsExactly("view.hideNotification()")
    }

    @Test
    fun `shows inside the window`() {
        val n = notification(startAt = "2000-01-01T00:00:00Z", endAt = "2999-01-01T00:00:00Z")
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    @Test
    fun `hides when a schedule date cannot be parsed`() {
        presenter.onNotificationChanged(notification(startAt = "tomorrow"))
        presenter.onNotificationChanged(notification(endAt = "2999-13-45"))
        assertThat(calls).containsExactly("view.hideNotification()", "view.hideNotification()").inOrder()
    }

    @Test
    fun `blank schedule values are ignored`() {
        val n = notification(startAt = " ", endAt = "")
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    // Update required label

    @Test
    fun `flags update required when app version is below minRequiredVersion`() {
        val n = notification(minRequiredVersion = Int.MAX_VALUE)
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, true))
    }

    @Test
    fun `flags update required for an action id this build does not know`() {
        val n = notification(actionId = "SOMETHING_NEW")
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, true))
    }

    @Test
    fun `maintenance notices are never flagged as update required`() {
        val n = notification(actionId = "MAINTENANCE", minRequiredVersion = Int.MAX_VALUE)
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    @Test
    fun `a plain announcement is not flagged as update required`() {
        val n = notification()
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    @Test
    fun `a url takes priority over an unknown action id`() {
        val n = notification(actionId = "SOMETHING_NEW", actionUrl = "https://example.com")
        presenter.onNotificationChanged(n)
        assertThat(calls).containsExactly(shownWith(n, false))
    }

    // Tap

    @Test
    fun `tap opens the play store when an update is required`() {
        presenter.onNotificationChanged(notification(minRequiredVersion = Int.MAX_VALUE, actionUrl = "https://example.com"))
        calls.clear()
        presenter.onActionClicked()
        assertThat(calls).containsExactly("navigator.openPlayStoreAppDetails()")
    }

    @Test
    fun `tap opens the url and dismisses the banner`() {
        val n = notification(actionUrl = " https://example.com/survey ")
        presenter.onNotificationChanged(n)
        calls.clear()
        presenter.onActionClicked()
        assertThat(calls).contains("navigator.openUrl(https://example.com/survey)")
        assertThat(dismissedHash).isEqualTo(n)
    }

    @Test
    fun `url takes priority over action id on tap`() {
        presenter.onNotificationChanged(notification(actionId = "PLANS", actionUrl = "https://example.com"))
        calls.clear()
        presenter.onActionClicked()
        assertThat(calls).contains("navigator.openUrl(https://example.com)")
        assertThat(calls).doesNotContain("view.showPlans()")
    }

    @Test
    fun `non http urls are ignored`() {
        presenter.onNotificationChanged(notification(actionUrl = "javascript:alert(1)"))
        calls.clear()
        presenter.onActionClicked()
        assertThat(calls).isEmpty()
    }

    @Test
    fun `tap opens the matching in-app screen`() {
        val expected = mapOf(
                "MUSCLE_GOALS" to "navigator.openMuscleGoal()",
                "PLANS" to "view.showPlans()",
                "SETTINGS" to "view.showSettings()",
                "EXERCISES" to "navigator.openExercises()")
        expected.forEach { (actionId, call) ->
            calls.clear()
            presenter.onNotificationChanged(notification(actionId = actionId))
            calls.clear()
            presenter.onActionClicked()
            assertThat(calls).containsExactly(call)
        }
    }

    @Test
    fun `tap does nothing and does not crash for NONE, MAINTENANCE or no action`() {
        listOf("NONE", "MAINTENANCE", null).forEach { actionId ->
            presenter.onNotificationChanged(notification(actionId = actionId))
            calls.clear()
            presenter.onActionClicked()
            assertThat(calls).isEmpty()
        }
    }

    @Test
    fun `tap without a notification does nothing`() {
        presenter.onActionClicked()
        assertThat(calls).isEmpty()
    }
}
