package com.sample.neuroid.us

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.storage.getDataStoreInstance
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.activities.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class TextUnitTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    /**
     * The sending of events to the server is stopped so that they are not eliminated from
     * the SharedPreferences and can be obtained one by one
     */
    @ExperimentalCoroutinesApi
    @Before
    fun stopSendEventsToServer() = runTest {
        NeuroID.getInstance()?.stop()
        NIDJobServiceManager.isSendEventsNowEnabled = false
    }

    /**
     * Validate FOCUS when the user click on editText
     */
    @Test
    fun test01ValidateFocusOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        delay(500)

        val eventType = "\"type\":\"FOCUS\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType, -1)
    }

    /**
     * Validate BLUR when the user change the focus
     */
    @Test
    fun test02ValidateBlurOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        getDataStoreInstance().clearEvents()
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        delay(600)

        Espresso.onView(ViewMatchers.withId(R.id.editText_password_field))
            .perform(ViewActions.click())
        delay(600)

        val eventType = "\"type\":\"BLUR\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }

    /**
     * Validate INPUT when the user type on editText
     */
    @Test
    fun test03ValidateInputText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        delay(500)
        val text = "Some text"
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.typeText(text))
        delay(500)

        val eventType = "\"type\":\"INPUT\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType, text.length)
    }

    /**
     * Validate TEXT_CHANGE when the user type on editText and change focus
     */
    @Test
    fun test04ValidateTypeTextOnEditText() = runTest {
        NIDLog.d("----> UITest", "-------------------------------------------------")
        delay(500) // When you go to the next test, the activity is destroyed and recreated
        getDataStoreInstance().clearEvents()
        Espresso.onView(ViewMatchers.withId(R.id.button_show_activity_fragments))
            .perform(ViewActions.click())
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.click())
        delay(500)
        Espresso.onView(ViewMatchers.withId(R.id.editText_normal_field))
            .perform(ViewActions.typeText("Some text"))
        delay(500)

        Espresso.onView(ViewMatchers.withId(R.id.editText_password_field))
            .perform(ViewActions.click())
        delay(500)

        val eventType = "\"type\":\"TEXT_CHANGE\""
        NIDSchema().validateEvents(getDataStoreInstance().getAllEvents(), eventType)
    }
}