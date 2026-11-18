// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.getBrowserWithNotes
import com.ichi2.testutils.ext.reopenWithLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class CardBrowserScreenScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `the browser displays results from the collection`() {
        runBlocking { col.reopenWithLanguage(language.option) }
        val noteType = col.notetypes.current().name
        val cherry = addNoteUsingNoteTypeName(noteType, "桜 · Cherry blossom", "Spring").firstCard()
        col.setUserFlagForCards(listOf(cherry.id), Flag.RED.code)
        val familiarity = addNoteUsingNoteTypeName(noteType, "熟悉 · Familiar", "Know well")
        familiarity.addTag("marked")
        col.updateNote(familiarity)
        col.sched.suspendCards(listOf(familiarity.firstCard().id))
        addNoteUsingNoteTypeName(noteType, "諦める · Give up", "Recognition")
        val browser = getBrowserWithNotes(noteCount = 0)
        waitForText("桜 · Cherry blossom")
        captureScreen("results")

        composeRule.onNodeWithContentDescription(browser.getString(R.string.browse_search_cards)).performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("(flag:1 OR flag:4)")
        waitForText("${TR.actionsFlagRed()} +1")
        captureScreen("search")

        composeRule.onNodeWithContentDescription(browser.getString(CommonString.dialog_cancel)).performClick()
        composeRule.onNodeWithContentDescription(browser.getString(R.string.browse_sort)).performClick()
        val noSorting = browser.getString(CommonString.card_browser_order_no_sorting_title)
        waitForText(noSorting)
        composeRule.onNodeWithText(noSorting).assertIsDisplayed()
        captureScreen("sort")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
