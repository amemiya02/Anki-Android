// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.testutils.setSyncServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The deck list continues the chosen first-run action after opening the collection. */
@RunWith(AndroidJUnit4::class)
class DeckPickerImportOnStartTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun skipStartupDialogs() {
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        InitialActivity.setUpgradedToLatestVersion(targetContext.sharedPrefs())
        targetContext.sharedPrefs().edit { putBoolean("backupPromptDisabled", true) }
        setIntroductionSlidesShown(true)
    }

    @After
    fun resetBackupSpace() {
        BackupManagerTestUtilities.reset()
    }

    @Test
    fun `import opens the system file picker once`() {
        val intent = DeckPicker.getIntent(targetContext).putExtra(DeckPicker.EXTRA_IMPORT, true)
        val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, intent)
        advanceRobolectricLooper()

        val picker = assertNotNull(shadowOf(deckPicker).nextStartedActivityForResult?.intent)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, picker.action)
        assertTrue(picker.hasCategory(Intent.CATEGORY_OPENABLE))
        assertFalse(deckPicker.intent.hasExtra(DeckPicker.EXTRA_IMPORT))
        deckPicker.refreshState()
        assertNull(shadowOf(deckPicker).nextStartedActivityForResult)
    }

    @Test
    fun `sync setup opens the server address screen inside Decks`() {
        setSyncServer(null)
        val intent = DeckPicker.getIntent(targetContext).putExtra(DeckPicker.EXTRA_SYNC_SETUP, true)
        val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, intent)
        advanceRobolectricLooper()

        val label = deckPicker.getString(CommonString.paper_sync_server_address)
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(label)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(label).assertIsDisplayed()
        assertNull(shadowOf(deckPicker).nextStartedActivityForResult)
        assertFalse(deckPicker.intent.hasExtra(DeckPicker.EXTRA_SYNC_SETUP))
    }
}
