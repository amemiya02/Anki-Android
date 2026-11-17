// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki.pages

import anki.scheduler.CardAnswer.Rating
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.anki.study.FinishedReason
import com.ichi2.anki.study.FinishedState
import com.ichi2.testutils.JvmTest
import org.json.JSONArray
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/** What the finished screen says about the selected Deck, read from the Engine. */
class CongratsViewModelFinishedStateTest : JvmTest() {
    private val viewModel = CongratsViewModel()

    private suspend fun finishedState(): FinishedState {
        viewModel.loadFinishedState()
        return assertNotNull(viewModel.finishedState.value)
    }

    @Test
    fun `a daily limit is why the session ended`() =
        runTest {
            updateDeckConfig(DEFAULT_DECK_ID) { new.perDay = 0 }
            addBasicNote()

            val state = finishedState()

            assertEquals(FinishedReason.DailyLimit, state.reason)
            assertTrue(state.newLimitReached)
            assertFalse(state.reviewLimitReached)
        }

    @Test
    fun `learning Cards later today say when the next one is ready`() =
        runTest {
            col.config.set("collapseTime", 0)
            val config = col.decks.configDictForDeckId(DEFAULT_DECK_ID)
            config.new.delays = JSONArray(listOf(10))
            col.decks.save(config)
            addBasicNote()
            col.sched.answerCard(assertNotNull(col.sched.card), Rating.AGAIN)

            val state = finishedState()

            assertEquals(FinishedReason.LearningLater, state.reason)
            val later = assertNotNull(state.learningLater)
            assertEquals(1, later.remaining)
            assertTrue(later.nextIn in 9.minutes..10.minutes, "next in ${later.nextIn}")
        }

    @Test
    fun `buried Cards are offered for unburying`() =
        runTest {
            val note = addBasicNote()
            col.sched.buryCards(listOf(note.firstCard().id))

            val state = finishedState()

            assertEquals(FinishedReason.Buried, state.reason)
            assertTrue(state.hasBuried)
        }

    @Test
    fun `nothing left today says when the next day starts`() =
        runTest {
            addBasicNote()
            col.sched.answerCard(assertNotNull(col.sched.card), Rating.EASY)

            val state = finishedState()

            assertEquals(FinishedReason.Done, state.reason)
            assertNull(state.learningLater)
            assertFalse(state.hasBuried)
            assertEquals(col.sched.dayCutoff * 1000, state.nextDayStartsAtMillis)
            assertEquals(col.sched.studiedToday().replace("\n", " "), state.studiedToday)
            assertTrue(state.studiedToday.isNotBlank())
        }

    @Test
    fun `the Deck path and kind come from the selected Deck`() =
        runTest {
            addBasicNote()
            val filtered = addDynamicDeck("Parent::Filtered", "")
            withCol { sched.emptyFilteredDeck(filtered) }

            val state = finishedState()

            assertEquals(listOf("Parent", "Filtered"), state.deckPath)
            assertTrue(state.isFiltered)
        }

    @Test
    fun `a regular Deck is not filtered`() =
        runTest {
            val state = finishedState()

            assertEquals(listOf("Default"), state.deckPath)
            assertFalse(state.isFiltered)
        }
}
