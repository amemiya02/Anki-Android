// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.SearchHistory
import com.ichi2.anki.browser.SearchHistory.SearchHistoryEntry
import com.ichi2.anki.getBrowserWithNotes
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StandardSearchFragmentTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    override fun setUp() {
        super.setUp()
        SearchHistory().clear()
    }

    @Test
    fun `recent searches are shown`() {
        seedHistory(3)
        openSearch()

        composeRule.onNodeWithText("query-2").assertIsDisplayed()
        composeRule.onNodeWithText("See more").assertDoesNotExist()
    }

    @Test
    fun `See more reveals searches older than the five most recent`() {
        seedHistory(6)
        openSearch()

        composeRule.onNodeWithText("query-0").assertDoesNotExist()
        composeRule.onNodeWithText("See more").performClick()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("query-0"))
        composeRule.onNodeWithText("query-0").assertIsDisplayed()
    }

    private fun seedHistory(count: Int) {
        val history = SearchHistory()
        repeat(count) { history.addRecent(SearchHistoryEntry("query-$it")) }
    }

    private fun openSearch() {
        getBrowserWithNotes(noteCount = 0)
        composeRule.onNodeWithContentDescription("Search cards").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Recent"), 10_000)
    }
}
