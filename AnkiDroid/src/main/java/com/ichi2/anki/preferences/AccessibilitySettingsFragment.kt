// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.preference.SwitchPreferenceCompat
import com.ichi2.anki.R
import com.ichi2.anki.common.android.Animations

/**
 * Fragment with preferences related to notifications
 */
class AccessibilitySettingsFragment : RemainingSettingsFragment(R.id.settings_accessibility) {
    override val preferenceResource: Int
        get() = R.xml.preferences_accessibility
    override val analyticsScreenNameConstant: String
        get() = "prefs.accessibility"

    override fun initSubscreen() = Unit

    override fun onResume() {
        super.onResume()
        requirePreference<SwitchPreferenceCompat>(R.string.safe_display_key).isEnabled =
            Animations.areSystemAnimationsEnabled(requireContext())
    }
}
