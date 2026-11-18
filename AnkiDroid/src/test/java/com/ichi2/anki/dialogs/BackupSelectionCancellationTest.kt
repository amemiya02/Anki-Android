// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.activity.ComponentDialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.BackupManager
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.dialogs.DatabaseErrorDialog.DatabaseErrorDialogType
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.anki.withDeckPicker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class BackupSelectionCancellationTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `Cancel returns to the deck list without reopening the warning`() =
        withDeckPicker(deckCount = 0) { activity ->
            createBackup()
            activity.showDatabaseErrorDialog(DatabaseErrorDialogType.DIALOG_CONFIRM_RESTORE_BACKUP)
            advanceRobolectricLooper()
            compose.onNodeWithText(activity.getString(CommonString.paper_dialog_replace_collection)).performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText(activity.getString(CommonString.dialog_cancel)).performClick()
            advanceRobolectricLooper()

            assertNull(activity.supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG))
        }

    @Test
    fun `Back returns to recovery options`() =
        withDeckPicker(deckCount = 0) { activity ->
            createBackup()
            CollectionManager.closeCollectionBlocking()
            activity.showDatabaseErrorDialog(DatabaseErrorDialogType.DIALOG_ERROR_HANDLING)
            activity.showDatabaseErrorDialog(DatabaseErrorDialogType.DIALOG_RESTORE_BACKUP)

            activity.currentDialog().onBackPressedDispatcher.onBackPressed()
            advanceRobolectricLooper()

            compose.onNodeWithText(activity.getString(CommonString.error_handling_title)).assertIsDisplayed()
        }

    private fun createBackup() {
        val directory = BackupManager.getBackupDirectory(CollectionHelper.getCurrentAnkiDroidDirectory(targetContext))
        File(directory, "backup-2026-09-29-12.00.00.colpkg").writeText("backup")
    }

    private fun DeckPicker.currentDialog(): ComponentDialog =
        (supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as DatabaseErrorDialog).requireDialog() as ComponentDialog
}
