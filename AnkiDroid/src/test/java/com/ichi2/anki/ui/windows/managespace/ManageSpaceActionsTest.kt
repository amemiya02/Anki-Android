// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.managespace

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Intent
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.StorageVolumeBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(AndroidJUnit4::class)
class ManageSpaceActionsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun clearingAppDataRequiresConfirmationAndCanBeCancelled() {
        recordAppDataSize()
        startActivityNormallyOpenCollectionWithIntent(
            ManageSpaceActivity::class.java,
            Intent(targetContext, ManageSpaceActivity::class.java),
        )
        advanceRobolectricLooper()

        val clearData = compose.onNodeWithText(targetContext.getString(CommonString.pref__delete_everything__title))
        clearData.performScrollTo().assertIsEnabled().performClick()
        val confirmation = compose.onNodeWithText(targetContext.getString(CommonString.dialog__delete_everything__message))
        compose.waitUntil(5_000) { confirmation.isDisplayed() }
        confirmation.assertIsDisplayed()
        compose.onNodeWithText(targetContext.getString(CommonString.dialog_cancel)).performClick()
        confirmation.assertDoesNotExist()
        clearData.assertIsDisplayed()
    }

    private fun recordAppDataSize() {
        val user = Process.myUserHandle()
        val volume =
            StorageVolumeBuilder("internal", targetContext.filesDir, "Internal", user, Environment.MEDIA_MOUNTED)
                .setIsPrimary(true)
                .build()
        val manager = requireNotNull(targetContext.getSystemService(StorageManager::class.java))
        shadowOf(manager).addStorageVolume(volume)

        val stats = ReflectionHelpers.newInstance(StorageStats::class.java)
        ReflectionHelpers.setField(stats, "dataBytes", 1024L)
        val statsManager = requireNotNull(targetContext.getSystemService(StorageStatsManager::class.java))
        shadowOf(statsManager).addStorageStats(StorageManager.UUID_DEFAULT, targetContext.packageName, user, stats)
    }
}
