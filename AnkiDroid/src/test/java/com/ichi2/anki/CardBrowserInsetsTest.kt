// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.dispatchInsets
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import androidx.compose.ui.unit.dp as composeDp

/** Public content remains visible inside the browser's window insets. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CardBrowserInsetsTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `search clears the side navigation bar and camera cutout`() =
        withCardBrowser(noteCount = 1) { browser ->
            val search = composeRule.onNodeWithContentDescription("Search cards")
            val before = search.getUnclippedBoundsInRoot()

            browser.dispatchInsets(navBarRight = 48.dp, cutoutLeft = 32.dp)
            advanceRobolectricLooper()

            val after = search.getUnclippedBoundsInRoot()
            assertTrue(after.left >= before.left + 32.composeDp)
            assertTrue(after.right <= before.right - 48.composeDp)
        }

    @Test
    fun `the last result remains above system navigation`() {
        repeat(30) { addBasicNote("Card ${it.toString().padStart(2, '0')}") }
        withCardBrowser(noteCount = 0) { browser ->
            val window = composeRule.onNode(isRoot()).getUnclippedBoundsInRoot()
            browser.dispatchInsets(navBarBottom = 48.dp, bottomCornerRadius = 34.dp)
            advanceRobolectricLooper()
            val results = composeRule.onNode(hasScrollToIndexAction())
            results.performScrollToIndex(30)
            // The jump can stop short of the end, leaving the last result uncomposed below the
            // viewport: scroll on until it is laid out, then check where it sits
            results.performScrollToNode(hasText("Card 29"))

            val last =
                composeRule
                    .onNodeWithText("Card 29")
                    .performScrollTo()
                    .assertIsDisplayed()
                    .getUnclippedBoundsInRoot()
            assertTrue(last.bottom <= window.bottom - 48.composeDp, "Last result bounds are $last. Window bounds are $window.")
        }
    }
}
