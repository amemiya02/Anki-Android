// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.content.Intent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.Flag
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.browser.search.SavedSearch
import com.ichi2.anki.browser.search.SavedSearches
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.common.crashreporting.CrashReporter
import com.ichi2.anki.filtered.FilteredDeckOptionsFragment
import com.ichi2.anki.getBrowserWithNotes
import com.ichi2.anki.libanki.DeckNameId
import com.ichi2.anki.throwOnShowError
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

/** Test of [CardBrowserFragment]. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CardBrowserFragmentTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `search results completed while stopped are displayed on restart - Issue 17759`() {
        val removed = addBasicNote("dog", "barks")
        addBasicNote("cat", "meows")
        ensureCollectionLoadIsSynchronous()
        ActivityScenario.launch(CardBrowser::class.java).use { scenario ->
            composeRule.waitUntilAtLeastOneExists(hasText("dog"), 10_000)
            lateinit var browser: CardBrowser
            scenario.onActivity { browser = it }

            scenario.moveToState(Lifecycle.State.CREATED)
            col.removeNotes(noteIds = listOf(removed.id))
            val previous = browser.viewModel.searchJob
            browser.viewModel.launchSearchForCards()
            advanceRobolectricLooperUntil {
                browser.viewModel.searchJob != previous && browser.viewModel.searchJob?.isCompleted == true
            }

            val completed = browser.viewModel.searchJob
            scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.onNodeWithText("cat").assertIsDisplayed()
            composeRule.onNodeWithText("dog").assertDoesNotExist()
            assertThat("resuming reuses the search", browser.viewModel.searchJob, sameInstance(completed))
        }
    }

    @Test
    fun `hardware Enter submits the search`() {
        addBasicNote("Atlas")
        addBasicNote("Cedar")
        getBrowserWithNotes(noteCount = 0)

        openSearch()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Atl")
        composeRule.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        composeRule.waitUntilDoesNotExist(hasSetTextAction(), 10_000)
        composeRule.onNodeWithText("Atlas", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `an equivalent saved search keeps the rows and restores its chip`() {
        assertThat(col.setUserFlagForCards(listOf(addBasicNote("Blue flower").firstCard().id), Flag.BLUE.code).count, equalTo(1))
        addBasicNote("White flower")
        runBlocking { SavedSearches.add(SavedSearch("Blue cards", "flag:4")) }
        getBrowserWithNotes(noteCount = 0)

        openSearch()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("flag:4")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitUntilDoesNotExist(hasText("White flower"), 10_000)
        openSearch()
        composeRule.onNodeWithText("Blue cards").performClick()

        composeRule.waitUntilDoesNotExist(hasSetTextAction(), 10_000)
        composeRule.onNodeWithText("Blue flower").assertIsDisplayed()
        composeRule.onNodeWithText("Blue").assertIsDisplayed()
    }

    @Test
    fun `search offers the manual link without advanced search tools`() {
        getBrowserWithNotes(noteCount = 0)
        openSearch()

        composeRule.onNodeWithText("How to write a search").assertIsDisplayed()
        composeRule.onNodeWithText("Advanced search").assertDoesNotExist()
    }

    @Test
    fun `the sort sheet restores the saved column and lets the user clear it`() {
        col.config.set("sortType", "noteCrt")
        col.config.set("sortBackwards", true)
        getBrowserWithNotes(noteCount = 1)

        composeRule.onNodeWithText("Created").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort").performClick()
        composeRule.onNodeWithText("No sorting").performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitUntilDoesNotExist(hasText("Created"), 10_000)
        composeRule.onNodeWithText("Sort").assertIsDisplayed()
    }

    private fun openSearch() {
        composeRule.onNodeWithContentDescription("Search cards").performClick()
        composeRule.waitUntilAtLeastOneExists(hasSetTextAction(), 10_000)
    }

    @Test
    fun `filtered deck search is the browser search`() =
        withCardBrowserFragment {
            // 'OR' binds looser than the selected deck, and '_' is a wildcard which also matches 'Chapter11'
            val first = addBasicNote("first").apply { moveToDeck("Chapter_1") }
            val second = addBasicNote("second").apply { moveToDeck("Chapter_1") }
            addBasicNote("second").apply { moveToDeck("Chapter11") }
            addBasicNote("first").apply { moveToDeck("Other") }
            val deckId = requireNotNull(col.decks.idForName("Chapter_1"))
            activityViewModel.searchRequestFlow.update { request ->
                request.copy(query = "first OR second").copyFilters { it.copy(decks = listOf(DeckNameId("Chapter_1", deckId))) }
            }

            val search = filteredDeckSearch()

            assertThat(col.findCards(search), containsInAnyOrder(first.firstCard().id, second.firstCard().id))
        }

    @Test
    fun `invalid browser query is not reported as a crash when creating a filtered deck`() {
        val originalReporter = CrashReportService.getReporter()
        val originalThrowOnShowError = throwOnShowError
        val reporter = mock<CrashReporter>()
        CrashReportService.setReporter(reporter)
        try {
            withCardBrowserFragment {
                throwOnShowError = false
                activityViewModel.setQuery("and").join()
                advanceRobolectricLooper()

                showFilteredDeckScreen()
                advanceRobolectricLooper()
                // The error handler sends its crash report when the dialog is dismissed.
                requireNotNull(ShadowDialog.getLatestDialog()) { "the error is shown" }.dismiss()
                advanceRobolectricLooper()

                verify(reporter, never()).sendExceptionReport(any<Throwable>(), anyOrNull(), anyOrNull(), any())
            }
        } finally {
            CrashReportService.setReporter(originalReporter)
            throwOnShowError = originalThrowOnShowError
        }
    }

    @Test
    fun `shortcut labels use Sentence case`() =
        withCardBrowserFragment {
            val offenders = shortcuts.shortcuts.map { it.label }.filterNot { it.isSentenceCase }
            assertThat("shortcut labels should be Sentence case, not Title Case", offenders, empty())
        }

    /** Opens the filtered deck screen, returning the search it was opened with */
    private fun CardBrowserFragment.filteredDeckSearch(): String {
        showFilteredDeckScreen()

        val activity = shadowOf(requireActivity())
        advanceRobolectricLooperUntil { activity.peekNextStartedActivity() != null }
        val intent = requireNotNull(activity.nextStartedActivity)
        assertThat(
            intent.getStringExtra(SingleFragmentActivity.EXTRA_FRAGMENT_NAME),
            equalTo(FilteredDeckOptionsFragment::class.java.name),
        )
        val arguments = requireNotNull(intent.getBundleExtra(SingleFragmentActivity.EXTRA_FRAGMENT_ARGS))
        return requireNotNull(arguments.getString(FilteredDeckOptionsFragment.ARG_SEARCH))
    }
}

private val capitalizedFollowingWord = Regex("""\s\p{Lu}\p{Ll}""")

// Material Design sentence case: the first word is capitalized; later words are not
private val String.isSentenceCase: Boolean
    get() = first().isUpperCase() && !capitalizedFollowingWord.containsMatchIn(this)

context(test: RobolectricTest)
fun withCardBrowserFragment(block: suspend CardBrowserFragment.() -> Unit) =
    test.runTest {
        val intent = Intent(test.targetContext, CardBrowser::class.java)
        ActivityScenario.launch<CardBrowser>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            lateinit var browser: CardBrowser
            scenario.onActivity { browser = it }
            assertThat("Activity is not finishing", !browser.isFinishing)
            block(browser.cardBrowserFragment)
        }
    }
