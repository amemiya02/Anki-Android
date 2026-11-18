// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import androidx.core.content.getSystemService
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.reviewreminders.ReviewReminderAlarmManager
import com.ichi2.anki.reviewreminders.ReviewRemindersDatabase
import com.ichi2.anki.services.NotificationService

/** Old values of one million or more represent "pending messages only" or "never". */
private const val LEGACY_PENDING_MESSAGES_ONLY = 1_000_000
private const val LEGACY_DUE_NOTIFICATION_ID = 1

/** Restore scheduled reminders on app startup and device boot. */
suspend fun scheduleAllNotifications(context: Context) {
    migrateLegacyNotifications(context)
    ReviewReminderAlarmManager.scheduleAllEnabledReviewReminderNotifications(context)
}

internal suspend fun migrateLegacyNotifications(context: Context) {
    val thresholdKey = context.getString(CommonPreferenceKey.pref_notifications_minimum_cards_due_key)
    // A locale-specific Context can bypass the Application's active-profile preference wrapper.
    val oldThreshold = (context.applicationContext.sharedPrefs().all[thresholdKey] as? String)?.toIntOrNull()
    ReviewRemindersDatabase.migrateLegacyThreshold(oldThreshold?.takeIf { it in 0 until LEGACY_PENDING_MESSAGES_ONLY })

    // Pending intents can survive an app update. This is the old broadcast, with no action.
    PendingIntentCompat
        .getBroadcast(
            context,
            0,
            Intent(context, NotificationService::class.java),
            PendingIntent.FLAG_NO_CREATE,
            false,
        )?.let { alarm ->
            context.getSystemService<AlarmManager>()?.cancel(alarm)
            alarm.cancel()
        }
    context.getSystemService<NotificationManager>()?.cancel(LEGACY_DUE_NOTIFICATION_ID)
}
