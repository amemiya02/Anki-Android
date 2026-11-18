// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.services.NotificationService
import com.ichi2.anki.utils.ext.getParcelableCompat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ReviewReminderWorkflowTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    @After
    fun clearReminderFixture() {
        ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
    }

    @Test
    fun `add a Deck reminder with a custom time and conditions and turn it off`() {
        val deck = addDeck("Vocabulary")
        Settings.System.putString(targetContext.contentResolver, Settings.System.TIME_12_24, "24")
        shadowOf(targetContext.getSystemService<NotificationManager>()!!).setNotificationsEnabled(true)

        withScheduleRemindersFragment { reminders ->
            compose.onNodeWithContentDescription(reminders.getString(CommonString.add_reminder)).performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText(reminders.getString(R.string.review_reminder_deck)).performScrollTo().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText("Vocabulary").performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText(reminders.getString(R.string.review_reminder_time)).performScrollTo().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(reminders.getString(R.string.review_reminder_keyboard_input)).performScrollTo().performClick()
            // The editor is still visible behind the clock dialog and has its own number field.
            val timeTitle = reminders.getString(R.string.review_reminder_select_time)
            val clockInputs = compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(hasAnyDescendant(hasText(timeTitle))))
            assertEquals(2, clockInputs.fetchSemanticsNodes().size)
            clockInputs[0].performClick().performTextReplacement("09")
            clockInputs[1].performClick().performTextReplacement("35")
            compose.onNodeWithText(reminders.getString(CommonString.dialog_ok)).performClick()
            advanceRobolectricLooper()

            compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("25")
            compose.onNodeWithText(reminders.getString(R.string.review_reminder_learning)).performScrollTo().performClick()
            compose.onNodeWithText(reminders.getString(R.string.review_reminder_only_if_unstudied)).performScrollTo().performClick()
            compose.onNodeWithText(reminders.getString(CommonString.save)).performClick()
            advanceRobolectricLooperUntil { storedReminder() != null }

            val saved = checkNotNull(storedReminder())
            assertEquals(ReviewReminderTime(9, 35), saved.time)
            assertEquals(ReviewReminderScope.DeckSpecific(deck), saved.scope)
            assertEquals(ReviewReminderCardTriggerThreshold(25), saved.cardTriggerThreshold)
            assertEquals(ReviewReminderThresholdFilter(countLrn = false), saved.thresholdFilter)
            assertTrue(saved.onlyNotifyIfNoReviews)
            assertTrue(hasAlarm(saved.id))

            val toggle = reminders.getString(R.string.review_reminder_switch, saved.time.toFormattedString(reminders.requireContext()))
            compose.onNodeWithContentDescription(toggle).performClick()
            advanceRobolectricLooperUntil { storedReminder()?.enabled == false }
            assertFalse(hasAlarm(saved.id))
            compose.onNodeWithContentDescription(toggle).assertIsOff()

            compose.onNodeWithText(saved.time.toFormattedString(reminders.requireContext())).performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(reminders.getString(CommonString.dialog_positive_delete)).performScrollTo().performClick()
            advanceRobolectricLooper()
            val confirmationTitle = reminders.getString(CommonString.add_edit_reminder_delete_confirmation_title)
            compose.onNodeWithText(confirmationTitle).assertIsDisplayed()
            compose
                .onNode(
                    hasText(reminders.getString(CommonString.dialog_positive_delete)) and
                        hasAnyAncestor(hasAnyDescendant(hasText(confirmationTitle))),
                ).performClick()
            advanceRobolectricLooperUntil { storedReminder() == null }
            compose.onNodeWithText(reminders.getString(CommonString.you_have_no_review_reminders)).assertIsDisplayed()
        }
    }

    private fun storedReminder(): ReviewReminder? =
        runBlocking { ReviewRemindersDatabase.getAllReminders().getRemindersList().singleOrNull() }

    @Suppress("DEPRECATION") // Robolectric exposes the recorded PendingIntent as a field.

    private fun hasAlarm(id: ReviewReminderId): Boolean =
        shadowOf(targetContext.getSystemService<AlarmManager>()!!).scheduledAlarms.any { alarm ->
            val intent = shadowOf(alarm.operation).savedIntent
            intent.action == NotificationService.NotificationServiceAction.ScheduleRecurringNotifications.actionString &&
                intent.extras?.getParcelableCompat<ReviewReminderId>(NotificationService.EXTRA_REVIEW_REMINDER_ID) == id
        }
}
