// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.testutils.withRequestIgnoreBatteryOptimizationsInManifest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertNotNull

/**
 * The actions offered by [ReminderTroubleshootingFragment] to resolve a failing check
 */
@RunWith(AndroidJUnit4::class)
class ReminderTroubleshootingResolveActionTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    /** 'Optimized': the battery optimization check is a warning, so its resolve action is displayed */
    @Before
    fun enableBatteryOptimization() = targetContext.setTroubleshootingChecks(batteryOptimization = CheckResult.Warning)

    @Test
    fun `battery optimization asks for an exemption when the permission is in the manifest`() {
        withRequestIgnoreBatteryOptimizationsInManifest(true) {
            withTroubleshootingFragment {
                tapBatteryFix(R.string.review_reminder_check_allow_background)

                val intent = assertNotNull(shadowOf(requireActivity()).nextStartedActivity)
                assertThat(intent.action, equalTo(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
                assertThat(intent.data, equalTo("package:${targetContext.packageName}".toUri()))
            }
        }
    }

    @Test
    fun `battery optimization opens the settings list when the permission is not in the manifest`() {
        withRequestIgnoreBatteryOptimizationsInManifest(false) {
            withTroubleshootingFragment {
                tapBatteryFix(CommonString.reminder_troubleshooting_action_open_battery_settings)

                val intent = assertNotNull(shadowOf(requireActivity()).nextStartedActivity)
                assertThat(intent.action, equalTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    @Test
    fun `battery optimization falls back to the settings list when exemption fails`() {
        throwOnAllSettingsExcept(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        withRequestIgnoreBatteryOptimizationsInManifest(true) {
            withTroubleshootingFragment {
                tapBatteryFix(R.string.review_reminder_check_allow_background)

                val intent = assertNotNull(shadowOf(requireActivity()).nextStartedActivity)
                assertThat(intent.action, equalTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    /**
     * Starting any activity other than the settings screen for [action] throws
     * `ActivityNotFoundException`
     */
    private fun throwOnAllSettingsExcept(
        @Suppress("SameParameterValue") action: String,
    ) {
        val settingsScreen = ComponentName("com.android.settings", action)
        shadowOf(targetContext.packageManager).apply {
            addActivityIfNotPresent(settingsScreen)
            addIntentFilterForActivity(settingsScreen, IntentFilter(action).apply { addCategory(Intent.CATEGORY_DEFAULT) })
        }
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).checkActivities(true)
    }

    private fun tapBatteryFix(label: Int) {
        compose.onNodeWithText(getResourceString(label)).performScrollTo().performClick()
    }
}
