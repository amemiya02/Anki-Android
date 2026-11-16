// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.dialogs.ImportFileSelectionFragment
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** The deck list opened from the first-run screen's import, which replaces Upstream's "Sync from AnkiWeb" */
@RunWith(AndroidJUnit4::class)
class DeckPickerImportOnStartTest : RobolectricTest() {
    /** No startup dialog may replace the import dialog */
    @Before
    fun skipStartupDialogs() {
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        InitialActivity.setUpgradedToLatestVersion(targetContext.sharedPrefs())
        targetContext.sharedPrefs().edit { putBoolean("backupPromptDisabled", true) }
    }

    @After
    fun resetBackupSpace() {
        BackupManagerTestUtilities.reset()
    }

    @Test
    fun `the deck list asked to import offers collection and deck packages`() {
        setIntroductionSlidesShown(true)
        val intent = Intent(targetContext, DeckPicker::class.java).putExtra(DeckPicker.EXTRA_IMPORT, true)
        val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, intent)
        advanceRobolectricLooper()

        val dialog =
            deckPicker.supportFragmentManager.fragments
                .filterIsInstance<ImportFileSelectionFragment>()
                .single()
        val items = (dialog.dialog as AlertDialog).listView.adapter
        assertEquals(
            listOf(
                deckPicker.getString(CommonString.import_deck_package),
                deckPicker.getString(CommonString.import_collection_package),
            ),
            (0 until items.count).map { items.getItem(it).toString() },
        )
    }
}
