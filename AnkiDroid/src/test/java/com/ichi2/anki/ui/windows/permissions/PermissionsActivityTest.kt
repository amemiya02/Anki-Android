// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.StoragePermissionSet
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.grantPermissions
import com.ichi2.testutils.publicCollectionPath
import com.ichi2.testutils.withAllFilesAccess
import com.ichi2.testutils.withManageExternalStorageInManifest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PermissionsActivityTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val continueButton
        get() = compose.onNodeWithText(getResourceString(CommonString.dialog_continue))

    @Test
    fun `collection permission opens inside Decks and returns there after Continue`() =
        withDeckPicker(deckCount = 1) { host ->
            grantPermissions(*StoragePermissionSet.LEGACY_ACCESS.permissions.toTypedArray())
            host.startActivity(PermissionsActivity.getIntent(host, StoragePermissionSet.LEGACY_ACCESS))
            advanceRobolectricLooper()

            continueButton.assertIsDisplayed()
            assertNull(shadowOf(host).nextStartedActivity)
            host.onBackPressedDispatcher.onBackPressed()
            assertFalse(host.isFinishing)
            continueButton.assertIsDisplayed().performClick()
            advanceRobolectricLooper()

            compose.onNodeWithText("Test Deck 0").assertIsDisplayed()
            assertFalse(host.isFinishing)
        }

    @Test
    fun testActivityCantBeClosedByBackButton() {
        testActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            assertFalse(activity.isFinishing)
        }
    }

    @Test
    fun testOnClickingContinueActivityFinishes() {
        grantPermissions(*StoragePermissionSet.LEGACY_ACCESS.permissions.toTypedArray())
        testActivity { activity ->
            continueButton.assertIsEnabled().performClick()
            assertTrue(activity.isFinishing)
        }
    }

    @Test // Issue 13574: completing the screen must record the storage decision
    @Config(sdk = [Build.VERSION_CODES.R])
    @SuppressLint("NewApi")
    fun `continuing decides the collection path`() {
        targetContext.sharedPrefs().edit { remove(CollectionHelper.PREF_COLLECTION_PATH) }
        withManageExternalStorageInManifest {
            withAllFilesAccess {
                testActivity(StoragePermissionSet.EXTERNAL_MANAGER) { activity ->
                    continueButton.assertIsEnabled().performClick()
                    assertTrue(activity.isFinishing)
                }
            }
        }
        assertEquals(publicCollectionPath, collectionPath)
    }

    @Test // Issue 13574: granting all-files access must complete the screen without Continue.
    @Config(sdk = [Build.VERSION_CODES.R])
    @SuppressLint("NewApi")
    fun `completing without Continue decides the collection path`() {
        targetContext.sharedPrefs().edit { remove(CollectionHelper.PREF_COLLECTION_PATH) }

        val settingsScreen = ComponentName("com.android.settings", "AllFilesAccessSettings")
        shadowOf(targetContext.packageManager).apply {
            addActivityIfNotPresent(settingsScreen).exported = true
            addIntentFilterForActivity(
                settingsScreen,
                IntentFilter(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addDataScheme("package")
                },
            )
        }

        withManageExternalStorageInManifest {
            testActivity(StoragePermissionSet.EXTERNAL_MANAGER) { activity ->
                continueButton.assertIsNotEnabled()
                compose.onNodeWithText(getResourceString(CommonString.all_files_access_title)).performClick()

                val shadowActivity = shadowOf(activity)
                val settingsIntent = assertNotNull(shadowActivity.nextStartedActivityForResult).intent
                assertEquals(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, settingsIntent.action)
                assertNull(collectionPath)

                withAllFilesAccess {
                    shadowActivity.receiveResult(settingsIntent, Activity.RESULT_CANCELED, Intent())
                }
                assertEquals(publicCollectionPath, collectionPath)
                assertTrue(activity.isFinishing)
            }
        }
    }

    private val collectionPath: String?
        get() = targetContext.sharedPrefs().getString(CollectionHelper.PREF_COLLECTION_PATH, null)

    @Test
    fun `error toast is shown if EXTRA_PERMISSIONS_SET is missing`() {
        val exception = assertFailsWith<NullPointerException> { testActivity(permissionSet = null) {} }
        assertEquals("Cannot run onActivity since Activity has been destroyed already", exception.message)
        assertEquals(getResourceString(CommonString.something_wrong), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `permission screens request the permissions required by the storage policy`() {
        StoragePermissionSet.entries.forEach { permissionSet ->
            val requests = permissionSet.permissionsFragment?.getDeclaredConstructor()?.newInstance()
            assertEquals(
                permissionSet.permissions.sorted(),
                requests
                    ?.permissionRequests()
                    .orEmpty()
                    .flatMap { it.permissions }
                    .sorted(),
            )
        }
    }

    private fun testActivity(
        permissionSet: StoragePermissionSet? = StoragePermissionSet.LEGACY_ACCESS,
        action: (PermissionsActivity) -> Unit,
    ) {
        val intent =
            if (permissionSet == null) {
                Intent(targetContext, PermissionsActivity::class.java)
            } else {
                PermissionsActivity.getIntent(targetContext, permissionSet)
            }
        ActivityScenario.launch<PermissionsActivity>(intent).use { scenario ->
            lateinit var activity: PermissionsActivity
            scenario.onActivity { activity = it }
            advanceRobolectricLooper()
            action(activity)
        }
    }
}
