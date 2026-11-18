// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commitNow
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.compose.ui.dialogs.ProgressDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog

/**
 * Simple [DialogFragment] to be used to show a "loading" ui state.
 */
class LoadingDialogFragment : DialogFragment() {
    private val message = mutableStateOf<String?>(null)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        message.value = arguments?.getString(ARG_MESSAGE)
        val canBeCancelled = arguments?.getBoolean(ARG_CANCELLABLE) ?: true
        isCancelable = canBeCancelled
        return createPaperDialog(requireContext()) { dialog ->
            ProgressDialog(
                title = message.value ?: getString(CommonString.dialog_processing),
                onCancel = if (canBeCancelled) dialog::cancel else null,
            )
        }.apply { setCanceledOnTouchOutside(canBeCancelled) }
    }

    fun updateMessage(text: String?) {
        arguments?.putString(ARG_MESSAGE, text)
        message.value = text
    }

    companion object {
        const val TAG = "LoadingDialogFragment"
        private const val ARG_MESSAGE = "key_message"
        private const val ARG_CANCELLABLE = "key_cancellable"

        /**
         * Creates an instance of [LoadingDialogFragment].
         * @param message optional message for the loading operation, will default to
         * [CommonString.dialog_processing] if not provided
         * @param cancellable if the dialog should be cancellable or not (also affects cancel when
         * touching outside the dialog window)
         */
        fun newInstance(
            message: String? = null,
            cancellable: Boolean = true,
        ) = LoadingDialogFragment().apply {
            arguments =
                Bundle().apply {
                    putString(ARG_MESSAGE, message)
                    putBoolean(ARG_CANCELLABLE, cancellable)
                }
            isCancelable = cancellable
        }
    }
}

/**
 * Shows a [LoadingDialogFragment].
 * This method will look for a previous instance of the dialog and reuse it(by changing the message)
 * if it's already showing and the new call doesn't modify input parameters(ex. [cancellable]). In
 * any other cases, the old instance will be removed and a new one will be used.
 *
 * Note: Multiple calls with the same [tag] will result in a single dialog being shown, this also
 * implies that a call to [dismissLoadingDialog] with that [tag] will dismiss the dialogs of all
 * those calls. Callers need to handle this scenario by combining the loading states and manually
 * handling showing/dismissing, or by using different tags.
 *
 * @param message the message to show along with the progress bar or null to default to
 * use "Processing..."
 * @param cancellable true if the dialog should be cancellable, false otherwise. This will also
 * apply to the [Dialog.setCanceledOnTouchOutside] property
 */
@MainThread
fun AnkiActivity.showLoadingDialog(
    message: String? = null,
    cancellable: Boolean = true,
    tag: String = LoadingDialogFragment.TAG,
) {
    val fragment =
        supportFragmentManager.findFragmentByTag(tag) as? LoadingDialogFragment
    val isAlreadyShowing = fragment?.dialog?.isShowing == true
    if (isAlreadyShowing) {
        // if a dialog is already showing and it has the same input params then just update the
        // dialog with the new message, otherwise remove the instance
        if (cancellable == fragment.isCancelable) {
            fragment.updateMessage(message)
            return
        }
    }
    // remove the previous fragment if an instance is found but it is not showing or different input
    // parameters were requested for the new dialog fragment
    removeImmediately(fragment)
    val loadingDialog = LoadingDialogFragment.newInstance(message, cancellable)
    // showNow() avoids a race condition - removal is synchronous
    loadingDialog.showNow(supportFragmentManager, tag)
}

/** Synchronously removes the provided [LoadingDialogFragment] if valid(not null) */
private fun AnkiActivity.removeImmediately(fragment: LoadingDialogFragment?) {
    if (fragment != null) {
        supportFragmentManager.commitNow { remove(fragment) }
    }
}

/**
 * Dismisses and removes the [LoadingDialogFragment] shown with [tag] if one is present.
 */
fun AnkiActivity.dismissLoadingDialog(tag: String = LoadingDialogFragment.TAG) {
    val fragment =
        supportFragmentManager.findFragmentByTag(tag) as? LoadingDialogFragment
    removeImmediately(fragment)
}
