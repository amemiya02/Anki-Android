// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.deckpicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.study.FinishedFragment
import com.ichi2.anki.study.ReviewSessionSummary
import com.ichi2.anki.ui.windows.reviewer.ReviewerFragment
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.BackupManagerTestUtilities
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class StudyAllTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @After
    override fun tearDown() {
        super.tearDown()
        BackupManagerTestUtilities.reset()
    }

    @Test
    fun `Study All revisits a completed Deck after other eligible Decks`() {
        val geography = addDeck("Geography")
        val history = addDeck("History")
        addCardTo(geography, "Capital")
        addCardTo(history, "Timeline")

        withDeckPicker(deckCount = 0) { host ->
            tap("Study All")
            val reviewer = awaitReviewer(host, geography)
            answerLastCard()
            finish(reviewer)
            awaitFinished(host)

            addCardTo(geography, "Another capital")
            tap("Done")
            val nextReviewer = awaitReviewer(host, history)
            answerLastCard()
            finish(nextReviewer)
            awaitFinished(host)
            tap("Done")

            awaitReviewer(host, geography).finishScreen()
            // Leaving a running review returns to Decks even when more Cards are available.
            awaitText("Study All")
            compose.onNodeWithText("Study All").assertIsDisplayed()
        }
    }

    @Test
    fun `a completed Study All session still shows its figures when the Deck refills before Finished opens`() {
        val geography = addDeck("Geography")
        addCardTo(geography, "Capital")

        withDeckPicker(deckCount = 0) { host ->
            tap("Study All")
            val reviewer = awaitReviewer(host, geography)
            answerLastCard()

            // Make more Cards eligible before the reviewer result reaches the Deck Picker.
            addCardTo(geography, "Another capital")
            finish(reviewer)

            awaitFinished(host)
            compose.onNodeWithText("Easy 1").assertIsDisplayed()
            tap("Done")
            awaitReviewer(host, geography)
        }
    }

    private fun addCardTo(deck: DeckId, front: String) {
        col.setDeck(listOf(addBasicNote(front).firstCard().id), deck)
    }

    private fun answerLastCard() {
        col.sched.answerCard(assertNotNull(col.sched.card), Rating.EASY)
        assertEquals(0, col.sched.totalCount())
    }

    private fun finish(reviewer: ReviewerFragment) {
        val summary = ReviewSessionSummary(again = 0, hard = 0, good = 0, easy = 1, elapsedMillis = 100)
        reviewer.finishScreen(ReviewerFragment.RESULT_NO_MORE_CARDS, summary.toIntent())
    }

    private fun awaitReviewer(host: DeckPicker, deck: DeckId): ReviewerFragment {
        advanceRobolectricLooperUntil { host.currentScreenFragment is ReviewerFragment }
        assertEquals(deck, col.decks.selected())
        return host.currentScreenFragment as ReviewerFragment
    }

    private fun awaitFinished(host: DeckPicker) {
        advanceRobolectricLooperUntil { host.currentScreenFragment is FinishedFragment }
        awaitText("Done")
    }

    private fun awaitText(text: String) {
        advanceRobolectricLooperUntil(lazyMessage = { "Expected '$text' to be displayed" }) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        awaitText(text)
        compose.onNodeWithText(text).performClick()
    }
}
