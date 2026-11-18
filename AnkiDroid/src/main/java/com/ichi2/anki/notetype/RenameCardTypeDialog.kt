// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import android.content.Context
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.compose.ui.dialogs.TextInputDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

class RenameCardTypeDialog {
    companion object {
        /**
         * @param prefill The text to initially appear in the EditText
         * @param currentName The name of the card type to be renamed
         * @param existingNames Unsaved card type names from the currently edited note type,
         * used for validation.
         */
        fun showInstance(
            context: Context,
            prefill: String,
            currentName: CardTypeName,
            existingNames: List<CardTypeName>,
            block: (result: CardTypeName) -> Unit,
        ) {
            createPaperDialog(context) { dialog ->
                TextInputDialog(
                    title = context.getString(CommonString.rename_card_type),
                    label = CollectionManager.TR.actionsNewName().removeSuffix(":"),
                    confirmText = context.getString(CommonString.rename),
                    initialText = prefill,
                    errorMessageFor = { text ->
                        val name = CardTypeName.fromString(text)
                        when {
                            name.value.replace("\"", "").isBlank() || currentName == name -> ""
                            name in existingNames -> context.getString(CommonString.error_name_exists)
                            else -> null
                        }
                    },
                    onConfirm = {
                        block(CardTypeName.fromString(it))
                        dialog.dismiss()
                    },
                    onDismissRequest = dialog::dismiss,
                )
            }.show()
        }
    }
}
