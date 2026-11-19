// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.SingleFragmentActivity.Companion.EXTRA_FRAGMENT_NAME
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.AccountFlowFragment
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.dialogs.DialogHandler
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.worker.toSyncAuth
import com.ichi2.anki.worker.toWorkData
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.setSyncServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowDialog
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Sign-in routes for AnkiWeb and the configured self-hosted server. */
@RunWith(AndroidJUnit4::class)
class SyncSetupTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.ON_DISK

    /** No startup dialog may replace the dialogs under test */
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
    fun `with no server address the sync button opens AnkiWeb sign-in`() =
        deckPicker {
            Prefs.hkey = ""
            setSyncServer(null)
            val dialogs = ShadowDialog.getShownDialogs().size

            onOptionsItemSelected(RoboMenuItem(R.id.action_sync))
            advanceRobolectricLooper()

            assertEquals(dialogs, ShadowDialog.getShownDialogs().size, "no 'log in' dialog")
            compose.onNodeWithText(getString(CommonString.sync_ankiweb_sign_in)).assertIsDisplayed()
            assertEquals(null, shadowOf(this).nextStartedActivity)
        }

    @Test
    fun `with a server address the sync button offers to sign in to that server`() =
        deckPicker {
            Prefs.hkey = ""
            setSyncServer(TEST_SYNC_SERVER)

            onOptionsItemSelected(RoboMenuItem(R.id.action_sync))
            advanceRobolectricLooper()

            compose.onNodeWithText(getString(CommonString.sync_server_not_logged_in_title)).assertIsDisplayed()
            compose.onNodeWithText(TR.syncSignIn()).performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText(getString(CommonString.paper_sync_sign_in_title)).assertIsDisplayed()
            assertEquals(null, shadowOf(this).nextStartedActivity)
        }

    @Test
    fun `the account screen opens AnkiWeb sign-in for a new profile`() {
        setSyncServer(null)

        assertAccountEntry(AccountActivity.getIntent(targetContext), startWithAddress = false)
    }

    @Test
    fun `a queued sync error waits for Decks when Settings opens first`() {
        setSyncServer(TEST_SYNC_SERVER)
        throwOnShowError = false
        DialogHandler.storeMessage(
            SyncErrorDialog.SyncErrorDialogMessageHandler(SyncErrorDialog.Type.DIALOG_CONNECTION_ERROR, "offline").toMessage(),
        )
        try {
            ActivityScenario.launch<PreferencesActivity>(PreferencesActivity.getIntent(targetContext)).use {
                advanceRobolectricLooper()
                compose.onNodeWithText(targetContext.getString(CommonString.something_wrong)).assertDoesNotExist()
            }
            deckPicker {
                advanceRobolectricLooper()
                compose.onNodeWithText(getString(CommonString.paper_sync_unreachable_title)).assertIsDisplayed()
            }
        } finally {
            DialogHandler.discardMessage()
        }
    }

    @Test
    fun `switching to AnkiWeb discards the confirmed server login and pending work`() {
        setSyncServer(TEST_SYNC_SERVER)
        Prefs.hkey = "old-server-token"
        Prefs.currentSyncUri = TEST_SYNC_SERVER
        val queued = requireNotNull(syncAuth()).toWorkData()
        Prefs.sharedPrefs.edit { putString(Prefs.key(R.string.custom_sync_certificate_key), "old server trust") }
        ActivityScenario.launch<AccountActivity>(AccountActivity.getSetupIntent(targetContext)).use {
            compose.onNodeWithText(targetContext.getString(CommonString.sync_use_ankiweb)).performScrollTo().performClick()
            advanceRobolectricLooper()
            val message = targetContext.getString(CommonString.sync_confirm_server_change, TEST_SYNC_SERVER, "AnkiWeb")
            compose.onNodeWithText(message).assertIsDisplayed()
            assertEquals("old-server-token", queued.toSyncAuth()?.hkey)
            compose.onNodeWithText(targetContext.getString(CommonString.dialog_cancel)).performClick()
            advanceRobolectricLooper()
            assertEquals(TEST_SYNC_SERVER, syncServerAddress())
            assertEquals("old server trust", Prefs.customSyncCertificate)

            compose.onNodeWithText(targetContext.getString(CommonString.sync_use_ankiweb)).performScrollTo().performClick()
            advanceRobolectricLooper()
            compose.onNode(hasText(targetContext.getString(CommonString.dialog_continue)) and hasAnyAncestor(isDialog())).performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(targetContext.getString(CommonString.sync_ankiweb_sign_in)).assertIsDisplayed()
            assertNull(Prefs.hkey)
            assertNull(Prefs.currentSyncUri)
            assertEquals("", syncCertificate())
            assertEquals("", Prefs.customSyncCertificate)
            assertNull(queued.toSyncAuth())
        }
    }

    private fun assertAccountEntry(
        intent: Intent,
        startWithAddress: Boolean,
    ) {
        assertEquals(AccountFlowFragment::class.jvmName, intent.getStringExtra(EXTRA_FRAGMENT_NAME))
        assertEquals(
            startWithAddress,
            intent.getBundleExtra(SingleFragmentActivity.EXTRA_FRAGMENT_ARGS)?.getBoolean(AccountFlowFragment.START_WITH_ADDRESS),
        )
    }
}
