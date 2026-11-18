// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.export

import android.os.Bundle
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.testutils.launchFragment
import org.junit.Rule
import org.junit.Test

class ExportDialogFragmentScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun collapsed() =
        withExportDialog {
            captureScreen("collapsed")
        }

    @Test
    fun expanded() =
        withExportDialog {
            compose.onNodeWithText("(.colpkg)", substring = true).performClick()
            captureScreen("expanded")
        }

    @Test
    fun deck() =
        withExportDialog(ExportDialogFragment.forDeckList(DEFAULT_DECK_ID).arguments) {
            captureScreen("deck")
        }

    private fun withExportDialog(
        fragmentArgs: Bundle? = null,
        action: () -> Unit,
    ) {
        ensureCollectionLoadIsSynchronous()
        launchFragment<ExportDialogFragment>(fragmentArgs).use {
            advanceRobolectricLooper()
            action()
        }
    }
}
