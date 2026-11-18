// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RemainingSettingsNavigationTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun changingBrowserFilenamesFromGeneralPersistsAfterReopeningSettings() {
        val intent = PreferencesActivity.getIntent(targetContext, GeneralSettingsFragment::class)
        val title = targetContext.getString(CommonString.card_browser_hide_media)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose
                .onNodeWithText(title)
                .performScrollTo()
                .assertIsOff()
                .performClick()
            assertTrue(Prefs.sharedPrefs.getBoolean(targetContext.getString(R.string.pref_display_filenames_in_browser_key), false))
        }
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithText(title).performScrollTo().assertIsOn()
        }
    }
}
