// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.HideSystemBars
import com.ichi2.anki.utils.CollectionPreferences
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

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

    @Test
    fun changingStartOfDayWritesTheCollectionPreference() {
        val intent = PreferencesActivity.getIntent(targetContext, StudyingSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val rollover = compose.onNodeWithText(targetContext.getString(CommonString.day_offset_with_description))
            rollover.performClick()
            val newTime = targetContext.resources.getQuantityString(CommonPlurals.day_offset_summ, 2, 2)
            compose.onNodeWithText(newTime).performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText(newTime).fetchSemanticsNodes().isNotEmpty() }
            rollover.assertIsDisplayed()
            runTest { assertEquals(2, CollectionPreferences.getDayOffset()) }
        }
    }

    @Test
    fun systemBarSelectionControlsTheDisplayCutoutSetting() {
        val intent = PreferencesActivity.getIntent(targetContext, StudyingSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val cutout = compose.onNodeWithText(targetContext.getString(CommonString.ignore_display_cutout))
            cutout.performScrollTo().assertIsNotEnabled().assertIsOff()
            compose.onNodeWithText(targetContext.getString(CommonString.hide_system_bars)).performScrollTo().performClick()
            compose.onNodeWithText(targetContext.getString(CommonString.hide_system_bars_all_bars)).performScrollTo().performClick()
            cutout.assertIsEnabled().performClick()
            assertEquals(HideSystemBars.ALL, Prefs.hideSystemBars)
            assertEquals(true, Prefs.ignoreDisplayCutout)
        }
    }
}
