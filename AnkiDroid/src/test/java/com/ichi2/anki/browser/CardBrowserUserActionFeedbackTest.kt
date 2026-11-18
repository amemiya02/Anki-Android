// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.app.Activity
import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.Flag
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.toBundle
import com.ichi2.anki.common.ui.TransitionDirection.DEFAULT
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.noteeditor.openNoteEditorWithArgs
import com.ichi2.anki.observability.undoableOp
import com.ichi2.testutils.ext.snackbarAction
import com.ichi2.testutils.ext.snackbarText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** User actions which refresh the browser count (Issue 22384). */
@RunWith(AndroidJUnit4::class)
class CardBrowserUserActionFeedbackTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `show marked menu shows the refreshed count`() {
        addBasicNote("cat", "meows").apply {
            addTag("marked")
            flush()
        }
        addBasicNote("dog", "barks")
        withBrowser {
            activityViewModel.searchForMarkedNotes().join()
            awaitSearch()
            assertCount(1)
        }
    }

    @Test
    fun `show suspended menu shows the refreshed count`() {
        val note = addBasicNote("cat", "meows")
        col.sched.suspendCards(listOf(note.firstCard().id))
        addBasicNote("dog", "barks")
        withBrowser {
            activityViewModel.searchForSuspendedCards().join()
            awaitSearch()
            assertCount(1)
        }
    }

    @Test
    fun `tag filter shows the refreshed count`() {
        addBasicNote("cat", "meows").apply {
            addTag("audit")
            flush()
        }
        addBasicNote("dog", "barks")
        withBrowser {
            filterByTag("audit")
            awaitSearch()
            assertCount(1)
        }
    }

    @Test
    fun `legacy flag filter shows the refreshed count`() {
        addBasicNote("cat", "meows")
        withBrowser {
            activityViewModel.setFlagFilter(Flag.RED)
            awaitSearch()
            assertCount(0)
        }
    }

    @Test
    fun `switching to notes shows the refreshed count`() {
        addBasicAndReversedNote("cat", "meows")
        withBrowser {
            assertEquals(2, activityViewModel.rowCount)
            activityViewModel.setCardsOrNotes(CardsOrNotes.NOTES).join()
            awaitSearch()
            assertEquals(1, activityViewModel.rowCount)
            composeRule.onNodeWithText("1 note shown").assertIsDisplayed()
            assertEquals(CardsOrNotes.NOTES, activityViewModel.flowOfLastCompletedSearch.value?.cardsOrNotes)
        }
    }

    @Test
    fun `saving a note editor change shows the refreshed count`() {
        val note = addBasicNote("cat", "meows")
        withBrowser {
            activityViewModel.setQuery("cat").join()
            awaitSearch()
            assertNull(snackbarText)
            val args = NoteEditorDestination.EditSelection(listOf(note.firstCard().id), DEFAULT).toBundle()
            val editor = openNoteEditorWithArgs(args)
            editor.setFieldValueFromUi(0, "dog")
            editor.saveNote()
            awaitSearch()
            assertEquals("dog", withCol { getNote(note.id).fields[0] })
            assertCount(0)
        }
    }

    @Test
    fun `leaving the note editor without changes stays quiet`() {
        val note = addBasicNote("cat", "meows")
        withBrowser {
            val args = NoteEditorDestination.EditSelection(listOf(note.firstCard().id), DEFAULT).toBundle()
            val editor = openNoteEditorWithArgs(args)
            editor.saveNote()
            awaitSearch()
            assertEquals(1, activityViewModel.rowCount)
            assertNull(snackbarText)
        }
    }

    @Test
    fun `returning after adding a note shows the refreshed count`() {
        addBasicNote("cat", "meows")
        withBrowser {
            val browser = requireActivity()
            assertTrue(onKeyUp(KeyEvent.KEYCODE_E, KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_E, 0, KeyEvent.META_CTRL_ON)))
            advanceRobolectricLooper()
            val started = assertNotNull(shadowOf(browser).nextStartedActivityForResult)
            addBasicNote("dog", "barks")
            shadowOf(browser).receiveResult(started.intent, Activity.RESULT_OK, Intent())
            awaitSearch()
            assertCount(2)
        }
    }

    @Test
    fun `mark and flag changes remain silent without rerunning search`() {
        val note = addBasicNote("cat", "meows")
        withBrowser {
            val completed = activityViewModel.flowOfLastCompletedSearch.value
            activityViewModel.selectRowAtPosition(0)
            activityViewModel.toggleMark()
            activityViewModel.updateSelectedCardsFlag(Flag.RED)
            advanceRobolectricLooper()
            assertEquals(completed, activityViewModel.flowOfLastCompletedSearch.value)
            assertNull(snackbarText)
            assertEquals(true, withCol { getNote(note.id).hasTag(this, "marked") })
        }
    }

    @Test
    fun `unrelated collection update stays quiet`() {
        val note = addBasicNote("cat", "meows")
        withBrowser {
            activityViewModel.setQuery("cat").join()
            awaitSearch()
            undoableOp {
                val changed = getNote(note.id)
                changed.setField(0, "dog")
                updateNote(changed)
            }
            awaitSearch()
            assertEquals(0, activityViewModel.rowCount)
            assertNull(snackbarText)
        }
    }

    @Test
    fun `note editor feedback survives returning to the browser and is shown only once`() =
        runTest {
            val note = addBasicNote("cat", "meows")
            run {
                ActivityScenario.launch<CardBrowser>(Intent(targetContext, CardBrowser::class.java)).use { scenario ->
                    lateinit var browser: CardBrowser
                    scenario.onActivity { browser = it }
                    browser.cardBrowserFragment.awaitSearch()
                    browser.viewModel.setSelectedDeck(SelectableDeck.AllDecks)
                    browser.cardBrowserFragment.awaitSearch()
                    assertNull(browser.snackbarText)

                    scenario.moveToState(Lifecycle.State.CREATED)
                    val args = NoteEditorDestination.EditSelection(listOf(note.firstCard().id), DEFAULT).toBundle()
                    val editor = openNoteEditorWithArgs(args)
                    editor.setFieldValueFromUi(0, "dog")
                    editor.saveNote()
                    browser.cardBrowserFragment.awaitSearch()
                    assertEquals("dog", withCol { getNote(note.id).fields[0] })
                    assertEquals(1, browser.viewModel.rowCount)
                    assertNull(browser.snackbarText)

                    scenario.moveToState(Lifecycle.State.RESUMED)
                    shadowOf(Looper.getMainLooper()).idle()
                    composeRule.onNodeWithText("dog").assertIsDisplayed()
                    composeRule.onNodeWithText("1 card shown").assertIsDisplayed()

                    // Let the snackbar expire, then return again without another edit.
                    repeat(3) { advanceRobolectricLooper() }
                    scenario.moveToState(Lifecycle.State.CREATED)
                    scenario.moveToState(Lifecycle.State.RESUMED)
                    shadowOf(Looper.getMainLooper()).idle()
                    assertNull(browser.snackbarText)
                }
            }
        }

    @Test
    fun `search all decks action shows the new count`() {
        addBasicNote("cat", "meows")
        val emptyDeck = addDeck("Empty")
        withBrowser {
            activityViewModel.setSelectedDeck(emptyDeck)
            awaitSearch()
            activityViewModel.setQuery("cat", feedbackMode = BrowserSearchFeedbackMode.SEARCH_RESULTS).join()
            awaitSearch()
            assertEquals(0, activityViewModel.rowCount)
            val action = assertNotNull(snackbarAction)
            assertEquals("Search all decks", action.text.toString())
            action.performClick()
            awaitSearch()
            assertEquals(true, activityViewModel.hasSelectedAllDecks())
            assertEquals("cat", activityViewModel.searchTerms)
            assertCount(1)
        }
    }

    @Test
    fun `explicit deep link query shows search feedback`() =
        runTest {
            addBasicNote("cat", "meows")
            addBasicNote("dog", "barks")
            run {
                val intent =
                    Intent(targetContext, CardBrowser::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = "anki://x-callback-url/browser?search=cat".toUri()
                    }
                ActivityScenario.launch<CardBrowser>(intent).use { scenario ->
                    lateinit var browser: CardBrowser
                    scenario.onActivity { browser = it }
                    browser.viewModel.searchJob?.join()
                    shadowOf(Looper.getMainLooper()).idle()
                    assertEquals("cat", browser.viewModel.searchTerms)
                    assertEquals(1, browser.viewModel.rowCount)
                    composeRule.onNodeWithText("1 card shown").assertIsDisplayed()
                }
            }
        }

    private fun withBrowser(block: suspend CardBrowserFragment.() -> Unit) =
        withCardBrowserFragment {
            awaitSearch()
            activityViewModel.setSelectedDeck(SelectableDeck.AllDecks)
            awaitSearch()
            assertNull(snackbarText)
            block()
        }

    private suspend fun CardBrowserFragment.awaitSearch() {
        activityViewModel.searchJob?.join()
        advanceRobolectricLooper()
    }

    private fun CardBrowserFragment.assertCount(count: Int) {
        assertEquals(count, activityViewModel.rowCount)
        composeRule.onNodeWithText("$count ${if (count == 1) "card" else "cards"} shown").assertIsDisplayed()
    }
}
