// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment

@RunWith(AndroidJUnit4::class)
class ReminderTroubleshootingLayoutTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `all available checks and copy details remain reachable in landscape`() {
        RuntimeEnvironment.setQualifiers("+land")
        targetContext.setTroubleshootingChecks(
            notificationPermission = CheckResult.Failed,
            notificationChannel = CheckResult.Failed,
            doNotDisturb = CheckResult.Warning,
            batteryOptimization = CheckResult.Warning,
            powerSavingMode = CheckResult.Warning,
        )
        withTroubleshootingFragment {
            val labels =
                listOf(
                    R.string.review_reminder_check_notifications,
                    R.string.review_reminder_check_channel,
                    R.string.review_reminder_check_dnd,
                    R.string.review_reminder_check_battery,
                    R.string.review_reminder_check_battery_saver,
                    R.string.review_reminder_check_copy,
                )
            labels.forEach {
                compose.onNodeWithText(getString(it)).performScrollTo().assertIsDisplayed()
            }
        }
    }
}
