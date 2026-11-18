// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.getBrowserWithNotes
import com.ichi2.anki.ui.internationalization.toSentenceCase
import com.ichi2.testutils.ext.reopenWithLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class BrowserDialogsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `browser options expose save and return to the browser`() {
        addBasicNote("Ginkgo", "Fan-shaped leaf")
        runBlocking { col.reopenWithLanguage(language.option) }
        val browser = getBrowserWithNotes(noteCount = 0)
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText("Ginkgo").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription(browser.getString(R.string.browse_more_actions)).performClick()
        val options = TR.browsingBrowserOptions().toSentenceCase(browser, com.ichi2.anki.ankicommon.R.string.sentence_browser_options)
        composeRule.onNodeWithText(options).performClick()
        val save = browser.getString(CommonString.save)
        composeRule.onNodeWithText(save).assertIsDisplayed()
        captureScreen("browser_options")
        composeRule.onNodeWithText(save).performClick()
        composeRule.onNodeWithText("Ginkgo").assertIsDisplayed()
    }
}
