// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.startup.getDefaultAnkiDroidDirectory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
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
            val setting = compose.onNodeWithText(title)
            setting.performScrollTo().assertIsOff().performClick()
            setting.assertIsOn()
            assertTrue(Prefs.sharedPrefs.getBoolean(targetContext.getString(R.string.pref_display_filenames_in_browser_key), false))
        }
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithText(title).performScrollTo().assertIsOn()
        }
    }

    @Test
    fun networkTimeoutOnlySavesValidConfirmedValues() {
        val intent = PreferencesActivity.getIntent(targetContext, SyncSettingsFragment::class)
        val key = targetContext.getString(R.string.sync_io_timeout_secs_key)
        Prefs.sharedPrefs.edit { putInt(key, 60) }
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val setting = compose.onNode(hasText(TR.preferencesNetworkTimeout()) and hasClickAction())
            val field = compose.onNode(hasSetTextAction())
            val save = compose.onNodeWithText(TR.actionsSave())
            setting.performScrollTo().performClick()
            field.performTextReplacement("")
            save.assertIsNotEnabled()
            field.performTextReplacement("75")
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_cancel)).performClick()
            assertEquals(60, Prefs.sharedPrefs.getInt(key, -1))

            setting.performClick()
            field.performTextReplacement("75")
            save.performClick()
            assertEquals(75, Prefs.sharedPrefs.getInt(key, -1))
        }
    }

    @Test
    fun advancedCollectionDirectoryOffersTheDefaultLocationAndCustomEntry() {
        val intent = PreferencesActivity.getIntent(targetContext, AdvancedSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val directory = compose.onNodeWithText(targetContext.getString(CommonString.col_path))
            directory.performScrollTo().performClick()
            val default = getDefaultAnkiDroidDirectory(targetContext).absolutePath
            val defaultRow = compose.onNode(hasText(default) and hasClickAction())
            compose.waitUntil(5_000) { defaultRow.isDisplayed() }
            defaultRow.assertIsDisplayed()
            compose.onNodeWithText(targetContext.getString(CommonString.pref_custom_path)).performClick()
            compose.onNode(hasSetTextAction()).assertIsDisplayed()
        }
    }

    @Test
    fun searchingForThirdPartyAppsRevealsTheRowInAdvanced() {
        val title = targetContext.getString(CommonString.thirdparty_apps_title)
        val intent = PreferencesActivity.getIntent(targetContext)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithContentDescription(targetContext.getString(R.string.settings_search)).performTextInput(title)
            compose.onNode(hasText(title) and hasClickAction() and !hasSetTextAction()).performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(title).assertIsDisplayed()
        }
    }
}
