// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.common.destinations.StatisticsDestination
import com.ichi2.anki.common.destinations.launchActivity
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.dialogs.DeckSelectionDialog
import com.ichi2.testutils.dispatchInsets
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StatisticsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `statistics heading and deck selector clear the top cutout`() =
        runTest {
            launchActivity<SingleFragmentActivity>(StatisticsDestination).use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { it.dispatchInsets(cutoutTop = 64.dp) }
                val heading = compose.onNodeWithText(TR.statisticsTitle()).assertIsDisplayed().fetchSemanticsNode()
                val deck = compose.onNodeWithText("Default").assertIsDisplayed().fetchSemanticsNode()
                assertTrue(heading.boundsInWindow.top >= 64 * targetContext.resources.displayMetrics.density)
                assertTrue(deck.boundsInWindow.top >= heading.boundsInWindow.bottom)
            }
        }

    @Test
    fun `shows 'Default' deck when collection is empty`() =
        runTest {
            launchActivity<SingleFragmentActivity>(StatisticsDestination).use {
                advanceUntilIdle()
                compose.onNodeWithText("Default").assertIsDisplayed()
            }
        }

    @Test
    fun `changing decks shows new deck name`() =
        runTest {
            val testDeckName1 = "TestDeckName1"
            val testDeckName2 = "TestDeckName2"
            val testDeck1 = addDeck(testDeckName1)
            withCol { decks.select(testDeck1) }
            addDeck(testDeckName2)
            launchActivity<SingleFragmentActivity>(StatisticsDestination).use {
                advanceUntilIdle()
                compose.onNodeWithText(testDeckName1).assertIsDisplayed()
                compose.onNodeWithText(testDeckName1).performClick()
                advanceUntilIdle()
                // select test deck 2
                compose.onNodeWithText(testDeckName2).performClick()
                // check the activity that it has the new deck name
                advanceUntilIdle()
                compose.onNodeWithText(testDeckName2).assertIsDisplayed()
            }
        }

    @Test
    fun `uses expected constraints for decks list selection dialog`() =
        runTest {
            // the statistics screen doesn't allow the selection of 'All Decks' and filtered decks,
            // also 'Default' deck should be enabled no matter its status(empty/not empty)
            launchActivity<SingleFragmentActivity>(StatisticsDestination).use { scenario ->
                advanceUntilIdle()
                compose.onNodeWithText("Default").performClick()
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val statisticsFragment =
                        activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                    assertNotNull(statisticsFragment)
                    val deckSelectionDialog =
                        activity.supportFragmentManager.findFragmentByTag(DeckSelectionDialog.TAG)
                    assertNotNull(deckSelectionDialog)
                    assertFalse(deckSelectionDialog.requireArguments().getBoolean(DeckSelectionDialog.ARG_ALLOW_ALL, true))
                    assertFalse(deckSelectionDialog.requireArguments().getBoolean(DeckSelectionDialog.ARG_ALLOW_FILTERED, true))
                    assertTrue(deckSelectionDialog.requireArguments().getBoolean(DeckSelectionDialog.ARG_SKIP_EMPTY_DEFAULT, false))
                }
            }
        }
}
