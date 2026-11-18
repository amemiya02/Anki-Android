// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSearchNavigationTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun aSearchFindsTheImagePickerByItsSummary() {
        openSettings().use {
            search(targetContext.getString(R.string.settings_choose_image_summary))
            result(targetContext.getString(R.string.settings_choose_image)).assertIsDisplayed()
        }
    }

    @Test
    fun searchingForReviewRemindersOpensTheEnabledScreen() {
        Prefs.newReviewRemindersEnabled = true
        openSettings().use {
            val title = targetContext.getString(CommonString.review_reminders_title)
            search(title)
            result(title).assertIsDisplayed().performClick()
            advanceRobolectricLooper()
            onView(withText(CommonString.add_reminder)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun searchingForTimeboxOpensTheTimeboxSubpage() {
        openSettings().use {
            val title = targetContext.getString(CommonString.time_limit)
            search(title)
            result(title).assertIsDisplayed().performClick()
            advanceRobolectricLooper()
            compose.onNode(hasText(title)).assertIsDisplayed()
        }
    }

    private fun openSettings(): ActivityScenario<PreferencesActivity> =
        ActivityScenario.launch(PreferencesActivity.getIntent(targetContext))

    private fun search(query: String) {
        compose.onNodeWithContentDescription(targetContext.getString(R.string.settings_search)).performTextInput(query)
    }

    private fun result(title: String) = compose.onNode(hasText(title) and hasClickAction() and !hasSetTextAction())
}
