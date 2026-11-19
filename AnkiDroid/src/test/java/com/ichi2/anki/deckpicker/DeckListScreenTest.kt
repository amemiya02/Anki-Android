// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.decks.DeckTreeNode
import anki.decks.deckTreeNode
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.sched.DeckNode
import com.ichi2.compose.theme.AnkiDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    private fun show(
        state: DeckListScreenState,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                AnkiDroidTheme {
                    DeckListScreen(state = state, actions = actions)
                }
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
    @Config(qualifiers = "zh-rCN-w320dp-h880dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a narrow deck list shows the full name of a nested deck with large counts`() {
        show(nestedDecks("2-N4"))

        assertTextFits("2-N4")
        assertCountsFitUnderHeadings()
    }

    @Test
    @Config(qualifiers = "ar-w320dp-h880dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a narrow right-to-left deck list wraps a long deck name at large text size`() {
        val name = "مراجعة قواعد اللغة اليابانية"
        show(nestedDecks(name), fontScale = 2f)

        assertTextFits(name)
        assertCountsFitUnderHeadings()
    }

    private fun nestedDecks(leafName: String): DeckListScreenState {
        val names = listOf("JLPT", "コース", "語彙", "文法", leafName)
        var child: DeckTreeNode? = null
        for (index in names.indices.reversed()) {
            child =
                deckTreeNode {
                    name = names[index]
                    deckId = index.toLong() + 10
                    level = index + 1
                    if (index == names.lastIndex) {
                        newCount = 30
                        learnCount = 41
                        reviewCount = 4966
                    }
                    child?.let { children.add(it) }
                }
        }
        val decks = DeckNode(checkNotNull(child), names.first()).filterAndFlattenDisplay(DeckFilters.create(""), 0L)
        return DeckListScreenState(decks = decks, isEmptyCollection = false, dueToday = 5037, studiedToday = 0)
    }

    private fun assertCountsFitUnderHeadings() {
        fun centerX(text: String): Float {
            val bounds = composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()
            return (bounds.left.value + bounds.right.value) / 2
        }

        val counts = listOf("30", "41", "4966")
        val headings = listOf(TR.actionsNew(), TR.schedulingLearning(), TR.decksReviewHeader())
        composeRule.onNodeWithText(counts.last(), useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        counts.zip(headings).forEach { (count, heading) ->
            val layout = textLayout(count)
            assertEquals(1, layout.lineCount, "Count '$count' must fit on one line within ${layout.size}")
            assertFalse(layout.isLineEllipsized(0), "Count '$count' ends in an ellipsis")
            assertEquals(count.length, layout.getLineEnd(0, visibleEnd = true))
            for (digit in count.indices) {
                val glyph = layout.getBoundingBox(digit)
                assertTrue(
                    glyph.left >= -1f && glyph.right <= layout.size.width + 1f && glyph.top >= -1f &&
                        glyph.bottom <= layout.size.height + 1f,
                    "Digit $digit of '$count' falls outside the text layout: $glyph within ${layout.size}",
                )
            }
            assertTrue(abs(centerX(heading) - centerX(count)) <= 1f, "The $count count is not centered under its heading")
        }
    }

    private fun assertTextFits(text: String) {
        val layout = textLayout(text)
        assertTrue(layout.size.width > 0, "'$text' has no space to render")
        assertFalse(layout.hasVisualOverflow, "'$text' is clipped")
        val finalLine = layout.lineCount - 1
        assertFalse(layout.isLineEllipsized(finalLine), "'$text' ends in an ellipsis")
        assertEquals(text.length, layout.getLineEnd(finalLine, visibleEnd = true))
    }

    private fun textLayout(text: String): TextLayoutResult {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        checkNotNull(node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action)(layouts)
        return layouts.single()
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
