// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.SingleFragmentActivity.Companion.EXTRA_FRAGMENT_NAME
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.AccountFlowFragment
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.dialogs.DialogHandler
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
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
import kotlin.test.assertNotNull

/** The deck list's route into sync setup, which replaces AnkiWeb sign-in (ADR-0001) */
@RunWith(AndroidJUnit4::class)
class SyncSetupTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

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
    fun `with no server address the sync button opens sync setup, not a sign-in form`() =
        deckPicker {
            Prefs.hkey = ""
            setSyncServer(null)
            val dialogs = ShadowDialog.getShownDialogs().size

            onOptionsItemSelected(RoboMenuItem(R.id.action_sync))
            advanceRobolectricLooper()

            assertEquals(dialogs, ShadowDialog.getShownDialogs().size, "no 'log in' dialog")
            compose.onNodeWithText(getString(CommonString.paper_sync_server_address)).assertIsDisplayed()
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
            compose.onNodeWithText(getString(CommonString.paper_sync_sign_in)).performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText(getString(CommonString.paper_sync_sign_in_title)).assertIsDisplayed()
            assertEquals(null, shadowOf(this).nextStartedActivity)
        }

    @Test
    fun `the account screen opens sync setup while no server address is set`() {
        setSyncServer(null)

        assertOpensSyncSetup(AccountActivity.getIntent(targetContext))
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

    private fun assertOpensSyncSetup(intent: Intent) {
        assertEquals(AccountActivity::class.java.name, intent.component?.className)
        assertAccountEntry(intent, startWithAddress = true)
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
