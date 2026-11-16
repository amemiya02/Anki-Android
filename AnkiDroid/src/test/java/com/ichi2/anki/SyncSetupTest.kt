// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.SingleFragmentActivity.Companion.EXTRA_FRAGMENT_ARGS
import com.ichi2.anki.SingleFragmentActivity.Companion.EXTRA_FRAGMENT_NAME
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.LoginFragment
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.preferences.CustomSyncServerSettingsFragment
import com.ichi2.anki.preferences.EXTRA_INITIAL_FRAGMENT
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.setSyncServer
import org.junit.After
import org.junit.Before
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
            val intent = assertNotNull(shadowOf(this).nextStartedActivity)
            assertOpensSyncSetup(intent)
        }

    @Test
    fun `with a server address the sync button offers to sign in to that server`() =
        deckPicker {
            Prefs.hkey = ""
            setSyncServer(TEST_SYNC_SERVER)

            onOptionsItemSelected(RoboMenuItem(R.id.action_sync))
            advanceRobolectricLooper()

            val dialog = assertNotNull(ShadowDialog.getLatestDialog() as? AlertDialog)
            assertEquals(
                getString(CommonString.sync_server_not_logged_in_title),
                dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.text?.toString(),
            )
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            advanceRobolectricLooper()

            val intent = assertNotNull(shadowOf(this).nextStartedActivity)
            assertEquals(AccountActivity::class.java.name, intent.component?.className)
            assertEquals(LoginFragment::class.jvmName, intent.getStringExtra(EXTRA_FRAGMENT_NAME))
        }

    @Test
    fun `the account screen opens sync setup while no server address is set`() {
        setSyncServer(null)

        assertOpensSyncSetup(AccountActivity.getIntent(targetContext))
    }

    private fun assertOpensSyncSetup(intent: Intent) {
        assertEquals(PreferencesActivity::class.java.name, intent.component?.className)
        val args = assertNotNull(intent.getBundleExtra(EXTRA_FRAGMENT_ARGS))
        assertEquals(CustomSyncServerSettingsFragment::class.jvmName, args.getString(EXTRA_INITIAL_FRAGMENT))
    }
}
