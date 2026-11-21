// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.core.net.toUri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ichi2.anki.browser.CardBrowserPaneFragment
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.anki.testutil.disableIntroductionSlide
import com.ichi2.anki.testutil.useResumedActivity
import com.ichi2.anki.testutil.waitUntil
import com.ichi2.anki.testutil.withBackupPromptDisabled
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertSame

/**
 * Sending the `anki://x-callback-url/browser?search=` deep link opens Browse in the app host.
 */
@RunWith(AndroidJUnit4::class)
class CardBrowserDeepLinkTest : InstrumentedTest() {
    @get:Rule
    val storagePermission = GrantStoragePermission.instance

    @Before
    fun setUp() {
        disableIntroductionSlide()
    }

    @Test
    fun deepLinkOpensCardBrowserWithSearch() =
        testContext.withBackupPromptDisabled {
            val query = "deepLinkDog${System.nanoTime()}"
            val dog = addNoteUsingBasicNoteType(query, "barks")
            addNoteUsingBasicNoteType("cat", "meows")
            val dogCard = dog.firstCard(col)
            val browserDeck = AnkiDroidApp.instance.sharedPrefsLastDeckIdRepository
            val previousDeck = browserDeck.lastDeckId
            browserDeck.lastDeckId = dogCard.did

            try {
                launchDeepLink(query)
                useResumedActivity<DeckPicker> { host ->
                    val browser = awaitBrowser(host)
                    waitUntil(message = { "Browse did not load the matching card for the deep-link search" }) {
                        onMain {
                            val model = browser.browserViewModel
                            val expectedId = if (model.cardsOrNotes == CardsOrNotes.NOTES) dog.id else dogCard.id
                            model.searchTerms == query && model.cards.singleOrNull()?.cardOrNoteId == expectedId
                        }
                    }
                }
            } finally {
                browserDeck.lastDeckId = previousDeck
            }
        }

    @Test
    fun backFromBrowserReturnsToDecksInTheSameActivity() =
        testContext.withBackupPromptDisabled {
            addNoteUsingBasicNoteType("dog", "barks")
            testContext.startActivity(DeckPicker.getIntent(testContext).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            useResumedActivity<DeckPicker> { host ->
                assertDecksVisible()
                launchDeepLink("dog")
                val browser = awaitBrowser(host)
                waitUntil(message = { "Back did not close the deep-linked Browse screen" }) {
                    onMain {
                        if (host.currentScreenFragment === browser) {
                            host.onBackPressedDispatcher.onBackPressed()
                        }
                        host.currentScreenFragment == null
                    }
                }
                assertDecksVisible()
                assertSame(host, TestUtils.activityInstance)
            }
        }

    private fun assertDecksVisible() {
        onView(withId(R.id.deck_picker_content)).check(matches(isDisplayed()))
    }

    private fun awaitBrowser(host: DeckPicker): CardBrowserPaneFragment {
        var browser: CardBrowserPaneFragment? = null
        waitUntil(message = { "The Browse destination did not open in DeckPicker" }) {
            onMain {
                browser = host.currentScreenFragment as? CardBrowserPaneFragment
                browser?.isResumed == true
            }
        }
        return checkNotNull(browser)
    }

    private fun launchDeepLink(query: String) {
        // Let Android resolve the app's implicit VIEW entry point.
        val intent =
            Intent(Intent.ACTION_VIEW, "anki://x-callback-url/browser?search=$query".toUri())
                .setPackage(testContext.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        testContext.startActivity(intent)
    }

    private fun onMain(condition: () -> Boolean): Boolean {
        var result = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = condition() }
        return result
    }
}
