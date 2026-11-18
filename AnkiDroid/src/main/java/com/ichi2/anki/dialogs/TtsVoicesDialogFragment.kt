// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.AndroidTtsVoice
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.dialogs.viewmodel.TtsVoicesViewModel
import com.ichi2.anki.dialogs.viewmodel.TtsVoicesViewModel.VoiceLoadingState
import com.ichi2.anki.libanki.TtsVoice
import com.ichi2.anki.localizedErrorMessage
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.utils.openUrl
import com.ichi2.utils.UiUtil.makeFullscreen
import timber.log.Timber

/**
 * A dialog which allows a user to preview Text to speech voices and copy them to the clipboard
 * for use in a card template
 *
 * This may be opened after {{tts-voices:}} is placed a card template
 *
 * It provides the user the ability to filter voices by online/offline, and whether the voice
 * is installed on the system
 *
 * We filter by online/offline to discourage data usage and reduce latency
 * Although uninstalled voices can be used, and a TTS should install an uninstalled voice,
 * in practice there are many bugs in the TTS Engines, so we want to explicitly inform a user
 *
 * As a future extension: this dialog should have a 'refresh' button/functionality to handle
 * voice installs
 *
 * @see [TtsVoicesViewModel]
 */
class TtsVoicesDialogFragment : DialogFragment() {
    private val viewModel: TtsVoicesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        settingsComposeView(R.id.tts_voices_screen) {
            val context = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            val voices by viewModel.availableVoicesFlow.collectAsStateWithLifecycle(VoiceLoadingState.Pending)
            val showInternet by viewModel.showInternetEnabled.collectAsStateWithLifecycle()
            val showUninstalled by viewModel.showNotInstalled.collectAsStateWithLifecycle()
            var text by rememberSaveable { mutableStateOf(getString(CommonString.tts_voices_default_text)) }
            val snackbar = remember { SnackbarHostState() }
            TtsVoicesScreen(
                state = TtsVoicesScreenState(voices, text, showInternet, showUninstalled),
                onBack = ::dismiss,
                onSettings = ::openTtsSettings,
                onText = { text = it },
                onShowInternet = { viewModel.showInternetEnabled.value = it },
                onShowUninstalled = { viewModel.showNotInstalled.value = it },
                onPlay = {
                    viewModel.setSpokenText(text)
                    viewModel.playVoice(it)
                },
                onCopy = viewModel::copyToClipboard,
                snackbar = snackbar,
            )
            LaunchedEffect(snackbar) {
                viewModel.uninstalledVoicePlayed.collect { voice ->
                    val result =
                        snackbar.showSnackbar(
                            message = context.getString(CommonString.tts_voices_selected_voice_should_be_installed),
                            actionLabel = context.getString(CommonString.tts_voices_use_selected_voice_without_install),
                            withDismissAction = true,
                        )
                    if (result == SnackbarResult.ActionPerformed) viewModel.copyToClipboard(voice)
                }
            }
            LaunchedEffect(snackbar) {
                viewModel.ttsPlaybackErrorFlow.collect { error ->
                    val result =
                        snackbar.showSnackbar(
                            message = error.localizedErrorMessage(context),
                            actionLabel = context.getString(CommonString.help),
                            withDismissAction = true,
                        )
                    if (result == SnackbarResult.ActionPerformed) context.openUrl(R.string.link_faq_tts)
                }
            }
        }

    fun openTtsSettings() {
        try {
            requireContext().startActivity(
                Intent("com.android.settings.TTS_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
            )
        } catch (e: ActivityNotFoundException) {
            Timber.w(e)
            showThemedToast(requireContext(), CommonString.tts_voices_failed_opening_tts_system_settings, shortLength = true)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.makeFullscreen()
    }

    override fun onResume() {
        super.onResume()
        viewModel.waitForRefresh()
    }
}

fun TtsVoice.tryDisplayLocalizedName(): String {
    if (this !is AndroidTtsVoice) {
        return this.toString()
    }

    if (engine == "com.google.android.tts") {
        return prettyPrintGoogle(this)
    }
    // exclude the engine when printing
    return voice.name
}

/**
 * Removes the prefix and suffix from a Google voice name, and provides the 'code' of the voice
 *
 * * sr => sr
 * * cmn-cn-x-ccd-network => ccd
 * * cmn-cn-x-ccc-local => ccc
 * * zh-CN-language => zh CN
 *
 * We can remove the Locale prefix as we have better locale data in the voice
 * We can remove the network/local suffix as this can be obtained from the TTS Engine
 */
fun prettyPrintGoogle(voice: AndroidTtsVoice): String {
    val parts =
        voice.voice.name
            .split("-")
            .toMutableList()

    if (parts.last() == "language") {
        return parts.dropLast(1).joinToString(" ")
    }

    if (parts.size == 1 && parts[0] == voice.lang) {
        return voice.voice.name
    }

    when (parts.last()) {
        "local", "network" -> parts.dropLast(1)
        else -> {}
    }

    if (parts.size == 5 && parts[2] == "x") {
        return parts[3]
    }

    return parts.joinToString(" ")
}
