// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import android.annotation.SuppressLint
import android.content.Context
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType
import com.ichi2.anki.dialogs.DatabaseErrorDialog.UninstallListItem.Companion.createNoStorageList
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

/**
 * Inform the user that Android 13+ has permanently revoked access to `WRITE_EXTERNAL_STORAGE`.
 * Typically due to app permissions being revoked from unused apps
 *
 * Their collection is safe, but inaccessible
 *
 * Provide recovery options
 *
 * Issue 14423
 *
 * @see DatabaseErrorDialogType.DIALOG_STORAGE_UNAVAILABLE_AFTER_UNINSTALL
 */
object AndroidPermanentlyRevokedPermissionsDialog {
    @SuppressLint("CheckResult")
    fun show(context: AnkiActivity) {
        val listItemData = createNoStorageList()

        val message =
            context.getString(
                CommonString.directory_revoked_after_inactivity,
                "WRITE_EXTERNAL_STORAGE",
                getCurrentAnkiDroidDirectoryPath(context),
            )
        createPaperDialog(context) { dialog ->
            PaperDialog(
                title = context.getString(CommonString.vague_error),
                onDismissRequest = {},
                mark = DialogMark(R.drawable.symbol_warning, warning = true),
            ) {
                DialogBody(message)
                listItemData.forEach { item ->
                    DialogChoiceRow(
                        DialogChoice(context.getString(item.stringRes)),
                        checked = false,
                        onClick = {
                            item.onClick(context)
                            if (item.dismissesDialog) dialog.dismiss()
                        },
                        multiple = false,
                    )
                }
            }
        }.apply { setCancelable(false) }.show()
    }

    private fun getCurrentAnkiDroidDirectoryPath(context: Context): String =
        try {
            CollectionHelper.getCurrentAnkiDroidDirectory(context).absolutePath
        } catch (e: Exception) {
            Timber.w(e)
            context.getString(CommonString.card_browser_unknown_deck_name)
        }
}
