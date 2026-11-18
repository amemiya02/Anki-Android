// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import com.ichi2.anki.CommonString
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

class DeckPickerNoSpaceLeftDialog : AnalyticsDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) {
            ErrorDialog(
                title = getString(CommonString.storage_full_title),
                message = getString(CommonString.backup_deck_no_storage_left),
                onDismissRequest = ::dismiss,
                dismissText = null,
                primaryAction = DialogAction(getString(CommonString.dialog_ok), ::dismiss),
            )
        }

    companion object {
        fun newInstance(): DeckPickerNoSpaceLeftDialog = DeckPickerNoSpaceLeftDialog()
    }
}
