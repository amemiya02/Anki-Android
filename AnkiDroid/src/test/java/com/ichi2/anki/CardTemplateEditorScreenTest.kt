// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.simulateKeyboard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardTemplateEditorScreenTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `template sections and field insertion are available with keyboard open`() =
        withCardTemplateEditor(col.notetypes.basicAndReversed) {
            simulateKeyboard()
            compose.onNodeWithText(TR.sentenceCase.frontTemplate).assertIsSelected()
            compose
                .onNodeWithText(TR.sentenceCase.backTemplate)
                .assertIsDisplayed()
                .performClick()
                .assertIsSelected()
            compose.onNodeWithText(TR.cardTemplatesTemplateStyling()).assertIsDisplayed()
            compose.onNodeWithText("Back").assertIsDisplayed().performClick()
            assertTrue(editText.text.toString().startsWith("{{Back}}"))
        }
}
