// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation

/**
 * More, the bottom bar's last tab on phones ([MoreScreen]). Its rows open what the side drawer's
 * Settings, Help and Support AnkiDroid opened, through the same [NavigationDrawerActivity] calls.
 */
class MoreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(inflater.context).apply {
        id = R.id.more_content
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AnkiDroidTheme {
                MoreScreen(
                    onSettings = { requireNavigationDrawerActivity().openSettings() },
                    onHelp = { requireNavigationDrawerActivity().openHelp() },
                    onSupport = { requireNavigationDrawerActivity().openSupport() },
                    modifier = Modifier.background(MaterialTheme.foundation.colors.background),
                )
            }
        }
    }
}
