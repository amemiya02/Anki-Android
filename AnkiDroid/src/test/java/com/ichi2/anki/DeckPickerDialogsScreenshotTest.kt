// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Test

@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class DeckPickerDialogsScreenshotTest : DeckListScreenshotTest() {
    @Test
    fun create_deck() {
        showDeckList("create_deck") {
            showCreateDeckDialog()
            val action = getString(CommonString.dialog_positive_create)
            composeRule.onNodeWithText(action).assertIsDisplayed()
            val label = CollectionManager.TR.actionsName().dropLastWhile { it == ':' }
            composeRule.onNodeWithText(label).performTextReplacement("日本語")
            composeRule.waitUntil {
                runCatching { composeRule.onNodeWithText(action).assertIsDisplayed() }.isSuccess
            }
        }
    }
}
