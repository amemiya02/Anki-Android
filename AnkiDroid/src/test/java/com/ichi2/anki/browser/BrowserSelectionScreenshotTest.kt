// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.pressBack
import anki.notetypes.StockNotetype
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonPlurals
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
import com.ichi2.anki.libanki.getStockNotetype
import com.ichi2.testutils.ext.reopenWithLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class BrowserSelectionScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `selection actions and columns adapt to theme language and font size`() {
        runBlocking { col.reopenWithLanguage(language.option) }
        addNoteUsingNoteTypeName(col.getStockNotetype(StockNotetype.Kind.KIND_BASIC).name, "Ginkgo", "Fan-shaped leaf")
        val browser = getBrowserWithNotes(noteCount = 0)
        waitForText("Ginkgo")

        composeRule.onNodeWithText("Ginkgo").performTouchInput { longClick() }
        waitForText(browser.getString(R.string.browse_selected_count, 1))
        captureScreen("selection")

        composeRule.onNodeWithContentDescription(browser.getString(R.string.browse_action_more)).performClick()
        val preview = browser.getString(CommonString.card_editor_preview_card)
        waitForText(preview)
        captureScreen("actions")
        val delete = browser.resources.getQuantityString(CommonPlurals.card_browser_delete_notes, 1)
        composeRule.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText(preview))).performScrollToNode(hasText(delete))
        composeRule.onNodeWithText(delete).assertIsDisplayed()
        captureScreen("actions_scrolled")

        pressBack()
        advanceRobolectricLooper()
        composeRule.onNodeWithContentDescription(browser.getString(CommonString.dialog_cancel)).performClick()
        composeRule.onNodeWithContentDescription(browser.getString(R.string.browse_more_actions)).performClick()
        val columns = browser.getString(CommonString.browse_manage_columns_main_heading)
        composeRule.onNodeWithText(columns).performClick()
        waitForText(browser.getString(R.string.browse_columns_shown))
        captureScreen("columns")
        val inColumns = hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, columns))
        composeRule.onNode(inColumns and hasText(browser.getString(R.string.browse_mode_notes))).performClick()
        waitForText(browser.getString(R.string.browse_columns_row_title))
        captureScreen("columns_notes")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
