// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewer

import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber

/** Card Template API modes for the study screen. */
enum class FullScreenMode(
    private val prefValue: String,
) {
    /** Display both navigation and buttons (default) */
    BUTTONS_AND_MENU("0"),

    /** Remove the menu bar, keeps answer button. */
    BUTTONS_ONLY("1"),

    /** Remove both the toolbar and answer buttons. */
    FULLSCREEN_ALL_GONE("2"),
    ;

    companion object {
        fun upgradeFromLegacyPreference(preferences: SharedPreferences) {
            if (!preferences.contains("fullscreenReview")) return

            Timber.i("Old version of Anki - Fixing Fullscreen")
            // clear fullscreen flag as we use a integer
            val old = preferences.getBoolean("fullscreenReview", false)
            val newValue = if (old) BUTTONS_ONLY else BUTTONS_AND_MENU
            preferences.edit {
                putString("fullscreenMode", newValue.prefValue)
                remove("fullscreenReview")
            }
        }
    }
}
