// SPDX-License-Identifier: GPL-3.0-or-later

@file:Suppress("unused")

package com.ichi2.utils

import android.app.Dialog
import android.content.DialogInterface
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.ichi2.anki.common.utils.android.HandlerUtils.executeOnMainThread
import timber.log.Timber

val AlertDialog.positiveButton: Button
    get() = getButton(DialogInterface.BUTTON_POSITIVE)

val AlertDialog.negativeButton: Button
    get() = getButton(DialogInterface.BUTTON_NEGATIVE)

val AlertDialog.neutralButton: Button?
    get() = getButton(DialogInterface.BUTTON_NEUTRAL)

/** Calls [Dialog.dismiss], ignoring errors */
fun Dialog.dismissSafely() {
    // The exception will be uncaught if not run on the main thread.
    executeOnMainThread {
        try {
            // safer to catch the exception to be sure dismiss() was called
            dismiss()
        } catch (e: IllegalArgumentException) {
            if (window == null || !isShowing) {
                Timber.d(e, "Dialog not attached to window manager")
                return@executeOnMainThread
            }
            Timber.w(e, "Dialog not attached to window manager")
        }
    }
}
