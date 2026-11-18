// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.app.NotificationManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.windows.permissions.PermissionsBottomSheet
import com.ichi2.testutils.rules.OverridePropertyRule
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** Tests AnkiDroid's permission sheet without changing the device's notification permission. */
@RunWith(AndroidJUnit4::class)
class ReviewReminderNotificationPermissionTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @get:Rule
    val notificationPreferences =
        OverridePropertyRule(
            Prefs::reminderNotifsRequestShown to false,
            Prefs::notificationsPermissionRequested to false,
            Prefs::notificationsBottomSheetShownBelowAPI33 to false,
        )

    @Before
    fun setUpReminders() {
        ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
        shadowOf(targetContext.getSystemService<NotificationManager>()!!).setNotificationsEnabled(false)
    }

    @After
    fun tearDownReminders() {
        ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
    }

    @Test
    fun notificationPermissionPromptIsOnlyShownOnce() {
        withScheduleRemindersFragment { fragment ->
            createReminder(expectedCount = 1)

            advanceRobolectricLooperUntil { fragment.permissionsBottomSheet?.dialog?.isShowing == true }
            assertThat(Prefs.reminderNotifsRequestShown, equalTo(true))
            compose.onNodeWithText(getResourceString(CommonString.dialog_cancel)).performClick()
            advanceRobolectricLooperUntil { fragment.permissionsBottomSheet == null }
            assertThat(storedReminders().size, equalTo(1))
        }
        // Only the reminder-specific flag should prevent the second prompt, including below API 33.
        Prefs.notificationsBottomSheetShownBelowAPI33 = false
        withScheduleRemindersFragment { fragment ->
            createReminder(expectedCount = 2)

            assertThat(fragment.permissionsBottomSheet, nullValue())
        }
    }

    @Test
    fun notificationPermissionPromptIsSkippedWhenGranted() {
        shadowOf(targetContext.getSystemService<NotificationManager>()!!).setNotificationsEnabled(true)
        withScheduleRemindersFragment { fragment ->
            createReminder(expectedCount = 1)

            assertThat(fragment.permissionsBottomSheet, nullValue())
            assertThat(
                "A sheet that was not shown must not consume the first request",
                Prefs.reminderNotifsRequestShown,
                equalTo(false),
            )
        }
    }

    @Test
    fun cancelCreatingReminderDoesNotRequestNotificationPermission() {
        withScheduleRemindersFragment { fragment ->
            openAddDialog()
            compose.onNodeWithText(getResourceString(CommonString.dialog_cancel)).performClick()
            advanceRobolectricLooper()

            assertThat(fragment.permissionsBottomSheet, nullValue())
            assertThat(Prefs.reminderNotifsRequestShown, equalTo(false))
            assertThat(storedReminders(), empty())
        }
    }

    private fun openAddDialog() {
        compose.onNodeWithContentDescription(getResourceString(CommonString.add_reminder)).performClick()
        advanceRobolectricLooper()
    }

    private fun createReminder(expectedCount: Int) {
        openAddDialog()
        compose.onNodeWithText(getResourceString(CommonString.save)).performClick()
        advanceRobolectricLooperUntil { storedReminders().size == expectedCount }
    }

    private fun storedReminders(): List<ReviewReminder> = runBlocking { ReviewRemindersDatabase.getAllReminders().getRemindersList() }

    private val ScheduleRemindersFragment.permissionsBottomSheet: PermissionsBottomSheet?
        get() = childFragmentManager.fragments.filterIsInstance<PermissionsBottomSheet>().singleOrNull()
}
