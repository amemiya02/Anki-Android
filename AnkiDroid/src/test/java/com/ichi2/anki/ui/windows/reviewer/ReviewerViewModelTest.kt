// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.scheduler.CardAnswer.Rating
import app.cash.turbine.test
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.utils.ext.cardStateCustomizer
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.anki.utils.CollectionPreferences
import com.ichi2.themes.Themes
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.json.JSONTokener
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ReviewerViewModelTest : RobolectricTest() {
    private val viewModelStore = ViewModelStore()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Before
    fun disableAutoplay() {
        // stored in collection; no need for a reset
        updateDeckConfig(col.decks.selected()) { autoplay = false }
    }

    @After
    fun clearViewModels() {
        viewModelStore.clear()
    }

    @Test
    fun `HTML answer marks nosuggest fields and requests a Done key`() =
        runTest {
            val field = htmlAnswerField("nosuggest:type")

            assertEquals("true", field.attr("data-ankidroid-nosuggest"))
            assertEquals("done", field.attr("enterkeyhint"))
        }

    @Test
    fun `ordinary HTML answers keep their keyboard defaults`() =
        runTest {
            val field = htmlAnswerField("type")

            assertFalse(field.hasAttr("data-ankidroid-nosuggest"))
            assertFalse(field.hasAttr("enterkeyhint"))
        }

    private suspend fun TestScope.htmlAnswerField(filter: String): Element {
        targetContext.sharedPrefs().edit { putBoolean("useInputTag", true) }
        val card = addBasicWithTypingNote("front", "back").firstCard()
        val noteType = card.noteType(col)
        noteType.templates[0].qfmt = "{{Front}}{{$filter:Back}}"
        col.notetypes.save(noteType)
        val viewModel = ReviewerViewModel(SavedStateHandle()).also { viewModelStore.put("reviewer", it) }
        lateinit var html: String
        viewModel.eval
            .filter { it.startsWith("_showQuestion(") }
            .test {
                viewModel.onPageFinished(false)
                advanceUntilIdle()
                // The first argument is a JSON string containing the question HTML.
                html = assertIs<String>(JSONTokener(awaitItem().removePrefix("_showQuestion(")).nextValue())
                cancelAndIgnoreRemainingEvents()
            }
        return assertNotNull(Jsoup.parseBodyFragment(html).selectFirst("input#typeans"))
    }

    @Test
    fun `the study bar names the Deck of the Card as a path`() =
        runTest {
            val did = addDeck("中文::HSK 4", setAsSelected = true)
            val card = addBasicNote().firstCard()
            col.setDeck(listOf(card.id), did)

            val studyBar = startStudying().studyBarFlow.value

            assertEquals(listOf("中文", "HSK 4"), studyBar.deckPath)
        }

    @Test
    fun `the study bar shows the remaining counts and the time-remaining estimate`() =
        runTest {
            repeat(6) { addBasicNote("front $it") }

            val studyBar = startStudying().studyBarFlow.value

            assertEquals("6", studyBar.counts?.new)
            // With no review history the estimate assumes 20 seconds a rep: 6 new cards are 2 minutes
            assertEquals(2, studyBar.minutesLeft)
        }

    @Test
    fun `the study bar hides what is left when the collection hides the remaining count`() =
        runTest {
            addBasicNote()
            CollectionPreferences.setShowRemainingDueCounts(false)

            val studyBar = startStudying().studyBarFlow.value

            assertNull(studyBar.counts)
            assertNull(studyBar.minutesLeft)
            assertNull(studyBar.progress)
        }

    @Test
    fun `the progress line fills as Cards leave the session`() =
        runTest {
            addBasicNote("one")
            addBasicNote("two")
            val viewModel = startStudying()
            assertEquals(0f, viewModel.studyBarFlow.value.progress)

            viewModel.onShowAnswer()
            advanceUntilIdle()
            viewModel.answerCard(Rating.EASY)
            advanceUntilIdle()

            assertEquals(0.5f, viewModel.studyBarFlow.value.progress)
        }

    @Test
    fun `Card Templates can style for the Dark theme with ankidroid_dark_mode`() =
        runTest {
            addBasicNote()
            Themes.currentTheme = NightTheme.DARK
            try {
                assertContains(questionBodyClasses(), "ankidroid_dark_mode")
                Themes.currentTheme = NightTheme.BLACK
                assertFalse("ankidroid_dark_mode" in questionBodyClasses())
            } finally {
                Themes.currentTheme = DayTheme.LIGHT
            }
        }

    @Test
    fun `short Cards are centred vertically when the setting is on`() =
        runTest {
            addBasicNote()
            assertFalse("vertically_centered" in questionBodyClasses())

            targetContext.sharedPrefs().edit { putBoolean("centerVertically", true) }

            assertContains(questionBodyClasses(), "vertically_centered")
        }

    /** The classes the study screen gives the Card's `<body>` when it shows the question */
    private suspend fun TestScope.questionBodyClasses(): List<String> {
        val viewModel = ReviewerViewModel(SavedStateHandle()).also { viewModelStore.put("reviewer${viewModelStore.keys().size}", it) }
        lateinit var classes: String
        viewModel.eval
            .filter { it.startsWith("_showQuestion(") }
            .test {
                viewModel.onPageFinished(false)
                advanceUntilIdle()
                classes = awaitItem().substringAfterLast(", '").substringBefore("');")
                cancelAndIgnoreRemainingEvents()
            }
        return classes.split(" ")
    }

    private fun TestScope.startStudying(): ReviewerViewModel {
        val viewModel = ReviewerViewModel(SavedStateHandle()).also { viewModelStore.put("reviewer", it) }
        viewModel.onPageFinished(false)
        advanceUntilIdle()
        viewModel.onStateMutationCallback()
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `answering waits for the custom scheduler of the first card`() =
        runTest {
            val card = addBasicNote().firstCard()
            col.config.cardStateCustomizer = "await new Promise(resolve => setTimeout(resolve, 5000));"
            val viewModel = ReviewerViewModel(SavedStateHandle()).also { viewModelStore.put("reviewer", it) }

            viewModel.onShowAnswer()
            viewModel.answerCard(Rating.GOOD)
            viewModel.onPageFinished(false)
            advanceUntilIdle()
            assertFalse(viewModel.showingAnswer.value)
            assertEquals(0, col.getCard(card.id).reps)

            viewModel.onStateMutationCallback()
            advanceUntilIdle()
            assertEquals(1, col.getCard(card.id).reps)
        }

    @Test
    fun `restored answer can be graded after process death`() =
        runTest {
            val card = addBasicNote().firstCard()
            val savedStateHandle = SavedStateHandle(mapOf("showingAnswer" to true))
            val viewModel = ReviewerViewModel(savedStateHandle).also { viewModelStore.put("reviewer", it) }

            viewModel.onPageFinished(true)
            advanceUntilIdle()
            assertTrue(viewModel.showingAnswer.value)

            viewModel.answerCard(Rating.GOOD)
            advanceUntilIdle()
            assertEquals(1, col.getCard(card.id).reps)
        }

    @Test
    fun `restored answer waits for the custom scheduler after process death`() =
        runTest {
            val card = addBasicNote().firstCard()
            col.config.cardStateCustomizer = "await new Promise(resolve => setTimeout(resolve, 5000));"
            val savedStateHandle = SavedStateHandle(mapOf("showingAnswer" to true))
            val viewModel = ReviewerViewModel(savedStateHandle).also { viewModelStore.put("reviewer", it) }

            viewModel.statesMutationEvalFlow.test {
                viewModel.onPageFinished(true)
                advanceUntilIdle()
                assertTrue(viewModel.showingAnswer.value)
                assertTrue(awaitItem().contains(col.config.cardStateCustomizer))

                viewModel.answerCard(Rating.GOOD)
                advanceUntilIdle()
                assertEquals(0, col.getCard(card.id).reps)
                cancelAndIgnoreRemainingEvents()

                viewModel.onStateMutationCallback()
                advanceUntilIdle()
                assertEquals(1, col.getCard(card.id).reps)
            }
        }

    @Test
    fun `restoring answer with retained view model does not rerun custom scheduler`() =
        runTest {
            val card = addBasicNote().firstCard()
            col.config.cardStateCustomizer = "await new Promise(resolve => setTimeout(resolve, 5000));"
            val viewModel = ReviewerViewModel(SavedStateHandle()).also { viewModelStore.put("reviewer", it) }

            viewModel.statesMutationEvalFlow.test {
                viewModel.onPageFinished(false)
                advanceUntilIdle()
                awaitItem()
                viewModel.onStateMutationCallback()
                viewModel.onShowAnswer()
                advanceUntilIdle()
                assertTrue(viewModel.showingAnswer.value)

                viewModel.onPageFinished(true)
                advanceUntilIdle()
                expectNoEvents()
                assertTrue(viewModel.showingAnswer.value)
                cancelAndIgnoreRemainingEvents()

                viewModel.answerCard(Rating.GOOD)
                advanceUntilIdle()
                assertEquals(1, col.getCard(card.id).reps)
            }
        }
}
