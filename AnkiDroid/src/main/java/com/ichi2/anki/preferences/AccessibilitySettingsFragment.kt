// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import com.ichi2.anki.R
import com.ichi2.anki.common.android.Animations
import com.ichi2.preferences.SwitchPreference

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
        requirePreference<SwitchPreference>(R.string.safe_display_key).isEnabled =
            Animations.areSystemAnimationsEnabled(requireContext())
    }
}
