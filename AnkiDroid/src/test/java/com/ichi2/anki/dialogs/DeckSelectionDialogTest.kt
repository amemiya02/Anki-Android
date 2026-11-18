// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Akshit Sinha <akshitsinha3@gmail.com>

package com.ichi2.anki.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.fragment.app.testing.launchFragment
import anki.decks.SetDeckCollapsedRequest
import anki.decks.deckTreeNode
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.sched.DeckNode
import com.ichi2.anki.model.SelectableDeck
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(RobolectricTestRunner::class)
class DeckSelectionDialogTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val search get() = compose.onNode(hasSetTextAction())

    @Test
    fun verifyDeckDisplayName() {
        val input = "deck::sub-deck::sub-deck2::sub-deck3"
        val expected = "sub-deck3"

        val deck = SelectableDeck.Deck(1234, input)
        val actual: String = deck.getDisplayName(targetContext)

        assertThat(actual, Matchers.equalTo(expected))
    }

    @Test
    fun testDialogCreation() {
        val decks: List<SelectableDeck> = listOf(SelectableDeck.Deck(5L, "deck"))
        val dialogTitle = "Select Deck"
        val summaryMessage = "Choose a deck from the list"

        val dialog = DeckSelectionDialog.newInstance(title = dialogTitle, templateEditorMessage = summaryMessage, decks = decks)
        assertNotNull(dialog)
        assertEquals(dialogTitle, dialog.arguments?.getString("arg_title"))
        assertEquals(summaryMessage, dialog.arguments?.getString("arg_template_editor_message"))
    }

    @Test
    fun `selecting child deck does not collect parent or sibling ids`() {
        // Build tree: Parent (id=1) -> Child1 (id=2), Child2 (id=3)
        val child1 = makeNode("Child1", deckId = 2, level = 2)
        val child2 = makeNode("Child2", deckId = 3, level = 2)
        val parent = makeNode("Parent", deckId = 1, level = 1, children = listOf(child1, child2))

        val selectedChild = parent.find(2L)!!
        val idsToRemove = mutableSetOf(selectedChild.did)
        selectedChild.forEach { idsToRemove.add(it.did) }

        assertThat(idsToRemove, containsInAnyOrder(2L))
        assertThat(idsToRemove, not(hasItem(1L)))
        assertThat(idsToRemove, not(hasItem(3L)))
    }

    @Test
    fun `selecting parent deck with nested children collects all descendants`() {
        // Build tree: A (id=1) -> B (id=2) -> C (id=3)
        //                      -> D (id=4)
        val c = makeNode("C", deckId = 3, level = 3)
        val b = makeNode("B", deckId = 2, level = 2, children = listOf(c))
        val d = makeNode("D", deckId = 4, level = 2)
        val a = makeNode("A", deckId = 1, level = 1, children = listOf(b, d))

        val idsToRemove = mutableSetOf(a.did)
        a.forEach { idsToRemove.add(it.did) }

        assertThat(idsToRemove, containsInAnyOrder(1L, 2L, 3L, 4L))
    }

    @Test
    fun `search entered while the deck tree loads remains applied`() {
        addDeck("Chosen")
        addDeck("Unrelated")
        val decks = allDecks()
        holdingCollectionAccess { scheduler ->
            launchDeckSelectionDialog(decks).use {
                search.performTextReplacement("Chosen")
                deck("Chosen").assertDoesNotExist()

                scheduler.advanceUntilIdle()
                advanceRobolectricLooper()

                search.assertTextContains("Chosen")
                deck("Chosen").assertIsDisplayed()
                compose.onNodeWithText("Unrelated").assertDoesNotExist()
            }
        }
    }

    @Test
    fun `search before load displays a matching child under a collapsed parent`() {
        addDeck("Parent::Chosen")
        collapseDeck("Parent")
        val decks = allDecks()
        holdingCollectionAccess { scheduler ->
            launchDeckSelectionDialog(decks).use {
                search.performTextReplacement("Chosen")
                compose.onNodeWithText("Parent::Chosen").assertDoesNotExist()

                scheduler.advanceUntilIdle()
                advanceRobolectricLooper()

                compose.onNodeWithText("Parent::Chosen").assertIsDisplayed()
            }
        }
    }

    @Test
    fun `changing the search during load uses only the latest query`() {
        addDeck("Chosen")
        addDeck("Unrelated")
        val decks = allDecks()
        holdingCollectionAccess { scheduler ->
            launchDeckSelectionDialog(decks).use {
                search.performTextReplacement("Unrelated")
                search.performTextReplacement("Chosen")

                scheduler.advanceUntilIdle()
                advanceRobolectricLooper()

                deck("Chosen").assertIsDisplayed()
                compose.onNodeWithText("Unrelated").assertDoesNotExist()
            }
        }
    }

    @Test
    fun `collapsing a search result keeps unrelated decks filtered out`() {
        addDeck("Parent::Parent Child")
        addDeck("Unrelated")
        launchDeckSelectionDialog(allDecks()).use {
            advanceRobolectricLooper()
            search.performTextReplacement("Parent")
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Parent::Parent Child"))
            deck("Parent::Parent Child").assertIsDisplayed()

            compose.onNodeWithContentDescription(getResourceString(CommonString.collapse)).performClick()

            search.assertTextContains("Parent")
            deck("Parent").assertIsDisplayed()
            compose.onNodeWithText("Parent::Parent Child").assertDoesNotExist()
            compose.onNodeWithText("Unrelated").assertDoesNotExist()
        }
    }

    @Test
    fun `multi selection stays open when the last search match is selected`() {
        addDeck("Chosen")
        addDeck("Unrelated")
        launchDeckSelectionDialog(allDecks(), allowMultipleSelection = true).use {
            search.performTextReplacement("Chosen")
            deck("Chosen").performClick()
            search.performTextReplacement("")
            deck("Chosen").assertDoesNotExist()
            compose.onNodeWithText("Unrelated").assertIsDisplayed()
        }
    }

    private fun deck(name: String) = compose.onNode(hasText(name) and hasClickAction() and !hasSetTextAction())

    private fun allDecks() = col.decks.allNamesAndIds().map { SelectableDeck.Deck(it) }

    private fun collapseDeck(name: String) {
        val deckId = requireNotNull(col.decks.idForName(name))
        col.decks.setCollapsed(deckId, true, SetDeckCollapsedRequest.Scope.REVIEWER)
        val tree = col.sched.deckDueTree()
        assertTrue(tree.find(deckId)!!.collapsed)
    }

    private fun launchDeckSelectionDialog(
        decks: List<SelectableDeck>,
        allowMultipleSelection: Boolean = false,
    ) = launchFragment<DeckSelectionDialog>(
        fragmentArgs = DeckSelectionDialog.newInstance(decks = decks, allowMultipleSelection = allowMultipleSelection).arguments,
        themeResId = CommonR.style.Theme_Light,
    )

    private fun holdingCollectionAccess(block: (TestCoroutineScheduler) -> Unit) {
        val queue = StandardTestDispatcher()
        val previousQueue = CollectionManager.setTestDispatcher(queue, useReentrantLock = false)
        try {
            block(queue.scheduler)
        } finally {
            CollectionManager.setTestDispatcher(previousQueue)
        }
    }

    private fun makeNode(
        name: String,
        deckId: Long,
        level: Int,
        collapsed: Boolean = false,
        children: List<DeckNode> = emptyList(),
    ): DeckNode {
        val treeNode =
            deckTreeNode {
                this.name = name
                this.deckId = deckId
                this.level = level
                this.collapsed = collapsed
                children.forEach { this.children.add(it.node) }
                this.reviewCount = 0
                this.newCount = 0
                this.learnCount = 0
                this.filtered = false
            }
        return DeckNode(treeNode, name)
    }
}
