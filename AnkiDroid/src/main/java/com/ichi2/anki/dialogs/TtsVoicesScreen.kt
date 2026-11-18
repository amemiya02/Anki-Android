// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ichi2.anki.AndroidTtsVoice
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.dialogs.viewmodel.TtsVoicesViewModel.VoiceLoadingState
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.anki.preferences.SettingsRow
import com.ichi2.anki.preferences.SettingsSwitchRow
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.anki.common.android.R as CommonR

internal data class TtsVoicesScreenState(
    val voices: VoiceLoadingState,
    val text: String,
    val showInternet: Boolean,
    val showUninstalled: Boolean,
)

@Composable
internal fun TtsVoicesScreen(
    state: TtsVoicesScreenState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onText: (String) -> Unit,
    onShowInternet: (Boolean) -> Unit,
    onShowUninstalled: (Boolean) -> Unit,
    onPlay: (AndroidTtsVoice) -> Unit,
    onCopy: (AndroidTtsVoice) -> Unit,
    snackbar: SnackbarHostState,
) {
    val foundation = MaterialTheme.foundation
    val locale = LocalConfiguration.current.locales[0]
    SettingsPage(stringResource(CommonString.tts_error_dialog_supported_voices_button_text), onBack) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(
                        Modifier.padding(top = 8.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            state.text,
                            onText,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.tts_voices_sample_text)) },
                            leadingIcon = { Icon(painterResource(CommonR.drawable.symbol_mic), null) },
                            singleLine = true,
                        )
                        SettingsRow(stringResource(CommonString.settings), icon = R.drawable.symbol_settings, onClick = onSettings)
                        SettingsSwitchRow(
                            stringResource(CommonString.tts_voices_chip_filter_to_internet),
                            checked = state.showInternet,
                            enabled = !state.showUninstalled,
                            icon = R.drawable.symbol_wifi,
                            onChange = onShowInternet,
                        )
                        SettingsSwitchRow(
                            stringResource(CommonString.tts_voices_chip_filter_to_installable_audio),
                            checked = state.showUninstalled,
                            icon = R.drawable.symbol_download_for_offline,
                            onChange = onShowUninstalled,
                        )
                    }
                }
                if (state.voices is VoiceLoadingState.Success) {
                    items(state.voices.voices) { voice ->
                        val unavailable = voice.unavailable()
                        SettingsRow(
                            title = voice.normalizedLocale.getDisplayName(locale),
                            summary = voice.tryDisplayLocalizedName(),
                            icon = if (voice.isNetworkConnectionRequired) R.drawable.symbol_wifi else R.drawable.symbol_offline_pin,
                            onClick = { onPlay(voice) },
                            trailing = {
                                IconButton(onClick = { if (unavailable) onSettings() else onCopy(voice) }) {
                                    Icon(
                                        painterResource(if (unavailable) R.drawable.symbol_download else R.drawable.symbol_content_copy),
                                        stringResource(if (unavailable) CommonString.settings else android.R.string.copy),
                                        tint = foundation.colors.onBackground,
                                    )
                                }
                            },
                        )
                    }
                } else {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    if (state.voices is VoiceLoadingState.Failure) {
        ErrorDialog(
            title = stringResource(CommonString.vague_error),
            message =
                state.voices.exception.localizedMessage
                    .orEmpty(),
            onDismissRequest = onBack,
        )
    }
}
