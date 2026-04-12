package com.everlog

import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
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
    fun clickGetStarted_ShowsRegistrationForm() {
        onView(withText("Get Started"))
            .check(matches(isDisplayed()))

        onView(withText("Get Started")).perform(click())

        onView(allOf(withText("Sign up"), isDescendantOfA(withId(R.id.toolbar))))
            .check(matches(isDisplayed()))

        onView(withHint("Name"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun loginWithUser_ShowsHomeScreen() {
        onView(withText("I have an account")).perform(click())

        onView(allOf(withHint("Email"), isDisplayed()))
            .perform(typeText("email@email.com"), closeSoftKeyboard())

        onView(allOf(withHint("Password"), isDisplayed()))
            .perform(typeText("password"), closeSoftKeyboard())

        onView(allOf(withText("Login"), isAssignableFrom(Button::class.java))).perform(click())

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
            } catch (e: NoMatchingViewException) {
                Thread.sleep(500)
            } catch (e: AssertionError) {
                Thread.sleep(500)
            }
        }
        onView(withId(viewId)).check(matches(isDisplayed()))
    }
}
