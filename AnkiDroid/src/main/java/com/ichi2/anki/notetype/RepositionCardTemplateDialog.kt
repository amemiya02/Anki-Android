// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import android.content.Context
import androidx.compose.ui.text.input.KeyboardType
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.compose.ui.dialogs.TextInputDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

class RepositionCardTemplateDialog {
    companion object {
        fun showInstance(
            context: Context,
            numberOfTemplates: Int,
            result: (Int) -> Unit,
        ) {
            createPaperDialog(context) { dialog ->
                val message = CollectionManager.TR.cardTemplatesEnterNewCardPosition1(numberOfTemplates)
                TextInputDialog(
                    title = "",
                    label = message,
                    confirmText = context.getString(CommonString.dialog_ok),
                    keyboardType = KeyboardType.Number,
                    errorMessageFor = { text ->
                        if (text.toIntOrNull()?.let { it in 1..numberOfTemplates } == true) null else ""
                    },
                    onConfirm = {
                        result(it.toInt())
                        dialog.dismiss()
                    },
                    onDismissRequest = dialog::dismiss,
                )
            }.show()
        }
    }
}
