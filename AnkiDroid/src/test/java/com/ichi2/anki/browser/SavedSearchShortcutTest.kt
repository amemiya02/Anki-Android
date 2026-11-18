// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.view.KeyEvent.KEYCODE_S
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.search.SavedSearch
import com.ichi2.anki.browser.search.SearchRequest
import com.ichi2.anki.common.utils.ext.savedFilters
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.testutils.pressKey
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SavedSearchShortcutTest : RobolectricTest() {
    @Test
    fun `selecting a saved search opened by shortcut submits it`() {
        val cat = addBasicNote("cat", "meows").firstCard()
        addBasicNote("dog", "barks")
        col.config.savedFilters = listOf(SavedSearch("Cats", "cat"))

        withCardBrowserFragment {
            activityViewModel.setSelectedDeck(SelectableDeck.AllDecks)
            awaitSearch()

            onView(isRoot()).pressKey(KEYCODE_S, ctrl = true, alt = true)
            onView(withText("Cats")).inRoot(isDialog()).perform(click())
            awaitSearch()

            assertEquals(SearchRequest("cat"), activityViewModel.searchRequestFlow.value)
            assertEquals(listOf(cat.id), activityViewModel.queryAllCardIds())
        }
    }

    private suspend fun CardBrowserFragment.awaitSearch() {
        activityViewModel.searchJob?.join()
        advanceRobolectricLooper()
    }
}
