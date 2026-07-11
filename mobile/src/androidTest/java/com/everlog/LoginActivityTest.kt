package com.everlog

import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
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
import com.everlog.ui.activities.login.LoginActivity
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
        onView(withId(R.id.showLoginBtn)).perform(click())

        onView(allOf(withHint("Email"), isDisplayed()))
            .perform(typeText(BuildConfig.E2E_TEST_USER_EMAIL), closeSoftKeyboard())

        onView(allOf(withHint("Password"), isDisplayed()))
            .perform(typeText(BuildConfig.E2E_TEST_USER_PASSWORD), closeSoftKeyboard())

        onView(allOf(withId(R.id.loginBtn), isAssignableFrom(Button::class.java))).perform(click())

        waitForView(R.id.tabBar)

        onView(withId(R.id.tabBar))
            .check(matches(isDisplayed()))

        onView(withId(R.id.newWorkoutBtn))
            .check(matches(isDisplayed()))
    }

    private fun waitForView(viewId: Int, timeoutMs: Long = 10000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeoutMs
        
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (e: NoMatchingViewException) {
                Thread.sleep(500)
            } catch (e: AssertionError) {
                Thread.sleep(500)
            }
        }
        onView(withId(viewId)).check(matches(isDisplayed()))
    }
}
