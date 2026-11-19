// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.sync.MeteredSyncPolicy
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.createPaperDialog

/** Asks permission to sync on a metered network, preserving the request and checkbox across recreation. */
class MeteredSyncDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) {
            var alwaysAllow by rememberSaveable { mutableStateOf(false) }
            ConfirmationDialog(
                title = getString(CommonString.button_sync),
                message = getString(CommonString.paper_sync_metered_error),
                confirm =
                    DialogAction(getString(CommonString.dialog_continue), {
                        parentFragmentManager.setFragmentResult(
                            REQUEST_METERED_SYNC,
                            Bundle(requireArguments()),
                        )
                        dismiss()
                    }),
                onDismissRequest = ::dismiss,
            ) {
                DialogChoiceRow(
                    choice = DialogChoice(getString(CommonString.paper_sync_metered_remember)),
                    checked = alwaysAllow,
                    onClick = {
                        alwaysAllow = !alwaysAllow
                        MeteredSyncPolicy.setAlwaysAllow(alwaysAllow)
                    },
                    multiple = true,
                )
            }
        }

    companion object {
        const val TAG = "meteredSyncConfirmation"
        const val REQUEST_METERED_SYNC = "request_metered_sync"
        const val RESULT_SYNC_MEDIA = "sync_media"
        const val MANUAL = "manual"

        fun newInstance(
            syncMedia: Boolean,
            manual: Boolean,
        ) = MeteredSyncDialog().apply {
            arguments =
                Bundle().apply {
                    putBoolean(RESULT_SYNC_MEDIA, syncMedia)
                    putBoolean(MANUAL, manual)
                }
        }
    }
}

fun FragmentActivity.registerMeteredSyncHandler(action: (syncMedia: Boolean, manual: Boolean) -> Unit) {
    setFragmentResultListener(MeteredSyncDialog.REQUEST_METERED_SYNC) { _, result ->
        action(result.getBoolean(MeteredSyncDialog.RESULT_SYNC_MEDIA), result.getBoolean(MeteredSyncDialog.MANUAL, true))
    }
}
