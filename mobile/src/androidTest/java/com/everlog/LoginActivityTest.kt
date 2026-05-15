package com.everlog

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.ui.activities.login.LoginActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun getStarted_displayed() {
        onView(withText("Get Started"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun getStarted_launchesHome() {
        onView(withText("Get Started"))
            .check(matches(isDisplayed()))

        onView(withText("Get Started")).perform(click())

        waitForView(R.id.tabBar)

        onView(withId(R.id.tabBar))
            .check(matches(isDisplayed()))

        onView(withId(R.id.addBtn))
            .check(matches(isDisplayed()))
    }

    private fun waitForView(viewId: Int, timeoutMs: Long = 10000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeoutMs
        
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (_: NoMatchingViewException) {
                Thread.sleep(500)
            } catch (_: AssertionError) {
                Thread.sleep(500)
            }
        }
        onView(withId(viewId)).check(matches(isDisplayed()))
    }
}
