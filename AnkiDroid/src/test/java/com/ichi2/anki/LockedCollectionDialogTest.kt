// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.testutils.BackendEmulatingOpenConflict
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class LockedCollectionDialogTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `launchCatchingTask explains a locked collection`() =
        withLockedCollection {
            throwOnShowError = false
            val controller = Robolectric.buildActivity(FragmentActivity::class.java).also(::saveControllerForCleanup)
            controller.get().setTheme(CommonR.style.Theme_Light)
            val activity = controller.setup().get()

            activity.launchCatchingTask { withCol { } }
            advanceRobolectricLooper()

            compose.onNodeWithText("Advanced settings", substring = true).assertIsDisplayed()
        }

    /**
     * The locked-collection dialog offers Settings, opened at the Advanced screen, where
     * changing the 'AnkiDroid directory' resolves the conflict
     */
    @Test
    fun `a locked collection error links to Advanced settings`() =
        withLockedCollection {
            throwOnShowError = false
            val controller = Robolectric.buildActivity(FragmentActivity::class.java).also(::saveControllerForCleanup)
            controller.get().setTheme(CommonR.style.Theme_Light)
            val activity = controller.setup().get()

            activity.launchCatchingTask { withCol { } }
            advanceRobolectricLooper()

            compose.onNodeWithText("Settings").performClick()
            advanceRobolectricLooper()

            val settings = shadowOf(activity).nextStartedActivity
            assertNotNull(settings, "Advanced settings should be opened")
            assertEquals(PreferencesActivity::class.qualifiedName, settings.component?.className)
        }

    /** Emulates #21051: another AnkiDroid install holds the collection lock */
    private fun withLockedCollection(block: () -> Unit) {
        BackendEmulatingOpenConflict.enable()
        try {
            block()
        } finally {
            BackendEmulatingOpenConflict.disable()
        }
    }
}
