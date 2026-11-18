// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.isDialog
import com.ichi2.compose.ui.dialogs.ProgressDialog
import org.junit.Test

@CheckAccessibility
class ProgressDialogScreenshotTest : ComposeScreenshotTest() {
    @Test
    fun `progress dialog with a cancel button`() =
        capture("progress_dialog", node = isDialog()) {
            ProgressDialog(
                title = stringResource(CommonString.dialog_processing),
                onCancel = {},
            )
        }
}
