// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.getBrowserWithNotes
import com.ichi2.testutils.ext.reopenWithLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
class BrowserDialogsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `browser options expose save and return to the browser`() {
        runBlocking { col.reopenWithLanguage(language.option) }
        addNoteUsingNoteTypeName(col.notetypes.current().name, "Ginkgo", "Fan-shaped leaf")
        val browser = getBrowserWithNotes(noteCount = 0)
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText("Ginkgo").fetchSemanticsNodes().isNotEmpty()
        }

        browser.cardBrowserFragment.showOptionsDialog()
        advanceRobolectricLooper()
        val save = browser.getString(CommonString.save)
        composeRule.onNodeWithText(save).assertIsDisplayed()
        val truncate = composeRule.onNodeWithText(browser.getString(CommonString.card_browser_truncate))
        truncate
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        captureScreen("browser_options")
        composeRule.onNodeWithText(save).performClick()
        composeRule.onNodeWithText("Ginkgo").assertIsDisplayed()
        browser.cardBrowserFragment.showOptionsDialog()
        advanceRobolectricLooper()
        truncate.performScrollTo().assertIsOn()
        composeRule.onNodeWithText(browser.getString(CommonString.dialog_cancel)).performClick()
    }
}
