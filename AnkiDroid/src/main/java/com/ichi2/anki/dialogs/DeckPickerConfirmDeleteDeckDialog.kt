// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.anki.utils.ext.requireLong
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.createPaperDialog

class DeckPickerConfirmDeleteDeckDialog : AnalyticsDialogFragment() {
    private val deckId get() = requireArguments().requireLong("deckId")
    private val deckName get() = requireArguments().getString("deckName")
    private val totalCards get() = requireArguments().getInt("totalCards")
    private val isFilteredDeck get() = requireArguments().getBoolean("isFilteredDeck")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message =
            if (isFilteredDeck) {
                getString(CommonString.delete_cram_deck_message, deckName)
            } else {
                resources.getQuantityString(CommonPlurals.delete_deck_message, totalCards, deckName, totalCards)
            }
        return createPaperDialog(requireContext()) {
            ConfirmationDialog(
                title = getString(CommonString.delete_deck_title),
                message = message,
                confirm =
                    DialogAction(with(requireContext()) { CollectionManager.TR.sentenceCase.deleteDeck }, {
                        (activity as DeckPicker).deleteDeck(deckId)
                        activity?.dismissAllDialogFragments()
                    }, DialogActionStyle.Destructive),
                onDismissRequest = { activity?.dismissAllDialogFragments() },
            )
        }
    }

    companion object {
        fun newInstance(
            deckName: String,
            deckId: DeckId,
            totalCards: Int,
            isFilteredDeck: Boolean,
        ): DeckPickerConfirmDeleteDeckDialog {
            val f = DeckPickerConfirmDeleteDeckDialog()
            val args = Bundle()
            args.putString("deckName", deckName)
            args.putLong("deckId", deckId)
            args.putInt("totalCards", totalCards)
            args.putBoolean("isFilteredDeck", isFilteredDeck)
            f.arguments = args
            return f
        }
    }
}
