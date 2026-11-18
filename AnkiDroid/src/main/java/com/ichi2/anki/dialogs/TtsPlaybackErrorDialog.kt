// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 RohanRaj123 <rajrohan88293@gmail.com>

package com.ichi2.anki.dialogs

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.TtsVoices
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.libanki.TTSTag
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogBody
import com.ichi2.compose.ui.dialogs.DialogMark
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber

object TtsPlaybackErrorDialog {
    fun ttsPlaybackErrorDialog(
        activity: Activity,
        fragmentManager: FragmentManager,
        ttsTag: TTSTag?,
    ) {
        activity.runOnUiThread {
            if (showMissingEngineDialogIfNeeded(activity)) return@runOnUiThread
            Timber.i("Displaying TTS Error: 'Language not supported'")
            createPaperDialog(activity) { dialog ->
                PaperDialog(
                    title = activity.getString(CommonString.tts_error_dialog_title),
                    onDismissRequest = dialog::dismiss,
                    mark = DialogMark(R.drawable.symbol_warning, warning = true),
                    actions =
                        listOf(
                            DialogAction(activity.getString(CommonString.help), {
                                dialog.dismiss()
                                activity.openUrl(R.string.link_faq_tts)
                            }),
                            DialogAction(activity.getString(CommonString.tts_error_dialog_change_button_text), {
                                dialog.dismiss()
                                openSettings(activity)
                            }),
                            DialogAction(activity.getString(CommonString.tts_error_dialog_supported_voices_button_text), {
                                dialog.dismiss()
                                showVoicesDialog(fragmentManager)
                            }, DialogActionStyle.Filled),
                        ),
                ) { DialogBody(activity.getString(CommonString.tts_error_dialog_reason_text, TtsVoices.ttsEngine, ttsTag?.lang)) }
            }.show()
        }
    }

    /**
     * Shows installation guidance if no TTS engine is installed.
     *
     * @return `true` if the dialog is shown or queued on the UI thread; `false` if an engine is installed.
     */
    fun showMissingEngineDialogIfNeeded(activity: Activity): Boolean {
        if (TtsVoices.hasInstalledEngine(activity)) return false

        Timber.w("no TTS engines installed")
        activity.runOnUiThread {
            createPaperDialog(activity) { dialog ->
                PaperDialog(
                    title = activity.getString(CommonString.tts_no_engine_title),
                    mark = DialogMark(R.drawable.symbol_warning, warning = true),
                    onDismissRequest = dialog::dismiss,
                    actions =
                        listOf(
                            DialogAction(activity.getString(CommonString.help), {
                                dialog.dismiss()
                                activity.openUrl(R.string.link_faq_tts_no_engine)
                            }),
                            DialogAction(activity.getString(CommonString.dialog_ok), dialog::dismiss, DialogActionStyle.Filled),
                        ),
                ) { DialogBody(activity.getString(CommonString.tts_no_engine_message)) }
            }.show()
        }
        return true
    }

    private fun openSettings(activity: Activity) {
        try {
            Timber.i("Opening TextToSpeech engine settings to change the engine")
            activity.startActivity(
                Intent("com.android.settings.TTS_SETTINGS").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            )
        } catch (e: Exception) {
            CrashReportService.sendExceptionReport(e, e.localizedMessage)
        }
    }

    private fun showVoicesDialog(fragmentManager: FragmentManager) {
        TtsVoicesDialogFragment().show(fragmentManager, "TTS_VOICES_DIALOG_FRAGMENT")
    }
}
