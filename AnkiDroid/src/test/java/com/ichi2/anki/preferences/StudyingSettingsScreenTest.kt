// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudyingSettingsScreenTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun studyingOpensDirectlyToSchedulingAndStudyScreenSettings() {
        ActivityScenario.launch<PreferencesActivity>(PreferencesActivity.getIntent(targetContext)).use {
            compose.onNodeWithText(targetContext.getString(R.string.settings_studying)).performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText(targetContext.getString(CommonString.time_limit)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(targetContext.getString(CommonString.toolbar_actions)).performScrollTo().assertIsDisplayed()
        }
    }
}
