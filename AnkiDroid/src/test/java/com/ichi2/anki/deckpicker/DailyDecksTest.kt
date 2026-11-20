// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.decks.SetDeckCollapsedRequest
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.time.MockTime
import com.ichi2.anki.common.time.SECONDS_PER_DAY
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyDecksTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @After
    override fun tearDown() {
        super.tearDown()
        BackupManagerTestUtilities.reset()
    }

    @Test
    fun `today's summary counts a subdeck once and shows empty queues`() {
        val nested = addDeck("Japanese::N3")
        col.setDeck(listOf(addBasicNote("Vocabulary").firstCard().id), nested)
        addBasicNote("Anatomy")
        addRevBasicNoteDueToday("Geography", "Answer")

        withDeckPicker(deckCount = 0) {
            composeRule.onNodeWithText("3 cards to study today").assertIsDisplayed()
            composeRule.onNodeWithText("0 studied so far").assertIsDisplayed()
            composeRule.onNodeWithText("New: 2").assertIsDisplayed()
            composeRule.onNodeWithText("Learning: 0").assertIsDisplayed()
            composeRule.onNodeWithText("Due: 1").assertIsDisplayed()
        }
    }

    @Test
    fun `a populated collection can be clear for today`() {
        val card = addBasicNote().firstCard()
        col.sched.suspendCards(listOf(card.id))

        withDeckPicker(deckCount = 0) {
            composeRule.onNodeWithText("Nothing left to study today").assertIsDisplayed()
            composeRule.onNodeWithText("New: 0").assertIsDisplayed()
            composeRule.onNodeWithText("Learning: 0").assertIsDisplayed()
            composeRule.onNodeWithText("Due: 0").assertIsDisplayed()
            composeRule.onNodeWithText("No decks yet").assertDoesNotExist()
        }
    }

    @Test
    fun `studying the current Deck under a collapsed parent finishes there and refreshes Decks`() {
        // Keep review timestamps on the Engine's current study day.
        TimeManager.resetWith(MockTime((col.sched.dayCutoff - SECONDS_PER_DAY + 60) * 1000, step = 10))
        val nested = addDeck("Japanese::N3")
        col.setDeck(listOf(addBasicNote("Vocabulary").firstCard().id), nested)
        addBasicNote("Anatomy")
        col.decks.select(nested)
        col.decks.setCollapsed(col.decks.id("Japanese"), collapsed = true, SetDeckCollapsedRequest.Scope.REVIEWER)

        withDeckPicker(deckCount = 0) { deckPicker ->
            composeRule.onNodeWithText("N3").assertDoesNotExist()
            tap("Study · N3")
            tap(deckPicker.getString(CommonString.show_answer))
            tap(deckPicker.getString(CommonString.ease_button_easy))
            tap(deckPicker.getString(CommonString.finished_back_to_decks))

            awaitText("1 studied so far")
            composeRule.onNodeWithText("1 card to study today").assertIsDisplayed()
            composeRule.onNodeWithText("New: 1").assertIsDisplayed()
            composeRule.onNodeWithText("Study · N3").assertDoesNotExist()
        }
    }

    private fun awaitText(text: String) =
        advanceRobolectricLooperUntil(lazyMessage = { "Expected to find the text '$text'" }) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private fun tap(text: String) {
        awaitText(text)
        composeRule.onNodeWithText(text).performClick()
    }
}
