// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Dialog
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import anki.sync.SyncAuth
import anki.sync.SyncCollectionResponse
import anki.sync.syncCollectionResponse
import app.cash.turbine.test
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.utils.ext.DIALOG_FRAGMENT_TAG
import com.ichi2.testutils.signInToSyncServer
import com.ichi2.utils.LanguageUtil.withUnicodeIsolation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(ParameterizedRobolectricTestRunner::class)
class SyncConflictTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @ParameterizedRobolectricTestRunner.Parameter
    @JvmField
    var qualifiers: String? = null

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun configurations(): Collection<String> = listOf("normal", "xlarge")
    }

    override fun setupTestDispatcher(dispatcher: TestDispatcher) {
        super.setupTestDispatcher(dispatcher)
        // undo the Main dispatcher override from `runTest`
        Dispatchers.resetMain()
    }

    @Before
    fun setUpConflict() {
        RuntimeEnvironment.setQualifiers(qualifiers)
        runBlocking { CollectionManager.discardBackend() }
        BackendFactory.setOverride {
            object : Backend() {
                override fun syncCollection(
                    auth: SyncAuth,
                    syncMedia: Boolean,
                ): SyncCollectionResponse =
                    syncCollectionResponse {
                        required = SyncCollectionResponse.ChangesRequired.FULL_SYNC
                    }
            }
        }
    }

    @After
    fun resetBackendFactory() {
        BackendFactory.setOverride(null)
    }

    @Test
    fun `sync conflict remains visible after sync finishes and decks reload`() =
        deckPicker {
            syncWithConflict()

            val dialog = conflictDialog()
            assertTrue(dialog.isShowing)
            compose.onNodeWithText(getString(CommonString.paper_sync_conflict_body)).assertIsDisplayed()
            compose.onNodeWithText(getString(CommonString.paper_sync_keep_phone)).assertIsDisplayed()
            compose.onNodeWithText(getString(CommonString.paper_sync_keep_server)).assertIsDisplayed()
        }

    @Test
    fun `sync conflict can be cancelled after deck refresh`() =
        deckPicker {
            syncWithConflict()
            val dialog = conflictDialog()
            reloadDecks()

            dialog.clickButton(CommonString.dialog_cancel)

            assertFalse(dialog.isShowing)
        }

    @Test
    fun `upload confirmation survives deck refresh`() =
        deckPicker {
            val confirmation = chooseSyncDirection(ConflictResolution.FULL_UPLOAD)

            reloadDecks()

            assertSame(confirmation, conflictDialog())
            assertTrue(confirmation.isShowing)
            compose
                .onNodeWithText(
                    getString(CommonString.sync_replace_remote, withUnicodeIsolation(requireNotNull(syncServerAddress()))),
                ).assertIsDisplayed()
        }

    @Test
    fun `download confirmation survives deck refresh`() =
        deckPicker {
            val confirmation = chooseSyncDirection(ConflictResolution.FULL_DOWNLOAD)

            reloadDecks()

            assertSame(confirmation, conflictDialog())
            assertTrue(confirmation.isShowing)
            compose
                .onNodeWithText(
                    getString(CommonString.sync_replace_local, withUnicodeIsolation(requireNotNull(syncServerAddress()))),
                ).assertIsDisplayed()
        }

    @Test
    fun `upload confirmation can be cancelled after deck refresh`() =
        deckPicker {
            val confirmation = chooseSyncDirection(ConflictResolution.FULL_UPLOAD)
            reloadDecks()

            confirmation.clickButton(CommonString.dialog_cancel)

            assertFalse(confirmation.isShowing)
        }

    @Test
    fun `download confirmation can be cancelled after deck refresh`() =
        deckPicker {
            val confirmation = chooseSyncDirection(ConflictResolution.FULL_DOWNLOAD)
            reloadDecks()

            confirmation.clickButton(CommonString.dialog_cancel)

            assertFalse(confirmation.isShowing)
        }

    private suspend fun DeckPicker.syncWithConflict() {
        signInToSyncServer()
        Prefs.lastSyncTime = 0
        viewModel.flowOfDecksReloaded.test {
            // Consume the replayed startup event, then wait for sync's own reload.
            awaitItem()
            handleNewSync(conflict = null, syncMedia = false)
            advanceRobolectricLooperUntil { Prefs.lastSyncTime != 0L }
            awaitItem()
            viewModel.loadDeckCounts?.join()
            advanceRobolectricLooper()
        }
    }

    private suspend fun DeckPicker.chooseSyncDirection(direction: ConflictResolution): Dialog {
        syncWithConflict()
        val button =
            when (direction) {
                ConflictResolution.FULL_UPLOAD -> CommonString.paper_sync_keep_phone
                ConflictResolution.FULL_DOWNLOAD -> CommonString.paper_sync_keep_server
            }
        conflictDialog().clickButton(button)
        return conflictDialog()
    }

    private suspend fun DeckPicker.reloadDecks() {
        viewModel.reloadDeckCounts().join()
        advanceRobolectricLooper()
    }

    private fun Dialog.clickButton(button: Int) {
        check(isShowing) { "The dialog must be visible before clicking a button" }
        compose.onNodeWithText(context.getString(button)).performClick()
        advanceRobolectricLooper()
    }

    private fun DeckPicker.conflictDialog(): Dialog {
        val fragment = assertNotNull(supportFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) as? SyncErrorDialog)
        return assertNotNull(fragment.dialog)
    }
}
