// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki.preferences

import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.preferences.profiles.SwitchProfilesFragment
import com.ichi2.anki.reviewreminders.ScheduleRemindersFragment
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.utils.CollectionPreferences
import com.ichi2.testutils.ext.clear
import com.ichi2.testutils.simulateSystemBars
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PreferencesScreenshotTest : ScreenshotTest() {
    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun timeboxSettingsFragmentDisabled() =
        runTest {
            CollectionPreferences.setTimeboxTimeLimit(0.toDuration(DurationUnit.MINUTES))
            withPreferencesActivity(TimeboxSettingsFragment::class) { activity ->
                activity.simulateSystemBars()
                captureScreen("TimeboxSettingsFragment_disabled")
            }
        }

    @Test
    fun timeboxSettingsFragmentEnabled() =
        runTest {
            CollectionPreferences.setTimeboxTimeLimit(20.toDuration(DurationUnit.MINUTES))
            withPreferencesActivity(TimeboxSettingsFragment::class) { activity ->
                activity.simulateSystemBars()
                captureScreen("TimeboxSettingsFragment_enabled")
            }
        }

    @Test
    fun switchProfiles() = withPreferencesActivity(SwitchProfilesFragment::class) { captureScreen("SwitchProfilesFragment") }

    @Test
    fun reviewReminders() = withPreferencesActivity(ScheduleRemindersFragment::class) { captureScreen("ScheduleRemindersFragment") }

    private fun withPreferencesActivity(
        fragmentClass: KClass<out Fragment>,
        block: (PreferencesActivity) -> Unit,
    ) {
        val intent = PreferencesActivity.getIntent(targetContext, fragmentClass)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            scenario.onActivity(block)
        }
    }
}
