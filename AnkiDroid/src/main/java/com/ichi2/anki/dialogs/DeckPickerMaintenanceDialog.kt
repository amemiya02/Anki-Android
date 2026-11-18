// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.requireString
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.createPaperDialog

/** Keeps a maintenance decision pending until the user confirms or dismisses it. */
class DeckPickerMaintenanceDialog : DialogFragment() {
    enum class Check(
        val requestKey: String,
    ) {
        MEDIA("request_check_media"),
        DATABASE("request_check_database"),
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val check = Check.valueOf(requireArguments().requireString(ARG_CHECK))
        val isMedia = check == Check.MEDIA
        return createPaperDialog(requireContext()) {
            ConfirmationDialog(
                title = if (isMedia) TR.sentenceCase.checkMediaTitle else TR.sentenceCase.checkDatabase,
                message = if (isMedia) getString(CommonString.check_media_warning) else requireArguments().requireString(ARG_WARNING),
                confirm =
                    DialogAction(getString(if (isMedia) CommonString.dialog_ok else CommonString.integrity_check_continue_anyway), {
                        parentFragmentManager.setFragmentResult(check.requestKey, Bundle())
                        dismiss()
                    }),
                onDismissRequest = ::dismiss,
            )
        }
    }

    companion object {
        const val TAG = "maintenanceConfirmation"
        private const val ARG_CHECK = "check"
        private const val ARG_WARNING = "warning"

        fun newInstance(
            check: Check,
            warning: String? = null,
        ) = DeckPickerMaintenanceDialog().apply {
            arguments =
                Bundle().apply {
                    putString(ARG_CHECK, check.name)
                    putString(ARG_WARNING, warning)
                }
        }
    }
}
