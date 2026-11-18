// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.ichi2.anki.R
import com.ichi2.anki.common.analytics.Analytics
import com.ichi2.compose.theme.AnkiDroidTheme

class HeaderFragment : Fragment() {
    private var selectedGroup by mutableStateOf<SettingsGroup?>(null)
    private var groups by mutableStateOf(emptyList<SettingsGroup>())
    private var searchIndex by mutableStateOf(emptyList<SettingsSearchEntry>())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Analytics.sendAnalyticsScreenView("prefs.initialPage")
        return ComposeView(requireContext()).apply {
            id = R.id.settings_home
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    var query by rememberSaveable { mutableStateOf("") }
                    val focus = LocalFocusManager.current
                    val keyboard = LocalSoftwareKeyboardController.current
                    val clearSearch = {
                        query = ""
                        focus.clearFocus()
                        keyboard?.hide()
                        Unit
                    }
                    BackHandler(query.isNotEmpty(), clearSearch)
                    SettingsHomeScreen(
                        groups = groups,
                        selectedGroup = selectedGroup,
                        index = searchIndex,
                        query = query,
                        onQueryChange = {
                            if (query.isBlank() && it.isNotBlank()) refreshSearchIndex()
                            query = it
                        },
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onGroup = { (parentFragment as PreferencesFragment).openGroup(it) },
                        onResult = {
                            clearSearch()
                            (parentFragment as PreferencesFragment).openSearchResult(it)
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        groups = SettingsGroup.visibleGroups()
        searchIndex = buildSettingsSearchIndex(requireContext(), groups)
    }

    // Refresh on a tablet when the adjacent settings pane edits a searchable value.
    internal fun refreshSearchIndex() {
        if (isResumed) searchIndex = buildSettingsSearchIndex(requireContext(), groups)
    }

    internal fun select(group: SettingsGroup?) {
        selectedGroup = group
    }
}
