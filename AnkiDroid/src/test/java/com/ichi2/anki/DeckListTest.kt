// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.study.FinishedFragment
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The deck list in the running app: tapping a Deck, its sub-Decks, and the empty state */
@RunWith(AndroidJUnit4::class)
class DeckListTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `tapping a Deck with Cards starts a study session`() {
        val spanish = addDeck("Spanish")
        col.setDeck(listOf(addBasicNote("hola", "hello").firstCard().id), spanish)

        deckPicker {
            shadowOf(this).clearNextStartedActivities()
            tapDeck("Spanish")

            val started = assertNotNull(shadowOf(this).nextStartedActivity)
            assertEquals(ReviewerFragment::class.java.name, started.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME))
            assertEquals(spanish, col.decks.selected())
        }
    }

    @Test
    fun `tapping a finished Deck opens the finished screen`() {
        val spanish = addDeck("Spanish")
        col.setDeck(listOf(addBasicNote("hola", "hello").firstCard().id), spanish)
        col.decks.select(spanish)
        col.sched.answerCard(assertNotNull(col.sched.card), anki.scheduler.CardAnswer.Rating.EASY)

        deckPicker {
            shadowOf(this).clearNextStartedActivities()
            composeRule.onNodeWithText("Done for today", useUnmergedTree = true).assertIsDisplayed()
            tapDeck("Spanish")

            val started = assertNotNull(shadowOf(this).nextStartedActivity)
            assertEquals(FinishedFragment::class.java.name, started.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME))
        }
    }

    @Test
    fun `an expanded Deck stays expanded when the deck list opens again`() {
        addDeck("Japanese::N3")

        deckPicker {
            composeRule.onNodeWithContentDescription("Expand").performClick()
            advanceRobolectricLooperUntil { composeRule.onAllNodesWithText("N3").fetchSemanticsNodes().isNotEmpty() }
        }
        deckPicker {
            // the first deck list is still open behind this one: ask this one what it shows
            assertTrue(displayedDecks.any { it.lastDeckNameComponent == "N3" }, "the sub-Deck is shown again")
        }
    }

    @Test
    fun `the deck list says how much is due today`() {
        val spanish = addDeck("Spanish")
        repeat(2) { col.setDeck(listOf(addBasicNote("front $it").firstCard().id), spanish) }

        deckPicker {
            composeRule.onNodeWithText("2 cards to study today").assertIsDisplayed()
            composeRule.onNodeWithText("0 studied so far").assertIsDisplayed()
        }
    }

    @Test
    fun `a Newcomer without Decks is told what to do first`() {
        deckPicker {
            composeRule.onNodeWithText("No decks yet").assertIsDisplayed()
            composeRule.onNodeWithText("Create a deck").performClick()
            advanceRobolectricLooper()

            assertNotNull(ShadowDialog.getLatestDialog(), "Create a deck opens the dialog that names the new Deck")
        }
    }

    @Test
    fun `the phone's deck list is titled Decks`() {
        deckPicker {
            composeRule.onNodeWithText("Decks").assertIsDisplayed()
        }
    }

    private fun DeckPicker.tapDeck(name: String) {
        composeRule.onNodeWithText(name).performClick()
        advanceRobolectricLooperUntil { shadowOf(this).peekNextStartedActivity() != null }
    }
}
