// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.ichi2.anki.databinding.FragmentMoreBinding
import com.ichi2.compose.theme.AnkiDroidTheme
import dev.androidbroadcast.vbpd.viewBinding

/**
 * More, the bottom bar's last tab on phones ([MoreScreen]). Its rows open what the side drawer's
 * Settings, Help and Support AnkiDroid opened, through the same [NavigationDrawerActivity] calls.
 */
class MoreFragment : Fragment(R.layout.fragment_more) {
    private val binding by viewBinding(FragmentMoreBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.moreContent.setContent {
            AnkiDroidTheme {
                MoreScreen(
                    onSettings = { requireNavigationDrawerActivity().openSettings() },
                    onHelp = { requireNavigationDrawerActivity().openHelp() },
                    onSupport = { requireNavigationDrawerActivity().openSupport() },
                )
            }
        }
    }
}
