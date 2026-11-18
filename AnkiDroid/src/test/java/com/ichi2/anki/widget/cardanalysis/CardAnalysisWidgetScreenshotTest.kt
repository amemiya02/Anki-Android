// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.widget.cardanalysis

import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.libanki.CardType
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.libanki.QueueType
import com.ichi2.widget.AppWidgetId
import com.ichi2.widget.cardanalysis.CardAnalysisWidget
import com.ichi2.widget.cardanalysis.CardAnalysisWidgetPreferences
import com.ichi2.widget.renderWidget
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.robolectric.RuntimeEnvironment

/**
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.widget.cardanalysis.CardAnalysisWidgetScreenshotTest"`
 *
 * Use the suite's default SDK until the backend supports multiple Robolectric classloaders.
 */
class CardAnalysisWidgetScreenshotTest : ScreenshotTest() {
    @Test
    fun size3x2() =
        runTest {
            captureDeck("size_3x2", widthDp = 262, heightDp = 220)
        }

    @Test
    fun size4x2() =
        runTest {
            captureDeck("size_4x2", widthDp = 349, heightDp = 220)
        }

    @Test
    fun size2x2() =
        runTest {
            captureDeck("size_2x2", widthDp = 175, heightDp = 220)
        }

    @Test
    fun size2x2TwoWords() =
        runTest {
            captureDeck("size_2x2_two_words", widthDp = 175, heightDp = 220, deckName = "Spanish Verbs")
        }

    @Test
    fun size2x1() =
        runTest {
            captureDeck("size_2x1", widthDp = 175, heightDp = 102)
        }

    @Test
    fun minimumSize() =
        runTest {
            captureDeck("minimum_size", widthDp = 84, heightDp = 56)
        }

    @Test
    fun size3x2LargeFont() =
        runTest {
            captureDeck("size_3x2_large_font", widthDp = 262, heightDp = 220, fontScale = 2f)
        }

    @Test
    fun size2x1LargeFont() =
        runTest {
            captureDeck("size_2x1_large_font", widthDp = 175, heightDp = 102, fontScale = 2f)
        }

    @Test
    fun missingDeck() =
        runTest {
            captureWidget("missing_deck", deckId = null, widthDp = 262, heightDp = 220)
        }

    @Test
    fun emptyCollection() =
        runTest {
            captureWidget("empty_collection", deckId = Consts.DEFAULT_DECK_ID, widthDp = 262, heightDp = 220)
        }

    private suspend fun TestScope.captureDeck(
        name: String,
        widthDp: Int,
        heightDp: Int,
        fontScale: Float = 1f,
        deckName: String = "Pharmacology",
    ) {
        val deckId =
            addDeck(deckName)
                .withNotes(count = 12)
                .withNotes(count = 3, queueType = QueueType.Lrn)
        addNotes(25).forEach { note ->
            note.firstCard().update {
                did = deckId
                type = CardType.Rev
                queue = QueueType.Rev
                due = 0
            }
        }
        captureWidget(name, deckId, widthDp, heightDp, fontScale)
    }

    private suspend fun TestScope.captureWidget(
        name: String,
        deckId: DeckId?,
        widthDp: Int,
        heightDp: Int,
        fontScale: Float = 1f,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        CardAnalysisWidgetPreferences(targetContext).saveSelectedDeck(WIDGET_ID, deckId)
        captureView(name, targetContext.renderWidget(CardAnalysisWidget(), width = widthDp, height = heightDp, id = WIDGET_ID))
    }

    companion object {
        private val WIDGET_ID = AppWidgetId(1)
    }
}
