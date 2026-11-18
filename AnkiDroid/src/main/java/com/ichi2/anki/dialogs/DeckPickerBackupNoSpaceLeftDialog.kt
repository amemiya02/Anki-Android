// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import com.ichi2.anki.BackupManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

class DeckPickerBackupNoSpaceLeftDialog : AnalyticsDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val space = BackupManager.getFreeDiscSpace(CollectionHelper.getCollectionPath(requireActivity()))
        return createPaperDialog(requireContext()) {
            ErrorDialog(
                title = getString(CommonString.storage_almost_full_title),
                message = getString(CommonString.storage_warning, space / 1024 / 1024),
                onDismissRequest = ::dismiss,
                dismissText = null,
                primaryAction = DialogAction(getString(CommonString.dialog_ok), { (activity as DeckPicker).finish() }),
            )
        }.apply { setCanceledOnTouchOutside(false) }
    }

    companion object {
        fun newInstance(): DeckPickerBackupNoSpaceLeftDialog = DeckPickerBackupNoSpaceLeftDialog()
    }
}
