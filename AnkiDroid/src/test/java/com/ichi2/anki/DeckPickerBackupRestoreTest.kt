// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class DeckPickerBackupRestoreTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.ON_DISK

    @Before
    fun setCollectionPath() {
        getPreferences().edit {
            putString(CollectionHelper.PREF_COLLECTION_PATH, tempFolder.newFolder("collection").absolutePath)
        }
    }

    @After
    fun resetBackupSpace() {
        BackupManagerTestUtilities.reset()
    }

    @Test
    fun `failed restore reports the error and leaves the original collection open`() {
        val note = addBasicNote("original", "answer")
        val backup = createInvalidBackup()

        withDeckPicker(deckCount = 0) { deckPicker ->
            throwOnShowError = false
            deckPicker.importColpkgListener = ImportColpkgListener { fail("Failed restore reported success") }

            deckPicker.restoreBackupAndWaitForError(backup)

            compose.onNodeWithText(TR.importingTheProvidedFileIsNotA()).assertIsDisplayed()
            // Check before accessing col: that getter would reopen it and hide the regression.
            assertTrue(CollectionManager.isOpenUnsafe(), "Failed restore left the collection closed")

            compose.onNodeWithText(targetContext.getString(CommonString.dialog_ok)).performClick()
            assertTrue(CollectionManager.isOpenUnsafe(), "Dismissing the restore error left the collection closed")
            assertEquals(listOf("original", "answer"), col.getNote(note.id).fields.toList())
        }
    }

    private fun createInvalidBackup(): File {
        val backupDirectory = BackupManager.getBackupDirectory(CollectionManager.getCollectionDirectory())
        val backup = File(backupDirectory, "backup-2020-07-07-07.00.00.colpkg")
        // A valid ZIP missing its collection produces the localized invalid-backup error.
        ZipOutputStream(backup.outputStream()).use { }
        return backup
    }

    private fun DeckPicker.restoreBackupAndWaitForError(backup: File) {
        assertTrue(shadowOf(this).clickMenuItem(R.id.action_restore_backup))
        advanceRobolectricLooper()
        compose.onNodeWithText(getString(CommonString.paper_dialog_replace_collection)).performClick()
        advanceRobolectricLooper()
        val chooser = assertNotNull(ShadowDialog.getLatestDialog())
        val date = LocalizedUnambiguousBackupTimeFormatter().getTimeOfBackupAsText(backup)
        compose.onNodeWithText(date).performClick()

        advanceRobolectricLooperUntil(lazyMessage = { "Expected a restore error dialog after selecting ${backup.name}" }) {
            val dialog = ShadowDialog.getLatestDialog()
            dialog != null && dialog !== chooser && dialog.isShowing
        }
    }
}
