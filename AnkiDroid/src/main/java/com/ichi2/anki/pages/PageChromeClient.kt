// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.view.WindowManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

open class PageChromeClient : WebChromeClient() {
    override fun onJsAlert(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        Timber.d("Displaying alert() dialog")
        try {
            showScriptDialog(view, message, result, confirm = false)
        } catch (e: IllegalStateException) {
            // window count is over max!!
            Timber.w(e, "onJsAlert: message ignored")
            // Report without opening a consent dialog while the window limit is exhausted.
            CrashReportService.sendExceptionReport(e, "onJsAlert:windowCount", additionalInfo = "$url: $message", onlyIfSilent = true)
            // Returning false asks WebView to show another dialog, which can fail for the same reason.
            result?.cancel()
        } catch (e: WindowManager.BadTokenException) {
            Timber.w(e, "onJsAlert: activity destroyed?")
            result?.cancel()
        }

        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        Timber.d("Displaying confirm() dialog")
        try {
            showScriptDialog(view, message, result, confirm = true)
        } catch (e: IllegalStateException) {
            Timber.w(e, "onJsConfirm: message ignored")
            // Report without opening a consent dialog while the window limit is exhausted.
            CrashReportService.sendExceptionReport(e, "onJsConfirm:windowCount", additionalInfo = "$url: $message", onlyIfSilent = true)
            result?.cancel()
        } catch (e: WindowManager.BadTokenException) {
            Timber.w(e, "onJsConfirm")
            result?.cancel()
        }
        return true
    }

    private fun showScriptDialog(
        view: WebView,
        message: String?,
        result: JsResult?,
        confirm: Boolean,
    ) {
        val context = view.context
        createPaperDialog(context) { dialog ->
            ConfirmationDialog(
                title = "",
                message = message.orEmpty(),
                confirm =
                    DialogAction(context.getString(CommonString.dialog_ok), {
                        result?.confirm()
                        dialog.dismiss()
                    }),
                dismissText = if (confirm) context.getString(CommonString.dialog_cancel) else null,
                onDismissRequest = {
                    result?.cancel()
                    dialog.dismiss()
                },
            )
        }.apply {
            setCancelable(!confirm)
            setOnCancelListener { result?.cancel() }
        }.show()
    }
}
