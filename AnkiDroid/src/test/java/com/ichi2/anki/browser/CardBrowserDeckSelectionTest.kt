// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.getBrowserWithNotes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CardBrowserDeckSelectionTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `the deck chip applies multiple decks and the empty selection restores all decks`() {
        addDeckNote("Japanese", "Ginkgo")
        addDeckNote("Chinese", "Lotus")
        addBasicNote("Pine")
        getBrowserWithNotes(noteCount = 0)

        composeRule.onNodeWithContentDescription("All decks").performClick()
        choose("Default")
        choose("Japanese")
        choose("Chinese")
        composeRule.onNodeWithText("Done").performClick()

        waitForResult("Ginkgo")
        waitForResult("Lotus")
        composeRule.onNodeWithText("Pine").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("All decks").performClick()
        composeRule.onNodeWithText("Japanese").assertIsOn().performClick()
        composeRule.onNodeWithText("Chinese").assertIsOn().performClick()
        composeRule.onNodeWithText("Done").performClick()
        waitForResult("Pine")
    }

    @Test
    fun `the search page restores Deck choices from a typed Boolean expression`() {
        addDeckNote("Japanese garden", "Ginkgo")
        addDeckNote("Chinese garden", "Lotus")
        addBasicNote("Pine")
        getBrowserWithNotes(noteCount = 0)

        composeRule.onNodeWithContentDescription("Search cards").performClick()
        composeRule.waitUntilAtLeastOneExists(hasSetTextAction(), 10_000)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("(deck:\"Japanese garden\" OR deck:\"Chinese garden\")")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        waitForResult("Ginkgo")
        waitForResult("Lotus")
        composeRule.onNodeWithText("Pine").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("All decks").performClick()
        composeRule.onNodeWithText("Japanese garden").assertIsOn()
        composeRule.onNodeWithText("Chinese garden").assertIsOn()
    }

    private fun addDeckNote(
        deckName: String,
        front: String,
    ) {
        val deck = addDeck(deckName)
        val card = addBasicNote(front).firstCard()
        col.setDeck(listOf(card.id), deck)
    }

    private fun choose(label: String) {
        composeRule.waitUntilAtLeastOneExists(hasText(label), 10_000)
        composeRule.onNode(hasText(label) and isToggleable()).performClick()
    }

    private fun waitForResult(title: String) {
        composeRule.waitUntilAtLeastOneExists(hasText(title), 10_000)
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }
}
