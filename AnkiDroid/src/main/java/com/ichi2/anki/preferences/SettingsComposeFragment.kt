// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.ichi2.compose.theme.AnkiDroidTheme

/** The preference key or subpage-specific identifier to bring into view after a search. */
internal val Fragment.settingsSearchTarget: String?
    get() = arguments?.getString(SETTINGS_SEARCH_TARGET)

internal const val SETTINGS_SEARCH_TARGET = "settings_search_target"
internal const val SETTINGS_IS_SUBPAGE = "settings_is_subpage"

/** Primary tablet panes share the back button in their left-hand Settings screen. */
internal val Fragment.settingsBackAction: (() -> Unit)?
    get() {
        val parent = parentFragment as? PreferencesFragment
        if (parent?.settingsIsSplit == true && arguments?.getBoolean(SETTINGS_IS_SUBPAGE) != true) return null
        return { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

internal fun Fragment.settingsComposeView(
    @IdRes id: Int,
    content: @Composable () -> Unit,
): ComposeView =
    ComposeView(requireContext()).apply {
        this.id = id
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { AnkiDroidTheme(content) }
    }
