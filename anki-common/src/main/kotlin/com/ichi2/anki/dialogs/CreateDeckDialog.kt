// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Akshay Jadhav <jadhavakshay0701@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.common.utils.ext.findActivity
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.Decks
import com.ichi2.anki.runCatching
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.compose.ui.dialogs.TextInputDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import net.ankiweb.rsdroid.exceptions.BackendDeckIsFilteredException
import net.ankiweb.rsdroid.exceptions.BackendNotFoundException
import timber.log.Timber

/**
 * A dialog which manages the creation of decks, subdecks and filtered decks.
 *
 * Also used for deck renames: [DeckDialogType.RENAME_DECK]
 *
 * required property: [onNewDeckCreated]. Called on successful creation of a deck
 */
class CreateDeckDialog(
    private val context: Context,
    private val title: CharSequence,
    private val deckDialogType: DeckDialogType,
    private val parentId: DeckId?,
    /** The deck to rename. Only used by [DeckDialogType.RENAME_DECK] */
    private val renamedDeckId: DeckId? = null,
) {
    lateinit var onNewDeckCreated: ((DeckId) -> Unit)

    /** Initial name when renaming a deck. */
    var deckName: String = ""
    private var shownDialog: Dialog? = null

    init {
        require(deckDialogType != DeckDialogType.RENAME_DECK || renamedDeckId != null) {
            "renamedDeckId is required to rename a deck"
        }
    }

    enum class DeckDialogType {
        DECK,
        SUB_DECK,
        RENAME_DECK,
    }

    private val activity: FragmentActivity
        get() = requireNotNull(context.findActivity<FragmentActivity>())

    private val textInputHint: String
        get() =
            when (deckDialogType) {
                DeckDialogType.RENAME_DECK -> TR.actionsNewName()
                DeckDialogType.DECK, DeckDialogType.SUB_DECK -> TR.actionsName()
            }.dropLastWhile { it == ':' }

    private val positiveButtonTextRes: Int
        get() = if (deckDialogType == DeckDialogType.RENAME_DECK) CommonString.rename else CommonString.dialog_positive_create

    private enum class Validation { Invalid, Duplicate, Valid }

    fun showComposeDialog(): Dialog {
        val dialog =
            createPaperDialog(context) { window ->
                var input by remember { mutableStateOf(deckName) }
                var validatedInput by remember { mutableStateOf<String?>(null) }
                var result by remember { mutableStateOf<Validation?>(null) }
                LaunchedEffect(input) {
                    val requested = input
                    val validationResult = activity.runCatching { checkDeckName(requested) }
                    if (input == requested) {
                        result = validationResult
                        validatedInput = requested
                    }
                }
                TextInputDialog(
                    title = title.toString(),
                    label =
                        if (deckDialogType ==
                            DeckDialogType.RENAME_DECK
                        ) {
                            textInputHint
                        } else {
                            context.getString(CommonString.paper_dialog_deck_name)
                        },
                    confirmText = context.getString(positiveButtonTextRes),
                    initialText = deckName,
                    onConfirm = { name ->
                        if (name == validatedInput && result == Validation.Valid) onPositiveButtonClicked(name)
                    },
                    onDismissRequest = window::dismiss,
                    onTextChanged = { input = it },
                    errorMessageFor = { name ->
                        context
                            .getString(
                                CommonString.error_name_exists,
                            ).takeIf { name == validatedInput && result == Validation.Duplicate }
                    },
                    helperText = context.getString(CommonString.create_deck_numeric_hint).takeIf { input.containsNumberLargerThanNine() },
                    enabled = input == validatedInput && result == Validation.Valid,
                )
            }
        shownDialog = dialog
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        return dialog
    }

    private suspend fun checkDeckName(text: CharSequence): Validation {
        val name = fullyQualifyDeckName(text)
        if (name == null || !Decks.isValidDeckName(name)) return Validation.Invalid
        val existingDeckId = withCol { decks.idForName(name) }
        return if (existingDeckId == null || existingDeckId == renamedDeckId) Validation.Valid else Validation.Duplicate
    }

    /**
     * Returns the fully qualified deck name for the provided input
     * @param dialogText The user supplied text in the dialog
     * @return [dialogText], or the deck name containing `::` in case of [DeckDialogType.SUB_DECK]
     */
    private suspend fun fullyQualifyDeckName(dialogText: CharSequence) =
        when (deckDialogType) {
            DeckDialogType.DECK, DeckDialogType.RENAME_DECK -> dialogText.toString()
            DeckDialogType.SUB_DECK -> withCol { decks.getSubdeckName(parentId!!, dialogText.toString()) }
        }

    fun createSubDeck(
        did: DeckId,
        deckName: String?,
    ) {
        shownDialog?.dismiss()
        activity.launchCatchingTask {
            val deckNameWithParentName = withCol { decks.getSubdeckName(did, deckName) }
            if (deckNameWithParentName == null) {
                Timber.d("CreateDeckDialog::createSubDeck - Not creating invalid subdeck name '%s'", deckName)
                displayFeedback(context.getString(CommonString.invalid_deck_name), Snackbar.LENGTH_LONG)
                return@launchCatchingTask
            }
            createDeck(deckNameWithParentName)
        }
    }

    fun createDeck(deckName: String) {
        if (Decks.isValidDeckName(deckName)) {
            activity.launchCatchingTask {
                if (createNewDeck(deckName)) {
                    // 11668: Display feedback if a deck is created
                    displayFeedback(context.getString(CommonString.deck_created))
                }
            }
        } else {
            Timber.d("CreateDeckDialog::createDeck - Not creating invalid deck name '%s'", deckName)
            displayFeedback(context.getString(CommonString.invalid_deck_name), Snackbar.LENGTH_LONG)
        }
        // The dialog should be dismissed after the Keyboard 'Done' or Deck 'Ok' button is pressed
        shownDialog?.dismiss()
    }

    private suspend fun createNewDeck(deckName: String): Boolean {
        try {
            // create normal deck or sub deck
            Timber.i("CreateDeckDialog::createNewDeck")
            val newDeckId = withCol { decks.id(deckName) }
            Timber.d("Created deck '%s'; id: %d", deckName, newDeckId)
            onNewDeckCreated(newDeckId)
        } catch (filteredAncestor: BackendDeckIsFilteredException) {
            Timber.w(filteredAncestor)
            displayFeedback(filteredAncestor.localizedMessage ?: filteredAncestor.message ?: "", Snackbar.LENGTH_LONG)
            return false
        }
        return true
    }

    private fun onPositiveButtonClicked(deckName: String) {
        if (deckName.isNotEmpty()) {
            when (deckDialogType) {
                DeckDialogType.DECK -> {
                    // create deck
                    createDeck(deckName)
                }
                DeckDialogType.RENAME_DECK -> {
                    renameDeck(deckName)
                }
                DeckDialogType.SUB_DECK -> {
                    // create sub deck
                    createSubDeck(parentId!!, deckName)
                }
            }
        }
    }

    fun renameDeck(newDeckName: String) {
        if (!Decks.isValidDeckName(newDeckName)) {
            Timber.w("CreateDeckDialog::renameDeck not renaming deck to invalid name")
            Timber.d("invalid deck name: %s", newDeckName)
            displayFeedback(context.getString(CommonString.invalid_deck_name), Snackbar.LENGTH_LONG)
        } else {
            val deckId = renamedDeckId!!
            activity.launchCatchingTask {
                try {
                    // the backend reports no change if the new name normalises to the old one
                    if (withCol { decks.rename(deckId, newDeckName) }.deck) {
                        onNewDeckCreated(deckId)
                        // 11668: Display feedback if a deck is renamed
                        displayFeedback(context.getString(CommonString.deck_renamed))
                    }
                } catch (e: BackendDeckIsFilteredException) {
                    Timber.w(e)
                    // We get a localized string from libanki to explain the error
                    displayFeedback(e.localizedMessage ?: e.message ?: "", Snackbar.LENGTH_LONG)
                } catch (e: BackendNotFoundException) {
                    Timber.w(e, "not renaming deck %d", deckId)
                }
            }
        }
        // The dialog should be dismissed after the Keyboard 'Done' or Deck 'Ok' button is pressed
        shownDialog?.dismiss()
    }

    private fun displayFeedback(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ) {
        if (context is Activity) {
            context.showSnackbar(message, duration)
        } else {
            showThemedToast(context, message, duration == Snackbar.LENGTH_SHORT)
        }
    }
}

// to not match times. Example: "12:34:56"
// we use (?:[^:]|^) to ensure ":56" doesn't match
// we use (?:[^:]|$) to ensure "12:" doesn't match
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun CharSequence.containsNumberLargerThanNine(): Boolean = Regex("""(?:[^:]|^)[1-9]\d+(?:[^:]|$)""").find(this) != null
