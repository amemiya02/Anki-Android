// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.ichi2.anki.common.android.R
import com.ichi2.compose.ui.preview.AnkiDroidPreview

/** Confirmation with a plain message and one named action. */
@Composable
fun MessageDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissText: String = stringResource(R.string.dialog_cancel),
    enabled: Boolean = true,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
) {
    ConfirmationDialog(
        title = title,
        message = message,
        confirm = DialogAction(confirmText, onConfirm, enabled = enabled),
        onDismissRequest = onDismissRequest,
        dismissText = dismissText,
        properties = properties,
    )
}

/** @see MessageDialog */
@Preview(name = "Confirm deletion", widthDp = 320)
@Composable
private fun DeleteMessageDialogPreview() {
    AnkiDroidPreview {
        MessageDialog(
            title = "Delete item",
            message = "Delete this item and its children? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}

/** @see MessageDialog */
@Preview(name = "Retry loading", widthDp = 320)
@Composable
private fun RetryMessageDialogPreview() {
    AnkiDroidPreview {
        MessageDialog(
            title = "Could not load items",
            message = "Check your connection and try again.",
            confirmText = "Retry",
            dismissText = "Close",
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}
