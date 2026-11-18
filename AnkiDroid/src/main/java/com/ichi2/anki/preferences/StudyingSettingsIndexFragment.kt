// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.ichi2.anki.CommonString
import com.ichi2.anki.R

/** Landing page while the Scheduling and Study screen preferences still have separate hosts. */
class StudyingSettingsIndexFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        settingsComposeView(R.id.settings_studying) {
            SettingsScrollPage(stringResource(R.string.settings_studying), settingsBackAction) {
                SettingsDivider()
                SettingsRow(
                    stringResource(CommonString.pref_cat_scheduling),
                    onClick = { (parentFragment as PreferencesFragment).openSubpage(ReviewingSettingsFragment()) },
                    trailing = { SettingsChevron() },
                )
                SettingsRow(
                    stringResource(R.string.settings_study_screen),
                    onClick = { (parentFragment as PreferencesFragment).openSubpage(ReviewerOptionsFragment()) },
                    trailing = { SettingsChevron() },
                )
            }
        }
}
