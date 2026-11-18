// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.content.Context
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.R
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.destinations.DeckOptionsEntry
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import com.ichi2.anki.ui.windows.reviewer.ReviewerViewModel
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

/**
 * Shows a list of decks from which the user can select one to display its deck options.
 * @see ReviewerFragment
 * @see ReviewerViewModel.emitDeckOptionsDestination
 */
@NeedsTest("verify null deck names => null entry")
fun Context.showDeckOptionsSelectionDialog(
    options: List<DeckOptionsEntry>,
    onDeckSelected: (DeckOptionsEntry) -> Unit,
) {
    Timber.i("Showing deck options selection dialog")
    createPaperDialog(this) { dialog ->
        SingleChoiceDialog(
            title = TR.deckConfigWhichDeck(),
            choices =
                options.map { option ->
                    DialogChoice(option.name.toString(), icon = R.drawable.symbol_filter_alt.takeIf { option.isFiltered })
                },
            selectedIndex = null,
            onDismissRequest = dialog::dismiss,
            onSelect = { index ->
                dialog.dismiss()
                onDeckSelected(options[index])
            },
        )
    }.show()
}
