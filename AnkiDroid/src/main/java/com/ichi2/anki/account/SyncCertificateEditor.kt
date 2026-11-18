// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.core.content.edit
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.settings.Prefs
import com.ichi2.compose.ui.dialogs.TextInputDialog

@Composable
internal fun SyncCertificateEditor(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var invalid by remember { mutableStateOf<String?>(null) }
    val error = stringResource(CommonString.dialog_invalid_custom_certificate)
    TextInputDialog(
        title = stringResource(CommonString.custom_sync_certificate_title),
        label = stringResource(CommonString.dialog_hint_certificate),
        description = stringResource(CommonString.paper_sync_certificate_hint),
        initialText = initialText,
        confirmText = stringResource(CommonString.dialog_ok),
        onDismissRequest = onDismiss,
        onConfirm = { certificate ->
            if (CollectionManager.updateCustomCertificate(certificate)) {
                Prefs.sharedPrefs.edit { putString(Prefs.key(R.string.custom_sync_certificate_key), certificate) }
                onSave(certificate)
            } else {
                invalid = certificate
            }
        },
        errorMessageFor = { if (it == invalid) error else null },
        validateOnInput = false,
        capitalization = KeyboardCapitalization.None,
        singleLine = false,
    )
}
