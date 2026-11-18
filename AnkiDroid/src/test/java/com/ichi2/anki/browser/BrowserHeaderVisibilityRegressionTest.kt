// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.view.KeyEvent.KEYCODE_S
import android.view.View
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.search.SavedSearch
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.testutils.ext.snackbarAction
import com.ichi2.testutils.ext.snackbarText
import com.ichi2.testutils.pressKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BrowserHeaderVisibilityRegressionTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `saved search opened by shortcut reports its count during multi-select`() {
        addBasicNote("cat", "meows")
        addBasicNote("dog", "barks")
        withCardBrowserFragment {
            activityViewModel.setSelectedDeck(SelectableDeck.AllDecks)
            awaitSearch()
            assertEquals(SaveSearchResult.SUCCESS, activityViewModel.saveSearch(SavedSearch("Cats", "cat")))
            composeRule.onNodeWithText("cat").performTouchInput { longClick() }
            composeRule.waitForIdle()
            assertTrue(activityViewModel.isInMultiSelectMode)

            onView(isRoot()).pressKey(KEYCODE_S, ctrl = true, alt = true)
            advanceRobolectricLooper()
            onView(withText("Cats")).inRoot(isDialog()).perform(click())
            composeRule.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                snackbarText == "1 card shown"
            }

            assertEquals("cat", activityViewModel.searchTerms)
            assertEquals(1, activityViewModel.rowCount)
            assertTrue(activityViewModel.isInMultiSelectMode)
            assertEquals(View.GONE, snackbarAction?.visibility)
        }
    }

    private suspend fun CardBrowserFragment.awaitSearch() {
        activityViewModel.searchJob?.join()
        advanceRobolectricLooper()
    }
}
