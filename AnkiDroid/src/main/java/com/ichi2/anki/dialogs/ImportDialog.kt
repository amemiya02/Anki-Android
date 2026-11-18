// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.fragment.app.activityViewModels
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.dialogs.ImportDialog.Type.DIALOG_IMPORT_ADD_CONFIRM
import com.ichi2.anki.dialogs.ImportDialog.Type.DIALOG_IMPORT_REPLACE_CONFIRM
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.ImportUtils
import timber.log.Timber
import java.net.URLDecoder

@NeedsTest("integration test: ImportDialog => DeckPicker")
class ImportDialog : AsyncDialogFragment() {
    private val importViewModel: ImportViewModel by activityViewModels()

    override val dialogHandlerMessage: DialogHandlerMessage
        get() =
            when (dialogType) {
                DIALOG_IMPORT_ADD_CONFIRM ->
                    ImportUtils.CollectionImportAdd(
                        packagePath,
                    )
                DIALOG_IMPORT_REPLACE_CONFIRM ->
                    ImportUtils.CollectionImportReplace(
                        packagePath,
                    )
            }

    private val dialogType: Type
        get() = Type.fromCode(requireArguments().getInt(ARG_DIALOG_TYPE))

    private val packagePath: String
        get() = requireArguments().getString(ARG_PACKAGE_PATH)!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val displayFileName = filenameFromPath(convertToDisplayName(packagePath))
        val replacingCollection = dialogType == DIALOG_IMPORT_REPLACE_CONFIRM
        val messageRes =
            if (replacingCollection) CommonString.import_message_replace_confirm else CommonString.import_dialog_message_add
        val actionRes = if (replacingCollection) CommonString.paper_dialog_replace_collection else CommonString.import_message_add

        return createPaperDialog(requireContext()) {
            ConfirmationDialog(
                title = getString(CommonString.import_title),
                message = getString(messageRes, displayFileName),
                confirm =
                    DialogAction(
                        label = getString(actionRes),
                        style = if (replacingCollection) DialogActionStyle.Destructive else DialogActionStyle.Filled,
                        onClick = {
                            if (replacingCollection) {
                                importViewModel.triggerImportReplace(packagePath)
                            } else {
                                importViewModel.triggerImportAdd(packagePath)
                            }
                            activity?.dismissAllDialogFragments()
                        },
                    ),
                onDismissRequest = ::dismiss,
            )
        }
    }

    private fun convertToDisplayName(name: String): String {
        // ImportUtils URLEncodes names, which isn't great for display.
        // NICE_TO_HAVE: Pass in the DisplayFileName closer to the source of the bad data, rather than fixing it here.
        return try {
            URLDecoder.decode(name, "UTF-8")
        } catch (e: Exception) {
            Timber.w(e, "Failed to convert filename to displayable string")
            name
        }
    }

    override val notificationMessage: String
        get() {
            return res().getString(CommonString.import_interrupted)
        }

    override val notificationTitle: String
        get() {
            return res().getString(CommonString.import_title)
        }

    enum class Type(
        val code: Int,
    ) {
        DIALOG_IMPORT_ADD_CONFIRM(0),
        DIALOG_IMPORT_REPLACE_CONFIRM(1),
        ;

        companion object {
            fun fromCode(code: Int) = Type.entries.first { code == it.code }
        }
    }

    companion object {
        const val ARG_DIALOG_TYPE = "dialogType"
        const val ARG_PACKAGE_PATH = "packagePath"

        /**
         * A set of dialogs which deal with importing a file
         *
         * @param dialogType An integer which specifies which of the sub-dialogs to show
         * @param packagePath the path of the package to import
         */
        @CheckResult
        fun newInstance(
            dialogType: Type,
            packagePath: String,
        ): ImportDialog =
            ImportDialog().apply {
                arguments =
                    Bundle().apply {
                        putInt(ARG_DIALOG_TYPE, dialogType.code)
                        putString(ARG_PACKAGE_PATH, packagePath)
                    }
            }

        private fun filenameFromPath(path: String): String = path.split("/").toTypedArray()[path.split("/").toTypedArray().size - 1]
    }
}
