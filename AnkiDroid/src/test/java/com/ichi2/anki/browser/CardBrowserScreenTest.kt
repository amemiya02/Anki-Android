// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.getBrowserWithNotes
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.withDeckPicker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CardBrowserScreenTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `a typed OR group selects both flag choices and returns both cards`() {
        flaggedNote("Dawn", Flag.RED)
        flaggedNote("Midnight", Flag.BLUE)
        addBasicNote("Noon")
        getBrowserWithNotes(noteCount = 0)

        composeRule.onNodeWithContentDescription("Search cards").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("(flag:4 OR flag:1)")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Noon").fetchSemanticsNodes().isEmpty() }
        waitForText("Dawn")
        waitForText("Midnight")

        val red = TR.actionsFlagRed()
        waitForText("$red +1")
        composeRule.onNodeWithText("$red +1").performClick()
        composeRule.onNodeWithText(red).assertIsOn()
        composeRule.onNodeWithText(TR.actionsFlagBlue()).assertIsOn()

        composeRule.onNodeWithText(red).performClick()
        composeRule.onNodeWithText("Done").performClick()
        waitForText("Midnight")
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Dawn").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun `notes mode collapses sibling cards and tapping the note opens its editor`() {
        addBasicAndReversedNote("Ginkgo", "Tree")
        val browser = getBrowserWithNotes(noteCount = 0)
        waitForText("2 cards shown")

        composeRule.onNodeWithText("Notes").performClick()
        waitForText("1 note shown")
        waitForText("Ginkgo")
        composeRule.onNodeWithText("Ginkgo").performClick()
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            shadowOf(browser).peekNextStartedActivity() != null
        }
        val editorIntent = assertNotNull(shadowOf(browser).nextStartedActivity)
        assertTrue(editorIntent.component?.className?.contains("NoteEditor") == true, editorIntent.toString())
    }

    @Test
    fun `reselecting Browse opens search and closing search brings back the bar`() {
        addBasicNote("Ginkgo")
        withDeckPicker(deckCount = 0) { deckPicker ->
            val bar = requireNotNull(deckPicker.binding.bottomNavigation)
            composeRule.onNodeWithTag("tab-browser").performClick()
            deckPicker.supportFragmentManager.executePendingTransactions()
            waitForText("Ginkgo")

            composeRule.onNodeWithTag("tab-browser").performClick()
            composeRule.waitUntil(10_000) { !bar.isVisible }
            composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
            assertFalse(bar.isVisible)

            deckPicker.onBackPressedDispatcher.onBackPressed()
            waitForText("Ginkgo")
            assertTrue(bar.isVisible)
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun `long pressing extends the selection and Select all includes the remaining results`() {
        listOf("Birch", "Ginkgo", "Lotus", "Maple").forEach { addBasicNote(it) }
        getBrowserWithNotes(noteCount = 0)

        composeRule.onNodeWithText("Birch").performTouchInput { longClick() }
        waitForText("1 selected")
        composeRule.onNodeWithText("Lotus").performTouchInput { longClick() }
        waitForText("3 selected")
        composeRule.onNodeWithText("Ginkgo").assertIsOn()
        composeRule.onNodeWithText("Maple").assertIsOff()

        composeRule.onNodeWithContentDescription("Select all").performClick()
        waitForText("4 selected")
        composeRule.onNodeWithText("Maple").assertIsOn()
        composeRule.onNodeWithText("Ginkgo").performClick()
        waitForText("3 selected")
        composeRule.onNodeWithText("Ginkgo").assertIsOff()

        composeRule.onNodeWithContentDescription("Toggle suspend").performClick()
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText("Suspended").fetchSemanticsNodes().size == 3
        }
    }

    @Test
    fun `flagging a selected note flags its sibling cards and a selected card opens preview`() {
        addBasicAndReversedNote("Ginkgo", "Tree")
        val browser = getBrowserWithNotes(noteCount = 0)
        composeRule.onNodeWithText("Notes").performClick()
        waitForText("1 note shown")

        composeRule.onNodeWithText("Ginkgo").performTouchInput { longClick() }
        waitForText("1 selected")
        composeRule.onNodeWithContentDescription("Flag card").performClick()
        val red = TR.actionsFlagRed()
        waitForText(red)
        composeRule.onNodeWithText(red).performClick()
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithContentDescription(red).fetchSemanticsNodes().size == 1
        }

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.onNodeWithText("Cards").performClick()
        waitForText("2 cards shown")
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithContentDescription(red).fetchSemanticsNodes().size == 2
        }

        composeRule.onAllNodesWithText("Ginkgo")[0].performTouchInput { longClick() }
        waitForText("1 selected")
        composeRule.onNodeWithContentDescription("More").performClick()
        composeRule.onNodeWithText("Preview").performClick()
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            shadowOf(browser).peekNextStartedActivity() != null
        }
        assertEquals(CardViewerActivity::class.java.name, shadowOf(browser).nextStartedActivity.component?.className)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xlarge-land")
    fun `the tablet keeps an editor next to the results`() {
        addBasicNote("Ginkgo", "Tree in the first row")
        addBasicNote("Lotus", "Flower in the second row")
        getBrowserWithNotes(noteCount = 0)

        waitForText("Lotus")
        onView(withText("Tree in the first row")).check(matches(isDisplayed()))
        composeRule.onNodeWithText("Lotus").performClick()
        advanceRobolectricLooper()
        onView(withText("Flower in the second row")).check(matches(isDisplayed()))
        composeRule.onNodeWithText("Ginkgo").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "zh-rCN-w360dp-h800dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a long deck name leaves the next filter chip on screen`() {
        val deck = "日语::eggrolls-JLPT10k-v3.5::3-N3::1-高频"
        addDeck(deck)
        getBrowserWithNotes(noteCount = 0)

        composeRule.onNodeWithContentDescription(getResourceString(R.string.browse_search_cards)).performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("\"deck:$deck\"")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        val allDecks = with(targetContext) { TR.sentenceCase.allDecks }
        val deckChip = hasContentDescription(allDecks) and hasText("1-高频", substring = true)
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodes(deckChip).fetchSemanticsNodes().isNotEmpty()
        }

        val screenWidth =
            composeRule
                .onAllNodes(isRoot())
                .onFirst()
                .getUnclippedBoundsInRoot()
                .right
        val cardState = composeRule.onNodeWithContentDescription(getResourceString(R.string.browse_card_state)).assertIsDisplayed()
        val bounds = cardState.getUnclippedBoundsInRoot()
        assertTrue(bounds.right <= screenWidth, "The card state chip ($bounds) runs off the screen ($screenWidth)")
    }

    private fun flaggedNote(
        title: String,
        flag: Flag,
    ) {
        assertEquals(1, col.setUserFlagForCards(listOf(addBasicNote(title).firstCard().id), flag.code).count)
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
