// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.getBrowserWithNotes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BrowserRenderedRowsTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `scrolling the last rendered search remains safe while a new search clears working cards - Issue 17759`() {
        val browser = getBrowserWithNotes(noteCount = 20)
        val original =
            browser.viewModel.renderedResults.value.ids
                .toList()
        assertEquals(20, original.size)

        browser.viewModel.cards.reset()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(15)
        assertEquals(original, browser.viewModel.renderedResults.value.ids)
    }

    @Test
    fun `newly published search results do not change the previously displayed rows - Issue 17759`() =
        runCardBrowserViewModelTest(notes = 2) {
            launchSearchForCards()
            searchJob?.join()
            val displayed = renderedResults.value
            val original = displayed.ids.toList()
            assertEquals(2, original.size)

            cards.reset()
            assertEquals(original, displayed.ids)
            launchSearchForCards("tag:does-not-exist").join()
            searchJob?.join()
            assertEquals(emptyList(), renderedResults.value.ids)
            assertEquals(original, displayed.ids)
        }
}
