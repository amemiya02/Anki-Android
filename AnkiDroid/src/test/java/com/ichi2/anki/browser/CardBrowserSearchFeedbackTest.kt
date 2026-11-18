// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.testutils.ext.snackbarAction
import com.ichi2.testutils.ext.snackbarText
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

/**
 * Integration coverage for result counts and operation feedback in Browse.
 */
@RunWith(AndroidJUnit4::class)
class CardBrowserSearchFeedbackTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `opening the browser does not show a snackbar - Issue 21242`() {
        addBasicNote("cat", "meows")
        withBrowser {
            assertThat(activityViewModel.rowCount, equalTo(1))
            assertThat(snackbarText, nullValue())
        }
    }

    @Test
    fun `count-only feedback follows header visibility`() {
        addBasicNote("cat", "meows")
        withBrowser {
            activityViewModel.setSelectedDeck(SelectableDeck.AllDecks)
            awaitSearch()
            composeRule.onNodeWithText("1 card shown").assertIsDisplayed()

            activityViewModel.setQuery("", feedbackMode = BrowserSearchFeedbackMode.SEARCH_RESULTS).join()
            awaitSearch()

            assertThat(snackbarText, nullValue())
        }
    }

    @Test
    fun `submitted search shows matching card count without an all-decks action`() {
        addBasicAndReversedNote("cat", "meows")
        addBasicNote("dog", "barks")
        withBrowser {
            activityViewModel.setSelectedDeck(SelectableDeck.AllDecks)
            awaitSearch()
            submitSearch("cat")
            assertThat(activityViewModel.rowCount, equalTo(2))
            assertThat(activityViewModel.selectedRowCount(), equalTo(0))
            composeRule.onNodeWithText("2 cards shown").assertIsDisplayed()
            assertThat(snackbarText, nullValue())
        }
    }

    @Test
    fun `search all decks keeps the query and finds matches outside the selected deck`() {
        addBasicNote("cat", "meows")
        addBasicNote("dog", "barks")
        val emptyDeck = addDeck("Empty")
        withBrowser {
            activityViewModel.setSelectedDeck(emptyDeck)
            awaitSearch()
            submitSearch("cat")
            assertThat(activityViewModel.rowCount, equalTo(0))
            assertThat(snackbarText, equalTo("No cards found in deck ‘Empty’"))
            val action = assertNotNull(snackbarAction)
            assertThat(action.visibility, equalTo(View.VISIBLE))
            assertThat(action.text.toString(), equalTo("Search all decks"))

            action.performClick()
            awaitSearch()
            assertThat(activityViewModel.hasSelectedAllDecks(), equalTo(true))
            assertThat(activityViewModel.searchTerms, equalTo("cat"))
            assertThat(activityViewModel.rowCount, equalTo(1))
        }
    }

    @Test
    fun `suspending a card updates the hidden search count - Issue 22384`() {
        addBasicNote("cat", "meows")
        addBasicNote("dog", "barks")
        withBrowser {
            submitSearch("-is:suspended")
            assertThat(activityViewModel.rowCount, equalTo(2))
            for ((count, message) in listOf(1 to "1 card shown", 0 to "0 cards shown")) {
                activityViewModel.selectRowAtPosition(0)

                activityViewModel.toggleSuspendCards().join()
                awaitSearch()

                assertThat(activityViewModel.rowCount, equalTo(count))
                composeRule.onNodeWithText(message).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `tag edits update the hidden search count - Issue 22384`() {
        addBasicNote("cat", "meows")
        addBasicNote("dog", "barks")
        withBrowser {
            submitSearch("-tag:edited")
            assertThat(activityViewModel.rowCount, equalTo(2))
            activityViewModel.selectRowAtPosition(0)

            activityViewModel.editSelectedCardsTags(listOf("edited"), emptyList())
            awaitSearch()

            assertThat(activityViewModel.rowCount, equalTo(1))
            composeRule.onNodeWithText("1 card shown").assertIsDisplayed()
        }
    }

    @Test
    fun `unfiltered suspend reports the count only when the header is hidden`() {
        addBasicNote("cat", "meows")
        addBasicNote("dog", "barks")
        withBrowser {
            activityViewModel.selectRowAtPosition(0)

            activityViewModel.toggleSuspendCards().join()
            awaitSearch()

            assertThat("the count need not change", activityViewModel.rowCount, equalTo(2))
            composeRule.onNodeWithText("2 cards shown").assertIsDisplayed()
            assertThat(snackbarText, nullValue())
        }
    }

    @Test
    fun `bury retains its undo feedback after refreshing the results`() {
        addBasicNote("cat", "meows")
        withBrowser {
            submitSearch("cat")
            activityViewModel.selectRowAtPosition(0)

            toggleBury().join()
            awaitSearch()

            assertThat(snackbarAction?.text?.toString(), equalTo("Undo"))
            assertThat(snackbarAction?.visibility, equalTo(View.VISIBLE))

            onUndo().join()
            awaitSearch()
            assertThat(snackbarText, containsString("undone"))

            // A previous operation's message must not suppress feedback for the next edit.
            activityViewModel.selectRowAtPosition(0)
            activityViewModel.toggleSuspendCards().join()
            awaitSearch()
            composeRule.onNodeWithText("1 card shown").assertIsDisplayed()
        }
    }

    @Test
    fun `repositioning keeps its operation feedback after refreshing the results`() {
        addBasicNote("cat", "meows")
        withBrowser {
            submitSearch("cat")
            activityViewModel.selectRowAtPosition(0)

            repositionCardsNoValidation(position = 10, step = 1, shuffle = false, shift = false).join()
            awaitSearch()

            assertThat(snackbarText, equalTo(TR.browsingChangedNewPosition(1)))
        }
    }

    private fun withBrowser(block: suspend CardBrowserFragment.() -> Unit) =
        withCardBrowserFragment {
            awaitSearch()
            activityViewModel.setSelectedDeck(SelectableDeck.AllDecks)
            awaitSearch()
            block()
        }

    private suspend fun CardBrowserFragment.submitSearch(query: String) {
        searchViewModel.submitCurrentSearch(activityViewModel.searchRequestFlow.value.copy(query = query)).join()
        awaitSearch()
    }

    private suspend fun CardBrowserFragment.awaitSearch() {
        activityViewModel.searchJob?.join()
        advanceRobolectricLooper()
    }
}
