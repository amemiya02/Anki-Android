// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.InitialActivity.StartupFailure.InitializationError
import com.ichi2.anki.R
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

object FatalErrorDialog {
    fun build(
        activity: AnkiActivity,
        failure: InitializationError,
    ): Dialog {
        Timber.i("Displaying 'Fatal error'")
        return createPaperDialog(activity) { dialog ->
            PaperDialog(
                title = activity.getString(CommonString.ankidroid_init_failed_webview_title),
                onDismissRequest = {},
                mark = DialogMark(R.drawable.symbol_error, warning = true),
                actions =
                    listOfNotNull(
                        failure.infoLink?.let { url ->
                            DialogAction(activity.getString(CommonString.help), {
                                activity.openUrl(url)
                                dialog.dismiss()
                            })
                        },
                        DialogAction(activity.getString(CommonString.close), {
                            activity.closeCollectionAndFinish()
                            dialog.dismiss()
                        }, DialogActionStyle.Filled),
                    ),
            ) {
                DialogBody(failure.toHumanReadableString(activity))
            }
        }.apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }
}
