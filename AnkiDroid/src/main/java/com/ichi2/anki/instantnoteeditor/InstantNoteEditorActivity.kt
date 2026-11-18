// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.instantnoteeditor

import android.content.Intent
import android.os.Bundle
import com.ichi2.anki.AbstractIntentHandler
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.utils.AssetHelper.TEXT_PLAIN

/** Dispatches shared text, retaining Android's published share target component. */
class InstantNoteEditorActivity : AbstractIntentHandler() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_SEND && intent.type == TEXT_PLAIN) {
            val args = Bundle().apply { putCharSequence(Intent.EXTRA_TEXT, intent.getCharSequenceExtra(Intent.EXTRA_TEXT)) }
            val destination = SingleFragmentActivity.getIntent(this, InstantNoteEditorFragment::class, args)
            startActivity(DeckPicker.getEntryIntent(this, destination))
        }
        finish()
    }

    /**
     * Enum representing the modes available for cloze functionality.
     */
    enum class ClozeMode {
        INCREMENT,
        NO_INCREMENT,
    }

    /**
     * This enum class represents the different edit modes available for the user interface element.
     */
    enum class EditMode {
        /**
         * In this mode, a single tap on the text will turn it to cloze
         */
        SINGLE_TAP,

        /**
         * In this mode, user can edit the text as they want
         */
        ADVANCED,
    }

    /**
     * Enum class that represent the dialog that can be shown when the InstantEditor is initialized
     * **/
    enum class DialogType {
        /** Indicates that no cloze note types were found. **/
        NO_CLOZE_NOTE_TYPES_DIALOG,

        /** Indicates that the editor dialog should be shown. **/
        SHOW_EDITOR_DIALOG,
    }
}
