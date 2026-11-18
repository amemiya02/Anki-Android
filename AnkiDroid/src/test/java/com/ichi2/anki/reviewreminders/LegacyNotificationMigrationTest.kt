// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonPreferenceKey
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.scheduleAllNotifications
import com.ichi2.anki.services.NotificationService
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LegacyNotificationMigrationTest : RobolectricTest() {
    @Test
    fun `startup migrates a due threshold once and cancels the old repeating alarm`() = runTest {
        val oldAlarm =
            assertNotNull(
                PendingIntentCompat.getBroadcast(
                    targetContext,
                    0,
                    Intent(targetContext, NotificationService::class.java),
                    0,
                    false,
                ),
            )
        targetContext.getSystemService<AlarmManager>()!!.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + AlarmManager.INTERVAL_DAY,
            AlarmManager.INTERVAL_DAY,
            oldAlarm,
        )
        setThreshold("25")
        val existing = ReviewReminder.createReviewReminder(time = ReviewReminderTime(22, 30))
        ReviewRemindersDatabase.insertReminder(existing)

        scheduleAllNotifications(targetContext)
        scheduleAllNotifications(targetContext)

        val reminders = ReviewRemindersDatabase.getRemindersForScope(ReviewReminderScope.Global)
        val migrated = reminders.getRemindersList().single { it.id != existing.id }
        assertEquals(ReviewReminderTime(8, 0), migrated.time)
        assertEquals(ReviewReminderCardTriggerThreshold(25), migrated.cardTriggerThreshold)
        assertEquals(ReviewReminderThresholdFilter(), migrated.thresholdFilter)
        assertTrue(migrated.enabled)
        assertNull(
            PendingIntentCompat.getBroadcast(
                targetContext,
                0,
                Intent(targetContext, NotificationService::class.java),
                PendingIntent.FLAG_NO_CREATE,
                false,
            ),
        )

        ReviewRemindersDatabase.deleteReminder(migrated)
        scheduleAllNotifications(targetContext)
        assertEquals(setOf(existing.id), ReviewRemindersDatabase.getAllReminders().getRemindersList().map { it.id }.toSet())
    }

    @Test
    fun `zero is an enabled threshold`() = runTest {
        setThreshold("0")

        scheduleAllNotifications(targetContext)

        val reminder = ReviewRemindersDatabase.getRemindersForScope(ReviewReminderScope.Global).getRemindersList().single()
        assertEquals(ReviewReminderCardTriggerThreshold(0), reminder.cardTriggerThreshold)
        assertTrue(reminder.enabled)
    }

    @Test
    fun `never and pending messages only do not create a review reminder`() = runTest {
        for (oldValue in listOf("2000000", "1000000", null)) {
            ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
            setThreshold(oldValue)
            scheduleAllNotifications(targetContext)
            assertTrue(ReviewRemindersDatabase.getAllReminders().isEmpty(), "Legacy value: $oldValue")
        }
    }

    private fun setThreshold(value: String?) {
        targetContext.sharedPrefs().edit {
            putString(targetContext.getString(CommonPreferenceKey.pref_notifications_minimum_cards_due_key), value)
        }
    }
}
