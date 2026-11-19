// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `the about screen names the app Hayaki and credits Upstream`() =
        withAboutScreen {
            compose.onNodeWithText("Hayaki").assertIsDisplayed()
            compose.onNodeWithText("AnkiDroid", ignoreCase = false).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Contributors", ignoreCase = false).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun `the about screen states the licence and links to the Fork's source`() =
        withAboutScreen {
            compose.onNodeWithText("GPL-3.0").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("AGPL-3.0").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Source code").performScrollTo().assertIsDisplayed()
        }

    private fun withAboutScreen(block: () -> Unit) {
        val intent = PreferencesActivity.getIntent(targetContext, AboutFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            advanceRobolectricLooper()
            block()
        }
    }
}
