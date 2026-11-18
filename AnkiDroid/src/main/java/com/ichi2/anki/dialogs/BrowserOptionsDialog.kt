// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Akshit Sinha <akshitsinha3@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.browser.BrowserColumnSelectionFragment
import com.ichi2.anki.browser.CardBrowserViewModel
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogDetail
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

class BrowserOptionsDialog : DialogFragment() {
    private val viewModel: CardBrowserViewModel by activityViewModels()

    private val cardsOrNotes by lazy {
        when (arguments?.getBoolean(ARG_CARDS_OR_NOTES)) {
            true -> CardsOrNotes.CARDS
            false -> CardsOrNotes.NOTES
            null -> {
                // Default case, and what we'll do if there were no arguments supplied
                Timber.w("BrowserOptionsDialog instantiated without configuration.")
                CardsOrNotes.CARDS
            }
        }
    }

    private val useBrowserControls get() = arguments?.getBoolean(ARG_USE_BROWSER_CONTROLS) == true

    private val isTruncated by lazy {
        arguments?.getBoolean(ARG_IS_TRUNCATED) ?: run {
            Timber.w("BrowserOptionsDialog instantiated without configuration.")
            false
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) {
            var mode by rememberSaveable { mutableStateOf(cardsOrNotes) }
            var truncated by rememberSaveable { mutableStateOf(isTruncated) }
            var ignoreAccents by rememberSaveable { mutableStateOf(viewModel.shouldIgnoreAccents) }
            val showDefaultSearch = useBrowserControls || Prefs.devUsingCardBrowserSearchView
            var search by rememberSaveable { mutableStateOf(if (showDefaultSearch) viewModel.defaultBrowserSearch else "") }
            PaperDialog(
                title = TR.sentenceCase.browserOptions,
                onDismissRequest = ::dismiss,
                actions =
                    listOf(
                        DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                        DialogAction(getString(CommonString.save), {
                            if (!useBrowserControls) viewModel.setCardsOrNotes(mode)
                            viewModel.setTruncated(truncated)
                            viewModel.setIgnoreAccents(ignoreAccents)
                            viewModel.setDefaultSearchText(search)
                            dismiss()
                        }, DialogActionStyle.Filled),
                    ),
            ) {
                if (!useBrowserControls) {
                    DialogDetail(TR.sentenceCase.toggleCardsNotes)
                    Column(Modifier.selectableGroup()) {
                        DialogChoiceRow(DialogChoice(getString(CommonString.show_cards)), mode == CardsOrNotes.CARDS, {
                            mode = CardsOrNotes.CARDS
                        }, multiple = false)
                        DialogChoiceRow(DialogChoice(getString(CommonString.show_notes)), mode == CardsOrNotes.NOTES, {
                            mode = CardsOrNotes.NOTES
                        }, multiple = false)
                    }
                }
                DialogChoiceRow(
                    DialogChoice(getString(CommonString.card_browser_truncate), getString(CommonString.truncate_content_help)),
                    truncated,
                    { truncated = !truncated },
                    multiple = true,
                )
                DialogDetail(TR.preferencesBrowsing())
                DialogChoiceRow(
                    DialogChoice(TR.preferencesIgnoreAccentsInSearch()),
                    ignoreAccents,
                    { ignoreAccents = !ignoreAccents },
                    multiple = true,
                )
                if (showDefaultSearch) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text(TR.preferencesDefaultSearchText()) },
                        placeholder = { Text(TR.preferencesDefaultSearchTextExample()) },
                        trailingIcon = {
                            if (search.isNotEmpty()) {
                                IconButton(onClick = { search = "" }) {
                                    Icon(
                                        painterResource(CommonR.drawable.symbol_close),
                                        getString(androidx.appcompat.R.string.abc_searchview_description_clear),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!useBrowserControls) {
                    TextButton(onClick = ::openColumnManager) { Text(getString(CommonString.browse_manage_columns)) }
                    TextButton(onClick = {
                        FlagRenameDialog().show(parentFragmentManager, "FlagRenameDialog")
                        dismiss()
                    }) { Text(getString(CommonString.rename_flag)) }
                }
            }
        }

    /** Opens [BrowserColumnSelectionFragment] for the current selection of [CardsOrNotes] */
    private fun openColumnManager() {
        val dialog = BrowserColumnSelectionFragment.createInstance(viewModel.cardsOrNotes)
        dialog.show(requireActivity().supportFragmentManager, null)
    }

    companion object {
        private const val ARG_USE_BROWSER_CONTROLS = "useBrowserControls"
        private const val ARG_CARDS_OR_NOTES = "cardsOrNotes"
        private const val ARG_IS_TRUNCATED = "isTruncated"

        fun newInstance(
            cardsOrNotes: CardsOrNotes,
            isTruncated: Boolean,
            useBrowserControls: Boolean = false,
        ): BrowserOptionsDialog {
            Timber.i("BrowserOptionsDialog::newInstance")
            return BrowserOptionsDialog().apply {
                arguments =
                    Bundle().apply {
                        putBoolean(ARG_CARDS_OR_NOTES, cardsOrNotes == CardsOrNotes.CARDS)
                        putBoolean(ARG_IS_TRUNCATED, isTruncated)
                        putBoolean(ARG_USE_BROWSER_CONTROLS, useBrowserControls)
                    }
            }
        }
    }
}
