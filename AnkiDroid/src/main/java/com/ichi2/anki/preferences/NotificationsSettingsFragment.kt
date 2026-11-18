// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.app.AlarmManager
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import androidx.preference.ListPreference
import com.ichi2.anki.CommonPreferenceKey
import com.ichi2.anki.R
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.services.BootService.Companion.scheduleNotification
import com.ichi2.anki.services.NotificationService

/**
 * Fragment with preferences related to notifications
 */
class NotificationsSettingsFragment : RemainingSettingsFragment(R.id.settings_notifications) {
    override val preferenceResource: Int
        get() = R.xml.preferences_notifications
    override val analyticsScreenNameConstant: String
        get() = "prefs.notifications"

    override fun initSubscreen() {
        // Minimum cards due
        // The number of cards that should be due today in a deck to justify adding a notification.
        requirePreference<ListPreference>(CommonPreferenceKey.pref_notifications_minimum_cards_due_key).apply {
            setOnPreferenceChangeListener { _, newValue ->
                // BootService checks the saved threshold when deciding whether to schedule an alarm.
                value = newValue as String
                if (value.toInt() < PENDING_NOTIFICATIONS_ONLY) {
                    scheduleNotification(TimeManager.time, requireContext())
                } else {
                    val intent =
                        PendingIntentCompat.getBroadcast(
                            requireContext(),
                            0,
                            Intent(requireContext(), NotificationService::class.java),
                            0,
                            false,
                        )
                    val alarmManager = requireActivity().getSystemService(ALARM_SERVICE) as AlarmManager
                    if (intent != null) {
                        alarmManager.cancel(intent)
                    }
                }
                true
            }
        }
    }
}
