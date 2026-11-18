// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SearchScreenRecreationAfterSubmitTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `search deck picker survives background without an earlier search`() = probe(submitFirst = false, recreate = false)

    @Test
    fun `search deck picker survives recreation after an earlier search`() = probe(submitFirst = true, recreate = true)

    @Test
    fun `search deck picker survives background after an earlier search`() = probe(submitFirst = true, recreate = false)

    private fun probe(
        submitFirst: Boolean,
        recreate: Boolean,
    ) {
        addBasicNote("Outside")
        val chosen = addDeck("Chosen")
        col.setDeck(listOf(addBasicNote("Inside").firstCard().id), chosen)
        ensureCollectionLoadIsSynchronous()
        ActivityScenario.launch(CardBrowser::class.java).use { scenario ->
            composeRule.waitUntilAtLeastOneExists(hasText("Outside"), 10_000)
            if (submitFirst) {
                openSearch()
                composeRule.onNode(hasSetTextAction()).performTextReplacement("deck:Chosen")
                composeRule.onNode(hasSetTextAction()).performImeAction()
                composeRule.waitUntilAtLeastOneExists(hasText("Inside"), 10_000)
            }
            openSearch()
            composeRule.onNodeWithContentDescription("All decks").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("Chosen"), 10_000)

            if (recreate) {
                scenario.recreate()
            } else {
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
            }
            composeRule.onNodeWithText("Done").assertIsDisplayed()
            composeRule.onNode(hasText("Chosen") and isToggleable()).assertIsDisplayed()
        }
    }

    private fun openSearch() {
        composeRule.onNodeWithContentDescription("Search cards").performClick()
        composeRule.waitUntilAtLeastOneExists(hasSetTextAction(), 10_000)
    }
}
