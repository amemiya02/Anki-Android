// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.DeckId
import com.ichi2.compose.theme.AnkiDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/** The deck list ([DeckListScreen]), driven by what a person sees */
@RunWith(AndroidJUnit4::class)
class DeckListScreenTest : RobolectricTest() {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<String>()

    private val actions =
        object : DeckListActions {
            override fun onDeckClick(deckId: DeckId) {
                events += "study ${name(deckId)}"
            }

            override fun onDeckCountsClick(deckId: DeckId) {
                events += "options ${name(deckId)}"
            }

            override fun onDeckLongClick(deckId: DeckId) {
                events += "menu ${name(deckId)}"
            }

            override fun onDeckRightClick(
                deckId: DeckId,
                x: Float,
                y: Float,
            ) {
                events += "right click ${name(deckId)}"
            }

            override fun onToggleExpand(deckId: DeckId) {
                events += "toggle ${name(deckId)}"
            }

            override fun onGetSharedDecks() {
                events += "get shared decks"
            }

            override fun onImport() {
                events += "import"
            }

            override fun onCreateDeck() {
                events += "create deck"
            }
        }

    private fun name(deckId: DeckId) = col.decks.name(deckId)

    private fun show(state: DeckListScreenState) {
        composeRule.setContent {
            AnkiDroidTheme {
                DeckListScreen(state = state, actions = actions)
            }
        }
    }

    /** The Decks of the collection as the deck list would show them */
    private fun decks(): List<DisplayDeckNode> =
        col.sched.deckDueTree().filterAndFlattenDisplay(DeckFilters.create(""), col.decks.current().id)

    private fun stateWithDecks(
        dueToday: Int = 0,
        studiedToday: Int = 0,
    ) = DeckListScreenState(decks = decks(), isEmptyCollection = false, dueToday = dueToday, studiedToday = studiedToday)

    @Test
    fun `each Deck shows its new, learning and due counts under the column headings`() {
        val spanish = addDeck("Spanish")
        repeat(3) { addBasicNote("front $it").firstCard().let { card -> col.setDeck(listOf(card.id), spanish) } }
        show(stateWithDecks())

        composeRule.onNodeWithText("Spanish").assertIsDisplayed()
        composeRule.onNodeWithText("New").assertIsDisplayed()
        composeRule.onNodeWithText("Learning").assertIsDisplayed()
        composeRule.onNodeWithText("Due").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("New: 3", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Learning: 0", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Due: 0", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a Deck with nothing left today says it is done`() {
        addDeck("Anatomy")
        show(stateWithDecks())

        composeRule.onNodeWithText("Done for today", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `the total due today and today's study count are shown`() {
        addDeck("Spanish")
        show(stateWithDecks(dueToday = 58, studiedToday = 9))

        composeRule.onNodeWithText("58 cards to study today").assertIsDisplayed()
        composeRule.onNodeWithText("9 studied so far").assertIsDisplayed()
    }

    @Test
    fun `tapping a Deck studies it, its counts open study options, and a long press opens its menu`() {
        addDeck("Spanish")
        show(stateWithDecks())

        composeRule.onNodeWithText("Spanish").performClick()
        composeRule.onNodeWithText("Done for today", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Spanish").performTouchInput { longClick() }

        assertEquals(listOf("study Spanish", "options Spanish", "menu Spanish"), events)
    }

    @Test
    fun `a screen reader offers a Deck's actions by name`() {
        addDeck("Spanish")
        show(stateWithDecks())

        val longClick = composeRule.onNodeWithText("Spanish").fetchSemanticsNode().config[SemanticsActions.OnLongClick]
        assertEquals("Deck actions", longClick.label)
        longClick.action?.invoke()

        assertEquals(listOf("menu Spanish"), events)
    }

    @Test
    fun `a Deck with sub-Decks expands and collapses`() {
        addDeck("Japanese::N3")
        show(stateWithDecks())

        composeRule.onNodeWithContentDescription("Expand").performClick()

        assertEquals(listOf("toggle Japanese"), events)
    }

    @Test
    fun `an expanded Deck shows its sub-Decks and can be collapsed`() {
        val japanese = addDeck("Japanese::N3").let { col.decks.id("Japanese") }
        col.decks.setCollapsed(japanese, collapsed = false, anki.decks.SetDeckCollapsedRequest.Scope.REVIEWER)
        show(stateWithDecks())

        composeRule.onNodeWithText("N3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Collapse").performClick()

        assertEquals(listOf("toggle Japanese"), events)
    }

    @Test
    fun `with no Decks, a Newcomer is told what to do first`() {
        show(DeckListScreenState(decks = decks(), isEmptyCollection = true, dueToday = 0, studiedToday = 0))

        composeRule.onNodeWithText("No decks yet").assertIsDisplayed()
        composeRule.onNodeWithText("Get shared decks").performClick()
        composeRule.onNodeWithText("Import a file").performClick()
        composeRule.onNodeWithText("Create a deck").performClick()

        assertEquals(listOf("get shared decks", "import", "create deck"), events)
    }
}
