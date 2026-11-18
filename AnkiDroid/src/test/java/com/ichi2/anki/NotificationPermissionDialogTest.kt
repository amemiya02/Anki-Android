// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.NotificationManager
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.ui.windows.permissions.PermissionsBottomSheet
import com.ichi2.testutils.EmptyAnkiActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [32])
class NotificationPermissionDialogTest : RobolectricTest(loadBackend = false) {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `when notifications are blocked the explanation can open the app notification settings`() {
        Robolectric.buildActivity(EmptyAnkiActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            shadowOf(assertNotNull(activity.getSystemService<NotificationManager>())).setNotificationsEnabled(false)
            PermissionsBottomSheet.launch(activity.supportFragmentManager, OptionalPermissionSet.LEGACY_NOTIFICATIONS)
            advanceRobolectricLooper()

            compose.onNodeWithText(activity.getString(CommonString.notifications_permission_explanation)).assertIsDisplayed()
            assertNull(shadowOf(activity).nextStartedActivity)
            compose.onNodeWithText(activity.getString(CommonString.open_settings)).performClick()

            val intent = assertNotNull(shadowOf(activity).nextStartedActivity)
            assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
            assertEquals(activity.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        }
    }
}
