// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Activity
import android.content.Intent
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.browser.toIntent
import com.ichi2.anki.common.destinations.BrowserDestination
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class LegacyShortcutNavigationTest : RobolectricTest() {
    @Test
    fun `the previously pinned Browse launcher shortcut opens DeckPicker`() {
        val legacy =
            BrowserDestination.Open
                .toIntent(targetContext)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        assertForwarded(CardBrowser::class.java, legacy)
    }

    @Test
    fun `the previously pinned Study launcher shortcut opens DeckPicker`() {
        val legacy =
            ReviewerFragment
                .getIntent(targetContext)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(NavigationDrawerActivity.EXTRA_STARTED_WITH_SHORTCUT, true)

        assertForwarded(CardViewerActivity::class.java, legacy)
    }

    private fun assertForwarded(
        activityClass: Class<out Activity>,
        legacy: Intent,
    ) {
        ensureCollectionLoadIsSynchronous()
        Robolectric.buildActivity(activityClass, legacy).use { controller ->
            val activity = controller.create().get()
            val launch = shadowOf(activity).nextStartedActivity
            assertEquals(DeckPicker::class.java.name, launch?.component?.className)
            val destination = IntentCompat.getParcelableExtra(launch, DeckPicker.EXTRA_ENTRY_SCREEN, Intent::class.java)!!
            assertEquals(legacy.component, destination.component)
            assertEquals(legacy.action, destination.action)
            assertEquals(
                legacy.getBooleanExtra(NavigationDrawerActivity.EXTRA_STARTED_WITH_SHORTCUT, false),
                destination.getBooleanExtra(NavigationDrawerActivity.EXTRA_STARTED_WITH_SHORTCUT, false),
            )
            assertTrue(activity.isFinishing)
        }
    }
}
