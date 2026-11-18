// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.Flag
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.getBrowserWithNotes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
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
        ensureCollectionLoadIsSynchronous()
        val deckPicker = startRegularActivity<DeckPicker>()
        val bar = requireNotNull(deckPicker.binding.bottomNavigation)

        bar.selectedItemId = NavigationItem.BROWSER.id
        waitForText("Ginkgo")
        bar.selectedItemId = NavigationItem.BROWSER.id
        composeRule.waitUntil(10_000) { !bar.isVisible }
        composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        assertFalse(bar.isVisible)

        deckPicker.onBackPressedDispatcher.onBackPressed()
        waitForText("Ginkgo")
        assertTrue(bar.isVisible)
    }

    private fun flaggedNote(
        title: String,
        flag: Flag,
    ) {
        col.setUserFlagForCards(listOf(addBasicNote(title).firstCard().id), flag.code)
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
