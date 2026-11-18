// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.utils.ext.ifNullOrEmpty
import com.ichi2.anki.utils.ext.requireString
import com.ichi2.anki.utils.ext.setFragmentResultListener
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.compose.ui.dialogs.ConfirmationDialog as PaperConfirmationDialog

/**
 * This is a reusable convenience class which makes it easy to show a confirmation dialog as a DialogFragment.
 * Create a new instance, call setArgs(...), setConfirm(), and setCancel() then show it via the fragment manager as usual.
 */
class ConfirmationDialog : DialogFragment() {
    private val message: String by lazy {
        requireArguments().requireString(ARG_MESSAGE)
    }

    private val title: String
        get() =
            requireArguments()
                .getString(ARG_TITLE)
                .ifNullOrEmpty { requireActivity().getString(R.string.app_name) }

    private val positiveButtonText: String
        get() =
            requireArguments()
                .getString(ARG_POSITIVE_BUTTON_TEXT)
                .ifNullOrEmpty { getString(CommonString.dialog_ok) }

    private var confirm = Runnable {} // Do nothing by default
    private var cancel = Runnable {} // Do nothing by default

    /**
     * Sets the message to display. Using [R.string.app_name] as the title.
     */
    fun setArgs(message: String) {
        setArgs(
            title = "",
            message = message,
            positiveButtonText = null,
        )
    }

    fun setArgs(
        title: String?,
        message: String,
        positiveButtonText: String? = null,
        destructive: Boolean = false,
    ) {
        arguments =
            Bundle().apply {
                putString(ARG_MESSAGE, message)
                putString(ARG_TITLE, title)
                putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText)
                putBoolean(ARG_DESTRUCTIVE, destructive)
            }
    }

    fun setConfirm(confirm: Runnable) {
        this.confirm = confirm
    }

    fun setCancel(cancel: Runnable) {
        this.cancel = cancel
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        createPaperDialog(requireContext()) {
            PaperConfirmationDialog(
                title = title,
                message = message,
                confirm =
                    DialogAction(
                        positiveButtonText,
                        {
                            setConfirmationResult()
                            confirm.run()
                            dismiss()
                        },
                        if (requireArguments().getBoolean(ARG_DESTRUCTIVE)) DialogActionStyle.Destructive else DialogActionStyle.Filled,
                    ),
                onDismissRequest = {
                    cancel.run()
                    dismiss()
                },
            )
        }

    private fun setConfirmationResult() {
        val requestKey = requireArguments().getString(ARG_REQUEST_KEY) ?: return
        setFragmentResult(requestKey, requireArguments().getBundle(ARG_RESULT) ?: Bundle())
    }

    companion object {
        /** The dialog message (required) */
        private const val ARG_MESSAGE = "message"

        /**
         * Optional dialog title. Default: [R.string.app_name]
         */
        private const val ARG_TITLE = "title"

        /** Optional text for the positive button. Default: "OK" */
        private const val ARG_POSITIVE_BUTTON_TEXT = "positiveButtonText"

        private const val ARG_DESTRUCTIVE = "destructive"
        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_RESULT = "result"

        fun newInstance(
            requestKey: String,
            message: String,
            title: String? = null,
            positiveButtonText: String? = null,
            result: Bundle = Bundle(),
        ) = ConfirmationDialog().apply {
            setArgs(title, message, positiveButtonText)
            requireArguments().putString(ARG_REQUEST_KEY, requestKey)
            requireArguments().putBundle(ARG_RESULT, result)
        }

        fun newFullSyncInstance(
            context: Context,
            requestKey: String,
            result: Bundle = Bundle(),
        ) = newInstance(
            requestKey = requestKey,
            message = context.getString(CommonString.full_sync_confirmation),
            result = result,
        )
    }
}

fun FragmentActivity.registerConfirmationHandler(
    requestKey: String,
    action: (result: Bundle) -> Unit,
) {
    setFragmentResultListener(requestKey) { _, result -> action(result) }
}
