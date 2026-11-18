// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.app.AlarmManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun choosingADueThresholdSchedulesDailyRemindersAndDisablingItCancelsThem() {
        val key = targetContext.getString(R.string.pref_notifications_minimum_cards_due_key)
        Prefs.sharedPrefs.edit { putString(key, PENDING_NOTIFICATIONS_ONLY.toString()) }
        val title = targetContext.getString(CommonString.notification_pref_title)
        val due = targetContext.getString(CommonString.notification_minimum_cards_due, 10)
        val intent = PreferencesActivity.getIntent(targetContext, NotificationsSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            fun repeatingAlarmExists(): Boolean {
                lateinit var alarms: AlarmManager
                scenario.onActivity { alarms = requireNotNull(it.getSystemService(AlarmManager::class.java)) }
                return shadowOf(alarms).scheduledAlarms.any { it.interval == AlarmManager.INTERVAL_DAY }
            }

            assertFalse(repeatingAlarmExists())
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithText(due).performScrollTo().performClick()
            assertEquals("10", Prefs.sharedPrefs.getString(key, null))
            assertTrue(repeatingAlarmExists())

            compose.onNodeWithText(title).performClick()
            val pendingOnly = targetContext.getString(CommonString.notify_when_pending)
            compose.onAllNodes(hasText(pendingOnly) and hasClickAction())[0].performScrollTo().performClick()
            assertFalse(repeatingAlarmExists())
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
