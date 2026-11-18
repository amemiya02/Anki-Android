// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.getBrowserWithNotes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class BrowserColumnsScreenTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `each mode remembers which column starts the result row`() {
        ensureCollectionLoadIsSynchronous()
        addBasicNote("Ginkgo", "Fan-shaped leaf")
        ActivityScenario.launch(CardBrowser::class.java).use {
            waitForText("Ginkgo")
            openColumns()
            composeRule.onNodeWithContentDescription("Hide Due").performClick()
            composeRule.onNodeWithContentDescription("Reorder Card Type").performClick()
            composeRule.onNodeWithText("Move up").performClick()

            composeRule.onNode(inColumns and hasText("Notes")).performClick()
            composeRule.onNodeWithContentDescription("Reorder Note Type").performClick()
            composeRule.onNodeWithText("Move up").performClick()
            composeRule.onNodeWithContentDescription("Back to Browse").performClick()
            waitForText("Card 1")
        }

        ActivityScenario.launch(CardBrowser::class.java).use {
            waitForText("Card 1")
            composeRule.onNodeWithText("Notes").performClick()
            waitForText("Basic")

            openColumns()
            composeRule.onNode(inColumns and hasText("Cards")).performClick()
            val columns = composeRule.onNode(inColumns and hasScrollToIndexAction())
            columns.performScrollToNode(hasContentDescription("Show Due"))
            composeRule.onNodeWithContentDescription("Show Due").performClick()
            columns.performScrollToNode(hasContentDescription("Hide Due"))
            composeRule.onNodeWithContentDescription("Hide Due").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Back to Browse").performClick()
            waitForText("Basic")
            composeRule.onNodeWithText("Cards").performClick()
            waitForText("Card 1")
        }
    }

    @Test
    fun `dragging a shown column updates the title and survives scrolling away`() {
        addBasicNote("Ginkgo", "Fan-shaped leaf")
        getBrowserWithNotes(noteCount = 0)
        openColumns()
        val handle = composeRule.onNodeWithContentDescription("Reorder Card Type")
        val firstHandle = composeRule.onNodeWithContentDescription("Reorder Sort Field")
        val delta =
            firstHandle
                .fetchSemanticsNode()
                .boundsInRoot.center.y -
                handle
                    .fetchSemanticsNode()
                    .boundsInRoot.center.y
        handle.performTouchInput { swipe(center, center + Offset(0f, delta), durationMillis = 400) }

        val list = composeRule.onNode(inColumns and hasScrollToIndexAction())
        list.performScrollToNode(hasText("Position"))
        composeRule.onNodeWithText("Position").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back to Browse").performClick()
        waitForText("Card 1")
    }

    private val inColumns = hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Columns"))

    private fun openColumns() {
        composeRule.onNodeWithContentDescription("More actions").performClick()
        composeRule.onNodeWithText("Columns").performClick()
        waitForText("Shown")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
