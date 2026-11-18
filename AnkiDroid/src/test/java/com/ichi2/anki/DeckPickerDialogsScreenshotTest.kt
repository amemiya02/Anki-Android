// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class DeckPickerDialogsScreenshotTest : DeckListScreenshotTest() {
    @Test
    fun create_deck() {
        showDeckList("create_deck") {
            showCreateDeckDialog()
            val action = getString(CommonString.dialog_positive_create)
            composeRule.onNodeWithText(action).assertIsDisplayed().assertIsNotEnabled()
            val label = getString(CommonString.paper_dialog_deck_name)
            val deckName = "日本語 · N3 語彙"
            composeRule.onNode(hasSetTextAction() and hasText(label)).performTextReplacement(deckName)
            advanceRobolectricLooperUntil {
                kotlin.runCatching { composeRule.onNodeWithText(action).assertIsEnabled() }.isSuccess
            }
            captureScreen("create_deck_dialog")
            composeRule.onNodeWithText(action).performClick()
            advanceRobolectricLooperUntil {
                kotlin.runCatching { composeRule.onNodeWithText(deckName).assertIsDisplayed() }.isSuccess
            }
        }
    }
}
