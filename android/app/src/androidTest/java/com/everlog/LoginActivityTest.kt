package com.everlog

import com.everlog.android.auth.R as AuthR
import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.auth.login.LoginActivity
import com.everlog.android.home.screen.HomeActivity
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun login_shows() {
        onView(withText("Continue as guest"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun login_withUser_showsHomeScreen() {
        onView(withId(AuthR.id.showLoginBtn)).perform(click())

        onView(allOf(withHint("Email"), isDisplayed()))
            .perform(typeText(BuildConfig.E2E_TEST_USER_EMAIL), closeSoftKeyboard())

        onView(allOf(withHint("Password"), isDisplayed()))
            .perform(typeText(BuildConfig.E2E_TEST_USER_PASSWORD), closeSoftKeyboard())

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val home = instrumentation.addMonitor(HomeActivity::class.java.name, null, false)
        onView(allOf(withId(AuthR.id.loginBtn), isAssignableFrom(Button::class.java))).perform(click())

        assertThat(instrumentation.waitForMonitorWithTimeout(home, HomeTimeoutMillis)).isNotNull()
        instrumentation.removeMonitor(home)
    }

    private companion object {
        const val HomeTimeoutMillis = 10_000L
    }
}
