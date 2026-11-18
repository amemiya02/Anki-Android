// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.ImportDialog
import com.ichi2.anki.export.ExportDialogFragment
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.EmptyAnkiActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertEquals
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class RemainingDialogsTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `discarding changes requires the explicit action`() {
        Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup().use { controller ->
            var discarded = 0

            fun showDialog() {
                DiscardChangesDialog.showDialog(controller.get()) { discarded++ }
            }
            showDialog()
            composeRule.onNodeWithText(with(controller.get()) { TR.sentenceCase.keepEditing }).performClick()
            assertEquals(0, discarded)
            showDialog()
            composeRule.onNodeWithText(targetContext.getString(CommonString.discard)).performClick()
            assertEquals(1, discarded)
        }
    }

    @Test
    fun `collection replacement names the file and lets the user cancel`() {
        val args = ImportDialog.newInstance(ImportDialog.Type.DIALOG_IMPORT_REPLACE_CONFIRM, "/cache/My%20Collection.colpkg").arguments
        launchFragment<ImportDialog>(fragmentArgs = args, themeResId = CommonR.style.Theme_Light).use {
            composeRule.onNodeWithText("My Collection.colpkg", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText(targetContext.getString(CommonString.paper_dialog_replace_collection)).assertIsDisplayed()
            composeRule.onNodeWithText(targetContext.getString(CommonString.dialog_cancel)).performClick()
            composeRule.onNodeWithText("My Collection.colpkg", substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun `export defaults to a collection and can switch to deck format`() {
        launchFragment<ExportDialogFragment>(themeResId = CommonR.style.Theme_Light).use {
            composeRule.onNodeWithText("(.colpkg)", substring = true).performClick()
            composeRule.onNodeWithText("(.apkg)", substring = true).performClick()
            composeRule.onNodeWithText(TR.exportingIncludeSchedulingInformation()).assertIsDisplayed()
        }
    }
}
