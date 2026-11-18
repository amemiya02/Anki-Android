// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.filtered

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.R
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.anki.utils.ext.window
import com.ichi2.anki.utils.openUrl

/** Builds or rebuilds a filtered Deck using the options held by [FilteredDeckOptionsViewModel]. */
class FilteredDeckOptionsFragment : Fragment() {
    private val viewModel by viewModels<FilteredDeckOptionsViewModel>()
    private val discardBackHandler =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                DiscardChangesDialog.showDialog(context = requireActivity()) {
                    finishScreen()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        settingsComposeView(R.id.filtered_deck_options_screen) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            FilteredDeckOptionsScreen(
                state = state,
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                onHelp = { requireActivity().openUrl(R.string.link_filtered_decks_help) },
                onName = viewModel::onDeckNameChange,
                onSearch = viewModel::onSearchChange,
                onSearchInBrowser = viewModel::onSearchInBrowser,
                onLimit = viewModel::onLimitChange,
                onOrder = viewModel::onCardsOptionsChange,
                onSecondFilter = viewModel::onSecondFilterStatusChange,
                onReschedule = viewModel::onRescheduleChange,
                onDelay = viewModel::onRescheduleDelayChange,
                onAllowEmpty = viewModel::onAllowEmptyChange,
                onExcludedCards = viewModel::onShowExcludedCards,
                onBuild = {
                    if ((viewModel.state.value as? FilteredDeckOptions)?.isBuildingAllowed == true) viewModel.build()
                },
                onErrorClosed = viewModel::clearError,
                onExit = { finishScreen() },
            )
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        } else {
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.background, typedValue, true)
            @Suppress("DEPRECATION")
            window.navigationBarColor = typedValue.data
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, discardBackHandler)
        viewModel.hasUnsavedChanges.collectInViewLifecycle { discardBackHandler.isEnabled = it }
        viewModel.state.collectInViewLifecycle { state ->
            when (state) {
                DeckBuilt -> finishScreen()
                is FilteredDeckOptions -> {
                    if (state.throwable == null && state.browserQuery != null) {
                        navigate(BrowserDestination.Search(query = state.browserQuery, allDecks = false))
                        viewModel.clearSearchInBrowser()
                    }
                }
                is Initializing -> Unit
            }
        }
    }

    companion object {
        const val ARG_DECK_ID = "arg_deck_id"
        const val ARG_SEARCH = "arg_search"
        const val ARG_SEARCH_2 = "arg_search_2"

        /**
         * Starts a [ConfigAwareSingleFragmentActivity] containing this fragment. If [search] or
         * [search2] are provided, they will be used as the default search text.
         * @param did the [DeckId] of a filtered deck. If it's non-zero, load and modify its settings
         * otherwise build a new deck and derive settings from the current deck.
         */
        fun getIntent(
            context: Context,
            did: DeckId = 0,
            search: String? = null,
            search2: String? = null,
        ): Intent =
            ConfigAwareSingleFragmentActivity.getIntent(
                context = context,
                fragmentClass = FilteredDeckOptionsFragment::class,
                arguments =
                    Bundle().apply {
                        putLong(ARG_DECK_ID, did)
                        putString(ARG_SEARCH, search)
                        putString(ARG_SEARCH_2, search2)
                    },
            )
    }
}
