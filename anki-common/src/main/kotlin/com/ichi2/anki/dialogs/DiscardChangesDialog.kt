// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.content.Context
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.android.R
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

object DiscardChangesDialog {
    fun showDialog(
        context: Context,
        positiveButtonText: String = context.getString(CommonString.discard),
        negativeButtonText: String = with(context) { TR.sentenceCase.keepEditing },
        neutralButtonText: String? = null,
        message: String = TR.cardTemplatesDiscardChanges(),
        negativeMethod: () -> Unit = {},
        neutralMethod: (() -> Unit)? = null,
        positiveMethod: () -> Unit,
    ) = createPaperDialog(context) { dialog ->
        PaperDialog(
            title = message,
            onDismissRequest = dialog::dismiss,
            mark = DialogMark(R.drawable.symbol_warning, warning = true),
            actions =
                buildList {
                    add(
                        DialogAction(negativeButtonText, {
                            dialog.dismiss()
                            negativeMethod()
                        }),
                    )
                    if (neutralButtonText != null && neutralMethod != null) {
                        add(
                            DialogAction(neutralButtonText, {
                                dialog.dismiss()
                                neutralMethod()
                            }),
                        )
                    }
                    add(
                        DialogAction(positiveButtonText, {
                            dialog.dismiss()
                            positiveMethod()
                        }, DialogActionStyle.Destructive),
                    )
                },
        )
    }.also {
        Timber.i("showing 'discard changes' dialog")
        it.show()
    }
}
